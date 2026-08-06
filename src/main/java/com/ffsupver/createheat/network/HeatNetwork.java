package com.ffsupver.createheat.network;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.block.thermalBlock.BaseThermalBlockBehaviour;
import com.ffsupver.createheat.block.thermalBlock.HeatStorage;
import com.ffsupver.createheat.block.tightCompressStone.HeatStorageBehaviour;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.HeatUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.ffsupver.createheat.util.HeatUtil.NO_HEAT_PROVIDE;

public class HeatNetwork extends TickingBlockNetwork{


    private int hasBlockTicking = 0; // if there is any block ticking, this will be larger than 0

    // heat storage
    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private final HeatStorage heatStorage;
    private final Set<UUID> connectedHeatStorageNetworks = new HashSet<>();
    private final Set<UUID> lastConnectedHeatStorageNetworks = new HashSet<>();
    private HeatUtil.HeatData heatGenDataLastTick = NO_HEAT_PROVIDE;
    private HeatInteractionData heatInteractionData = new HeatInteractionData(Set.of(), NO_HEAT_PROVIDE);
    private HeatUtil.HeatData heatCostDataLastTick = NO_HEAT_PROVIDE;
    private HeatUtil.HeatIOData displayHeatData = HeatUtil.HeatIOData.NO_HEAT_IO;
    private final HeatStorageBehaviour.SuperHeatStorage displayHeatStorage = new HeatStorageBehaviour.SuperHeatStorage(0);

    // heat transfer processor
    private final Map<BlockPos, HeatTransferProcesser> transferProcesserMap = new HashMap<>();

    public HeatNetwork(UUID networkID,Set<BlockPos> connectedBlocks) {
        super(networkID,connectedBlocks);
        this.heatStorage = new HeatStorage(connectedBlocks.size() * MAX_HEAT.get());
    }

    @Override
    public Set<? extends TickingBlockNetwork> onNetworkSplit(Set<BlockPos> disconnectedBlocks, ServerLevel level) {
        return HeatService.onHeatNetworkSplit(disconnectedBlocks,level);
    }

