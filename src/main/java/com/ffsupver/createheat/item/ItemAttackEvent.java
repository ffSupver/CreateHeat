package com.ffsupver.createheat.item;

import com.ffsupver.createheat.item.thermalTool.ThermalTool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class ItemAttackEvent {
    public static void onAttack(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        BlockState state = level.getBlockState(pos);
        if (stack.getItem() instanceof ThermalTool) {
           boolean placed = ThermalTool.attackOnBlock(level,pos,state,player);
           event.setCanceled(placed);
        }

    }
}
