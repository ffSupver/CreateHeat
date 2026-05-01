package com.ffsupver.createheat.api;

import com.ffsupver.createheat.registries.CHBlockEntityTickers;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class BoilerUpdater {
    public static void registerTicker(){
        CHBlockEntityTickers.registerBlockEntityTicker(BoilerUpdater::tick);
    }

    private static void tick(BlockPos pos, ServerLevel level, BlockEntity blockEntity) {
            Optional<FluidTankBlockEntity> fluidTankControllerBEOptional = getBoilerControllerBE(blockEntity);
            if (fluidTankControllerBEOptional.isPresent()){
                BlockPos posBelow = pos.below();
                if (!(level.getBlockEntity(posBelow) instanceof FluidTankBlockEntity)){
                    if (CHBoilerUpdaters.shouldUpdate(posBelow,level)){
                        fluidTankControllerBEOptional.get().updateBoilerTemperature();
                    }
                }
            }
    }


    @FunctionalInterface
    public interface Tester{
        boolean shouldUpdate(BlockPos posBelowBoiler,ServerLevel level);
    }

    /***
     * Safely check if block above is boiler
     * @param blockEntity blockEntity to check(block above this will be checked)
     * @return Boiler Controller BlockEntity
     */
    public static Optional<FluidTankBlockEntity> getBoilerControllerBE(BlockEntity blockEntity){
        if (blockEntity instanceof FluidTankBlockEntity fluidTankBlockEntity){
            if(fluidTankBlockEntity.getControllerBE() instanceof FluidTankBlockEntity fCBE && fCBE.boiler.attachedEngines > 0){
                return Optional.of(fCBE);
            }
        }
        return Optional.empty();
    }
}
