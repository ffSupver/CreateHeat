package com.ffsupver.createheat.compat;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.neoforged.bus.api.IEventBus;

public interface CHModCompat {
    public String getModId();
    public void init(IEventBus eventBus);
    default void registerPonder(PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER){}
    default void registerBoilerHeater(){}
}