    public boolean tick(ServerLevel level){
        boolean shouldSave = super.tick(level);

        // check if network need to tick logic
        if (hasBlockTicking == 0){ // the first and second tick, BlockEntity will not tick even the block is loaded
            return shouldSave;
        }
        hasBlockTicking = 0;

        // check heat storage connection
        Set<UUID> heatStorageNetworkNeedToCheck = new HashSet<>();
        Set<UUID> heatStorageNetworkNeedToAdd = new HashSet<>();
        for (UUID heatStorageNetworkId : lastConnectedHeatStorageNetworks){
            if (!connectedHeatStorageNetworks.contains(heatStorageNetworkId)){
                heatStorageNetworkNeedToAdd.add(heatStorageNetworkId);
            }
        }
        for (UUID heatStorageNetworkId : connectedHeatStorageNetworks){
            if (!lastConnectedHeatStorageNetworks.contains(heatStorageNetworkId)){
                heatStorageNetworkNeedToCheck.add(heatStorageNetworkId);
            }
        }

        lastConnectedHeatStorageNetworks.clear();
        connectedHeatStorageNetworks.addAll(heatStorageNetworkNeedToAdd);
        boolean removedHeatStorageNetwork = false;
        if(NetworkService.isLoad(NetworkService.Services.HEAT_STORAGE)){
            for (UUID heatStorageNetworkId : heatStorageNetworkNeedToCheck) {
                TickingBlockNetwork heatStorageNetwork = NetworkService.getNetwork(level, heatStorageNetworkId, NetworkService.Services.HEAT_STORAGE);
                boolean isConnected = heatStorageNetwork != null && BlockUtil.isConnect(connectedBlocks, heatStorageNetwork.connectedBlocks);
                if (!isConnected) {
                    connectedHeatStorageNetworks.remove(heatStorageNetworkId);
                    removedHeatStorageNetwork = true;
                }
            }
        }
        shouldSave = shouldSave || removedHeatStorageNetwork || !heatStorageNetworkNeedToAdd.isEmpty();

        HeatStorage.Snapshot lastHeatStorage = heatStorage.snapshot();

        insertHeat(level,heatGenDataLastTick);
        heatStorage.extract(heatInteractionData.getCostHeat().heat(),false);

        Set<HeatStorageBehaviour.SuperHeatStorage> heatStorageNetworkStorages = new HashSet<>();
        for (UUID heatStorageNetworkId : connectedHeatStorageNetworks) {
            if (NetworkService.getNetwork(level, heatStorageNetworkId, NetworkService.Services.HEAT_STORAGE) instanceof HeatStorageNetwork heatStorageNetwork){
                heatStorageNetworkStorages.add(heatStorageNetwork.getHeatStorage());
            }
        }

        displayHeatData = new HeatUtil.HeatIOData(heatGenDataLastTick,heatCostDataLastTick);
        displayHeatStorage.clear();
        displayHeatStorage.merge(heatStorage);
        for (HeatStorage connectHeatStorage : heatStorageNetworkStorages){
            displayHeatStorage.merge(connectHeatStorage);
        }

        calculateHeatCanProvideThisTick(heatGenDataLastTick,heatStorageNetworkStorages);

        heatGenDataLastTick = NO_HEAT_PROVIDE;
        heatCostDataLastTick = NO_HEAT_PROVIDE;

        shouldSave = shouldSave || !lastHeatStorage.equals(heatStorage.snapshot());

        // process heat transfer processor
        int tickSkip = 1; // no cooldown yet
        Set<BlockPos> heatTransferProcesserToRemovePosSet = new HashSet<>();
        for (Map.Entry<BlockPos,HeatTransferProcesser> entry : transferProcesserMap.entrySet()){
            BlockPos hTPPos = entry.getKey();
            if (!level.isLoaded(hTPPos)){
                break;
            }

            HeatTransferProcesser heatTransferProcesser = entry.getValue();

            // get heat provide to hTP, and check connection with hTP
            List<HeatUtil.HeatData> heatSetProvideToHTP = new ArrayList<>();
            AtomicBoolean isConnected = new AtomicBoolean(false);
            BlockUtil.AllDirectionOf(hTPPos,thermalBlockPos->{
                Optional<BaseThermalBlockBehaviour> thermalBlockBehaviourOp = getThermalBlockBehaviour(level,thermalBlockPos);
                if (thermalBlockBehaviourOp.isPresent()){
                    BaseThermalBlockBehaviour thermalBlockBehaviour = thermalBlockBehaviourOp.get();
                    if (thermalBlockBehaviour.getHeatNetworkId().equals(networkID)){
                        heatSetProvideToHTP.add(thermalBlockBehaviour.getCurrentHeatCostData());
                        isConnected.set(true);
                    }
                }
            });

            // remove hTP if not connected without ticking it
            if (!isConnected.get()){
                heatTransferProcesserToRemovePosSet.add(hTPPos);
                break;
            }

            // tick hTP
            HeatUtil.HeatData heatProvideToHTP = NO_HEAT_PROVIDE;
            for (HeatUtil.HeatData heatData : heatSetProvideToHTP){
                heatProvideToHTP = heatProvideToHTP.merge(heatData);
            }
            if (heatTransferProcesser.needHeat(level,hTPPos,null,heatProvideToHTP.heat(),tickSkip,heatProvideToHTP.superHeatCount())){
                heatTransferProcesser.acceptHeat(level,hTPPos,new HeatTransferProcesser.HeatAcceptData(heatProvideToHTP.heat(),tickSkip,heatProvideToHTP.superHeatCount()));
            }else {
                heatTransferProcesserToRemovePosSet.add(hTPPos);
            }
        }
        heatTransferProcesserToRemovePosSet.stream().map(pos -> Map.entry(pos,transferProcesserMap.get(pos))).toList().forEach(transferProcesserMap.entrySet()::remove);
        shouldSave = shouldSave || !heatTransferProcesserToRemovePosSet.isEmpty();


        return shouldSave;
    }

