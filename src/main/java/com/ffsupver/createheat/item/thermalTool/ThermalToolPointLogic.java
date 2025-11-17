package com.ffsupver.createheat.item.thermalTool;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.BiConsumer;

public enum ThermalToolPointLogic {
    FLUID_TANK((lLevel,lPos)->{
        IFluidHandler iFH = lLevel.getCapability(Capabilities.FluidHandler.BLOCK,lPos,null);
        if (iFH != null) {
            iFH.fill(new FluidStack(Fluids.WATER,90), IFluidHandler.FluidAction.EXECUTE);
        }
    },(level, pos) -> null,ThermalToolPointType.WATER),
    SUPER_HEAT_SOURCE((lLevel,lPos)->{},(level, pos) -> null,ThermalToolPointType.SUPER_HEAT),
    HEAT_SOURCE((lLevel,lPos)->{},(level, pos) -> SUPER_HEAT_SOURCE,ThermalToolPointType.HEAT);

    private final BiConsumer<ServerLevel, BlockPos> tick;
    private final NextLogic nextLogic;
    private final ThermalToolPointType type;

    ThermalToolPointLogic(BiConsumer<ServerLevel, BlockPos> tick,NextLogic nextLogic,ThermalToolPointType type) {
        this.tick = tick;
        this.type = type;
        this.nextLogic = nextLogic;
    }

    public void tick(ServerLevel level,BlockPos pos){
        tick.accept(level,pos);
    }
    public ThermalToolPointLogic nextLogic(ServerLevel level,BlockPos pos){return nextLogic.next(level,pos);}

    public ThermalToolPointType getType() {
        return type;
    }

    public CompoundTag toNbt(){
        CompoundTag tag = new CompoundTag();
        tag.putString("logic",this.name());
        return tag;
    }
    public static ThermalToolPointLogic fromNbt(Tag tag){
        return fromNbt((CompoundTag) tag);
    }

    public static ThermalToolPointLogic fromNbt(CompoundTag tag){
        String name = tag.getString("logic");
        return ThermalToolPointLogic.valueOf(name);
    }

    @FunctionalInterface
    interface NextLogic{
       ThermalToolPointLogic next(ServerLevel level,BlockPos pos);
    }
}
