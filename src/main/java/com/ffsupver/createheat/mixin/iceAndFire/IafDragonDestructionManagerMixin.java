package com.ffsupver.createheat.mixin.iceAndFire;

import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.util.BlockUtil;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonDestructionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

import static com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlockEntity.hitDragonFireInput;

@Mixin(IafDragonDestructionManager.class)
public class IafDragonDestructionManagerMixin {
    @Inject(method = "destroyAreaBreath",at = @At(value = "HEAD"))
    private static void onHitByFrameAccess$destroyAreaBreath(Level level, BlockPos center, DragonBaseEntity dragon, CallbackInfo ci){
        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos centerPos : Mods.collectHitBlockPos(level,center)){
            hitDragonFireInput(level,dragon,centerPos);
            checked.add(centerPos);

            BlockUtil.AllDirectionOf(centerPos, (pos) -> {
                if (checked.contains(pos)){
                    return;
                }

                hitDragonFireInput(level, dragon, pos);
                checked.add(pos);
            });
        }
    }
    @Inject(method = "destroyAreaCharge",at = @At(value = "HEAD"))
    private static void onHitByFrameAccess$destroyAreaCharge(Level level, BlockPos center, DragonBaseEntity dragon, CallbackInfo ci){
        hitDragonFireInput(level, dragon, center);
    }
}
