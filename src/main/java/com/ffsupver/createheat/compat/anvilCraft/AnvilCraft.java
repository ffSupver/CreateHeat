package com.ffsupver.createheat.compat.anvilCraft;

import com.ffsupver.createheat.api.anvilCraft.HeatableBlockHeatTransferProcesserData;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.ffsupver.createheat.registries.CHDatapacks;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.simibubi.create.api.boiler.BoilerHeater;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.IEventBus;

import static com.simibubi.create.api.registry.SimpleRegistry.Provider.forBlockTag;


public class AnvilCraft implements CHModCompat {
    public static final ResourceKey<Registry<HeatableBlockHeatTransferProcesserData>> HEATABLE_BLOCK_HTP_DATA = CHDatapacks.key("anvil_craft");


    @Override
    public String getModId() {
        return Mods.ModIds.ANVIL_CRAFT.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        CHHeatTransferProcessers.registerHeatTransferProcesser(HeatableBlockTransferProcesser.TYPE.getPath(), () -> HeatableBlockTransferProcesser::new);
        CHHeatTransferProcessers.registerHeatTransferProcesser(HeatCollectorTransferProcesser.TYPE.getPath(), () -> HeatCollectorTransferProcesser::new);
        CHHeatTransferProcessers.registerOptionalNeedHeatBlock(state -> state.is(BlockTags.CAULDRONS));
        CHBoilerUpdaters.registerBoilerUpdater(HeatProducerBoilHeater::shouldUpdateBoiler);

        eventBus.addListener(Mods.registerDatapack(HEATABLE_BLOCK_HTP_DATA,HeatableBlockHeatTransferProcesserData.CODEC));
    }

    @Override
    public void registerBoilerHeater() {
        BoilerHeater.REGISTRY.registerProvider(forBlockTag(HeatProducerBoilHeater.BLOCK_TAG,new HeatProducerBoilHeater()));
    }
}
