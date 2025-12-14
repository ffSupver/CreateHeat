package com.ffsupver.createheat.compat.pneumaticcraft;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.item.thermalTool.ThermalToolUseActions;
import com.ffsupver.createheat.registries.CHBlockEntityTickers;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.block.entity.IHeatExchangingTE;
import me.desht.pneumaticcraft.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;

public class Pneumaticcraft implements CHModCompat {
    public static final double SUPER_HEAT_TEMPERATURE = 805; // 805->烈焰人燃烧室超级加热的温度(K)
    public static final double REGULAR_HEAT_TEMPERATURE = 390;
    @Override
    public String getModId() {
        return Mods.ModIds.PNEUMATICCRAFT.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        CHHeatTransferProcessers.registerHeatTransferProcesser(PneumaticcraftHeatTransferProcesser.TYPE.getPath(),()->PneumaticcraftHeatTransferProcesser::new);
        CHBoilerUpdaters.registerBoilerUpdater(
                (posBelowBoiler, level) ->
                level.getBlockState(posBelowBoiler).is(ModBlocks.VORTEX_TUBE.get()) &&
                level.getBlockEntity(posBelowBoiler) instanceof IHeatExchangingTE iHeatExchangingTE && iHeatExchangingTE.getHeatExchanger(Direction.UP) != null
        );
        CHBlockEntityTickers.registerBlockEntityTicker(Pneumaticcraft::tickBoilerHeater);
        ThermalToolUseActions.registerAction(
                (level, pos, state, player, isShift) -> level.getBlockEntity(pos) instanceof IHeatExchangingTE,
                (level, pos, state, player, isShift) -> {
                    if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IHeatExchangingTE iHeatExchangingTE) {
                        double temperature = iHeatExchangingTE.getHeatExchanger().getTemperature();
                        player.displayClientMessage(Component.literal("Temperature: %.4f".formatted( temperature)),true);
                        return true;
                    }
                    return false;
                }
                );
    }

    @Override
    public void registerBoilerHeater() {
        BoilerHeater.REGISTRY.registerProvider(
                SimpleRegistry.Provider.forBlockTag(CHTags.BlockTag.PNEUMATIC_BOILER_HEATER,
                        (level, pos, state) -> boilerHeater(pos, level, level.getBlockEntity(pos))
                )
        );
    }

    private float boilerHeater(BlockPos pos, Level level, BlockEntity blockEntity){
        if (blockEntity instanceof IHeatExchangingTE iHeatExchangingTE) {
            IHeatExchangerLogic iHeatExchangerLogic = iHeatExchangingTE.getHeatExchanger(Direction.UP);
            if (iHeatExchangerLogic != null) {
                //确保不会出现0-1之间的数
                double temperature = iHeatExchangerLogic.getTemperature();
                double heat = getHeatFromTemperature(temperature);
                return heat >= 1 ? ((float) heat) : (heat > 0 ? 0 : BoilerHeater.NO_HEAT);
            }
        }
        return BoilerHeater.NO_HEAT;
    }

    private static void tickBoilerHeater(BlockPos pos, ServerLevel level, BlockEntity blockEntity){
        BlockEntity blockEntityAbove = level.getBlockEntity(pos.above());
        boolean hasBoilerAbove = blockEntityAbove instanceof FluidTankBlockEntity fluidTankBlockEntity && fluidTankBlockEntity.getControllerBE().boiler.attachedEngines > 0;
        boolean hasThermalBlock = level.getBlockState(pos.above()).is(CHTags.BlockTag.THERMAL_BLOCKS);
        if ((hasBoilerAbove || hasThermalBlock) && blockEntity instanceof IHeatExchangingTE iHeatExchangingTE){
            IHeatExchangerLogic exchangerLogic = iHeatExchangingTE.getHeatExchanger(Direction.UP);
            if (exchangerLogic != null) {
               double temperature = exchangerLogic.getTemperature();
                float heatFromTemperature = getHeatFromTemperature(temperature);
                exchangerLogic.addHeat(-heatFromTemperature*20);
            }
        }
    }

    private static float getHeatFromTemperature(double temperature){
        if (temperature >= REGULAR_HEAT_TEMPERATURE){
            double delta = temperature - REGULAR_HEAT_TEMPERATURE;
            return (float) (delta / (SUPER_HEAT_TEMPERATURE - REGULAR_HEAT_TEMPERATURE));
        }
        return 0;
    }
}
