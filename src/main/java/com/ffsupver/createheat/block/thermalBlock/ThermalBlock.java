package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class ThermalBlock extends Block implements IBE<ThermalBlockEntity>, IWrenchable {

    public ThermalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        withBlockEntityDo(level,pos,ThermalBlockEntity::checkNeighbour);
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.getBlock().equals(newState.getBlock())){
            withBlockEntityDo(level,pos,ThermalBlockEntity::destroy);
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }


    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (state.getValue(HEAT_LEVEL)){
            case NONE -> 0;
            case SMOULDERING -> 15;
            case FADING -> 15;
            case KINDLED -> 15;
            case SEETHING -> 8;
        };
    }

    @Override
    public Class<ThermalBlockEntity> getBlockEntityClass() {
        return ThermalBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThermalBlockEntity> getBlockEntityType() {
        return CHBlocks.THERMAL_BLOCK_ENTITY.get();
    }
}
