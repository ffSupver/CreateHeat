package com.ffsupver.createheat.item;

import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class ThermalTool extends Item {
    public ThermalTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity){
            Player player = context.getPlayer();
            BlockPos controllerPos = thermalBlockEntity.getControllerPos();
            ThermalBlockEntity controllerEntity = thermalBlockEntity.getControllerEntity();
            if (player != null && !level.isClientSide()) {
                if (player.isCrouching()){
                    player.displayClientMessage(Component.literal(
                            "total heat:" + controllerEntity.getAllHeatForDisplay()
                            ).withStyle(ChatFormatting.RED), true
                    );
                }else {
                    player.displayClientMessage(Component.literal(
                                    "Connect count :" + controllerEntity.getConnectedBlocks().size() + " heat:" + controllerEntity.getHeat() +
                                            " Controller x:" + controllerPos.getX() + " y:" + controllerPos.getY() + " z:" + controllerPos.getZ() + " heatStorage:" + controllerEntity.getHeatStorage()
                            ).withStyle(ChatFormatting.RED), true
                    );
                }
            }
            return InteractionResult.SUCCESS;
        }else if (level.getBlockState(pos).is(Blocks.TNT)){
            if (!level.isClientSide()){
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                level.explode(null, pos.getX(),pos.getY(),pos.getZ(), 5, true, Level.ExplosionInteraction.TRIGGER);
            }
        }
        return super.useOn(context);
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return super.useOnRelease(stack);
    }
}
