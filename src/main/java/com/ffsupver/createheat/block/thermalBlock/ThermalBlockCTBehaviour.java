package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.registries.CHSpriteShifts;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.ffsupver.createheat.block.thermalBlock.ThermalBlockCTBehaviour.BlockType.BASE;
import static com.ffsupver.createheat.block.thermalBlock.ThermalBlockCTBehaviour.BlockType.SMART;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class ThermalBlockCTBehaviour extends ConnectedTextureBehaviour.Base {

    private static Map<BlockType,Map<BlazeBurnerBlock.HeatLevel,CTSpriteShiftEntry>> SHIFT_MAP = thermalBlockShiftMap();

    private final BlockType type;

    public ThermalBlockCTBehaviour(int blockTypeOrder) {
        this.type = BlockType.values()[blockTypeOrder];
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        return super.connectsTo(state,other,reader,pos,otherPos,face) && state.hasProperty(HEAT_LEVEL) && other.hasProperty(HEAT_LEVEL) && state.getValue(HEAT_LEVEL).equals(other.getValue(HEAT_LEVEL));
    }

    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (Config.CONNECT_BLOCK_TEXTURE.get()){
            return SHIFT_MAP.get(type).get(state.getValue(HEAT_LEVEL));
        }else {
            return null;
        }
    }

    private static Map<BlockType, Map<BlazeBurnerBlock.HeatLevel,CTSpriteShiftEntry>> thermalBlockShiftMap(){
        Map<BlazeBurnerBlock.HeatLevel,CTSpriteShiftEntry> thermalBlock = Map.of(
                NONE,CHSpriteShifts.THERMAL_BLOCK_NONE,
                KINDLED,CHSpriteShifts.THERMAL_BLOCK_KINDLED,
                SMOULDERING,CHSpriteShifts.THERMAL_BLOCK_KINDLED,
                FADING,CHSpriteShifts.THERMAL_BLOCK_KINDLED,
                SEETHING,CHSpriteShifts.THERMAL_BLOCK_SEETHING
        );

        Map<BlazeBurnerBlock.HeatLevel,CTSpriteShiftEntry> smartThermalBlock = Map.of(
                NONE,CHSpriteShifts.SMART_THERMAL_BLOCK_NONE,
                KINDLED,CHSpriteShifts.SMART_THERMAL_BLOCK_KINDLED,
                SMOULDERING,CHSpriteShifts.SMART_THERMAL_BLOCK_KINDLED,
                FADING,CHSpriteShifts.SMART_THERMAL_BLOCK_KINDLED,
                SEETHING,CHSpriteShifts.SMART_THERMAL_BLOCK_SEETHING
        );

        return Map.of(
                BASE,thermalBlock,
                SMART,smartThermalBlock
        );
    }

    protected enum BlockType {
        BASE,SMART
    }
}
