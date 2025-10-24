package com.ffsupver.createheat.recipe;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.ffsupver.createheat.registries.CHRecipes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.ffsupver.createheat.util.BlockUtil.AllDirectionOf;

public class HeatRecipeTransferProcesser extends HeatTransferProcesser {
    public static final ResourceLocation TYPE = CreateHeat.asResource("heat_recipe");
    private boolean finished = false;
    private boolean removed = false;
    private boolean isMainProcesser = false;
    private int acceptedHeat = 0;
    private int totalHeat = 0;
    private BlockPos fr = null;
    private HeatRecipeTransferProcesser mainProcesser = null;

    public HeatRecipeTransferProcesser() {
        super(TYPE);
    }

    @Override
    public boolean needHeat(Level level, BlockPos checkPos, @Nullable Direction face) {
        Optional<RecipeHolder<HeatRecipe>> optionalRH = getRecipe(level,checkPos);
        if (optionalRH.isPresent()){
            if (face == null){
                return !finished;
            }

            ConnectableBlockEntity<?> thBlockAttach = getAttachThermalBlock(level,checkPos,face);

            AtomicReference<Optional<HeatTransferProcesser>> oHRP = new AtomicReference<>(Optional.empty());
            AllDirectionOf(checkPos,
                    (checkControllerPos,f)-> {
                        if (!f.equals(face.getOpposite()) && level.getBlockEntity(checkControllerPos) instanceof ConnectableBlockEntity<?> connectableBlockEntity) {
                            ThermalBlockEntityBehaviour otherTE = BlockEntityBehaviour.get(connectableBlockEntity,ThermalBlockEntityBehaviour.TYPE);
                            if (otherTE != null && !connectableBlockEntity.getControllerPos().equals(thBlockAttach.getControllerPos())) {
                                Optional<HeatTransferProcesser> otherHTP = otherTE.getHeatTransferProcesserByOther(checkPos);
                                if (otherHTP.isPresent() && otherHTP.get() instanceof HeatRecipeTransferProcesser hRTP && hRTP.isMainProcesser){
                                    oHRP.set(otherHTP);
                                }
                            }
                        }
                    },
                    //找到mainProcesser就终止
                    c-> oHRP.get().isPresent() && oHRP.get().get() instanceof HeatRecipeTransferProcesser hRTP && hRTP.isMainProcesser
            );

            if (oHRP.get().isEmpty()){
                this.isMainProcesser = true;
                this.mainProcesser = null;
            }else if (oHRP.get().get() instanceof HeatRecipeTransferProcesser mainRecipeP){
                this.mainProcesser = mainRecipeP.isMainProcesser ? mainRecipeP : mainRecipeP.mainProcesser;
            }

            this.fr = checkPos.relative(face.getOpposite());

            return true;
        }else {
            return false;
        }
    }

    @Override
    public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide,int tickSkip) {
        if (isMainProcesser){
            acceptHeatAsMainP(level,hTPPos,heatProvide,tickSkip);
        }else {
            if (mainProcesser.removed){
                //转换到新的mainProcesser
                if (mainProcesser.mainProcesser == null){
                    mainProcesser.switchToNew(this);
                    acceptHeatAsMainP(level,hTPPos,heatProvide,tickSkip);
                }else {
                    mainProcesser = mainProcesser.mainProcesser;
                    mainProcesser.mainAcceptHeat(heatProvide);
                }
            }else {
                mainProcesser.mainAcceptHeat(heatProvide);
            }
        }
    }

    private void mainAcceptHeat(int heat){
        this.acceptedHeat += heat;
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
    public void onControllerRemove() {
        this.removed = true;
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

    private void acceptHeatAsMainP(Level level, BlockPos hTPPos, int heatProvide, int tickSkip){
        Optional<RecipeHolder<HeatRecipe>> optionalRH =  getRecipe(level,hTPPos);
        if (optionalRH.isPresent()){
            HeatRecipe recipe = optionalRH.get().value();
            acceptedHeat += heatProvide;
            if (acceptedHeat >= recipe.getMinHeatPerTick() * tickSkip){
                totalHeat += acceptedHeat;
                if (totalHeat >= recipe.getHeatCost()){
                    doneRecipe(level,recipe,hTPPos);
                }
            }
        }
        acceptedHeat = 0;
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

    private void switchToNew(HeatRecipeTransferProcesser newTransferP){
        this.mainProcesser = newTransferP;
        newTransferP.isMainProcesser = true;
        newTransferP.mainProcesser = null;
        newTransferP.totalHeat = this.totalHeat;
        newTransferP.acceptedHeat = this.acceptedHeat;
    }

    private static ConnectableBlockEntity<?> getAttachThermalBlock(Level level, BlockPos transferProcesserPos, Direction face){
        BlockPos thBlockPos = transferProcesserPos.relative(face.getOpposite());
        if (level.getBlockEntity(thBlockPos) instanceof ConnectableBlockEntity<?> connectableBlockEntity &&
                BlockEntityBehaviour.get(connectableBlockEntity,ThermalBlockEntityBehaviour.TYPE) != null){
            return connectableBlockEntity;
        }
        return null;
    }

    private static Optional<RecipeHolder<HeatRecipe>> getRecipe(Level level,BlockPos checkPos){
        BlockState checkState = level.getBlockState(checkPos);
        HeatRecipe.HeatRecipeTester tester = new HeatRecipe.HeatRecipeTester(checkState);
        return level.getRecipeManager().getRecipeFor(CHRecipes.HEAT_RECIPE.get(),tester,level);
    }
}
