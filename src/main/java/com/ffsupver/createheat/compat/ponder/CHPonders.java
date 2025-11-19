package com.ffsupver.createheat.compat.ponder;

import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.compat.ponder.scenes.ThermalBlockScene;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

import static com.ffsupver.createheat.registries.CHBlocks.*;

public class CHPonders {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper){
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.forComponents(THERMAL_BLOCK,SMART_THERMAL_BLOCK,COPYCAT_THERMAL_BLOCK)
                .addStoryBoard("thermal_block/use",ThermalBlockScene::use)
                .addStoryBoard("thermal_block/storage",ThermalBlockScene::storage)
                .addStoryBoard("thermal_block/recipe",ThermalBlockScene::recipe)
                .addStoryBoard("thermal_block/smart_thermal_block",ThermalBlockScene::smartThermalBlock);

        Mods.registerPonder(HELPER);
    }
}
