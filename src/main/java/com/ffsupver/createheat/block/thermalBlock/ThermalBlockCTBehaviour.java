package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.registries.CHSpriteShifts;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class ThermalBlockCTBehaviour extends ConnectedTextureBehaviour.Base {

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        return super.connectsTo(state,other,reader,pos,otherPos,face) && state.hasProperty(HEAT_LEVEL) && other.hasProperty(HEAT_LEVEL) && state.getValue(HEAT_LEVEL).equals(other.getValue(HEAT_LEVEL));
    }

    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (Config.CONNECT_BLOCK_TEXTURE.get()){
            return switch (state.getValue(HEAT_LEVEL)) {
                case NONE -> CHSpriteShifts.THERMAL_BLOCK_NONE;
                case KINDLED, SMOULDERING, FADING -> CHSpriteShifts.THERMAL_BLOCK_KINDLED;
                case SEETHING -> CHSpriteShifts.THERMAL_BLOCK_SEETHING;
            };
        }else {
            return null;
        }
    }
}
