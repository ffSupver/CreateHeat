package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour.TYPE;

public class BaseThermalBlockEntity extends ConnectableBlockEntity<BaseThermalBlockEntity> implements IHaveGoggleInformation {


    public BaseThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean canConnect(ConnectableBlockEntity<?> toCheck) {
        return ThermalBlockEntityBehaviour.canConnect(toCheck);
    }

    @Override
    protected BaseThermalBlockEntity castToSubclass() {
        return this;
    }

    @Override
    public void addConnectedPos(BlockPos pos) {
        super.addConnectedPos(pos);
        getThBehaviour().calculateHeatStorage();
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return getThBehaviour().addToGoggleTooltip(tooltip,isPlayerSneaking);
    }

    @Override
    public void walkAllBlocks(BlockPos exceptFor) {
        super.walkAllBlocks(exceptFor);
        getThBehaviour().calculateHeatStorage();
    }


    @Override
    protected void mergeController(BlockPos oldControllerPos, ConnectableBlockEntity<?> oldControllerEntity, BlockPos newControllerPos, ConnectableBlockEntity<?> newControllerEntity) {
        getThBehaviour().mergeController(oldControllerPos,oldControllerEntity.getBehaviour(TYPE),newControllerPos,ThermalBlockEntityBehaviour.getFromCBE(newControllerEntity));
    }

    @Override
    protected void switchToNewControllerWhenDestroy(BlockPos newPos, ConnectableBlockEntity<?> newControllerEntity) {
        getThBehaviour().switchToNewControllerWhenDestroy(newPos,newControllerEntity.getBehaviour(TYPE));
    }



    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new ThermalBlockEntityBehaviour(this));
    }

    public void stepOn(Entity entity){
        if (isBurning() && !entity.isSteppingCarefully() && entity instanceof LivingEntity){
            boolean hurt = entity.hurt(getLevel().damageSources().hotFloor(),1);
            if (hurt){
                getThBehaviour().getHeatStorage().extract(ThermalBlockEntityBehaviour.MAX_COOLDOWN, false);
            }
        }
    }

    public boolean isBurning(){
        ThermalBlockEntityBehaviour thermalBlockEntityBehaviour = getControllerEntity().getThBehaviour();
        return thermalBlockEntityBehaviour != null && thermalBlockEntityBehaviour.getHeatStorage().getAmount() > ThermalBlockEntityBehaviour.MAX_HEAT.get();
    }

    private ThermalBlockEntityBehaviour getThBehaviour(){
       return getBehaviour(TYPE);
    }
}
