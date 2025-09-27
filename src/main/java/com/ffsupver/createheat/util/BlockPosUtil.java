package com.ffsupver.createheat.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

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
}
