package com.ffsupver.createheat.item.thermalTool;

import com.ffsupver.createheat.registries.CHItems;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/** 仅客户端
 *  client only
 */
public class ThermalToolPointRenderer {
    private final static Map<BlockPos, ThermalToolPointType> currentPoints = new HashMap<>();
    public static void tick(){
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;

        if (player == null || level == null){
            return;
        }



        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.is(CHItems.THERMAL_TOOL)){
            currentPoints.forEach((pos,point)->drawPoint(level,pos,point));
        }
    }
    public static void drawPoint(Level level, BlockPos pos, ThermalToolPointType type){
        VoxelShape shape = level.getBlockState(pos).getShape(level,pos);
        AABB aabb;
        if (shape.isEmpty()){
            aabb = new AABB(0,0,0,1,1,1);
        }else {
            aabb = shape.bounds();
        }
        Outliner.getInstance().showAABB(type.object(),aabb.move(pos))
                .colored(type.getColor())
                .lineWidth(1/16f);
    }

    public static void addPoint(BlockPos pos, ThermalToolPointType pointType){
        currentPoints.putIfAbsent(pos,pointType);
    }

    public static void removePoint(BlockPos pos){
        currentPoints.remove(pos);
    }


    public static void updateAllPoints(Map<BlockPos, ThermalToolPointType> blockPosThermalToolPointTypeMap) {
        currentPoints.clear();
        currentPoints.putAll(blockPosThermalToolPointTypeMap);
    }
}
