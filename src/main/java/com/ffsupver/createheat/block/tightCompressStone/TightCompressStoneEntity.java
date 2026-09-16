package com.ffsupver.createheat.block.tightCompressStone;

import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.network.NetworkService;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TightCompressStoneEntity extends SmartBlockEntity{
    public TightCompressStoneEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Set<UUID> getNeighborNetworkId(){
        return getNetworkBehaviour().getNeighborNetworkId();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new NetworkBehaviour(this, NetworkService.Services.HEAT_STORAGE));
        behaviours.add(new HeatStorageBehaviour(this));
    }

    public void setNetworkId(UUID id){
        getNetworkBehaviour().setNetworkId(id);
    }

    public UUID getNetworkId(){
        return getNetworkBehaviour().getNetworkId();
    }

    public HeatStorageBehaviour getBehaviour(){
        return getBehaviour(HeatStorageBehaviour.TYPE);
    }
    public NetworkBehaviour getNetworkBehaviour(){
        return getBehaviour(NetworkBehaviour.TYPE);
    }
}
