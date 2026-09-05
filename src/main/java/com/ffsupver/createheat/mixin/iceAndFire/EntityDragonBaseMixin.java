package com.ffsupver.createheat.mixin.iceAndFire;

import com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlockEntity;
import com.ffsupver.createheat.compat.Mods;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

import static com.ffsupver.createheat.util.BlockUtil.isInDistance;

@Mixin(DragonBaseEntity.class)
public abstract class EntityDragonBaseMixin extends Mob {
    @Shadow
    public BlockPos burningTarget;

    @Shadow
    protected abstract void breathFireAtPos(BlockPos blockPos);

    @Shadow
    public abstract void setBreathingFire(boolean breathing);

    @Shadow
    public abstract boolean canPositionBeSeen(double x, double y, double z);

    protected EntityDragonBaseMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }


    @Inject(method = "updateBurnTarget",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",shift = At.Shift.AFTER),remap = false, cancellable = true)
    private void breathWhenTargeted$updateBurnTargetMixin(CallbackInfo ci, @Local(name = "maxDist") float maxDist){
        BlockEntity blockEntity = this.level().getBlockEntity(burningTarget);
        if (blockEntity instanceof DragonFireInputBlockEntity) {
            this.getLookControl().setLookAt((double)this.burningTarget.getX() + (double)0.5F, (double)this.burningTarget.getY() + (double)0.5F, (double)this.burningTarget.getZ() + (double)0.5F, 180.0F, 180.0F);
            Set<BlockPos> logicalPosSet = Mods.collectGlobalBlockPos(level(),burningTarget);
            BlockPos closestPos = burningTarget;
            BlockPos dragonPos = new BlockPos(getBlockX(), getBlockY(), getBlockZ());
            double closestDistSqr = closestPos.distSqr(dragonPos);
            for (BlockPos pos : logicalPosSet){
                double distSqr = pos.distSqr(dragonPos);
                if (closestDistSqr >= distSqr){
                    closestPos = pos;
                    closestDistSqr = distSqr;
                }
            }


            Vec3 closestCenter = closestPos.getCenter();
            if (closestDistSqr < maxDist && canPositionBeSeen(closestCenter.x, closestCenter.y, closestCenter.z)){
                this.breathFireAtPos(closestPos);
                this.setBreathingFire(true);
            }
            this.burningTarget = null;
            ci.cancel();
        }
    }
    @Inject(method = "canPositionBeSeen",at = @At(value = "TAIL"), cancellable = true)
    private void breathWhenTargeted$canPositionBeSeenMixin(double x, double y, double z, CallbackInfoReturnable<Boolean> cir, @Local(name = "result") HitResult result){
        if (!cir.getReturnValue() && isInDistance(result.getLocation(), new Vec3(x, y, z),level(),2.0D)){
            cir.setReturnValue(true);
        }
    }
}
