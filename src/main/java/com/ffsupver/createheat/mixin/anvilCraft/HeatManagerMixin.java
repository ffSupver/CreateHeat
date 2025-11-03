package com.ffsupver.createheat.mixin.anvilCraft;

import com.ffsupver.createheat.compat.anvilCraft.HeatProducerBoilHeater;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(HeaterManager.class)
public class HeatManagerMixin {
    @Inject(method = "tick",at = @At(value = "INVOKE", target = "Ljava/util/Set;addAll(Ljava/util/Collection;)Z",ordinal = 1))
    public void heatableBlocksAccessor$tick(CallbackInfo ci, @Local Map<BlockPos, HeatTierLine.Point> heatableBlocks){//加热CAN_HEAT_THROUGH
        HeatProducerBoilHeater.updateHeatableBlockMap(heatableBlocks);
    }
}
