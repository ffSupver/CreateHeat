package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ThermalPipeBlockItem extends BlockItem {
    public ThermalPipeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clickPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos checkPos = clickPos.relative(clickedFace);
        Level level = context.getLevel();
        BlockState checkState = level.getBlockState(checkPos);
        if (checkState.is(CHTags.BlockTag.THERMAL_PIPE_CONNECT) || checkState.getBlock() instanceof ThermalPipeBlock){
            BlockState clickState = level.getBlockState(clickPos);
            if (clickState.getBlock() instanceof ThermalPipeBlock thermalPipeBlock){
                thermalPipeBlock.updateNetworkConnect(level,clickPos, clickState, clickedFace,false);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState original = super.getPlacementState(context);
        if (original == null){
            return null;
        }

        Direction direction = context.getClickedFace();
        BlockPos placePos = context.getClickedPos();
        BlockPos clickPos = placePos.relative(direction.getOpposite());
        Level level = context.getLevel();
        Set<Direction> shouldConnect = new HashSet<>();

        BlockState clickState = level.getBlockState(clickPos);
        if (clickState.is(CHTags.BlockTag.THERMAL_PIPE_CONNECT) || clickState.getBlock() instanceof ThermalPipeBlock){
            shouldConnect.add(direction.getOpposite());
        }

        return ThermalPipeBlock.setDirectionConnect(original, shouldConnect);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        // 1. 先走原版放置流程（内部会调用 getPlacementState）
        InteractionResult result = super.place(context);
        if (!result.consumesAction()) {
            return result;
        }

        Level level = context.getLevel();
        // 只在服务端更新邻居，客户端由服务端同步
        if (level.isClientSide) {
            return result;
        }

        BlockPos pos = context.getClickedPos();
        BlockState placedState = level.getBlockState(pos);
        if (!(placedState.getBlock() instanceof ThermalPipeBlock)) {
            return result;
        }

        // 2. 依据“已放置方块”的连接位，反向通知相邻管道
        BlockUtil.AllDirectionOf(pos, (neighborPos, faceFromPos) -> {
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof ThermalPipeBlock thermalPipeBlock)) {
                return;
            }

            // faceFromPos 是“从 pos 指向 neighborPos”的方向
            // 如果新管道在该方向没有连接，就不需要通知邻居
            if (!thermalPipeBlock.canDirectionConnect(placedState,faceFromPos)) {
                return;
            }

            // 邻居需要连接的方向与 faceFromPos 相反
            Direction faceFromNeighbor = faceFromPos.getOpposite();
            BlockState newNeighborState = ThermalPipeBlock.setDirectionConnect(neighborState,faceFromNeighbor);

            // 仅在状态真正发生变化时才写回，避免无谓的方块更新
            if (newNeighborState != neighborState) {
                level.setBlock(neighborPos, newNeighborState, Block.UPDATE_ALL);
            }
        });

        return result;
    }
}