    private void insertHeat(ServerLevel level,HeatUtil.HeatData heatGenDataLastTick){
        int leftHeat = heatStorage.insert(heatGenDataLastTick.heat());
        if (leftHeat > 0){
            HeatUtil.HeatData toInsert = new HeatUtil.HeatData(leftHeat,heatGenDataLastTick.superHeatCount());
            for (UUID heatStorageNetworkId : connectedHeatStorageNetworks) {
                if(NetworkService.getNetwork(level, heatStorageNetworkId, NetworkService.Services.HEAT_STORAGE) instanceof HeatStorageNetwork heatStorageNetwork){
                    toInsert = heatStorageNetwork.insert(level,toInsert);
                }
            }
        }
    }

    private void calculateHeatCanProvideThisTick(HeatUtil.HeatData heatGenDataLastTick,Set<HeatStorageBehaviour.SuperHeatStorage> heatStorageNetworkStorages) {
        HeatUtil.HeatData heatProvideFromStorage = new HeatUtil.HeatData(heatStorage.getAmount(),0);
        HeatUtil.HeatData heatProvideFromNetwork = heatGenDataLastTick.merge(heatProvideFromStorage);



        heatInteractionData.setHeatStorageNetworks(heatStorageNetworkStorages);
        heatInteractionData.setNetworkHeatRemain(heatProvideFromNetwork);
    }


    public void onBlockTick(BlockPos pos, HeatUtil.HeatData heatGenData, HeatUtil.HeatData heatCostData,Set<UUID> neighborHeatStorageNetworkIds){
        hasBlockTicking += 1;

        this.heatGenDataLastTick = this.heatGenDataLastTick.merge(heatGenData);
        this.heatCostDataLastTick = this.heatCostDataLastTick.merge(heatCostData);
        this.heatInteractionData.extractHeat(heatCostData,false);

        lastConnectedHeatStorageNetworks.addAll(neighborHeatStorageNetworkIds);
    }

    @Override
    protected void onBlockSetChanged() {
        calculateHeatCapacity();
    }

    @Override
    protected void onUnloadedBlockLoaded(ServerLevel level, BlockPos unloadedPos) {
        getNetworkBehaviour(level, unloadedPos).ifPresent(
                networkBehaviour->networkBehaviour.setNetworkId(networkID)
        );
    }

    @Override
    protected void onBlockMergeToNetwork(ServerLevel serverLevel, BlockPos pos, TickingBlockNetwork finalNetwork) {
        getNetworkBehaviour(serverLevel, pos).ifPresent(
                networkBehaviour ->
                        networkBehaviour.setNetworkId(finalNetwork.getNetworkID())
        );
    }

    private void calculateHeatCapacity(){
        heatStorage.setCapacity(connectedBlocks.size() * MAX_HEAT.get());
    }

    public HeatInteractionData getHeatInteractionData() {
        return heatInteractionData;
    }


    public HeatTransferProcesser getTransferProcesser(BlockPos pos) {
        return transferProcesserMap.get(pos);
    }

    public void addTransferProcesser(BlockPos pos,HeatTransferProcesser transferProcesser) {
        transferProcesserMap.putIfAbsent(pos,transferProcesser);
        needToSave = true;
    }

    public HeatStorage.Snapshot getDisplayHeatStorage() {
        HeatStorage dis = new HeatStorage(displayHeatStorage.getCapacity() + displayHeatStorage.getSuperCapacity());
        dis.setAmount(displayHeatStorage.getAmount() + displayHeatStorage.getSuperAmount());
        return dis.snapshot();
    }
    public HeatUtil.HeatIOData getDisplayHeatData() {
        return displayHeatData;
    }


    private void setTransferProcesserMap(Map<BlockPos, HeatTransferProcesser> transferProcesserMap) {
        this.transferProcesserMap.clear();
        this.transferProcesserMap.putAll(transferProcesserMap);
    }

