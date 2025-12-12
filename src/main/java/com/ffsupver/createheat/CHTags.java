package com.ffsupver.createheat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import static com.ffsupver.createheat.CreateHeat.MODID;

public class CHTags {
    public static <T>TagKey<T> tagOf(Registry<T> registry, String nameSpace, String path){
        return TagKey.create(registry.key(), ResourceLocation.fromNamespaceAndPath(nameSpace,path));
    }

    public static <T> TagKey<T> createHeatTag(Registry<T> registry,String path){
        return tagOf(registry,MODID,path);
    }

    public static TagKey<Block> cHBlockTag(String path){
        return createHeatTag(BuiltInRegistries.BLOCK,path);
    }

    public static void register(){}


    public static class BlockTag{
        public static TagKey<Block> SHOULD_HEAT = cHBlockTag("should_heat");
        public static TagKey<Block> CUSTOM_BOILER_HEATER = cHBlockTag("custom_boiler_heater");
        public static TagKey<Block> THERMAL_BLOCKS = cHBlockTag("thermal_blocks");
        public static TagKey<Block> COPYCAT_THERMAL_BLOCK_DENY = cHBlockTag("copycat_thermal_block_deny");
        public static TagKey<Block> HEAT_ENTITY_ABOVE = cHBlockTag("heat_entity_above");
        //铁砧工艺联动
        public static TagKey<Block> CAN_HEAT_THROUGH = cHBlockTag("anvilcraft/can_heat_through");
        //气动工艺联动
        public static TagKey<Block> PNEUMATIC_BOILER_HEATER = cHBlockTag("pneumaticcraft/boiler_heater");
    }
}
