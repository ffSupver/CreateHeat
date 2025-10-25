package com.ffsupver.createheat.compat.coldSweat;

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class ThermalBlockEffect extends BlockTemp {
    private final double baseTemp;
    private final double distanceDecline;
    public ThermalBlockEffect(double baseTemp, double distanceDecline, Block... blocks){
        super(blocks);
        this.baseTemp = baseTemp;
        this.distanceDecline = distanceDecline;
    }

    @Override
    public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
        if (state.hasProperty(HEAT_LEVEL)){
            if (!state.getValue(HEAT_LEVEL).equals(BlazeBurnerBlock.HeatLevel.NONE)){
                double temp = switch (state.getValue(HEAT_LEVEL)){
                    case SEETHING -> baseTemp * 2;
                    case KINDLED,FADING,SMOULDERING -> baseTemp;
                    default -> 0;
                };
                return temp / Math.max(1,distance * distanceDecline);
            }
        }
        return 0;
    }

    @Override
    public double maxEffect() {
        return 4 * baseTemp;
    }
}
