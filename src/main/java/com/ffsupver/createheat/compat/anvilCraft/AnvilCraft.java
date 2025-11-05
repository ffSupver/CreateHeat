package com.ffsupver.createheat.compat.anvilCraft;

import com.ffsupver.createheat.api.anvilCraft.HeatableBlockHeatTransferProcesserData;
import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.item.thermalTool.ThermalToolUseActions;
import com.ffsupver.createheat.registries.CHBoilerUpdaters;
import com.ffsupver.createheat.registries.CHDatapacks;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.simibubi.create.api.boiler.BoilerHeater;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.heatable.HeatableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;

import java.util.Optional;

import static com.simibubi.create.api.registry.SimpleRegistry.Provider.forBlockTag;


public class AnvilCraft implements CHModCompat {
    public static final ResourceKey<Registry<HeatableBlockHeatTransferProcesserData>> HEATABLE_BLOCK_HTP_DATA = CHDatapacks.key("anvil_craft");


    @Override
    public String getModId() {
        return Mods.ModIds.ANVIL_CRAFT.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        CHHeatTransferProcessers.registerHeatTransferProcesser(HeatableBlockTransferProcesser.TYPE.getPath(), () -> HeatableBlockTransferProcesser::new);
        CHHeatTransferProcessers.registerHeatTransferProcesser(HeatCollectorTransferProcesser.TYPE.getPath(), () -> HeatCollectorTransferProcesser::new);
        CHHeatTransferProcessers.registerOptionalNeedHeatBlock(state -> state.is(BlockTags.CAULDRONS));
        CHBoilerUpdaters.registerBoilerUpdater(HeatProducerBoilHeater::shouldUpdateBoiler);
        ThermalToolUseActions.registerAction(AnvilCraft::isHeatable,AnvilCraft::changeHeatTierByThermalTool);

        eventBus.addListener(Mods.registerDatapack(HEATABLE_BLOCK_HTP_DATA,HeatableBlockHeatTransferProcesserData.CODEC));
    }
    @Override
    public void registerBoilerHeater() {
        BoilerHeater.REGISTRY.registerProvider(forBlockTag(HeatProducerBoilHeater.BLOCK_TAG,new HeatProducerBoilHeater()));
    }

    private static boolean isHeatable(Level level, BlockPos pos, BlockState state, Player player, boolean isShift) {
       Optional<HeatTier> entryOp = HeatRecorder.getTier(level,pos,state);
        return entryOp.isPresent();
    }

    private static boolean changeHeatTierByThermalTool(Level level, BlockPos pos, BlockState state, Player player, boolean isShift) {
        Optional<Block> newBlockOp = isShift ? HeatRecorder.getPrevTierHeatableBlock(level,pos,state) : HeatRecorder.getNextTierHeatableBlock(level,pos,state);
        if (newBlockOp.isPresent()) {
            Block newBlock = newBlockOp.get();
            level.setBlock(pos, newBlock.defaultBlockState(), 3);
            if (newBlock instanceof HeatableBlock) {
                Optional.ofNullable(level.getBlockEntity(pos))
                        .filter(HeatableBlockEntity.class::isInstance)
                        .map(HeatableBlockEntity.class::cast)
                        .ifPresent(be -> be.setDuration(1200));
                return true;
            } else {
                return newBlock.equals(Blocks.NETHERITE_BLOCK);
            }
        }
        return false;
    }

}
