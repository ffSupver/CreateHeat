package com.ffsupver.createheat.item;

import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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
            if (player != null && !level.isClientSide()) {
                player.sendSystemMessage(Component.literal(
                        "Connect count :" + thermalBlockEntity.getConnectedBlocks().size() +
                                "\n x:"+controllerPos.getX()+" y:"+controllerPos.getY()+" z:"+controllerPos.getZ()
                        )
                );
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        super.onStopUsing(stack, entity, count);
        Level level = entity.level();
        if (!level.isClientSide() && count == 0){
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 3, true, Level.ExplosionInteraction.MOB);
        }
    }
}
