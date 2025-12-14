package com.ffsupver.createheat.api;

import com.ffsupver.createheat.registries.CHBlockEntityTickers;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BoilerUpdater {
    public static void registerTicker(){
        CHBlockEntityTickers.registerBlockEntityTicker(BoilerUpdater::tick);
    }

    private static void tick(BlockPos pos, ServerLevel level, BlockEntity blockEntity) {
            if (blockEntity instanceof FluidTankBlockEntity fluidTankBlockEntity && fluidTankBlockEntity.getControllerBE().boiler.attachedEngines > 0){
                BlockPos posBelow = pos.below();
                if (!(level.getBlockEntity(posBelow) instanceof FluidTankBlockEntity)){
                    if (CHBoilerUpdaters.shouldUpdate(posBelow,level)){
                        fluidTankBlockEntity.getControllerBE().updateBoilerTemperature();
                    }
                }
            }
    }


    @FunctionalInterface
    public interface Tester{
        boolean shouldUpdate(BlockPos posBelowBoiler,ServerLevel level);
    }
}
