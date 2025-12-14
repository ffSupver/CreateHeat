package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.api.BlockEntityTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public class CHBlockEntityTickers {
    private static final Set<BlockEntityTicker.Ticker> TICKERS = new HashSet<>();
    public static void registerBlockEntityTicker(BlockEntityTicker.Ticker ticker){
        TICKERS.add(ticker);
    }

    public static void tryTick(BlockPos pos, ServerLevel level, BlockEntity blockEntity){
        TICKERS.forEach(ticker -> ticker.tick(pos,level,blockEntity));
    }
}
