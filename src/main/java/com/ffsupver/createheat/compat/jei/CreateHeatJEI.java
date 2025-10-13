package com.ffsupver.createheat.compat.jei;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.compat.jei.category.HeatCategory;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.registries.CHItems;
import com.ffsupver.createheat.registries.CHRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
@JeiPlugin
public class CreateHeatJEI implements IModPlugin {
    public static ResourceLocation ID = CreateHeat.asResource("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new HeatCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(HeatCategory.TYPE,fillRecipes(CHRecipes.HEAT_RECIPE.get()));

        registration.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,List.of(CHItems.THERMAL_TOOL.asStack()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(CHBlocks.THERMAL_BLOCK.get(),HeatCategory.TYPE);
    }

    public static <T> mezz.jei.api.recipe.RecipeType<T> recipeType(String path, Class<T> recipeClass){
        return mezz.jei.api.recipe.RecipeType.create(CreateHeat.MODID,path,recipeClass);
    }

    public static <I extends RecipeInput,T extends Recipe<I>> List<RecipeHolder<T>> getAllRecipes(RecipeType<T> recipeType){
       return Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType);
    }

    private static <I extends RecipeInput,T extends Recipe<I>> List<T> fillRecipes(RecipeType<T> recipeType){
        return getAllRecipes(recipeType).stream().map(RecipeHolder::value).toList();
    }
}
