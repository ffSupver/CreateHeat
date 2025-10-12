package com.ffsupver.createheat.compat;

import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Mods {
    private static Map<String, Supplier<CHModCompat>> MODS = new HashMap<>();

    private static void loadMods(){
        MODS.clear();

        MODS.put(IceAndFire.MOD_ID,IceAndFire::new);
    }

    public static void init(IEventBus eventBus){
        loadMods();

        for (Map.Entry<String,Supplier<CHModCompat>> entry : MODS.entrySet()){
            if (ModList.get().isLoaded(entry.getKey())){
                entry.getValue().get().init(eventBus);
            }
        }
    }

    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event, ResourceKey<CreativeModeTab> tabKey, ItemLike... itemLikes){
        if (event.getTabKey().equals(tabKey)){
            Arrays.stream(itemLikes).forEach(event::accept);
        }
    }
}
