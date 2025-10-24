package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.block.tightCompressStone.ConnectableBlock;
import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public  class SmartThermalBlock extends ConnectableBlock<SmartThermalBlockEntity> implements IWrenchable {
    public SmartThermalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL);
    }

    @Override
    public Class<SmartThermalBlockEntity> getBlockEntityClass() {
        return SmartThermalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartThermalBlockEntity> getBlockEntityType() {
        return CHBlocks.SMART_THERMAL_BLOCK_ENTITY.get();
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
}
