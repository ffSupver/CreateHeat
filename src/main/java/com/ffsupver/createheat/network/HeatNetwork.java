package com.ffsupver.createheat.network;

import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HeatNetwork {
    private final UUID networkID;
    private final Set<BlockPos> connectedBlocks;
    private boolean needToSave; // notify the network to save outside of ticking
    private boolean shouldRemove; // marked network as removed from service
    public boolean shouldCheckConnection; // notify the network to check connection next tick

//    private final Set<BlockPos> lastTickPosSet = new HashSet<>();

    public HeatNetwork(UUID networkID,Set<BlockPos> connectedBlocks) {
        this.networkID = networkID;
        this.connectedBlocks = connectedBlocks;
//        this.lastTickPosSet.addAll(connectedBlocks); // add all blocks to lastTickPosSet when creating. Prevent network from removing blocks at first tick.
    }

    public boolean tick(ServerLevel level){
        boolean shouldSave = false;

        // check and remove lost blocks
//        Set<BlockPos> lostBlocks = new HashSet<>();
//        connectedBlocks.forEach(pos -> {
//            System.out.println("checking block:"+pos+" isloaded:"+level.isLoaded(pos)+" lastTickPos:"+lastTickPosSet.contains(pos));
//            if (level.isLoaded(pos) && !lastTickPosSet.contains(pos)) {
//                lostBlocks.add(pos);
//            }
//        });
//        if (!lostBlocks.isEmpty()){
//            lostBlocks.forEach(connectedBlocks::remove);
//            shouldSave = true;
//        }
//        lastTickPosSet.clear();
//        System.out.println("remain blocks:" + connectedBlocks.size()+"/"+connectedBlocks);

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
            }

            shouldCheckConnection = false;
        }


        if (needToSave){
            shouldSave = true;
            needToSave = false;
        }
        return shouldSave;
    }

    public boolean shouldRemove() {
        return shouldRemove;
    }

    public void onBlockTick(BlockPos pos){
//        lastTickPosSet.add(pos);
    }

    public void addBlock(BlockPos pos){
        connectedBlocks.add(pos);
//        lastTickPosSet.add(pos);
        needToSave = true;
    }

    public void removeBlock(BlockPos pos){
        connectedBlocks.remove(pos);
//        lastTickPosSet.remove(pos);
        needToSave = true;
        shouldCheckConnection = true;
    }

    public UUID getNetworkID() {
        return networkID;
    }

    public static HeatNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        UUID networkID = nbt.getUUID("id");
        Set<BlockPos> connectedBlocks = new HashSet<>(NbtUtil.readBlockPosFromNbtList(nbt.getList("connected_blocks", Tag.TAG_COMPOUND)));
        return new HeatNetwork(networkID,connectedBlocks);
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("id",networkID);
        nbt.put("connected_blocks",NbtUtil.writeBlockPosToNbtList(connectedBlocks));
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
