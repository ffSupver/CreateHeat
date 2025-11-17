package com.ffsupver.createheat.api;

import com.ffsupver.createheat.block.HeatProvider;

public record SimpleHeatProvider( int heatPerTick,int supperHeatCount) implements HeatProvider {
    @Override
    public int getHeatPerTick() {
        return heatPerTick;
    }
    @Override
    public int getSupperHeatCount() {
        return supperHeatCount;
    }
}
