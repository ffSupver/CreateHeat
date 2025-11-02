package com.ffsupver.createheat.compat.coldSweat;

import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.registries.CHBlocks;
import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public class ColdSweat implements CHModCompat {
    @Override
    public String getModId() {
        return Mods.ModIds.COLD_SWEAT.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(ColdSweat::registerBlockTemp);
    }



    private static void registerBlockTemp(BlockTempRegisterEvent event){
        event.register(new ThermalBlockEffect(
                0.4,2,
                CHBlocks.THERMAL_BLOCK.get(),
                CHBlocks.SMART_THERMAL_BLOCK.get()
        ));

        Mods.executeIfModLoad(Mods.ModIds.ICE_AND_FIRE.ModId, iceAndFire->{
            event.register(new DragonFireInputEffect());
        });
    }
}
