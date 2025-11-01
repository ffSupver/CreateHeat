package com.ffsupver.createheat.compat;

import com.ffsupver.createheat.compat.anvilCraft.AnvilCraft;
import com.ffsupver.createheat.compat.coldSweat.ColdSweat;
import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import com.ffsupver.createheat.compat.pneumaticcraft.Pneumaticcraft;
import com.mojang.serialization.Codec;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Mods {
    private static final Map<String, Supplier<CHModCompat>> MOD_SUPPLIERS = new HashMap<>();
    private static final Map<String,CHModCompat> MODS = new HashMap<>();
    private static boolean hasInit = false;

    private static void loadMods(){
        if (!hasInit){
            MOD_SUPPLIERS.clear();

            addMod(ModIds.IceAndFire.ModId, IceAndFire::new);
            addMod(ModIds.PNEUMATICCRAFT.ModId, Pneumaticcraft::new);
            addMod(ModIds.COLD_SWEAT.ModId, ColdSweat::new);
            addMod(ModIds.ANVIL_CRAFT.ModId, AnvilCraft::new);

            MOD_SUPPLIERS.forEach(Mods::intiMod);

            hasInit = true;
        }

    }

    public static void init(IEventBus eventBus){
        loadMods();

        executeIfLoad(chModCompat->chModCompat.init(eventBus));

        eventBus.addListener(Mods::registerBoilerHeater);
    }

    public static void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER){
        loadMods();
        executeIfLoad(chModCompat-> chModCompat.registerPonder(HELPER));
    }

    private static void addMod(String modId,Supplier<CHModCompat> modCompatSupplier){
        if (isModLoad(modId)){
            MOD_SUPPLIERS.put(modId,modCompatSupplier);
        }
    }

    /**
     * Not check if mod is loaded
     */
    private static void intiMod(String modId,Supplier<CHModCompat> modCompatSupplier){
        MODS.put(modId,modCompatSupplier.get());
    }

    private static boolean isModLoad(String modId){
        return ModList.get().isLoaded(modId);
    }

    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event, ResourceKey<CreativeModeTab> tabKey, ItemLike... itemLikes){
        if (event.getTabKey().equals(tabKey)){
            Arrays.stream(itemLikes).forEach(event::accept);
        }
    }

    public static <T> Consumer<DataPackRegistryEvent.NewRegistry> registerDatapack(ResourceKey<Registry<T>> key, Codec<T> codec) {
       return event ->  event.dataPackRegistry(key,codec,codec);
    }

    private static void registerBoilerHeater(FMLCommonSetupEvent event){
        event.enqueueWork(()->{
            executeIfLoad(CHModCompat::registerBoilerHeater);
        });
    }

    private static void executeIfLoad(Consumer<CHModCompat> runnable){
        for (Map.Entry<String,CHModCompat> entry : MODS.entrySet()){
            if (ModList.get().isLoaded(entry.getKey())){
               runnable.accept(entry.getValue());
            }
        }
    }

    public static void executeIfModLoad(String modTd,Consumer<CHModCompat> runnable){
        if (MODS.containsKey(modTd)){
            runnable.accept(MODS.get(modTd));
        }
    }

    public enum ModIds{
        IceAndFire("iceandfire"),
        PNEUMATICCRAFT("pneumaticcraft"),
        COLD_SWEAT("cold_sweat"),
        ANVIL_CRAFT("anvilcraft");
        public final String ModId;

        ModIds(String modId) {
            ModId = modId;
        }
    }
}
