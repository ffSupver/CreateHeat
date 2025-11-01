package com.ffsupver.createheat.compat.anvilCraft;

import com.ffsupver.createheat.api.anvilCraft.HeatableBlockHeatTransferProcesserData;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.registries.CHDatapacks;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;


public class AnvilCraft implements CHModCompat {
    public static final ResourceKey<Registry<HeatableBlockHeatTransferProcesserData>> HEATABLE_BLOCK_HTP_DATA = CHDatapacks.key("anvil_craft");


    @Override
    public String getModId() {
        return Mods.ModIds.ANVIL_CRAFT.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        CHHeatTransferProcessers.registerHeatTransferProcesser(HeatableBlockTransferProcesser.TYPE.getPath(), () -> HeatableBlockTransferProcesser::new);
        eventBus.addListener(Mods.registerDatapack(HEATABLE_BLOCK_HTP_DATA,HeatableBlockHeatTransferProcesserData.CODEC));
    }
}
