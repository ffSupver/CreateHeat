package com.ffsupver.createheat.item.thermalTool;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.ffsupver.createheat.util.BlockUtil;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ThermalTool extends Item {
    public ThermalTool(Properties properties) {
        super(properties);
    }



    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        boolean shiftDown = player.isShiftKeyDown();
        if (level.getBlockEntity(pos) instanceof ConnectableBlockEntity<?> connectableBlockEntity){
          return   userOnConnectableBlock(shiftDown,level,connectableBlockEntity,player);
        }else if (state.is(AllBlocks.FLUID_TANK)){
            if (level instanceof ServerLevel serverLevel){
                ThermalToolPointLogic logic = ThermalToolPointLogic.FLUID_TANK;
                ThermalToolPointServer.tiggerPoint(serverLevel.dimension(),pos,logic);
            }
        }else if (state.is(Blocks.TNT)){
            if (!level.isClientSide()){
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                level.explode(null, pos.getX(),pos.getY(),pos.getZ(), 5, true, Level.ExplosionInteraction.TRIGGER);
            }
        }
        return super.useOn(context);
    }

    private static InteractionResult userOnConnectableBlock(boolean shiftDown,Level level,ConnectableBlockEntity<?> connectableBlockEntity,Player player){
        BlockPos controllerPos = connectableBlockEntity.getControllerPos();
        ThermalBlockEntityBehaviour controllerEntity = connectableBlockEntity.getBehaviour(ThermalBlockEntityBehaviour.TYPE);
        if (player != null && !level.isClientSide()) {
            if (shiftDown){
                player.displayClientMessage(Component.literal(
                                "total heat:" + controllerEntity.getAllHeatForDisplay()
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
        return InteractionResult.SUCCESS;
    }

    public static boolean attackOnBlock(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel){
            if (state.is(AllBlocks.FLUID_TANK)){
                if (!player.isShiftKeyDown()) {
                    return placeBoiler(serverLevel, pos);
                }else {
                    return removeBoiler(serverLevel,pos,player);
                }
            }
        }
        return false;
    }

    private static boolean placeBoiler(ServerLevel serverLevel,BlockPos pos){
        boolean canPlace = true;
        for (int i = -1; i<2 ;i++){
            if (!canPlace){
                break;
            }
            for (int j = -1; j<2 ;j++){
                if(!(i == 0 && j == 0) && !serverLevel.getBlockState(pos.offset(i,0,j)).isAir()){
                    canPlace = false;
                    break;
                }
            }
        }
        if (canPlace){
            Optional<StructureTemplate> structureTemplateOp = serverLevel.getStructureManager().get(CreateHeat.asResource("boiler"));
            if (structureTemplateOp.isPresent()) {
                StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.NONE);
                BlockPos posToPlace = pos.offset(-1, 0, -1);
                structureTemplateOp.get().placeInWorld(serverLevel, posToPlace, posToPlace, settings, serverLevel.getRandom(), 2);
                serverLevel.playSound(null,posToPlace, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS,2F,1.0F);
                return true;
            }
        }
        return false;
    }

    private static boolean removeBoiler(ServerLevel serverLevel,BlockPos pos,Player player){
        Set<BlockPos> NeedRemovePosSet = new HashSet<>();
        BlockUtil.walkAllBlocks(pos,NeedRemovePosSet,
                b-> {
                    BlockState state = serverLevel.getBlockState(b);
                    if (state.is(AllBlocks.STEAM_ENGINE)){
                        BlockPos shaftPos = b.relative(SteamEngineBlock.getFacing(state),2);
                        if (serverLevel.getBlockState(shaftPos).is(AllBlocks.POWERED_SHAFT)){
                            NeedRemovePosSet.add(shaftPos);
                        }
                        return true;
                    }
                    return state.is(AllBlocks.FLUID_TANK);
                },64
        );
        NeedRemovePosSet.forEach(p->serverLevel.destroyBlock(p,false,player));
        return !NeedRemovePosSet.isEmpty();
    }
}
