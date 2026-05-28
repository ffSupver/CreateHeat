package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.util.BlockUtil;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BaseThermalBlockEntity1 extends SmartBlockEntity implements IHaveGoggleInformation {

    public BaseThermalBlockEntity1(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
    }

    public Set<UUID> getNeighborNetworkId(){
        Set<UUID> neighborNetworkIds = new HashSet<>();
        BlockUtil.AllDirectionOf(getBlockPos(), neighborPos->{
            if (getLevel().getBlockEntity(neighborPos) instanceof BaseThermalBlockEntity1 neighborBE){
                UUID neighborID = neighborBE.getHeatNetworkId();
                if (neighborID != null){
                    neighborNetworkIds.add(neighborID);
                }
            }
        });
        return neighborNetworkIds;
    }

    public void setHeatNetworkId(UUID heatNetworkId) {
        if (getBaseThermalBlockBehaviour() != null){
            getBaseThermalBlockBehaviour().setHeatNetworkId(heatNetworkId);
        }
    }

    public UUID getHeatNetworkId() {
        if (getBaseThermalBlockBehaviour() != null){
            return getBaseThermalBlockBehaviour().getHeatNetworkId();
        }
        return null;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BaseThermalBlockBehaviour baseThermalBlockBehaviour = getBaseThermalBlockBehaviour();
        boolean hasNetwork = baseThermalBlockBehaviour != null && baseThermalBlockBehaviour.getHeatNetworkId() != null;
        if (hasNetwork){
            return baseThermalBlockBehaviour.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        return false;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new BaseThermalBlockBehaviour(this));
    }

    public BaseThermalBlockBehaviour getBaseThermalBlockBehaviour() {
        return getBehaviour(BaseThermalBlockBehaviour.TYPE);
    }
}
