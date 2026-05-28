package com.ffsupver.createheat.network;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.block.thermalBlock.BaseThermalBlockBehaviour;
import com.ffsupver.createheat.block.thermalBlock.HeatStorage;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.HeatUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class HeatNetwork {
    private final UUID networkID;
    private final Set<BlockPos> connectedBlocks;
    private boolean needToSave; // notify the network to save outside of ticking
    private boolean shouldRemove; // marked network as removed from service
    public boolean shouldCheckConnection; // notify the network to check connection next tick
    private final Set<BlockPos> unloadedBlocks = new HashSet<>(); // blocks never loaded after added to network, unloadedBlocks will be checked each tick and removed from this set once they are loaded

    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private final HeatStorage heatStorage;
    private HeatUtil.HeatData heatDataLastTick = HeatUtil.NO_HEAT_PROVIDE;
    private HeatUtil.HeatData displayHeatData;

    public HeatNetwork(UUID networkID,Set<BlockPos> connectedBlocks) {
        this.networkID = networkID;
        this.connectedBlocks = connectedBlocks;
        this.heatStorage = new HeatStorage(connectedBlocks.size() * MAX_HEAT.get());
    }

    public boolean tick(ServerLevel level){
        boolean shouldSave = false;

        // check unloaded blocks
        if (!unloadedBlocks.isEmpty()){
            Iterator<BlockPos> unloadedBlockPosIterator = unloadedBlocks.iterator();
            while (unloadedBlockPosIterator.hasNext()) {
                BlockPos unloadedPos = unloadedBlockPosIterator.next();
                if (level.isLoaded(unloadedPos)) {
                    BaseThermalBlockBehaviour baseThermalBlockBehaviour = BlockEntityBehaviour.get(level, unloadedPos, BaseThermalBlockBehaviour.TYPE);
                    if (baseThermalBlockBehaviour != null) {
                        baseThermalBlockBehaviour.setHeatNetworkId(networkID);
                    }
                    unloadedBlockPosIterator.remove();
                }
            }
        }


        // check if network is empty
        if (connectedBlocks.isEmpty()){
            shouldRemove = true;
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
        heatStorage.insert(heatDataLastTick.heat());
        displayHeatData = heatDataLastTick;
        heatDataLastTick = HeatUtil.NO_HEAT_PROVIDE;

        System.out.println("ticking "+level.dimension()+" heat:"+heatStorage+" lastHeat:"+heatDataLastTick+" blocks:"+connectedBlocks.size()+" / "+connectedBlocks);

        if (needToSave){
            shouldSave = true;
            needToSave = false;
        }
        return shouldSave;
    }

    public boolean shouldRemove() {
        return shouldRemove;
    }

    public void onBlockTick(BlockPos pos, HeatUtil.HeatData heatData){
        this.heatDataLastTick = this.heatDataLastTick.merge(heatData);
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
            BaseThermalBlockBehaviour baseThermalBlockBehaviour = BlockEntityBehaviour.get(serverLevel, pos, BaseThermalBlockBehaviour.TYPE);
            if (baseThermalBlockBehaviour != null) {
                baseThermalBlockBehaviour.setHeatNetworkId(finalNetwork.getNetworkID());
            }
            finalNetwork.addBlock(pos, serverLevel.isLoaded(pos));
        }
        this.shouldRemove = true;
    }

    private void calculateHeatCapacity(){
        heatStorage.setCapacity(connectedBlocks.size() * MAX_HEAT.get());
    }

    public HeatStorage.Snapshot getDisplayHeatStorage() {
        return heatStorage.snapshot();
    }
    public HeatUtil.HeatData getDisplayHeatData() {
        return displayHeatData;
    }

    public UUID getNetworkID() {
        return networkID;
    }

    private void setUnloadedBlocks(Set<BlockPos> unloadedBlocks) {
        this.unloadedBlocks.clear();
        this.unloadedBlocks.addAll(unloadedBlocks);
    }
    public static HeatNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        UUID networkID = nbt.getUUID("id");
        Set<BlockPos> connectedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("connected_blocks", Tag.TAG_COMPOUND)));
        Set<BlockPos> unloadedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("unloaded_blocks", Tag.TAG_COMPOUND)));


        HeatNetwork heatNetwork = new HeatNetwork(networkID,connectedBlocks);
        heatNetwork.setUnloadedBlocks(unloadedBlocks);
        heatNetwork.heatStorage.fromNbt(nbt.getCompound("heat_storage"));
        return heatNetwork;
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("id",networkID);
        nbt.put("connected_blocks",NbtUtil.writeBlockPosToNbtList(connectedBlocks));
        nbt.put("unloaded_blocks",NbtUtil.writeBlockPosToNbtList(unloadedBlocks));
        nbt.put("heat_storage",heatStorage.toNbt());
        return nbt;
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
