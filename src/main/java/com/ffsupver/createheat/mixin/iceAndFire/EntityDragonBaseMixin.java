package com.ffsupver.createheat.mixin.iceAndFire;

import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlockEntity;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonBaseEntity.class)
public abstract class EntityDragonBaseMixin extends Mob {
    @Shadow
    public BlockPos burningTarget;

    @Shadow
    protected abstract void breathFireAtPos(BlockPos blockPos);

    @Shadow
    public abstract void setBreathingFire(boolean breathing);

    protected EntityDragonBaseMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }


    @Inject(method = "updateBurnTarget",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",shift = At.Shift.AFTER),remap = false, cancellable = true)
    private void breathWhenTargeted$updateBurnTargetMixin(CallbackInfo ci){
        BlockEntity blockEntity = this.level().getBlockEntity(burningTarget);
        if (blockEntity instanceof DragonFireInputBlockEntity) {
            this.getLookControl().setLookAt((double)this.burningTarget.getX() + (double)0.5F, (double)this.burningTarget.getY() + (double)0.5F, (double)this.burningTarget.getZ() + (double)0.5F, 180.0F, 180.0F);
            this.breathFireAtPos(this.burningTarget);
            this.setBreathingFire(true);
            ci.cancel();
        }
    }
}
