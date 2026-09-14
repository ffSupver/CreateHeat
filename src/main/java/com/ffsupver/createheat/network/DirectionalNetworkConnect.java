package com.ffsupver.createheat.network;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface DirectionalNetworkConnect {
    /**
     * Can this block connect to other block in this direction?
     * @param thisState this block state
     * @param direction from this block to other block
     * @return
     */
    boolean canDirectionConnect(BlockState thisState, Direction direction);
}
