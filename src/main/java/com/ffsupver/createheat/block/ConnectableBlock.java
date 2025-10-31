package com.ffsupver.createheat.block;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ConnectableBlock<T extends ConnectableBlockEntity<T>,C extends T> extends Block implements IBE<C> {
    public ConnectableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        withBlockEntityDo(level,pos, T::checkNeighbour);
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.getBlock().equals(newState.getBlock())){
            withBlockEntityDo(level,pos, T::destroy);
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
