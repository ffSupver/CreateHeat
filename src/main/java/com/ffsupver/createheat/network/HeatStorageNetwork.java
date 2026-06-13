package com.ffsupver.createheat.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

import static com.ffsupver.createheat.network.NetworkService.Services.HEAT_STORAGE;

public class HeatStorageNetwork extends TickingBlockNetwork{
    public HeatStorageNetwork(UUID networkID, Set<BlockPos> connectedBlocks) {
        super(networkID, connectedBlocks);
    }

    @Override
    public Set<? extends TickingBlockNetwork> onNetworkSplit(Set<BlockPos> disconnectedBlocks, ServerLevel level) {
        return NetworkService.onNetworkSplit(disconnectedBlocks, level, HEAT_STORAGE);
    }

    public static HeatStorageNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        System.out.println("loadingHeatStorageNetworkNbt:"+nbt);

        HeatStorageNetwork heatStorageNetwork = fromNbt(nbt, HeatStorageNetwork::new);
        return heatStorageNetwork;
    }

    @Override
    public String toString() {
        return "HeatStorageNetwork{" +
                "size="+connectedBlocks.size()+ "   networkID=" + networkID + "    blocks="+connectedBlocks+
                '}';
    }
}
