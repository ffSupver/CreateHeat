package com.ffsupver.createheat.block.tightCompressStone;

import com.ffsupver.createheat.util.BlockUtil;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TightCompressStoneEntity1 extends SmartBlockEntity{
    public TightCompressStoneEntity1(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Set<UUID> getNeighborNetworkId(){
        Set<UUID> neighborNetworkIds = new HashSet<>();
        BlockUtil.AllDirectionOf(getBlockPos(), neighborPos->{
            if (getLevel().getBlockEntity(neighborPos) instanceof TightCompressStoneEntity1 neighborBE){
                UUID neighborID = neighborBE.getNetworkId();
                if (neighborID != null){
                    neighborNetworkIds.add(neighborID);
                }
            }
        });
        return neighborNetworkIds;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new HeatStorageBehaviour(this));
    }

    public void setNetworkId(UUID id){
        getBehaviour().setNetworkId(id);
    }

    public UUID getNetworkId(){
        return getBehaviour().getNetworkId();
    }

    public HeatStorageBehaviour getBehaviour(){
        return getBehaviour(HeatStorageBehaviour.TYPE);
    }
}
