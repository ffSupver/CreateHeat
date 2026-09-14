package com.ffsupver.createheat.block.thermalBlock;

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

public class ThermalPipeBlockEntity extends SmartBlockEntity {
    public ThermalPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new NetworkBehaviour(this, NetworkService.Services.HEAT));
    }

    public NetworkBehaviour getNetworkBehaviour() {
        return getBehaviour(NetworkBehaviour.TYPE);
    }

    public void updateConnection(boolean disconnect){
        NetworkBehaviour networkBehaviour = getNetworkBehaviour();
        UUID networkId = networkBehaviour.getNetworkId();
        Set<UUID> neighborNetworkId = networkBehaviour.getNeighborNetworkId();
        networkBehaviour.setNetworkId(NetworkService.updateNetwork(getBlockPos(),level,networkId,neighborNetworkId,disconnect, NetworkService.Services.HEAT));
    }

}
