package com.ffsupver.createheat.compat.iceAndFire;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlock;
import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlockEntity;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.registries.CHCreativeTab;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class IceAndFire {
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



    public static void init(IEventBus eventBus) {
        eventBus.addListener(IceAndFire::addItemsToCreativeTab);
        eventBus.addListener(IceAndFire::registerBoilerHeater);
    }

    private static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event){
        if (event.getTabKey().equals(CHCreativeTab.MAIN_TAB.getKey())){
            event.accept(DRAGON_FIRE_INPUT);
        }
    }

    private static void registerBoilerHeater(FMLCommonSetupEvent event){
        event.enqueueWork(()->
                BoilerHeater.REGISTRY.register(DRAGON_FIRE_INPUT.get(), HeatProvider.HEATER)
        );
    }
}
