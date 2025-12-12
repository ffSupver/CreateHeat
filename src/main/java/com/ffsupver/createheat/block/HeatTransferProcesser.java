package com.ffsupver.createheat.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public abstract class HeatTransferProcesser {
    private final ResourceLocation typeId;

    protected HeatTransferProcesser(ResourceLocation typeId) {
        this.typeId = typeId;
    }

    /** Should HeatTransferProcesser work
     *
     * @param level Level for checking
     * @param pos Pos to check.
     * @param face Face from Thermal Block to check pos. If is null, face should be regarded as the right face.
     * @param heat heat provided this check
     * @param tickSkip tick skipped from last check
     * @param superHeatCount super heat count
     * @return need to be heat or not
     */
    public abstract boolean needHeat(Level level, BlockPos pos, @Nullable Direction face,int heat,int tickSkip,int superHeatCount);
    public void acceptHeat(Level level, BlockPos hTPPos,HeatAcceptData heatAcceptData){
        acceptHeat(level,hTPPos,heatAcceptData.heatProvide(),heatAcceptData.tickSkip(),heatAcceptData.superHeatCount());
    }
    public abstract void acceptHeat(Level level, BlockPos hTPPos,int heatProvide,int tickSkip,int superHeatCount);
    public abstract boolean shouldProcessEveryTick();
    public boolean shouldHeatAt(Direction face){return true;}
    public boolean shouldWriteAndReadFromNbt(){return false;}
    public CompoundTag toNbt(){
        return null;
    }
    public void fromNbt(CompoundTag nbt){}

    public ResourceLocation getTypeId() {
        return typeId;
    }

    public void onControllerRemove(){}

    public record HeatAcceptData(int heatProvide, int tickSkip,int superHeatCount){}
}
