package com.ffsupver.createheat.mixin;

import com.ffsupver.createheat.block.thermalBlock.CopycatThermalBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(
            method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z",
        at = @At(value = "HEAD"),
            cancellable = true)
    private static void copycatMenuValid$stillValid(ContainerLevelAccess access, Player player, Block targetBlock, CallbackInfoReturnable<Boolean> cir){
        if (access.evaluate(
                (l, p)-> {
                    if (l.getBlockState(p).getBlock() instanceof CopycatThermalBlock) {
                        BlockState material = CopycatThermalBlock.getMaterial(l, p);
                        return material.is(targetBlock);
                    }
                    return false;
                },
                false
        )){
            cir.setReturnValue(true);
        }
    }
}
