package com.ffsupver.createheat.compat.pneumaticcraft;

import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.simibubi.create.api.boiler.BoilerHeater;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.block.entity.IHeatExchangingTE;
import me.desht.pneumaticcraft.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

public class Pneumaticcraft implements CHModCompat {
    @Override
    public String getModId() {
        return Mods.ModIds.PNEUMATICCRAFT.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        CHHeatTransferProcessers.registerHeatTransferProcesser(new PneumaticcraftHeatTransferProcesser());
        CHBoilerUpdaters.registerBoilerUpdater(
                (posBelowBoiler, level) ->
                level.getBlockState(posBelowBoiler).is(ModBlocks.VORTEX_TUBE.get()) &&
                level.getBlockEntity(posBelowBoiler) instanceof IHeatExchangingTE iHeatExchangingTE && iHeatExchangingTE.getHeatExchanger(Direction.UP) != null
        );
    }

    @Override
    public void registerBoilerHeater() {
        BoilerHeater.REGISTRY.register(ModBlocks.VORTEX_TUBE.get(), (level, pos, state) -> {
            if (level.getBlockEntity(pos) instanceof IHeatExchangingTE iHeatExchangingTE){
               IHeatExchangerLogic iHeatExchangerLogic = iHeatExchangingTE.getHeatExchanger(Direction.UP);
                if (iHeatExchangerLogic != null) {
                    //确保不会出现0-1之间的数, 805->烈焰人燃烧室超级加热的温度(K)
                    double temperature = iHeatExchangerLogic.getTemperature();
                    return temperature >= 805 ? (float) (iHeatExchangerLogic.getTemperature() / 805) : temperature > 390 ? 0 : BoilerHeater.NO_HEAT;
                }
            }
            return BoilerHeater.NO_HEAT;
        });
    }

    public static class PneumaticcraftHeatTransferProcesser extends HeatTransferProcesser {

        @Override
        public boolean needHeat(Level level, BlockPos hTPPos, @Nullable Direction face) {
            if (level.getBlockState(hTPPos).is(ModBlocks.VORTEX_TUBE.get())){
                return false;
            }
            IHeatExchangingTE heatExchangerLogic = getHeatExchangeLogic(level,hTPPos);
            return heatExchangerLogic != null;
        }

        @Override
        public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide) {
            IHeatExchangingTE heatExchangingTE = getHeatExchangeLogic(level,hTPPos);
            if (heatExchangingTE != null){
                heatExchangingTE.getHeatExchanger().addHeat(heatProvide * 1.9);
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
}
