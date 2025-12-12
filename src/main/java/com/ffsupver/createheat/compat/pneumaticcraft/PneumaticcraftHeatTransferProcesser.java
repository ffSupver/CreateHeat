package com.ffsupver.createheat.compat.pneumaticcraft;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import me.desht.pneumaticcraft.common.block.entity.IHeatExchangingTE;
import me.desht.pneumaticcraft.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import static com.ffsupver.createheat.compat.pneumaticcraft.Pneumaticcraft.REGULAR_HEAT_TEMPERATURE;
import static com.ffsupver.createheat.compat.pneumaticcraft.Pneumaticcraft.SUPER_HEAT_TEMPERATURE;

public class PneumaticcraftHeatTransferProcesser extends HeatTransferProcesser {
    public static final ResourceLocation TYPE = CreateHeat.asResource("pneumaticcraft_compat");
    private static final double TEMPERATURE_BUFFER = 10;

    protected PneumaticcraftHeatTransferProcesser() {
        super(TYPE);
    }

    @Override
    public boolean needHeat(Level level, BlockPos hTPPos, @Nullable Direction face,int heat,int tickSkip,int superHeatCount) {
        if (level.getBlockState(hTPPos).is(ModBlocks.VORTEX_TUBE.get())){
            return false;
        }
        IHeatExchangingTE heatExchangerLogic = getHeatExchangeLogic(level,hTPPos);
        if (heatExchangerLogic == null){
            return false;
        }
        boolean hasSuperHeat = superHeatCount > 0;
        double temperature = heatExchangerLogic.getHeatExchanger().getTemperature();
        return temperature <= (hasSuperHeat ? SUPER_HEAT_TEMPERATURE : REGULAR_HEAT_TEMPERATURE) + TEMPERATURE_BUFFER;
    }

    @Override
    public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide,int tickSkip,int superHeatCount) {
        IHeatExchangingTE heatExchangingTE = getHeatExchangeLogic(level,hTPPos);
        if (heatExchangingTE != null){
            double temperature = heatExchangingTE.getHeatExchanger().getTemperature();
            double maxTemperature = (superHeatCount > 0 ? SUPER_HEAT_TEMPERATURE : REGULAR_HEAT_TEMPERATURE);
            if (temperature < maxTemperature){
                heatExchangingTE.getHeatExchanger().addHeat(heatProvide * 1.9);
            }
        }
    }

    @Override
    public boolean shouldProcessEveryTick() {
        return true;
    }

    private IHeatExchangingTE getHeatExchangeLogic(Level level, BlockPos pos){
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof IHeatExchangingTE heatExchangingTE){
            return heatExchangingTE;
        }
        return null;
    }
}
