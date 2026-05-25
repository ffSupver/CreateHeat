package com.ffsupver.createheat.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class HeatNetwork {
    private final UUID networkID;

    public HeatNetwork(UUID networkID) {
        this.networkID = networkID;
    }

    public boolean tick(ServerLevel level){
        return false;
    }

    public static HeatNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        UUID networkID = nbt.getUUID("id");
        return new HeatNetwork(networkID);
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("id",networkID);
        return toNbt();
    }
}
