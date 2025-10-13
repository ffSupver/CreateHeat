package com.ffsupver.createheat.compat;

import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
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

        addMod(ModIds.IceAndFire.ModId, IceAndFire::new);

    }

    public static void init(IEventBus eventBus){
        loadMods();

        for (Map.Entry<String,Supplier<CHModCompat>> entry : MODS.entrySet()){
            if (ModList.get().isLoaded(entry.getKey())){
                entry.getValue().get().init(eventBus);
            }
        }
    }

    public static void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER){
        loadMods();

        for (Map.Entry<String,Supplier<CHModCompat>> entry : MODS.entrySet()){
            if (ModList.get().isLoaded(entry.getKey())){
                entry.getValue().get().registerPonder(HELPER);
            }
        }
    }

    private static void addMod(String modId,Supplier<CHModCompat> modCompatSupplier){
        if (ModList.get().isLoaded(modId)){
            MODS.put(modId,modCompatSupplier);
        }
    }

    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event, ResourceKey<CreativeModeTab> tabKey, ItemLike... itemLikes){
        if (event.getTabKey().equals(tabKey)){
            Arrays.stream(itemLikes).forEach(event::accept);
        }
    }

    public enum ModIds{
        IceAndFire("iceandfire");
        public final String ModId;

        ModIds(String modId) {
            ModId = modId;
        }
    }
}
