package com.ffsupver.createheat.item.thermalTool;


import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ThermalToolPointRenderNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        // 注册到客户端
        registrar.playToClient(
                ThermalToolPointRenderPack.TYPE,
                ThermalToolPointRenderPack.STREAM_CODEC,
                ThermalToolPointRenderNetwork::handleHeatDataSync
        );
    }

    private static void handleHeatDataSync(ThermalToolPointRenderPack thermalToolPointRenderPack, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 在客户端主线程执行
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                // 更新客户端数据
                ThermalToolPointRenderer.updateAllPoints(thermalToolPointRenderPack.pointPosMap());
            }
        });
    }
}
