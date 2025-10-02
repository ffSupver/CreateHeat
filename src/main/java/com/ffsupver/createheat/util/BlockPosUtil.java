package com.ffsupver.createheat.util;

import com.ffsupver.createheat.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class BlockPosUtil {
    public static void AllDirectionOf(BlockPos startPos, Consumer<BlockPos> f){
        AllDirectionOf(startPos,f,b->false);
    }
    public static void AllDirectionOf(BlockPos startPos, Consumer<BlockPos> f, Predicate<BlockPos> shouldBreak){
        for (Direction d : Direction.values()){
            if (shouldBreak.test(startPos.relative(d))){
                break;
            }
            f.accept(startPos.relative(d));
        }
    }


    public static void walkAllBlocks(BlockPos startPos, Set<BlockPos> walkedBlockPos, Predicate<BlockPos> check,int maxRange,int currentRange) {
        // 如果超出范围,或者当前位置已经遍历过.或者不满足条件，则返回
        if (currentRange > maxRange || walkedBlockPos.contains(startPos) || !check.test(startPos)) {
            return;
        }

        // 将当前位置添加到已遍历集合中
        walkedBlockPos.add(startPos);

        // 遍历六个方向（上、下、北、南、西、东）
        AllDirectionOf(startPos,neighborPos->{
            // 递归遍历相邻方块
            walkAllBlocks(neighborPos, walkedBlockPos, check,maxRange,currentRange + 1);
        });
    }

    public static void walkAllBlocks(BlockPos startPos, Set<BlockPos> walkedBlockPos, Predicate<BlockPos> check){
        walkAllBlocks(startPos,walkedBlockPos,check, Config.MAX_CONNECT_RANGE.get(),0);
    }
}
