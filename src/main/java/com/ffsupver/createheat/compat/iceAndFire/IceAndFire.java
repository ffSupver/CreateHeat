package com.ffsupver.createheat.compat.iceAndFire;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.api.iceAndFire.DragonHeater;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlock;
import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlockEntity;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.compat.ponder.scenes.iceAndFire.DragonFireInputScenes;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.registries.CHCreativeTab;
import com.ffsupver.createheat.registries.CHDatapacks;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public class IceAndFire implements CHModCompat {
    private static final CreateRegistrate REGISTRATE = CreateHeat.registrate();
    public static final BlockEntry<DragonFireInputBlock> DRAGON_FIRE_INPUT = REGISTRATE
            .block("dragon_fire_input",DragonFireInputBlock::new)
            .properties(properties -> BlockBehaviour.Properties.ofFullCopy(CHBlocks.THERMAL_BLOCK.get()))
            .item()
            .build()
            .register();
    public static final BlockEntityEntry<DragonFireInputBlockEntity> DRAGON_FIRE_INPUT_BLOCK_ENTITY = REGISTRATE
            .blockEntity("dragon_fire_input", DragonFireInputBlockEntity::new)
            .validBlock(DRAGON_FIRE_INPUT)
            .register();


    public static final ResourceKey<Registry<DragonHeater>> DRAGON_HEATER = CHDatapacks.key("dragon_heater");

    @Override
    public String getModId() {
        return Mods.ModIds.ICE_AND_FIRE.ModId;
    }

    public void init(IEventBus eventBus) {
        eventBus.addListener(IceAndFire::addItemsToCreativeTabA);
        eventBus.addListener(IceAndFire::registerDatapack);
    }

    private static void addItemsToCreativeTabA(BuildCreativeModeTabContentsEvent event){
        Mods.addItemsToCreativeTab(event,CHCreativeTab.MAIN_TAB.getKey(),DRAGON_FIRE_INPUT);
    }

    private static void registerDatapack(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(DRAGON_HEATER,DragonHeater.CODEC,DragonHeater.CODEC);
    }

    @Override
    public void registerBoilerHeater() {
        BoilerHeater.REGISTRY.register(DRAGON_FIRE_INPUT.get(), HeatProvider.HEATER);
    }

    @Override
    public void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER) {
        HELPER.forComponents(DRAGON_FIRE_INPUT)
                .addStoryBoard("ice_and_fire/use", DragonFireInputScenes::use);
    }
}
