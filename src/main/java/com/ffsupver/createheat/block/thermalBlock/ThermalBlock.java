package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class ThermalBlock extends BaseThermalBlock<BaseThermalBlockEntity> implements IWrenchable {

    public ThermalBlock(Properties properties) {
        super(properties, BaseThermalBlockEntity.class,()-> CHBlocks.THERMAL_BLOCK_ENTITY.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        addParticles(state,level,pos,random);
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
