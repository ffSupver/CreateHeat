package com.ffsupver.createheat.compat.coldSweat;

import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlock.BURNING;

public class DragonFireInputEffect extends BlockTemp {
    public DragonFireInputEffect(){
        super(IceAndFire.DRAGON_FIRE_INPUT.get());
    }
    @Override
    public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
        return state.hasProperty(BURNING) && state.getValue(BURNING) ? 1.0 : 0;
    }

    @Override
    public double maxEffect() {
        return 3;
    }
}