    private void setConnectedHeatStorageNetworks(Collection<UUID> connectedHeatStorageNetworks){
        this.connectedHeatStorageNetworks.clear();
        this.connectedHeatStorageNetworks.addAll(connectedHeatStorageNetworks);
    }

    private Optional<BaseThermalBlockBehaviour> getThermalBlockBehaviour(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(BlockEntityBehaviour.get(level, pos, BaseThermalBlockBehaviour.TYPE));
    }

    private Optional<NetworkBehaviour> getNetworkBehaviour(ServerLevel level, BlockPos pos) {
        NetworkBehaviour networkBehaviour = BlockEntityBehaviour.get(level, pos, NetworkBehaviour.TYPE);
        if (networkBehaviour != null && networkBehaviour.checkNetworkType(NetworkService.Services.HEAT)){
            return Optional.of(networkBehaviour);
        }
        return Optional.empty();
    }

    public static HeatNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        Map<BlockPos, HeatTransferProcesser> transferProcesserMap = new HashMap<>(NbtUtil.readMapFromNbtList(
                nbt.getList("transfer_processers", Tag.TAG_COMPOUND),
                NbtUtil::blockPosFromNbt,
                CHHeatTransferProcessers::fromNbt
        ));
        HeatInteractionData heatInteractionData = HeatInteractionData.fromNbt(nbt);

