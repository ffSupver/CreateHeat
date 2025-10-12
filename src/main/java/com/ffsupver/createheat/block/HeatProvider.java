package com.ffsupver.createheat.block;

import com.ffsupver.createheat.util.HeatUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface HeatProvider {
    public int getHeatPerTick();
    public int getSupperHeatCount();


    BoilerHeater HEATER = HeatProvider::getBoilerHeat;

    static float getBoilerHeat(Level level, BlockPos pos, BlockState blockState) {
        if (level.getBlockEntity(pos) instanceof HeatProvider provider && provider.getHeatPerTick() > 0){
            System.out.println("provied "+provider.getHeatPerTick()+" "+HeatUtil.toBoilerHeat(provider.getHeatPerTick()));
            return HeatUtil.toBoilerHeat(provider.getHeatPerTick());
        }
        return BoilerHeater.NO_HEAT;
    }
}
