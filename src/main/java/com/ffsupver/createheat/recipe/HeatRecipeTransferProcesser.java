package com.ffsupver.createheat.recipe;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.MainHeatTransferProcesser;
import com.ffsupver.createheat.registries.CHRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class HeatRecipeTransferProcesser extends MainHeatTransferProcesser {
    public static final ResourceLocation TYPE = CreateHeat.asResource("heat_recipe");
    private boolean finished = false;
    private int totalHeat = 0;

    public HeatRecipeTransferProcesser() {
        super(TYPE);
    }

    @Override
    protected boolean needHeatBefore(Level level, BlockPos pos, Direction face) {
        Optional<RecipeHolder<HeatRecipe>> optionalRH = getRecipe(level,pos);
        if (optionalRH.isPresent()){
            if (face == null){
                return !finished;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void acceptHeatAsMain(Level level, BlockPos hTPPos, int heatProvide, int tickSkip) {
        Optional<RecipeHolder<HeatRecipe>> optionalRH =  getRecipe(level,hTPPos);
        if (optionalRH.isPresent()){
            HeatRecipe recipe = optionalRH.get().value();
            if (heatProvide >= recipe.getMinHeatPerTick() * tickSkip){
                totalHeat += heatProvide;
                if (totalHeat >= recipe.getHeatCost()){
                    doneRecipe(level,recipe,hTPPos);
                }
            }
        }
    }

    @Override
    public boolean shouldProcessEveryTick() {
        return false;
    }

    @Override
    public boolean shouldWriteAndReadFromNbt() {
        return isMainProcesser;
    }

    @Override
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("total",totalHeat);
        nbt.putInt("accepted",acceptedHeat);
        nbt.putBoolean("is_main",isMainProcesser);
        return nbt;
    }

    @Override
    public void fromNbt(CompoundTag nbt) {
        totalHeat = nbt.getInt("total");
        acceptedHeat = nbt.getInt("accepted");
        isMainProcesser = nbt.getBoolean("is_main");
    }

    private void doneRecipe(Level level, HeatRecipe heatRecipe, BlockPos processPos) {
        BlockState outputState = heatRecipe.getOutputBlock();
        if (outputState.getFluidState().is(FluidTags.WATER) && level.dimensionType().ultraWarm()) {
            level.destroyBlock(processPos, false);
        } else {
            level.setBlock(processPos, heatRecipe.getOutputBlock(), 3);
        }
        this.finished = true;
    }


    private static Optional<RecipeHolder<HeatRecipe>> getRecipe(Level level,BlockPos checkPos){
        BlockState checkState = level.getBlockState(checkPos);
        HeatRecipe.HeatRecipeTester tester = new HeatRecipe.HeatRecipeTester(checkState);
        return level.getRecipeManager().getRecipeFor(CHRecipes.HEAT_RECIPE.get(),tester,level);
    }
}
