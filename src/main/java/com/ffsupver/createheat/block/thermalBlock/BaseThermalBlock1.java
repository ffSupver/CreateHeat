package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.network.HeatService;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.util.BlockUtil;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.NONE;

public class BaseThermalBlock1 extends Block implements IBE<BaseThermalBlockEntity1>, IWrenchable {
    public BaseThermalBlock1(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (state.getValue(HEAT_LEVEL)){
            case NONE -> 0;
            case SMOULDERING, FADING, KINDLED -> 15;
            case SEETHING -> 12;
        };
    }

    /**
     * Will NOT be called when assembled by Sable
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        System.out.println("onPlace"+pos+" old:"+oldState+" new:"+state);
        super.onPlace(state, level, pos, oldState, movedByPiston);
        withBlockEntityDo(level,pos,baseThermalBlockEntity -> {
            UUID networkId = baseThermalBlockEntity.getNeighborNetworkId();
            baseThermalBlockEntity.setHeatNetworkId(HeatService.addBlockToNetwork(pos,level,networkId));
        });
    }

    /**
     * Will be called when assembled by Sable
     */
    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        System.out.println("onRemove"+pos+" old:"+oldState+" new:"+newState);
        if (!oldState.getBlock().equals(newState.getBlock())){ // destroy
            withBlockEntityDo(level,pos,baseThermalBlockEntity -> {
                if (baseThermalBlockEntity.getHeatNetworkId() != null){
                    HeatService.removeBlockFromNetwork(baseThermalBlockEntity.getLevel(),pos,baseThermalBlockEntity.getHeatNetworkId());
                }
            });
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    @Override
    public Class<BaseThermalBlockEntity1> getBlockEntityClass() {
        return BaseThermalBlockEntity1.class;
    }

    @Override
    public BlockEntityType<? extends BaseThermalBlockEntity1> getBlockEntityType() {
        return CHBlocks.THERMAL_BLOCK_ENTITY1.get();
    }
}
