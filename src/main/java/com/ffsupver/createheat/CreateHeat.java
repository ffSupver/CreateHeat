package com.ffsupver.createheat;

import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.registries.*;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(CreateHeat.MODID)
public class CreateHeat {
    public static final String MODID = "createheat";
    public static final Logger LOGGER = LogUtils.getLogger();


    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );



    public CreateHeat(IEventBus modEventBus, ModContainer modContainer) throws NoSuchFieldException, IllegalAccessException {
        CHCreativeTab.register(modEventBus);

        REGISTRATE.registerEventListeners(modEventBus);

        CHItems.register();
        CHBlocks.register();
        CHTags.register();
        CHRecipes.register(modEventBus);

        CHHeatTransferProcessers.bootSetup();

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (CreateHeat) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(CHDatapacks::register);
        modEventBus.addListener(CreateHeat::init);

        Mods.init(modEventBus);
    }

    private static void init(FMLCommonSetupEvent event){
        event.enqueueWork(()->{
            CHBlocks.registerBoilHeater();
        });
        NeoForge.EVENT_BUS.addListener(CHDatapacks::onDatapackReload);
    }



    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Create Heat Loading");
    }

    public static CreateRegistrate registrate(){
        return REGISTRATE;
    }
    public static ResourceLocation asResource(String path){
        return  ResourceLocation.fromNamespaceAndPath(MODID,path);
    }
}
