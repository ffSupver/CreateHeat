package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.network.DirectionalNetworkConnect;
import com.ffsupver.createheat.network.HeatService;
import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class ThermalPipeBlock extends Block implements IBE<ThermalPipeBlockEntity>, IWrenchable, DirectionalNetworkConnect {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public ThermalPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public Class<ThermalPipeBlockEntity> getBlockEntityClass() {
        return ThermalPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThermalPipeBlockEntity> getBlockEntityType() {
        return CHBlocks.THERMAL_PIPE_BLOCK_ENTITY.get();
    }

    @Override
    public boolean canDirectionConnect(BlockState thisState, Direction direction) {
        switch (direction) {
            case NORTH -> {
                return thisState.getValue(NORTH);
            }
            case SOUTH -> {
                return thisState.getValue(SOUTH);
            }
            case UP -> {
                return thisState.getValue(UP);
            }
            case DOWN -> {
                return thisState.getValue(DOWN);
            }
            case EAST -> {
                return thisState.getValue(EAST);
            }
            case WEST -> {
                return thisState.getValue(WEST);
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.getBlock().equals(state.getBlock())){ // new block
            withBlockEntityDo(level, pos, thermalPipeBlockEntity -> {
                Set<UUID> neighborNetworkId = thermalPipeBlockEntity.getNetworkBehaviour().getNeighborNetworkId();
                thermalPipeBlockEntity.getNetworkBehaviour().setNetworkId(HeatService.addBlockToNetwork(pos, level, neighborNetworkId));
            });
        }
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.getBlock().equals(newState.getBlock())){ // destroy
            withBlockEntityDo(level,pos,thermalPipeBlockEntity -> {
                NetworkBehaviour networkBehaviour = thermalPipeBlockEntity.getNetworkBehaviour();
                if (networkBehaviour != null){
                    HeatService.removeBlockFromNetwork(thermalPipeBlockEntity.getLevel(),pos,networkBehaviour.getNetworkId());
                }
            });
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        Direction direction = Direction.fromDelta(neighborPos.getX() - pos.getX(), neighborPos.getY() - pos.getY(), neighborPos.getZ() - pos.getZ());
        if (direction != null && state.getBlock() instanceof ThermalPipeBlock thermalPipeBlock && thermalPipeBlock.canDirectionConnect(state, direction)){
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.is(CHTags.BlockTag.THERMAL_PIPE_CONNECT) && !(neighborState.getBlock() instanceof ThermalPipeBlock)){
                BlockState newState = setDirectionConnect(state, direction, false);
                level.setBlock(pos, newState, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation();

        // 世界坐标 → 方块局部坐标（0..16）
        double lx = (hit.x - pos.getX()) * 16;
        double ly = (hit.y - pos.getY()) * 16;
        double lz = (hit.z - pos.getZ()) * 16;

        Direction clickedPart = getClickedPart(lx, ly, lz);
        // clickedPart == null 表示点到了中心方块

        if (clickedPart != null && canDirectionConnect(state, clickedPart)){
            updateNetworkConnect(level, pos, state, clickedPart, true);
            return InteractionResult.SUCCESS;
        }
        if (clickedPart == null && !canDirectionConnect(state, context.getClickedFace())){
            BlockState connectState = level.getBlockState(pos.relative(context.getClickedFace()));
            if (connectState.is(CHTags.BlockTag.THERMAL_PIPE_CONNECT) || connectState.getBlock() instanceof ThermalPipeBlock) {
                updateNetworkConnect(level, pos, state, context.getClickedFace(), false);
                return InteractionResult.SUCCESS;
            }
        }

        return IWrenchable.super.onWrenched(state, context);
    }

    /**
     * 判断命中点落在管道的哪一节
     * calculate which part of the pipe is clicked
     * @return 被击中的“臂”方向；若命中中心方块则返回 null.   the direction of the arm that is clicked; if the center block is hit, return null
     */
    private static Direction getClickedPart(double lx, double ly, double lz) {
        // 坐标轴对应：x∈[0,16]、y∈[0,16]、z∈[0,16]
        // north = -Z（lz 小的一侧），south = +Z，west = -X，east = +X，down = -Y，up = +Y
        boolean inX = lx >= 5 && lx <= 11;
        boolean inY = ly >= 5 && ly <= 11;
        boolean inZ = lz >= 5 && lz <= 11;

        if (!inX && inY && inZ) return lx < 5 ? Direction.WEST  : Direction.EAST;
        if (!inY && inX && inZ) return ly < 5 ? Direction.DOWN  : Direction.UP;
        if (!inZ && inX && inY) return lz < 5 ? Direction.NORTH : Direction.SOUTH;

        return null; // 命中中心方块
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape voxelShape = Block.box(5,5,5,11,11,11);
        if (state.getValue(NORTH)){
            voxelShape = Shapes.join(voxelShape, Block.box(5,5,0,11,11,5), BooleanOp.OR);
        }
        if (state.getValue(SOUTH)){
            voxelShape = Shapes.join(voxelShape, Block.box(5,5,11,11,11,16), BooleanOp.OR);
        }
        if (state.getValue(EAST)){
            voxelShape = Shapes.join(voxelShape, Block.box(11,5,5,16,11,11), BooleanOp.OR);
        }
        if (state.getValue(WEST)){
            voxelShape = Shapes.join(voxelShape, Block.box(0,5,5,5,11,11), BooleanOp.OR);
        }
        if (state.getValue(UP)){
            voxelShape = Shapes.join(voxelShape, Block.box(5,11,5,11,16,11), BooleanOp.OR);
        }
        if (state.getValue(DOWN)){
            voxelShape = Shapes.join(voxelShape, Block.box(5,0,5,11,5,11), BooleanOp.OR);
        }
        return voxelShape;
    }

    /** set new BlockState and update network connect
     * @param state new BlockState
     * @param direction the direction to update
     * @param disconnect if the network is disconnected, needing block connection check
     */
    public void updateNetworkConnect(Level level, BlockPos pos, BlockState state, Direction direction, boolean disconnect){
        // if block of connection direction is ThermalPipeBlock, update its connection
        BlockPos connectPos = pos.relative(direction);
        BlockState connectState = level.getBlockState(connectPos);
        if (connectState.getBlock() instanceof ThermalPipeBlock){
            BlockState newConnectState = setDirectionConnect(connectState, direction.getOpposite(), !disconnect);
            level.setBlock(connectPos, newConnectState, Block.UPDATE_ALL);
        }

        BlockState newState = setDirectionConnect(state,direction, !disconnect);
        level.setBlock(pos,newState,Block.UPDATE_ALL);
        if (newState != state){ // update network connect
            withBlockEntityDo(level, pos, thermalPipeBlockEntity -> {
                thermalPipeBlockEntity.updateConnection(disconnect);
            });
        }
    }

    public static BlockState setDirectionConnect(BlockState state, Direction direction, boolean connect){
        switch (direction) {
            case NORTH -> {
                return state.setValue(NORTH, connect);
            }
            case SOUTH -> {
                return state.setValue(SOUTH, connect);
            }
            case UP -> {
                return state.setValue(UP, connect);
            }
            case DOWN -> {
                return state.setValue(DOWN, connect);
            }
            case EAST -> {
                return state.setValue(EAST, connect);
            }
            case WEST -> {
                return state.setValue(WEST, connect);
            }
            default -> {
                return state;
            }
        }
    }

    public static BlockState setDirectionConnect(BlockState state, Direction direction){
        return setDirectionConnect(state,direction,true);
    }

    public static BlockState setDirectionConnect(BlockState state, Collection<Direction> directions){
        for (Direction direction : directions) {
            state = setDirectionConnect(state, direction);
        }
        return state;
    }
}
