package com.ffsupver.createheat.network;

import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

public abstract class TickingBlockNetwork {
    protected final UUID networkID;
    protected final Set<BlockPos> connectedBlocks;
    protected boolean needToSave; // notify the network to save outside of ticking
    protected boolean shouldRemove; // marked network as removed from service
    public boolean shouldCheckConnection; // notify the network to check connection next tick
    private final Set<BlockPos> unloadedBlocks = new HashSet<>(); // blocks never loaded after added to network, unloadedBlocks will be checked each tick and removed from this set once they are loaded


    public TickingBlockNetwork(UUID networkID, Set<BlockPos> connectedBlocks) {
        this.networkID = networkID;
        this.connectedBlocks = connectedBlocks;
    }

    /**
     *  Call when blocks disconnect. Try to create new networks for the disconnected blocks and return these networks.
     * @param disconnectedBlocks blocks disconnected from this network
     * @return a set of network that will contain the disconnected blocks
     */
    public abstract Set<? extends TickingBlockNetwork> onNetworkSplit(Set<BlockPos> disconnectedBlocks,ServerLevel level);

    public boolean tick(ServerLevel level){
        // check if network is empty
        if (connectedBlocks.isEmpty()){
            shouldRemove = true;
        }

        // check unloaded blocks
        if (!unloadedBlocks.isEmpty()){
            Iterator<BlockPos> unloadedBlockPosIterator = unloadedBlocks.iterator();
            while (unloadedBlockPosIterator.hasNext()) {
                BlockPos unloadedPos = unloadedBlockPosIterator.next();
                if (level.isLoaded(unloadedPos)) {
                    onUnloadedBlockLoaded(level, unloadedPos);
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
//                Set<TickingBlockNetwork> newNetworks = HeatService.onHeatNetworkSplit(disconnectedBlocks,level);
                Set<? extends TickingBlockNetwork> newNetworks = onNetworkSplit(disconnectedBlocks,level);

                // remove blocks that are already in new networks
                for (TickingBlockNetwork newNetwork : newNetworks){
                    connectedBlocks.removeAll(newNetwork.connectedBlocks);
                }

                onBlockSetChanged();
            }

            shouldCheckConnection = false;
        }

        // save network and reset needToSave
        if (needToSave){
            needToSave = false;
            return true;
        }

        return shouldRemove;
    }

    public void addBlock(BlockPos pos,boolean isLoaded) {
        connectedBlocks.add(pos);
        needToSave = true;
        if (!isLoaded) {
            unloadedBlocks.add(pos);
        }

        onBlockSetChanged();
    }

    public void removeBlock(BlockPos pos){
        connectedBlocks.remove(pos);
        needToSave = true;
        shouldCheckConnection = true;

        onBlockSetChanged();
    }

    public void mergeSelfTo(ServerLevel serverLevel, TickingBlockNetwork finalNetwork) {
        for (BlockPos pos : connectedBlocks) {
            onBlockMergeToNetwork(serverLevel, pos, finalNetwork);
            finalNetwork.addBlock(pos, serverLevel.isLoaded(pos));
        }
        this.shouldRemove = true;
    }

    public boolean shouldRemove() {
        return shouldRemove;
    }

    /**
     * called when a block is merged to another network
     * @param serverLevel server level
     * @param pos merged block pos
     * @param finalNetwork final network, the network that the block is merged to
     */
    protected void onBlockMergeToNetwork(ServerLevel serverLevel,BlockPos pos,TickingBlockNetwork finalNetwork){}

    /**
     * called when a block, which is marked as unloaded, is loaded first time after being added to network
     * @param level server level
     * @param unloadedPos unloaded block pos, which is loaded now
     */
    protected void onUnloadedBlockLoaded(ServerLevel level, BlockPos unloadedPos){}
    protected void onBlockSetChanged(){}

    protected void setUnloadedBlocks(Set<BlockPos> unloadedBlocks) {
        this.unloadedBlocks.clear();
        this.unloadedBlocks.addAll(unloadedBlocks);
    }

    public UUID getNetworkID() {
        return networkID;
    }


    protected static <T extends TickingBlockNetwork> T fromNbt(CompoundTag nbt, BiFunction<UUID, Set<BlockPos>, T> factory) {
        UUID networkID = nbt.getUUID("id");
        Set<BlockPos> connectedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("connected_blocks", Tag.TAG_COMPOUND)));
        Set<BlockPos> unloadedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("unloaded_blocks", Tag.TAG_COMPOUND)));

        T network = factory.apply(networkID, connectedBlocks);
        network.setUnloadedBlocks(unloadedBlocks);
        return network;
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("id",networkID);
        nbt.put("connected_blocks",NbtUtil.writeBlockPosToNbtList(connectedBlocks));
        nbt.put("unloaded_blocks",NbtUtil.writeBlockPosToNbtList(unloadedBlocks));

        return nbt;
    }
}
