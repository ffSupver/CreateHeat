package com.ffsupver.createheat.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public abstract class HeatTransferProcesser {
    /** Should HeatTransferProcesser work
     *
     * @param level Level for checking
     * @param pos Pos to check.
     * @param face Face from Thermal Block to check pos. If is null, face should be regarded as the right face.
     * @return need to be heat or not
     */
    public abstract boolean needHeat(Level level, BlockPos pos, @Nullable Direction face);
    public abstract void acceptHeat(Level level, BlockPos hTPPos,int heatProvide);
    public abstract boolean shouldProcessEveryTick();
}
