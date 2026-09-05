package com.ffsupver.createheat.network;

import com.ffsupver.createheat.block.NetworkBehaviour;
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
    private final HeatStorageNetworkStorage heatStorage = new HeatStorageNetworkStorage(0);

    public HeatStorageNetwork(UUID networkID, Set<BlockPos> connectedBlocks) {
        super(networkID, connectedBlocks);
    }

    @Override
    public boolean tick(ServerLevel level) {
        boolean shouldSave = super.tick(level);


        HeatStorageBehaviour.SuperHeatStorage.SuperSnapshot heatChange = heatStorage.getHeatChange();
        heatStorage.clear();

        for (BlockPos pos : connectedBlocks){
            HeatStorageBehaviour heatStorageBehaviour = BlockEntityBehaviour.get(level,pos,HeatStorageBehaviour.TYPE);
            if (heatStorageBehaviour != null){
                HeatUtil.HeatData superHeatExtracted = NO_HEAT_PROVIDE;
                HeatUtil.HeatData heatExtracted = NO_HEAT_PROVIDE;
                if (heatChange.superAmount() < 0){
                    superHeatExtracted = heatStorageBehaviour.extractHeat(new HeatUtil.HeatData(-heatChange.superAmount(),1),false);
                }
                if (heatChange.amount() < 0){
                    heatExtracted = heatStorageBehaviour.extractHeat(new HeatUtil.HeatData(-heatChange.amount(),0),false);
                }

                // heatChange的值是负的，所以要加
                heatChange = new HeatStorageBehaviour.SuperHeatStorage.SuperSnapshot(heatChange.amount() + heatExtracted.heat(),heatChange.superAmount() + superHeatExtracted.heat(),heatChange.capacity(),heatChange.superCapacity());

                heatStorage.merge(heatStorageBehaviour.getSuperHeatStorage());
                if (heatStorageBehaviour.getSuperHeatStorage().getSuperAmount() > 0){
                    heatStorage.superHeatCount += 1;
                }
            }
        }
        heatStorage.updateSnapshot();



        return shouldSave;
    }

    /**
     * Insert heat to the network. Try to insert heat to all blocks in the network.
     * @param level server level
     * @param heatData heat data to insert
     * @return heat data left
     */
    public HeatUtil.HeatData insert(ServerLevel level,HeatUtil.HeatData heatData) {
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
        NetworkBehaviour networkBehaviour = BlockEntityBehaviour.get(serverLevel,pos, NetworkBehaviour.TYPE);
        if (networkBehaviour != null && networkBehaviour.checkNetworkType(HEAT_STORAGE)){
            networkBehaviour.setNetworkId(finalNetwork.getNetworkID());
        }
    }

    public HeatStorageBehaviour.SuperHeatStorage getHeatStorage() {
        return heatStorage;
    }

    public static HeatStorageNetwork fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;

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
                "size="+connectedBlocks.size() +
                "   heatStorage=" + heatStorage +
                "   networkID=" + networkID +
                "    blocks="+connectedBlocks +
                '}';
    }

    private static class HeatStorageNetworkStorage extends HeatStorageBehaviour.SuperHeatStorage{
        private SuperSnapshot superSnapshot;
        private int superHeatCount;  // super heat count storage

        public HeatStorageNetworkStorage(int capacity) {
            super(capacity);
        }

        @Override
        public HeatUtil.HeatData extract(HeatUtil.HeatData heatData, boolean simulate) {
            if (heatData.superHeatCount() > superHeatCount){
                return NO_HEAT_PROVIDE;
            }

            HeatUtil.HeatData extract = super.extract(heatData, simulate);
            if (!simulate){
                superHeatCount -= extract.superHeatCount();
            }
            return extract;
        }

        @Override
        public void clear() {
            super.clear();
            superHeatCount = 0;
        }

        public SuperSnapshot getHeatChange(){
            if (superSnapshot == null){
                return new SuperSnapshot(0,0,0,0);
            }else {
                return new SuperSnapshot(getAmount() - superSnapshot.amount(),getSuperAmount() - superSnapshot.superAmount(),0,0);
            }
        }

        public void updateSnapshot(){
            superSnapshot = superSnapshot();
        }
    }
}
