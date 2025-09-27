package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.recipe.HeatRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.ffsupver.createheat.CreateHeat.MODID;

public class CHRecipes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPE = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static DeferredHolder<RecipeType<? extends Recipe<?>>, RecipeType<HeatRecipe>> HEAT_RECIPE = registerRecipe(HeatRecipe.ID,()->HeatRecipe.TYPE,()->HeatRecipe.SERIALIZER);
    public static <T extends Recipe<?>> DeferredHolder<RecipeType<? extends Recipe<?>>, RecipeType<T>> registerRecipe(
            String id, Supplier<RecipeType<T>> recipeTypeSupplier,Supplier<RecipeSerializer<T>> recipeSerializerSupplier
    ){
       RECIPE_SERIALIZER.register(id,recipeSerializerSupplier);
        return RECIPE_TYPE.register(id ,recipeTypeSupplier);
    }

    public static void register(IEventBus modBus){
        RECIPE_TYPE.register(modBus);
        RECIPE_SERIALIZER.register(modBus);
    }

}
