package com.ffsupver.createheat.compat.aeronautics;

import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
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
        CHModCompat.super.registerPonder(HELPER);
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
