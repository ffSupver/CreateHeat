package com.ffsupver.createheat.compat.anvilCraft;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import dev.dubhe.anvilcraft.block.entity.HeatCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class HeatCollectorTransferProcesser extends HeatTransferProcesser {
    public static final ResourceLocation TYPE = CreateHeat.asResource("anvil_craft_heat_collector");

    private final static int MAX_COOLDOWN = 4; //要是2的倍数否则可能导致余数丢失
    private int coolDown = 0;
    private int totalPerCoolDown = 0;
    private int avgHeatPerTick;
    protected HeatCollectorTransferProcesser() {
        super(TYPE);
    }

    @Override
    public boolean needHeat(Level level, BlockPos pos, @Nullable Direction face) {
        return level.getBlockEntity(pos) instanceof HeatCollectorBlockEntity;
    }

    @Override
    public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide, int tickSkip) {
        if (level.getBlockEntity(hTPPos) instanceof HeatCollectorBlockEntity heatCollectorBlockEntity){
            boolean shouldAddHeat = acceptHeatToTotal(heatProvide,tickSkip);
            int superHeatCount = avgHeatPerTick / Config.HEAT_PER_SEETHING_BLAZE.get();
            if (shouldAddHeat){
                int heatToInput = superHeatCount * MAX_COOLDOWN / 2;
                heatCollectorBlockEntity.inputtingHeat(heatToInput);
            }
        }
    }

    private boolean acceptHeatToTotal(int heatProvide, int tickSkip){
        coolDown+= tickSkip;
        totalPerCoolDown += heatProvide;
        if (coolDown >= MAX_COOLDOWN){
            int actuallyTick = coolDown;
            coolDown = 0;
            avgHeatPerTick = totalPerCoolDown / actuallyTick;
            totalPerCoolDown = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldProcessEveryTick() {
        return true;
    }
}
