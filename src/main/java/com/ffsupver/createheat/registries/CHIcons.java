package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

public class CHIcons extends AllIcons {
    public static final ResourceLocation ICON_ATLAS_C = CreateHeat.asResource("textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE_C = 32;
    private static int xC = 0, yC = -1;

    public static final CHIcons I_HEAT_LEVEL_NONE = newRow(),
            I_HEAT_LEVEL_KINDLED = next(),
            I_HEAT_LEVEL_SEETHING = newRow();


    private int iconXC;
    private int iconYC;


    private static CHIcons next() {
        return new CHIcons(++xC, yC);
    }

    private static CHIcons newRow() {
        return new CHIcons(xC = 0, ++yC);
    }

    public CHIcons(int xC, int yC) {
        super(xC, yC);
        iconXC = xC * 16;
        iconYC = yC * 16;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS_C, x, y, 0, iconXC, iconYC, 16, 16, ICON_ATLAS_SIZE_C, ICON_ATLAS_SIZE_C);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int color) {
        VertexConsumer builder = buffer.getBuffer(RenderType.text(ICON_ATLAS_C));
        Matrix4f matrix = ms.last().pose();
        Color rgb = new Color(color);
        int light = LightTexture.FULL_BRIGHT;

        Vec3 vec1 = new Vec3(0, 0, 0);
        Vec3 vec2 = new Vec3(0, 1, 0);
        Vec3 vec3 = new Vec3(1, 1, 0);
        Vec3 vec4 = new Vec3(1, 0, 0);

        float u1 = iconXC * 1f / ICON_ATLAS_SIZE_C;
        float u2 = (iconXC + 16) * 1f / ICON_ATLAS_SIZE_C;
        float v1 = iconYC * 1f / ICON_ATLAS_SIZE_C;
        float v2 = (iconYC + 16) * 1f / ICON_ATLAS_SIZE_C;

        vertex(builder, matrix, vec1, rgb, u1, v1, light);
        vertex(builder, matrix, vec2, rgb, u1, v2, light);
        vertex(builder, matrix, vec3, rgb, u2, v2, light);
        vertex(builder, matrix, vec4, rgb, u2, v1, light);
    }

    @OnlyIn(Dist.CLIENT)
    private void vertex(VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
        builder.addVertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z)
                .setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
                .setUv(u, v)
                .setLight(light);
    }
}
