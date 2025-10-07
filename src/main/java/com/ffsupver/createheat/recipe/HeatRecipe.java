package com.ffsupver.createheat.recipe;

import com.ffsupver.createheat.api.BlockStateTester;
import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class HeatRecipe implements Recipe<HeatRecipe.HeatRecipeTester>{
    private final BlockStateTester inputBlock;
    private final BlockState outputBlock;
    private final int heatCost;
    private final int minHeatPerTick;

    public final static String ID = "heat";
    public final static RecipeType<HeatRecipe> TYPE = RecipeBuildHelper.recipeType(ID);
    public final static RecipeSerializer<HeatRecipe> SERIALIZER = RecipeBuildHelper.recipeSerializer(fromJson());




    public HeatRecipe(BlockStateTester inputBlock, BlockState outputBlock, int heatCost, int minHeatPerTick) {
        this.inputBlock = inputBlock;
        this.outputBlock = outputBlock;
        this.heatCost = heatCost;
        this.minHeatPerTick = minHeatPerTick;
    }

    @Override
    public boolean matches(HeatRecipeTester heatRecipeTester, Level level) {
        return heatRecipeTester.checkState(inputBlock);
    }

    @Override
    public ItemStack assemble(HeatRecipeTester recipeInput, HolderLookup.Provider provider) {
        return outputBlock.getBlock().asItem().getDefaultInstance();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return outputBlock.getBlock().asItem().getDefaultInstance();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Input : "+this.inputBlock+" Output : "+this.outputBlock+" cost:"+heatCost+" min:"+minHeatPerTick;
    }

    public BlockState getOutputBlock() {
        return outputBlock;
    }

    public int getHeatCost() {
        return heatCost;
    }

    public int getMinHeatPerTick() {
        return minHeatPerTick;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        System.out.println("G "+inputBlock.toIngredient()+" ");
        return inputBlock.toIngredient();
    }

    private static MapCodec<HeatRecipe> fromJson(){
        return RecordCodecBuilder.mapCodec(p->{
        Products.P4<RecordCodecBuilder.Mu<HeatRecipe>, Integer,BlockStateTester,BlockState,Integer> p1 =  p.group(
                Codec.INT.fieldOf("heat").forGetter(h->h.heatCost),
                BlockStateTester.CODEC.fieldOf("input").forGetter(h->h.inputBlock),
                BlockState.CODEC.fieldOf("output").forGetter(h->h.outputBlock),
                Codec.INT.fieldOf("min_heat").forGetter(h->h.minHeatPerTick)
        );
        return p1.apply(p,(heat,input,output,min)->new HeatRecipe(input,output,heat,min));
        });
    }


    public static class HeatRecipeTester implements RecipeInput {
        private final BlockState state;

        public HeatRecipeTester(BlockState state) {
            this.state = state;
        }

        public boolean checkState(BlockStateTester tester) {
           return tester.test(state);
        }

        @Override
        public ItemStack getItem(int i) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return state.isAir();
        }
    }
}
