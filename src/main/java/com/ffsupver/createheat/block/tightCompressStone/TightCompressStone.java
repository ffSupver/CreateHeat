package com.ffsupver.createheat.block.tightCompressStone;

import com.ffsupver.createheat.registries.CHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TightCompressStone extends ConnectableBlock<TightCompressStoneEntity> implements IBlockExtension {
    public static final Property<Heat> HEAT = EnumProperty.create("heat",Heat.class);
    public TightCompressStone(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT,Heat.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(HEAT).getLight();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!state.getValue(HEAT).equals(Heat.NONE) && !entity.isSteppingCarefully() && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().hotFloor(), state.getValue(HEAT).equals(Heat.REGULAR_HEAT) ? 1.0F : 2.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HEAT).equals(Heat.NONE) && random.nextInt(2) == 0){
            Direction direction = Direction.getRandom(random);
            BlockPos particlePos = pos.relative(direction);
            BlockState particlePosState = level.getBlockState(particlePos);
            if (!state.canOcclude() || !particlePosState.isFaceSturdy(level,particlePos,direction.getOpposite())){
                double d0 = direction.getStepX() == 0 ? random.nextDouble() : (double)0.5F + (double)direction.getStepX() * 0.6;
                double d1 = direction.getStepY() == 0 ? random.nextDouble() : (double)0.5F + (double)direction.getStepY() * 0.6;
                double d2 = direction.getStepZ() == 0 ? random.nextDouble() : (double)0.5F + (double)direction.getStepZ() * 0.6;
                ParticleOptions particleOptions = state.getValue(HEAT).equals(Heat.REGULAR_HEAT) ? ParticleTypes.DRIPPING_LAVA : ParticleTypes.SOUL_FIRE_FLAME;
                level.addParticle(particleOptions,pos.getX() + d0,pos.getY() + d1,pos.getZ() + d2,0,0,0);
            }
        }
    }

    @Override
    public Class<TightCompressStoneEntity> getBlockEntityClass() {
        return TightCompressStoneEntity.class;
    }

    @Override
    public BlockEntityType<? extends TightCompressStoneEntity> getBlockEntityType() {
        return CHBlocks.TIGHT_COMPRESSED_STONE_ENTITY.get();
    }

    public enum Heat implements StringRepresentable {
        NONE("none", 0),
        REGULAR_HEAT("regular_heat", 15),
        SUPER_HEAT("super_heat", 12);
        private final String name;
        private final int light;

        Heat(String name, int light) {
            this.name = name;
            this.light = light;
        }

        public int getLight() {
            return light;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
