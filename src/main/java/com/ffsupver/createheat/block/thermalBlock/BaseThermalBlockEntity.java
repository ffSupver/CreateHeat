package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.network.NetworkService;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BaseThermalBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public BaseThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
    }

    public Set<UUID> getNeighborNetworkId(){
        return getNetworkBehaviour().getNeighborNetworkId();
//        Set<UUID> neighborNetworkIds = new HashSet<>();
//        BlockUtil.AllDirectionOf(getBlockPos(), neighborPos->{
//            NetworkBehaviour neighborNetworkBehaviour = NetworkBehaviour.get(getLevel(), neighborPos, NetworkBehaviour.TYPE);
//            if (neighborNetworkBehaviour != null && neighborNetworkBehaviour.checkNetworkType(NetworkService.Services.HEAT)){
//                neighborNetworkIds.add(neighborNetworkBehaviour.getNetworkId());
//            }
//        });
//        return neighborNetworkIds;
    }

    public void setHeatNetworkId(UUID heatNetworkId) {
        if (getNetworkBehaviour() != null){
            getNetworkBehaviour().setNetworkId(heatNetworkId);
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
        NetworkBehaviour networkBehaviour = new NetworkBehaviour(this, NetworkService.Services.HEAT);
        BaseThermalBlockBehaviour baseThermalBlockBehaviour = new BaseThermalBlockBehaviour(this);
        setUpBaseThermalBlockBehaviour(baseThermalBlockBehaviour);
        behaviours.add(networkBehaviour);
        behaviours.add(baseThermalBlockBehaviour);
    }

    protected void setUpBaseThermalBlockBehaviour(BaseThermalBlockBehaviour baseThermalBlockBehaviour){}

    public void stepOn(Entity entity){
        if (isBurning() && !entity.isSteppingCarefully() && entity instanceof LivingEntity){
            entity.hurt(getLevel().damageSources().hotFloor(),1);
        }
    }


    /**
     * @return true if the block is marked as isBurning
     */
    public boolean isBurning(){
        BaseThermalBlockBehaviour baseThermalBlockBehaviour = getBaseThermalBlockBehaviour();
        return baseThermalBlockBehaviour != null && baseThermalBlockBehaviour.isBurning();
    }

    public BaseThermalBlockBehaviour getBaseThermalBlockBehaviour() {
        return getBehaviour(BaseThermalBlockBehaviour.TYPE);
    }

    public NetworkBehaviour getNetworkBehaviour() {
        return getBehaviour(NetworkBehaviour.TYPE);
    }
}
