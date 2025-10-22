package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;

public class CHSpriteShifts {
    public static final CTSpriteShiftEntry THERMAL_BLOCK_NONE = omni("thermal_block/none")
            , THERMAL_BLOCK_KINDLED = omni("thermal_block/kindled")
            ,THERMAL_BLOCK_SEETHING = omni("thermal_block/seething");


    private static CTSpriteShiftEntry omni(String name) {
        return getCT(AllCTTypes.OMNIDIRECTIONAL, name);
    }
    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, CreateHeat.asResource("block/" + blockTextureName),
                CreateHeat.asResource("block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return getCT(type, blockTextureName, blockTextureName);
    }
}
