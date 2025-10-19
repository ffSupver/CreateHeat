package com.ffsupver.createheat.util;

import com.ffsupver.createheat.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class BlockUtil {
    public static void AllDirectionOf(BlockPos startPos, Consumer<BlockPos> f){
        AllDirectionOf(startPos,(b,fa)->f.accept(b));
    }
    public static void AllDirectionOf(BlockPos startPos, BiConsumer<BlockPos,Direction> f){
        AllDirectionOf(startPos,f,b->false);
    }
    public static void AllDirectionOf(BlockPos startPos, Consumer<BlockPos> f, Predicate<BlockPos> shouldBreak){
        AllDirectionOf(startPos,(b,fa)->f.accept(b),shouldBreak);
    }
    public static void AllDirectionOf(BlockPos startPos, BiConsumer<BlockPos,Direction> f, Predicate<BlockPos> shouldBreak){
        for (Direction d : Direction.values()){
            if (shouldBreak.test(startPos.relative(d))){
                break;
            }
            f.accept(startPos.relative(d),d);
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

    public static boolean checkState(BlockState checkState,BlockState state) {
        boolean sameBlock = checkState.getBlock().equals(state.getBlock());
        if (!sameBlock){
            return false;
        }

        Collection<Property<?>> properties = checkState.getProperties();
        for (Property<?> property : properties){
            boolean nS = state.hasProperty(property) && state.getValue(property).equals(checkState.getValue(property));
            if (!nS){
                return false;
            }
        }

        return true;
    }
}
