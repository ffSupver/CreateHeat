package com.ffsupver.createheat.mixin.anvilCraft;

import dev.dubhe.anvilcraft.block.item.HeliostatsItem;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeliostatsItem.class)
public class HeliostatsItemMixin {
    @Redirect(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            )
    )
    private boolean addLightCanPassBlockToHeliostatsTarget$useOn(BlockState instance, Block block){ //手持定日镜可以选中流体储罐
        return instance.is(block) || instance.is(ModBlockTags.HEATABLE_BLOCKS);
    }
}
