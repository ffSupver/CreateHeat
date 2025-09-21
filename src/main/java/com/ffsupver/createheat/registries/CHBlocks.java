package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlock;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntity;
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
            .item(BlockItem::new)
            .build()
            .register();

    public static final BlockEntityEntry<ThermalBlockEntity> THERMAL_BLOCK_ENTITY = REGISTRATE
            .blockEntity("thermal_block",ThermalBlockEntity::new)
            .validBlock(THERMAL_BLOCK)
            .register();


    public static void registerBoilHeater(){
        BoilerHeater.REGISTRY.register(THERMAL_BLOCK.get(),BoilerHeater.BLAZE_BURNER);
    }

    public static void register() {
    }
}

