package com.ffsupver.createheat.compat.aeronautics;

import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.compat.ponder.scenes.aeronautics.AeronauticsSimulateScene;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.simulated_team.simulated.index.SimBlocks;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;

import java.util.Set;

public class AeronauticsSimulateCompat implements CHModCompat {
    @Override
    public String getModId() {
        return Mods.ModIds.SIMULATED.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {
        CHHeatTransferProcessers.registerHeatTransferProcesser(PortableEngineBlockHeatTransferProcesser.TYPE.getPath(),()->PortableEngineBlockHeatTransferProcesser::new);
    }

    @Override
    public void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER) {
        HELPER.forComponents(CHBlocks.THERMAL_BLOCK)
                .addStoryBoard("aeronautics/use", AeronauticsSimulateScene::use);
        HELPER.forComponents(SimBlocks.PORTABLE_ENGINES)
                .addStoryBoard("aeronautics/use", AeronauticsSimulateScene::use);
    }

    @Override
    public void registerBoilerHeater() {
        CHModCompat.super.registerBoilerHeater();
    }

    @Override
    public Set<BlockPos> getGlobalBlockPos(Level level, BlockPos pos) {
        return CHModCompat.super.getGlobalBlockPos(level, pos);
    }

    @Override
    public Set<BlockPos> findHitBlockPos(Level level, BlockPos pos) {
        return CHModCompat.super.findHitBlockPos(level, pos);
    }
}
