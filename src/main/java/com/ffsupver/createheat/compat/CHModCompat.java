package com.ffsupver.createheat.compat;

import net.neoforged.bus.api.IEventBus;

public interface CHModCompat {
    public String getModId();
    public void init(IEventBus eventBus);
}
