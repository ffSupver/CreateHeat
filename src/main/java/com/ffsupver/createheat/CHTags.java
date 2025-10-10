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
    }
}
