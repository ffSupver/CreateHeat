package com.ffsupver.createheat.network;

import com.ffsupver.createheat.block.tightCompressStone.HeatStorageBehaviour;
import com.ffsupver.createheat.util.HeatUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.UUID;

import static com.ffsupver.createheat.network.NetworkService.Services.HEAT_STORAGE;
import static com.ffsupver.createheat.util.HeatUtil.NO_HEAT_PROVIDE;

public class HeatStorageNetwork extends TickingBlockNetwork{
    private final HeatStorageBehaviour.SuperHeatStorage heatStorage = new HeatStorageBehaviour.SuperHeatStorage(0);
    private HeatUtil.HeatData heatDataLastTickRemain = NO_HEAT_PROVIDE;

    public HeatStorageNetwork(UUID networkID, Set<BlockPos> connectedBlocks) {
        super(networkID, connectedBlocks);
    }

    @Override
    public boolean tick(ServerLevel level) {
        boolean shouldSave = super.tick(level);

        int capacity = 0;
        int amount = 0;
        for (BlockPos pos : connectedBlocks){
            HeatStorageBehaviour heatStorageBehaviour = BlockEntityBehaviour.get(level,pos,HeatStorageBehaviour.TYPE);
            if (heatStorageBehaviour != null){
                capacity += heatStorageBehaviour.getCapacity();
                amount += heatStorageBehaviour.getAmount();
            }
        }
        heatStorage.setCapacity(capacity);
        heatStorage.setAmount(amount);


        return shouldSave;
    }

    /**
     * Insert heat to the network. Try to insert heat to all blocks in the network.
     * @param level server level
     * @param heatData heat data to insert
     * @return heat data left
     */
    public HeatUtil.HeatData insert(ServerLevel level,HeatUtil.HeatData heatData) {
        System.out.println("HeatStorageNetwork insertingHeat:"+heatData);
        for (BlockPos pos : connectedBlocks){
            HeatStorageBehaviour heatStorageBehaviour = BlockEntityBehaviour.get(level,pos,HeatStorageBehaviour.TYPE);
            if (heatStorageBehaviour != null){
                heatData = heatStorageBehaviour.insertHeat(heatData);
            }
        }
        return heatData;
    }


    @Override
    public Set<? extends TickingBlockNetwork> onNetworkSplit(Set<BlockPos> disconnectedBlocks, ServerLevel level) {
        return NetworkService.onNetworkSplit(disconnectedBlocks, level, HEAT_STORAGE);
    }

    @Override
    protected void onBlockMergeToNetwork(ServerLevel serverLevel, BlockPos pos, TickingBlockNetwork finalNetwork) {
        HeatStorageBehaviour heatStorageBehaviour = BlockEntityBehaviour.get(serverLevel,pos,HeatStorageBehaviour.TYPE);
        if (heatStorageBehaviour != null){
            heatStorageBehaviour.setNetworkId(finalNetwork.getNetworkID());
        }
    }

    public HeatStorageBehaviour.SuperHeatStorage getHeatStorage() {
        return heatStorage;
    }

    public static HeatStorageNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        System.out.println("loadingHeatStorageNetworkNbt:"+nbt);

        HeatStorageNetwork heatStorageNetwork = fromNbt(nbt, HeatStorageNetwork::new);
        heatStorageNetwork.heatStorage.fromNbt(nbt.getCompound("heat_storage"));
        return heatStorageNetwork;
    }

    @Override
    public CompoundTag toNbt() {
        CompoundTag nbt = super.toNbt();
        nbt.put("heat_storage",heatStorage.toNbt());
        return nbt;
    }

    @Override
    public String toString() {
        return "HeatStorageNetwork{" +
                "size="+connectedBlocks.size()+ "   networkID=" + networkID + "    blocks="+connectedBlocks+
                '}';
    }
}
