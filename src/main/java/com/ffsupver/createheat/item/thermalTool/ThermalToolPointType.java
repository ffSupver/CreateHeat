package com.ffsupver.createheat.item.thermalTool;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum ThermalToolPointType {
    WATER(0x3366FF);
    public static final StreamCodec<RegistryFriendlyByteBuf,ThermalToolPointType> STREAM_CODEC = StreamCodec.of(
            FriendlyByteBuf::writeEnum,
            buf->buf.readEnum(ThermalToolPointType.class)
    );
    private final int color; // 0xrrggbb

    ThermalToolPointType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
    public Object object(){
        return new ThermalToolPointRenderer();
    }
}