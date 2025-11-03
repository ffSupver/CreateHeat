package com.ffsupver.createheat.compat.anvilCraft;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.boiler.BoilerHeater;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class HeatProducerBoilHeater implements BoilerHeater {
    public static TagKey<Block> BLOCK_TAG = CHTags.BlockTag.CAN_HEAT_THROUGH;
    private static Map<BlockPos, HeatTierLine.Point> heatableBlocksMapCached = Map.of();

    public static void updateHeatableBlockMap(Map<BlockPos, HeatTierLine.Point> newMap){
        heatableBlocksMapCached = newMap;
    }

    @Override
    public float getHeat(Level level, BlockPos pos, BlockState state) {
        BlockPos posUp = pos.above();
        if (!(canHeatBlock(level,posUp))){
            return NO_HEAT;
        }
        if (!state.is(BLOCK_TAG) || !heatableBlocksMapCached.containsKey(posUp)) {
            return NO_HEAT;
        }
        HeatTierLine.Point point = heatableBlocksMapCached.get(posUp);
        return switch (point.tier()){
            case NORMAL -> NO_HEAT;
            case HEATED -> 0;
            case REDHOT -> 1;
            case GLOWING -> 2;
            case INCANDESCENT -> 3;
            case OVERHEATED -> 4;
        };
    }

    public static boolean canHeatBlock(Level level,BlockPos pos){
        BlockState state = level.getBlockState(pos);
        boolean isFluidTank = state.is(AllBlocks.FLUID_TANK.get());
        boolean isThermalBlock = (level.getBlockEntity(pos) instanceof ConnectableBlockEntity<?> c && ThermalBlockEntityBehaviour.getFromCBE(c) != null);
        return isFluidTank || isThermalBlock;
    }

    public static boolean shouldUpdateBoiler(BlockPos posBelowBoiler, ServerLevel level){
        BlockState state = level.getBlockState(posBelowBoiler);
        return state.is(BLOCK_TAG);
    }
}