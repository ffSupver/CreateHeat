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

public class HeatNetwork {
    private final UUID networkID;
    private final Set<BlockPos> connectedBlocks;
    private boolean needToSave; // notify the network to save outside of ticking
    private boolean shouldRemove; // marked network as removed from service
    public boolean shouldCheckConnection; // notify the network to check connection next tick
    private final Set<BlockPos> unloadedBlocks = new HashSet<>(); // blocks never loaded after added to network, unloadedBlocks will be checked each tick and removed from this set once they are loaded

    private int hasBlockTicking = 0; // if there is any block ticking, this will be larger than 0

    // heat storage
    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private final HeatStorage heatStorage;
    private HeatUtil.HeatData heatGenDataLastTick = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatData heatDataLastTickRemain = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatData heatCostDataLastTick = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatIOData displayHeatData = HeatUtil.HeatIOData.NO_HEAT_IO;

    // heat transfer processor
    private final Map<BlockPos, HeatTransferProcesser> transferProcesserMap = new HashMap<>();

    public HeatNetwork(UUID networkID,Set<BlockPos> connectedBlocks) {
        this.networkID = networkID;
        this.connectedBlocks = connectedBlocks;
        this.heatStorage = new HeatStorage(connectedBlocks.size() * MAX_HEAT.get());
    }

    public boolean tick(ServerLevel level){
        // check if network is empty
        if (connectedBlocks.isEmpty()){
            shouldRemove = true;
        }

        // check if network need to tick logic
        if (hasBlockTicking == 0){ // the first and second tick, BlockEntity will not tick even the block is loaded
            return needToSave;
        }
        hasBlockTicking = 0;

        boolean shouldSave = false;
        HeatStorage.Snapshot lastHeatStorage = heatStorage.snapshot();

        // check unloaded blocks
        if (!unloadedBlocks.isEmpty()){
            Iterator<BlockPos> unloadedBlockPosIterator = unloadedBlocks.iterator();
            while (unloadedBlockPosIterator.hasNext()) {
                BlockPos unloadedPos = unloadedBlockPosIterator.next();
                if (level.isLoaded(unloadedPos)) {
                    getThermalBlockBehaviour(level, unloadedPos).ifPresent(
                            baseThermalBlockBehaviour->baseThermalBlockBehaviour.setHeatNetworkId(networkID)
                    );
                    unloadedBlockPosIterator.remove();
                }
            }
        }

        // check block connection
        if (!connectedBlocks.isEmpty() && shouldCheckConnection){
            Set<BlockPos> checkedPosSet = new HashSet<>();
            BlockUtil.walkAllBlocks(connectedBlocks.iterator().next(),checkedPosSet, connectedBlocks::contains);
            if (connectedBlocks.size() > checkedPosSet.size()){
                Set<BlockPos> disconnectedBlocks = new HashSet<>(connectedBlocks);
                disconnectedBlocks.removeAll(checkedPosSet);
                Set<HeatNetwork> newNetworks = HeatService.onHeatNetworkSplit(disconnectedBlocks,level);

                // remove blocks that are already in new networks
                for (HeatNetwork newNetwork : newNetworks){
                    connectedBlocks.removeAll(newNetwork.connectedBlocks);
                }

                calculateHeatCapacity();
            }

            shouldCheckConnection = false;
        }

        // process heat
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

        System.out.println("ticking "+level.dimension()+"   heat:"+heatStorage+"   lastHeat:"+ heatDataLastTickRemain +"   hTPs:"+transferProcesserMap+"   blocks:"+connectedBlocks.size()+" / "+connectedBlocks);

        if (needToSave){
            shouldSave = true;
            needToSave = false;
        }
        return shouldSave;
    }

    private HeatUtil.HeatData calculateHeatCanProvideThisTick(HeatUtil.HeatData heatGenDataLastTick) {
        HeatUtil.HeatData heatProvideFromStorage = new HeatUtil.HeatData(heatStorage.getAmount(),0);
        return heatGenDataLastTick.merge(heatProvideFromStorage);
    }

