package com.ffsupver.createheat.compat.jei.category;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.compat.jei.CreateHeatJEI;
import com.ffsupver.createheat.recipe.HeatRecipe;
import com.ffsupver.createheat.registries.CHBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class HeatCategory implements IRecipeCategory<HeatRecipe> {
    private static final List<Vec3i> HEAT_POSITIONS = List.of(
            new Vec3i(0,0,-1),
            new Vec3i(0,1,0),
            new Vec3i(-1,0,0),
            new Vec3i(1,0,0),
            new Vec3i(0,0,1),
            new Vec3i(0,-1,0)
    );
    public static final RecipeType<HeatRecipe> TYPE = CreateHeatJEI.recipeType("heat", HeatRecipe.class);
    @Override
    public RecipeType<HeatRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("createheat.recipe.heat");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return new ItemIcon(()-> CHBlocks.THERMAL_BLOCK.get().asItem().getDefaultInstance());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HeatRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT)
                .setPosition(20,100)
                .addIngredients(recipe.getIngredients().getFirst())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT)
                .setPosition(217,70)
                .addItemStack(recipe.getOutputBlock().getBlock().asItem().getDefaultInstance())
                .setOutputSlotBackground();
    }

    @Override
    public int getWidth() {
        return 255;
    }

    @Override
    public int getHeight() {
        return 128;
    }

    @Override
    public void draw(HeatRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
        Component heatDec = Component.translatable("createheat.recipe.heat.amount",recipe.getMinHeatPerTick(),recipe.getHeatCost());
        guiGraphics.drawString(Minecraft.getInstance().font, heatDec,getWidth()/2 - 20,100,0xFF0000);

        int scale = 32;
        int xOffsetInput = 40;
        int yOffsetInput = 70;
        PoseStack matrixStack = guiGraphics.pose();
        matrixStack.pushPose();
        translateBlock(xOffsetInput,yOffsetInput,matrixStack);

        drawBlock(recipe.getInputBlock(),0,0,0,scale,guiGraphics);
        drawThermalBlockByHeat(recipe.getMinHeatPerTick(), Config.HEAT_PER_FADING_BLAZE.get(),Config.HEAT_PER_SEETHING_BLAZE.get(),scale,guiGraphics);

        matrixStack.popPose();

        int xOffsetOutput = 200;
        int yOffsetOutput = 50;
        matrixStack.pushPose();
        translateBlock(xOffsetOutput,yOffsetOutput,matrixStack);

        drawBlock(recipe.getOutputBlock(),0,0,0,scale,guiGraphics);

        matrixStack.popPose();
    }

    private static void translateBlock(int xOffset,int yOffset,PoseStack matrixStack){
        matrixStack.translate(xOffset, yOffset, 200);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
    }

    private static void drawBlock(BlockState state,int x,int y,int z,int scale,GuiGraphics guiGraphics){
        AnimatedKinetics.defaultBlockElement(state)
                .atLocal(x, y, z)
                .scale(scale)
                .render(guiGraphics);
    }

    private static BlockState thermalBlock(BlazeBurnerBlock.HeatLevel heatLevel){
        return CHBlocks.THERMAL_BLOCK.getDefaultState().setValue(HEAT_LEVEL,heatLevel);
    }

    private static void drawThermalBlockByHeat(int minHeat,int heatPerRegular,int heatPerSuper,int scale,GuiGraphics guiGraphics){
        int left = minHeat;
        for (Vec3i pos : HEAT_POSITIONS){
            boolean superH = left >= heatPerSuper;
            BlockState thermalBlock = thermalBlock(superH ? SEETHING : KINDLED);
            drawBlock(thermalBlock,pos.getX(),pos.getY(),pos.getZ(),scale,guiGraphics);
            left -= superH ? heatPerSuper : heatPerRegular;
            if (left <= 0){
                break;
            }
        }
    }
}
