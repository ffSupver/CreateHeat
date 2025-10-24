package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.thermalBlock.*;
import com.ffsupver.createheat.block.tightCompressStone.TightCompressStone;
import com.ffsupver.createheat.block.tightCompressStone.TightCompressStoneEntity;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class CHBlocks {

    private static final CreateRegistrate REGISTRATE = CreateHeat.registrate();

    static {
        REGISTRATE.setCreativeTab(CHCreativeTab.MAIN_TAB);
    }

    public static final BlockEntry<ThermalBlock> THERMAL_BLOCK = REGISTRATE
            .block("thermal_block", ThermalBlock::new)
            .properties(p-> BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK))
            .onRegister(CreateRegistrate.connectedTextures(()->new ThermalBlockCTBehaviour(0)))
            .onRegister(CreateRegistrate.casingConnectivity((b, cc) -> cc.makeCasing(b, CHSpriteShifts.THERMAL_BLOCK_NONE)))
            .item(BlockItem::new)
            .build()
            .register();

    public static final BlockEntry<TightCompressStone> TIGHT_COMPRESSED_STONE = REGISTRATE
            .block("tight_compressed_stone",TightCompressStone::new)
            .properties(p->BlockBehaviour.Properties.ofFullCopy(Blocks.STONE))
            .item()
            .build()
            .register();

    public static final BlockEntry<SmartThermalBlock> SMART_THERMAL_BLOCK = REGISTRATE
            .block("smart_thermal_block", SmartThermalBlock::new)
            .properties(p-> BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK))
            .onRegister(CreateRegistrate.connectedTextures(()->new ThermalBlockCTBehaviour(1)))
            .onRegister(CreateRegistrate.casingConnectivity((b, cc) -> cc.makeCasing(b, CHSpriteShifts.THERMAL_BLOCK_NONE)))
            .item(BlockItem::new)
            .build()
            .register();
    public static final BlockEntityEntry<ThermalBlockEntity> THERMAL_BLOCK_ENTITY = REGISTRATE
            .blockEntity("thermal_block",ThermalBlockEntity::new)
            .validBlock(THERMAL_BLOCK)
            .register();
    public static final BlockEntityEntry<TightCompressStoneEntity> TIGHT_COMPRESSED_STONE_ENTITY = REGISTRATE
            .blockEntity("tight_compressed_stone",TightCompressStoneEntity::new)
            .validBlock(TIGHT_COMPRESSED_STONE)
            .register();

    public static final BlockEntityEntry<SmartThermalBlockEntity> SMART_THERMAL_BLOCK_ENTITY = REGISTRATE
            .blockEntity("smart_thermal_block",SmartThermalBlockEntity::new)
            .validBlock(SMART_THERMAL_BLOCK)
            .register();


    public static void registerBoilHeater(){
        BoilerHeater.REGISTRY.register(THERMAL_BLOCK.get(),BoilerHeater.BLAZE_BURNER);
        BoilerHeater.REGISTRY.register(SMART_THERMAL_BLOCK.get(),BoilerHeater.BLAZE_BURNER);
    }

    public static void register() {
    }
}

