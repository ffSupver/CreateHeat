package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.block.ConnectableBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class BaseThermalBlock<T extends BaseThermalBlockEntity> extends ConnectableBlock<BaseThermalBlockEntity,T> implements IWrenchable {

    private final Class<T> blockEntityClass;
    private final Supplier<BlockEntityType<? extends T>> blockEntityType;
    public BaseThermalBlock(Properties properties, Class<T> blockEntityClass, Supplier<BlockEntityType<? extends T>> blockEntityType) {
        super(properties);
        this.blockEntityClass = blockEntityClass;
        this.blockEntityType = blockEntityType;
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE));
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

    @Override
    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    @Override
    public BlockEntityType<? extends T> getBlockEntityType() {
        return blockEntityType.get();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        addParticles(state,level,pos,random);
    }


    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (isBurning(state,level,pos)){
            withBlockEntityDo(level, pos, baseThermalBlockEntity -> baseThermalBlockEntity.stepOn(entity));
        }
    }

    @Override
    public boolean isBurning(BlockState state, BlockGetter level, BlockPos pos) {
        BaseThermalBlockEntity baseThermalBlock = getBlockEntity(level,pos);
        return baseThermalBlock != null && baseThermalBlock.isBurning();
    }

    public static void addParticles(BlockState state, Level level, BlockPos pos, RandomSource random){
        if (!state.getValue(HEAT_LEVEL).equals(BlazeBurnerBlock.HeatLevel.NONE) && random.nextInt(2) == 0){
            Direction direction = Direction.getRandom(random);
            BlockPos particlePos = pos.relative(direction);
            BlockState particlePosState = level.getBlockState(particlePos);
            if (!state.canOcclude() || !particlePosState.isFaceSturdy(level,particlePos,direction.getOpposite())){
                double d0 = direction.getStepX() == 0 ? random.nextDouble() : (double)0.5F + (double)direction.getStepX() * 0.5;
                double d1 = direction.getStepY() == 0 ? random.nextDouble() : (double)0.5F + (double)direction.getStepY() * 0.5;
                double d2 = direction.getStepZ() == 0 ? random.nextDouble() : (double)0.5F + (double)direction.getStepZ() * 0.5;
                Vec3 position = new Vec3(pos.getX() + d0,pos.getY() + d1,pos.getZ() + d2);
                Vec3 speed = position.subtract(pos.getCenter()).scale(random.nextDouble() * 0.04f);
                ParticleOptions particleOptions = state.getValue(HEAT_LEVEL).equals(BlazeBurnerBlock.HeatLevel.KINDLED) ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
                level.addParticle(particleOptions,position.x(),position.y(),position.z(),speed.x(),speed.y(),speed.z());
            }
        }
    }
}
