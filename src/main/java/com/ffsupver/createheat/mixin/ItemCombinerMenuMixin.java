package com.ffsupver.createheat.mixin;

import com.ffsupver.createheat.block.thermalBlock.CopycatThermalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Shadow
    protected abstract boolean isValidBlock(BlockState blockState);

    @Inject(method = "lambda$stillValid$1",at = @At(value = "HEAD"), cancellable = true)
    private void copycatMenuValid$stillValid(Player player, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir){
        if (level.getBlockState(pos).getBlock() instanceof CopycatThermalBlock) {
            BlockState material = CopycatThermalBlock.getMaterial(level, pos);
            cir.setReturnValue(isValidBlock(material));
        }
    }
}
