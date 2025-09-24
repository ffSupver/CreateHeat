package com.ffsupver.createheat.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ffsupver.createheat.CreateHeat.MODID;
import static com.ffsupver.createheat.registries.CHBlocks.THERMAL_BLOCK;
import static com.ffsupver.createheat.registries.CHBlocks.TIGHT_COMPRESSED_STONE;
import static com.ffsupver.createheat.registries.CHItems.THERMAL_TOOL;

public class CHCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("create_heat",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.createheat"))
                .withTabsBefore(CreativeModeTabs.COMBAT)
                .icon(()-> THERMAL_BLOCK.asStack())
            .displayItems((parameters, output) -> {
                output.accept(THERMAL_TOOL);
                output.accept(THERMAL_BLOCK);
                output.accept(TIGHT_COMPRESSED_STONE);
            })
            .build());



    public static void register(IEventBus modEventBus){
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
