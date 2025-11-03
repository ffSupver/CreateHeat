package com.ffsupver.createheat.mixin.anvilCraft;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.ffsupver.createheat.registries.CHBlocks.SMART_THERMAL_BLOCK;
import static com.ffsupver.createheat.registries.CHBlocks.THERMAL_BLOCK;
import static com.simibubi.create.AllBlocks.BLAZE_BURNER;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING;
import static dev.dubhe.anvilcraft.init.block.ModBlocks.HEATER;

@Mixin(AbstractProcessRecipe.Property.class)
public class AbstractProcessRecipeMixin {

    @Shadow
    private List<BlockStatePredicate> inputBlocks;
    /**让导热块和烈焰人燃烧室代替加热器
    */
    @Inject(
            method = "setInputBlocks(Ljava/util/List;)Ldev/dubhe/anvilcraft/recipe/anvil/wrap/AbstractProcessRecipe$Property;",
        at = @At(value = "RETURN")
    )
    public void addThAndBlAsHeater$setInputBlocks(List<BlockStatePredicate> inputBlocks, CallbackInfoReturnable<AbstractProcessRecipe.Property> cir) {
        if (inputBlocks.size() != 1){ //只修改只有一个加热器的情况
            return;
        }

        boolean testingHeaters = inputBlocks.stream().anyMatch(blockStatePredicate -> {
            HolderSet<Block> blockHolderSet = blockStatePredicate.getBlocks();
           boolean hasHeater = blockHolderSet.size() == 1 && blockHolderSet.get(0).is(HEATER.getKey()); //只有一个加热器的情况
           boolean needNotOverLoad = blockStatePredicate.getProperties().stream().anyMatch(
                   pMs->pMs.stream().anyMatch(pM-> pM.name().equals(HeaterBlock.OVERLOAD.getName()) && pM.valueMatcher() instanceof BlockStatePredicate.ExactMatcher(
                           String value
                   ) && value.equals("false"))
           );
            return hasHeater && needNotOverLoad;
        });

        if (testingHeaters){
            this.inputBlocks = List.of(
                    BlockStatePredicate.builder().of(
                            HEATER.get(),
                            BLAZE_BURNER.get(),
                            THERMAL_BLOCK.get(),SMART_THERMAL_BLOCK.get()
                    ).with(HEAT_LEVEL, SEETHING).or().with(HeaterBlock.OVERLOAD,false).build()
            );
        }
    }

}