package com.ffsupver.createheat.network;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.block.thermalBlock.BaseThermalBlockBehaviour;
import com.ffsupver.createheat.block.thermalBlock.HeatStorage;
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

public class HeatNetwork extends TickingBlockNetwork{


    private int hasBlockTicking = 0; // if there is any block ticking, this will be larger than 0

    // heat storage
    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private final HeatStorage heatStorage;
    private final Set<UUID> connectedHeatStorageNetworks = new HashSet<>();
    private final Set<UUID> lastConnectedHeatStorageNetworks = new HashSet<>();
    private HeatUtil.HeatData heatGenDataLastTick = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatData heatDataLastTickRemain = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatData heatCostDataLastTick = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatIOData displayHeatData = HeatUtil.HeatIOData.NO_HEAT_IO;

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

        // process heat
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

        heatStorage.insert(heatGenDataLastTick.heat());
        heatStorage.extract(heatCostDataLastTick.heat(),false);

        displayHeatData = new HeatUtil.HeatIOData(heatGenDataLastTick,heatCostDataLastTick);
        heatDataLastTickRemain = calculateHeatCanProvideThisTick(heatGenDataLastTick);

        heatGenDataLastTick = HeatUtil.NO_HEAT_PROVIDE;
        heatCostDataLastTick = HeatUtil.NO_HEAT_PROVIDE;

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
            HeatUtil.HeatData heatProvideToHTP = HeatUtil.NO_HEAT_PROVIDE;
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

        System.out.println("tickingHeatNetwork "+level.dimension()+"   heat:"+heatStorage+"   lastHeat:"+ heatDataLastTickRemain +"   hTPs:"+transferProcesserMap+"   blocks:"+connectedBlocks.size()+" / "+connectedBlocks+" storage="+connectedHeatStorageNetworks);

        return shouldSave;
    }

    private HeatUtil.HeatData calculateHeatCanProvideThisTick(HeatUtil.HeatData heatGenDataLastTick) {
        HeatUtil.HeatData heatProvideFromStorage = new HeatUtil.HeatData(heatStorage.getAmount(),0);
        return heatGenDataLastTick.merge(heatProvideFromStorage);
    }


    public void onBlockTick(BlockPos pos, HeatUtil.HeatData heatGenData, HeatUtil.HeatData heatCostData,Set<UUID> neighborHeatStorageNetworkIds){
        hasBlockTicking += 1;

        this.heatGenDataLastTick = this.heatGenDataLastTick.merge(heatGenData);
        this.heatCostDataLastTick = this.heatCostDataLastTick.merge(heatCostData);
        this.heatDataLastTickRemain = this.heatDataLastTickRemain.sub(heatCostData);

        lastConnectedHeatStorageNetworks.addAll(neighborHeatStorageNetworkIds);
    }

    @Override
    protected void onBlockSetChanged() {
        calculateHeatCapacity();
    }

    @Override
    protected void onUnloadedBlockLoaded(ServerLevel level, BlockPos unloadedPos) {
        getThermalBlockBehaviour(level, unloadedPos).ifPresent(
                baseThermalBlockBehaviour->baseThermalBlockBehaviour.setHeatNetworkId(networkID)
        );
    }

    @Override
    protected void onBlockMergeToNetwork(ServerLevel serverLevel, BlockPos pos, TickingBlockNetwork finalNetwork) {
        getThermalBlockBehaviour(serverLevel, pos).ifPresent(
                baseThermalBlockBehaviour ->
                        baseThermalBlockBehaviour.setHeatNetworkId(finalNetwork.getNetworkID())
        );
    }

    private void calculateHeatCapacity(){
        heatStorage.setCapacity(connectedBlocks.size() * MAX_HEAT.get());
    }

    public HeatUtil.HeatData getHeatDataLastTickRemain() {
        return heatDataLastTickRemain;
    }
    public HeatTransferProcesser getTransferProcesser(BlockPos pos) {
        return transferProcesserMap.get(pos);
    }

    public void addTransferProcesser(BlockPos pos,HeatTransferProcesser transferProcesser) {
        transferProcesserMap.putIfAbsent(pos,transferProcesser);
        needToSave = true;
    }

    public HeatStorage.Snapshot getDisplayHeatStorage() {
        return heatStorage.snapshot();
    }
    public HeatUtil.HeatIOData getDisplayHeatData() {
        return displayHeatData;
    }


    private void setTransferProcesserMap(Map<BlockPos, HeatTransferProcesser> transferProcesserMap) {
        this.transferProcesserMap.clear();
        this.transferProcesserMap.putAll(transferProcesserMap);
    }
    private void setHeatDataLastTickRemain(HeatUtil.HeatData heatDataLastTickRemain) {
        this.heatDataLastTickRemain = heatDataLastTickRemain;
    }
    private void setConnectedHeatStorageNetworks(Collection<UUID> connectedHeatStorageNetworks){
        this.connectedHeatStorageNetworks.clear();
        this.connectedHeatStorageNetworks.addAll(connectedHeatStorageNetworks);
    }

    private Optional<BaseThermalBlockBehaviour> getThermalBlockBehaviour(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(BlockEntityBehaviour.get(level, pos, BaseThermalBlockBehaviour.TYPE));
    }

    public static HeatNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        System.out.println("loadingHeatNetworkNbt:"+nbt);
        Map<BlockPos, HeatTransferProcesser> transferProcesserMap = new HashMap<>(NbtUtil.readMapFromNbtList(
                nbt.getList("transfer_processers", Tag.TAG_COMPOUND),
                NbtUtil::blockPosFromNbt,
                CHHeatTransferProcessers::fromNbt
        ));

        HeatNetwork heatNetwork = fromNbt(nbt, HeatNetwork::new);
        heatNetwork.heatStorage.fromNbt(nbt.getCompound("heat_storage"));
        heatNetwork.setConnectedHeatStorageNetworks(NbtUtil.readListFromNbt(nbt.getList("connected_heat_storage",Tag.TAG_COMPOUND),uuidNbt-> ((CompoundTag)uuidNbt).getUUID("uuid")));
        heatNetwork.setTransferProcesserMap(transferProcesserMap);
        heatNetwork.setHeatDataLastTickRemain(HeatUtil.HeatData.fromNbt(nbt.getCompound("heat_data_last_tick_remain")));
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
        nbt.put("heat_data_last_tick_remain",heatDataLastTickRemain.toNbt());
        System.out.println("savingHeatNetworkNbt:"+nbt);
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
}
