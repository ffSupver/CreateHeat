package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public  class SmartThermalBlock extends BaseThermalBlock<SmartThermalBlockEntity> implements IWrenchable {
    public SmartThermalBlock(Properties properties) {
        super(properties, SmartThermalBlockEntity.class,()->CHBlocks.SMART_THERMAL_BLOCK_ENTITY.get());
    }


    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (state.getValue(HEAT_LEVEL)){
            case NONE -> 0;
            case SMOULDERING, FADING, KINDLED -> 13;
            case SEETHING -> 10;
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        ThermalBlock.addParticles(state,level,pos,random);
    }

    @Override
    public boolean isBurning(BlockState state, BlockGetter level, BlockPos pos) {
        SmartThermalBlockEntity smartThermalBlock = getBlockEntity(level,pos);
        return smartThermalBlock != null && smartThermalBlock.isBurning() && super.isBurning(state, level, pos);
    }
}
