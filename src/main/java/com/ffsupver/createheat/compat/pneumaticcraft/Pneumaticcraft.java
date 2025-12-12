package com.ffsupver.createheat.compat.pneumaticcraft;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.item.thermalTool.ThermalToolUseActions;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.registry.SimpleRegistry;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.block.entity.IHeatExchangingTE;
import me.desht.pneumaticcraft.common.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;

public class Pneumaticcraft implements CHModCompat {
    public static final double SUPER_HEAT_TEMPERATURE = 805;
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
                        (level, pos, state) -> {
                            if (level.getBlockEntity(pos) instanceof IHeatExchangingTE iHeatExchangingTE) {
                                IHeatExchangerLogic iHeatExchangerLogic = iHeatExchangingTE.getHeatExchanger(Direction.UP);
                                if (iHeatExchangerLogic != null) {
                                    //确保不会出现0-1之间的数, 805->烈焰人燃烧室超级加热的温度(K)
                                    double temperature = iHeatExchangerLogic.getTemperature();
                                    return temperature >= SUPER_HEAT_TEMPERATURE ? (float) (iHeatExchangerLogic.getTemperature() / SUPER_HEAT_TEMPERATURE) : temperature > REGULAR_HEAT_TEMPERATURE ? 0 : BoilerHeater.NO_HEAT;
                                }
                            }
                            return BoilerHeater.NO_HEAT;
                        }
                )
        );
    }
}
