package com.ffsupver.createheat.block.thermalBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

public class CopycatThermalBlockEntityRenderer extends SmartBlockEntityRenderer<CopycatThermalBlockEntity> {
    public CopycatThermalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CopycatThermalBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity.isFurnace()){
            Direction facing = blockEntity.getMaterial().getValue(HorizontalDirectionalBlock.FACING);
            ItemStack inputItem = blockEntity.getInputItemStack();
            ItemStack outputItem = blockEntity.getOutputItemStack();
            ItemStack renderItem = inputItem.isEmpty() ? outputItem : inputItem;
            if (!renderItem.isEmpty()){
                BakedModel bakedModel = itemRenderer.getModel(renderItem, null, null, 0);
                boolean blockItem = bakedModel.isGui3d();
                float scale = blockItem ? 0.4f : 0.5f;
                ms.pushPose();
                ms.translate(.5f,0.5f,.5f);
                Vec3 offset = new Vec3(facing.getNormal().getX(),facing.getNormal().getY(),facing.getNormal().getZ()).scale(.5f);
                ms.translate(offset.x(),offset.y(),offset.z());
                ms.translate(0,blockEntity.getItemRenderOffset(),0);

                if (!blockItem){
                    ms.mulPose(Axis.XP.rotationDegrees(90));
                }
                ms.scale(scale,scale,scale);
                int blockLight = LevelRenderer.getLightColor(level,pos.relative(facing));
                itemRenderer.renderStatic(renderItem,ItemDisplayContext.FIXED,blockLight, OverlayTexture.NO_OVERLAY,ms,buffer,blockEntity.getLevel(),0);
                ms.popPose();
            }
        }
    }
}
