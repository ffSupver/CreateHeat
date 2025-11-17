package com.ffsupver.createheat.item.thermalTool;

import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class ThermalToolUseActions {
    private static Map<LevelBlockPosTester, LevelBlockPosConsumer> ACTIONS = new HashMap<>();

    public static void bootSetup(){
        registerAction((
                level, pos, state, player,isShift) -> level.getBlockEntity(pos) instanceof ConnectableBlockEntity<?>,
                (level, pos, state, player,isShift) -> {
                    if (level.getBlockEntity(pos) instanceof ConnectableBlockEntity<?> connectableBlockEntity){
                       return userOnConnectableBlock(isShift,level,connectableBlockEntity,player);
                    }
                    return false;
                }
        );
        registerAction(
            isBlock(AllBlocks.FLUID_TANK),
            (level, pos, state, player, isShift) -> {
                if (level instanceof ServerLevel serverLevel){
                    ThermalToolPointLogic logic = ThermalToolPointLogic.FLUID_TANK;
                    ThermalToolPointServer.tiggerPoint(serverLevel.dimension(),pos,logic);
                }
                return true;
            }
        );
        registerAction(
                isBlock(AllBlocks.BLAZE_BURNER),
                (level, pos, state, player, isShift) -> {
                    if (state.getValue(HEAT_LEVEL)!= BlazeBurnerBlock.HeatLevel.NONE){
                        ItemStack creativeCake = AllItems.CREATIVE_BLAZE_CAKE.asStack();
                        BlazeBurnerBlock.tryInsert(state,level,pos,creativeCake,true,false,false);
                        return true;
                    }
                    return false;
                }
        );
        registerAction(
                isBlock(Blocks.TNT),
                (level, pos, state, player, isShift) -> {
                    if (!level.isClientSide()){
                        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                        level.explode(null, pos.getX(),pos.getY(),pos.getZ(), 5, true, Level.ExplosionInteraction.TRIGGER);
                    }
                    return true;
                }
        );
    }

    public static void registerAction(LevelBlockPosTester tester,LevelBlockPosConsumer consumer){
        ACTIONS.put(tester,consumer);
    }

    public static boolean onUse(Level level, BlockPos pos, BlockState state,Player player,boolean shiftDown){
        for (Map.Entry<LevelBlockPosTester, LevelBlockPosConsumer> entry : ACTIONS.entrySet()){
            if (entry.getKey().test(level,pos,state,player,shiftDown)){
               boolean accepted = entry.getValue().accept(level,pos,state,player,shiftDown);
               if (accepted){
                    return true;
               }
            }
        }
        return false;
    }

    public static  LevelBlockPosTester isBlock(Block block){
        return (level, pos, state, player, isShift) -> state.is(block);
    }
    public static  LevelBlockPosTester isBlock(Holder<Block> block){
        return (level, pos, state, player, isShift) -> state.is(block);
    }
    public static  LevelBlockPosTester isBlock(HolderSet<Block> block){
        return (level, pos, state, player, isShift) -> state.is(block);
    }
    public static  LevelBlockPosTester isBlock(TagKey<Block> block){
        return (level, pos, state, player, isShift) -> state.is(block);
    }

    @FunctionalInterface
    public interface LevelBlockPosTester{
        boolean test(Level level,BlockPos pos,BlockState state,Player player,boolean isShift);
    }
    @FunctionalInterface
    public interface LevelBlockPosConsumer{
        boolean accept(Level level,BlockPos pos,BlockState state,Player player,boolean isShift);
    }

    private static boolean userOnConnectableBlock(boolean shiftDown, Level level, ConnectableBlockEntity<?> connectableBlockEntity, Player player){
        BlockPos controllerPos = connectableBlockEntity.getControllerPos();
        ThermalBlockEntityBehaviour controllerEntity = connectableBlockEntity.getBehaviour(ThermalBlockEntityBehaviour.TYPE);
        if (player != null && !level.isClientSide()) {
            if (shiftDown){
                player.displayClientMessage(Component.literal(
                                "total heat:" + ThermalBlockEntityBehaviour.getFromCBE(connectableBlockEntity.getControllerEntity()).getAllHeatForDisplay()
                        ).withStyle(ChatFormatting.RED), true
                );
            }else {
                player.displayClientMessage(Component.literal(
                                "Connect count :" + controllerEntity.getBlockSize() + " heat:" + controllerEntity.getHeat() +
                                        " Controller x:" + controllerPos.getX() + " y:" + controllerPos.getY() + " z:" + controllerPos.getZ() + " heatStorage:" + controllerEntity.getHeatStorage()
                        ).withStyle(ChatFormatting.RED), true
                );
            }
        }
        return true;
    }
}
