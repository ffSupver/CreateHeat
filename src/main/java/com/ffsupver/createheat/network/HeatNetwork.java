package com.ffsupver.createheat.network;

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
    private boolean needToSave;
    private boolean shouldRemove;

    public HeatNetwork(UUID networkID,Set<BlockPos> connectedBlocks) {
        this.networkID = networkID;
        this.connectedBlocks = connectedBlocks;
    }

    public boolean tick(ServerLevel level){
        boolean shouldSave = false;

        if (connectedBlocks.isEmpty()){
            shouldRemove = true;
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

    public void addBlock(BlockPos pos){
        connectedBlocks.add(pos);
        needToSave = true;
    }

    public void removeBlock(BlockPos pos){
        connectedBlocks.remove(pos);
        needToSave = true;
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
                ", connectedBlocks=" + connectedBlocks +
                '}';
    }
}
