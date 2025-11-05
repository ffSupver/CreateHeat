package com.ffsupver.createheat.item.thermalTool;

import com.ffsupver.createheat.CreateHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record ThermalToolPointRenderPack(Map<BlockPos,ThermalToolPointType> pointPosMap) implements CustomPacketPayload {
    public static final ResourceLocation ID = CreateHeat.asResource("thermal_tool_point_sync");
    public static final Type<ThermalToolPointRenderPack> TYPE = new Type<>(ID);
    public static StreamCodec<RegistryFriendlyByteBuf, ThermalToolPointRenderPack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new,BlockPos.STREAM_CODEC,ThermalToolPointType.STREAM_CODEC),
            ThermalToolPointRenderPack::pointPosMap,
            ThermalToolPointRenderPack::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
