package com.ffsupver.createheat.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class RecipeBuildHelper {
    public static <C extends Recipe<?>> RecipeType<C> recipeType(String id){
        return new RecipeType<>() {
        };
    }
    public static <C extends Recipe<?>> RecipeSerializer<C> recipeSerializer(
            MapCodec<C> codec
    ){
        return new RecipeSerializer<C>() {
            @Override
            public MapCodec<C> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, C> streamCodec() {
                return StreamCodec.of(this::toNetwork,this::fromNetwork);
            }

            private C fromNetwork(RegistryFriendlyByteBuf byteBuf){
               return byteBuf.readJsonWithCodec(codec.codec());
            }
            private void toNetwork(RegistryFriendlyByteBuf byteBuf,C recipe){
                byteBuf.writeJsonWithCodec(codec.codec(),recipe);
            }
        };
    }

}
