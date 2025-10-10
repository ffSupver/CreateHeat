package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.api.CustomHeater;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;


public class CHDatapacks {
    public static final ResourceKey<Registry<CustomHeater>> CUSTOM_HEATER = key("heater");
    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(CreateHeat.asResource(name));
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CUSTOM_HEATER,CustomHeater.CODEC,CustomHeater.CODEC);
    }

    public static void onDatapackReload(AddReloadListenerEvent event) {
        CustomHeater.registerBoilerHeater(event.getRegistryAccess());
    }
}
