package com.ffsupver.createheat.compat;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;

import java.util.Set;

public interface CHModCompat {
    public String getModId();
    public void init(IEventBus eventBus);
    default void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER){}
    default void registerBoilerHeater(){}

    /** find global block pos from local pos (etc. local pos in subLevel)
     * @param level
     * @param pos local pos
     * @return
     */
    default Set<BlockPos> getGlobalBlockPos(Level level, BlockPos pos){
        return Set.of();
    }
    /** find hit block pos from global pos (etc. hit in subLevel)
     * @param level
     * @param pos global pos
     * @return
     */
    default Set<BlockPos> findHitBlockPos(Level level, BlockPos pos){
        return Set.of();
    }
}
