package com.ffsupver.createheat.compat;

import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import com.ffsupver.createheat.compat.pneumaticcraft.Pneumaticcraft;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Mods {
    private static Map<String, Supplier<CHModCompat>> MODS = new HashMap<>();

    private static void loadMods(){
        MODS.clear();

        addMod(ModIds.IceAndFire.ModId, IceAndFire::new);
        addMod(ModIds.PNEUMATICCRAFT.ModId, Pneumaticcraft::new);

    }

    public static void init(IEventBus eventBus){
        loadMods();

        executeIfLoad(chModCompatSupplier->chModCompatSupplier.get().init(eventBus));

        eventBus.addListener(Mods::registerBoilerHeater);
    }

    public static void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER){
        loadMods();

        executeIfLoad(chModCompatSupplier-> chModCompatSupplier.get().registerPonder(HELPER));
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

    private static void registerBoilerHeater(FMLCommonSetupEvent event){
        event.enqueueWork(()->{
            executeIfLoad(chModCompatSupplier->chModCompatSupplier.get().registerBoilerHeater());
        });
    }

    private static void executeIfLoad(Consumer<Supplier<CHModCompat>> runnable){
        for (Map.Entry<String,Supplier<CHModCompat>> entry : MODS.entrySet()){
            if (ModList.get().isLoaded(entry.getKey())){
               runnable.accept(entry.getValue());
            }
        }
    }

    public enum ModIds{
        IceAndFire("iceandfire"),
        PNEUMATICCRAFT("pneumaticcraft");
        public final String ModId;

        ModIds(String modId) {
            ModId = modId;
        }
    }
}
