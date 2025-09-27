package com.ffsupver.createheat.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RecipeBuildHelper {
    public static <C extends Recipe<?>> RecipeType<C> recipeType(String id){
        return new RecipeType<>() {
        };
    }
    public static <C extends Recipe<?>> RecipeSerializer<C> recipeSerializer(
            MapCodec<C> codec,
            Function<RegistryFriendlyByteBuf,C> fromNetWork,
            BiConsumer<RegistryFriendlyByteBuf,C> toNetWork
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
                return fromNetWork.apply(byteBuf);
            }
            private void toNetwork(RegistryFriendlyByteBuf byteBuf,C recipe){
                 toNetWork.accept(byteBuf,recipe);
            }
        };
    }

}
