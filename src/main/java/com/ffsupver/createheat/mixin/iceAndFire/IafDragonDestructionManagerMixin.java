package com.ffsupver.createheat.mixin.iceAndFire;

import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlockEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IafDragonDestructionManager.class)
public class IafDragonDestructionManagerMixin {
    @Inject(method = "destroyAreaBreath",at = @At(value = "HEAD"))
    private static void onHitByFrameAccess$destroyAreaBreath(Level level, BlockPos center, DragonBaseEntity dragon, CallbackInfo ci){
        if (level.getBlockEntity(center) instanceof DragonFireInputBlockEntity dragonFireInputBlockEntity){
            dragonFireInputBlockEntity.onHitByFrame(dragon.getDragonStage());
        }
    }
    @Inject(method = "destroyAreaCharge",at = @At(value = "HEAD"))
    private static void onHitByFrameAccess$destroyAreaCharge(Level level, BlockPos center, DragonBaseEntity dragon, CallbackInfo ci){
        if (level.getBlockEntity(center) instanceof DragonFireInputBlockEntity dragonFireInputBlockEntity){
            dragonFireInputBlockEntity.onHitByFrame(dragon.getDragonStage());
        }
    }
}