    public boolean shouldRemove() {
        return shouldRemove;
    }

    public void onBlockTick(BlockPos pos, HeatUtil.HeatData heatGenData, HeatUtil.HeatData heatCostData){
        hasBlockTicking += 1;

        this.heatGenDataLastTick = this.heatGenDataLastTick.merge(heatGenData);
        this.heatCostDataLastTick = this.heatCostDataLastTick.merge(heatCostData);
        this.heatDataLastTickRemain = this.heatDataLastTickRemain.sub(heatCostData);
    }

    public void addBlock(BlockPos pos,boolean isLoaded) {
        connectedBlocks.add(pos);
        needToSave = true;
        if (!isLoaded) {
            unloadedBlocks.add(pos);
        }

        calculateHeatCapacity();
    }

    public void removeBlock(BlockPos pos){
        connectedBlocks.remove(pos);
        needToSave = true;
        shouldCheckConnection = true;

        calculateHeatCapacity();
    }


    public void mergeSelfTo(ServerLevel serverLevel, HeatNetwork finalNetwork) {
        for (BlockPos pos : connectedBlocks) {
            getThermalBlockBehaviour(serverLevel, pos).ifPresent(
                    baseThermalBlockBehaviour ->
                            baseThermalBlockBehaviour.setHeatNetworkId(finalNetwork.getNetworkID())
            );
            finalNetwork.addBlock(pos, serverLevel.isLoaded(pos));
        }
        this.shouldRemove = true;
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

    public UUID getNetworkID() {
        return networkID;
    }

    private void setUnloadedBlocks(Set<BlockPos> unloadedBlocks) {
        this.unloadedBlocks.clear();
        this.unloadedBlocks.addAll(unloadedBlocks);
    }
    private void setTransferProcesserMap(Map<BlockPos, HeatTransferProcesser> transferProcesserMap) {
        this.transferProcesserMap.clear();
        this.transferProcesserMap.putAll(transferProcesserMap);
    }
    private void setHeatDataLastTickRemain(HeatUtil.HeatData heatDataLastTickRemain) {
        this.heatDataLastTickRemain = heatDataLastTickRemain;
    }

    private Optional<BaseThermalBlockBehaviour> getThermalBlockBehaviour(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(BlockEntityBehaviour.get(level, pos, BaseThermalBlockBehaviour.TYPE));
    }

    public static HeatNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        System.out.println("loadingHeatNetworkNbt:"+nbt);
        UUID networkID = nbt.getUUID("id");
        Set<BlockPos> connectedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("connected_blocks", Tag.TAG_COMPOUND)));
        Set<BlockPos> unloadedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("unloaded_blocks", Tag.TAG_COMPOUND)));
        Map<BlockPos, HeatTransferProcesser> transferProcesserMap = new HashMap<>(NbtUtil.readMapFromNbtList(
                nbt.getList("transfer_processers", Tag.TAG_COMPOUND),
                NbtUtil::blockPosFromNbt,
                CHHeatTransferProcessers::fromNbt
        ));

        HeatNetwork heatNetwork = new HeatNetwork(networkID,connectedBlocks);
        heatNetwork.setUnloadedBlocks(unloadedBlocks);
        heatNetwork.heatStorage.fromNbt(nbt.getCompound("heat_storage"));
        heatNetwork.setTransferProcesserMap(transferProcesserMap);
        heatNetwork.setHeatDataLastTickRemain(HeatUtil.HeatData.fromNbt(nbt.getCompound("heat_data_last_tick_remain")));
        return heatNetwork;
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("id",networkID);
        nbt.put("connected_blocks",NbtUtil.writeBlockPosToNbtList(connectedBlocks));
        nbt.put("unloaded_blocks",NbtUtil.writeBlockPosToNbtList(unloadedBlocks));
        nbt.put("heat_storage",heatStorage.toNbt());
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
                '}';
    }
}