        HeatNetwork heatNetwork = fromNbt(nbt, HeatNetwork::new);
        heatNetwork.heatStorage.fromNbt(nbt.getCompound("heat_storage"));
        heatNetwork.setConnectedHeatStorageNetworks(NbtUtil.readListFromNbt(nbt.getList("connected_heat_storage",Tag.TAG_COMPOUND),uuidNbt-> ((CompoundTag)uuidNbt).getUUID("uuid")));
        heatNetwork.setTransferProcesserMap(transferProcesserMap);
//        heatNetwork.setHeatDataLastTickRemain(HeatUtil.HeatData.fromNbt(nbt.getCompound("heat_data_last_tick_remain")));
        heatNetwork.heatInteractionData = heatInteractionData;
        return heatNetwork;
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = super.toNbt();
        nbt.put("heat_storage",heatStorage.toNbt());
        nbt.put("connected_heat_storage",NbtUtil.writeToNbtList(connectedHeatStorageNetworks,uuid->{
            CompoundTag uuidNbt = new CompoundTag();
            uuidNbt.putUUID("uuid",uuid);
            return uuidNbt;
        }));
        nbt.put("transfer_processers",NbtUtil.writeMapToNbtList(
                transferProcesserMap,
                NbtUtil::blockPosToNbt,
                CHHeatTransferProcessers::toNbt
        ));
        heatInteractionData.toNbt(nbt);
        return nbt;
    }

    /** print all info to console for debug
     */
    public void printInfo(){
        CreateHeat.LOGGER.info(
                "[HeatNetwork Info]:\n==============\nid={}  blockCount={}\nheatStorage={}   heatData={}\ntransferProcessers:{}\nblocks:{}\n==============",
                networkID,connectedBlocks.size(), heatStorage,displayHeatData,transferProcesserMap,connectedBlocks
        );
    }

    @Override
    public String toString() {
        return "HeatNetwork{" +
                "networkID=" + networkID +
                "block counts: "+connectedBlocks.size()+
                ", connectedBlocks=" + connectedBlocks +
                ", connectedHeatStorage=" + connectedHeatStorageNetworks +
                '}';
    }

    public static class HeatInteractionData{
        public Set<HeatStorageBehaviour.SuperHeatStorage> heatStorageNetworks;
        public HeatUtil.HeatData networkHeatRemain;
        public HeatUtil.HeatData lastNetworkHeatRemain;

        public HeatInteractionData(Set<HeatStorageBehaviour.SuperHeatStorage> heatStorageNetworks, HeatUtil.HeatData networkHeatRemain) {
            this.heatStorageNetworks = heatStorageNetworks;
            this.networkHeatRemain = networkHeatRemain;
            this.lastNetworkHeatRemain = networkHeatRemain;
        }

        /**
         * extract heat from heat storage networks
         * @param heatData heat to extract
         * @param simulate whether to simulate the extraction
         * @return the heat actually extracted
         */
        public HeatUtil.HeatData extractHeat(HeatUtil.HeatData heatData,boolean simulate){
            if (heatData.heat() <= 0){
                return NO_HEAT_PROVIDE;
            }

            HeatUtil.HeatData leftHeatData = heatData;
            for (HeatStorageBehaviour.SuperHeatStorage heatStorage : heatStorageNetworks){
                HeatUtil.HeatData extractedHeatData = heatStorage.extract(leftHeatData, simulate);
                leftHeatData = leftHeatData.sub(extractedHeatData);

                if (leftHeatData.heat() <= 0 && leftHeatData.superHeatCount() <= 0){
                    return heatData.sub(leftHeatData);
                }
            }

            if (networkHeatRemain.heat() >= leftHeatData.heat() && networkHeatRemain.superHeatCount() >= leftHeatData.superHeatCount()){
                if (!simulate){
                    networkHeatRemain = networkHeatRemain.sub(leftHeatData);
                }
                return heatData;
            }else if (networkHeatRemain.heat() >= leftHeatData.heat()) {
                int leftSuperHeatCount = leftHeatData.superHeatCount() - networkHeatRemain.superHeatCount();
                if (!simulate) {
                    networkHeatRemain = networkHeatRemain.sub(new HeatUtil.HeatData(leftHeatData.heat(), networkHeatRemain.superHeatCount()));
                }
                return new HeatUtil.HeatData(heatData.heat(), heatData.superHeatCount() - leftSuperHeatCount);
            }else if (networkHeatRemain.superHeatCount() >= leftHeatData.superHeatCount()){
                int leftHeatCount = leftHeatData.heat() - networkHeatRemain.heat();
                if (!simulate) {
                    networkHeatRemain = networkHeatRemain.sub(new HeatUtil.HeatData(networkHeatRemain.heat(), leftHeatData.superHeatCount()));
                }
                return new HeatUtil.HeatData(heatData.heat() - leftHeatCount, heatData.superHeatCount());
            }else {
                HeatUtil.HeatData notExtractedHeat = leftHeatData.sub(networkHeatRemain);
                if (!simulate) {
                    networkHeatRemain = new HeatUtil.HeatData(0, 0);
                }
                return heatData.sub(notExtractedHeat);
            }
        }

        public HeatUtil.HeatData getAllHeat(){
            HeatUtil.HeatData heatData = networkHeatRemain;
            for (HeatStorageBehaviour.SuperHeatStorage heatStorage : heatStorageNetworks){
                HeatUtil.HeatData heatStorageHeatData = heatStorage.getAllHeat();
                heatData = heatData.merge(heatStorageHeatData);
            }
            return heatData;
        }

        public HeatUtil.HeatData getCostHeat(){
            return lastNetworkHeatRemain.sub(networkHeatRemain);
        }

        public CompoundTag toNbt(CompoundTag nbt){
            nbt.put("heat_data_last_tick_remain",networkHeatRemain.toNbt());
            return nbt;
        }

        public static HeatInteractionData fromNbt(CompoundTag nbt){
            HeatUtil.HeatData networkHeatRemain = HeatUtil.HeatData.fromNbt(nbt.getCompound("heat_data_last_tick_remain"));
            return new HeatInteractionData(Set.of(),networkHeatRemain);
        }

        public void setNetworkHeatRemain(HeatUtil.HeatData networkHeatRemain) {
            this.networkHeatRemain = networkHeatRemain;
            this.lastNetworkHeatRemain = networkHeatRemain;
        }

        public void setHeatStorageNetworks(Set<HeatStorageBehaviour.SuperHeatStorage> heatStorageNetworks) {
            this.heatStorageNetworks = heatStorageNetworks;
        }

        @Override
        public String toString() {
            return "HeatInteractionData{" +
                    "heatStorageNetworks=" + heatStorageNetworks +
                    ", networkHeatRemain=" + networkHeatRemain +
                    '}';
        }
    }
}
