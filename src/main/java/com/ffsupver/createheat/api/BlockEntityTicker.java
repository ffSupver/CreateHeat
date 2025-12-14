package com.ffsupver.createheat.api;

import com.ffsupver.createheat.mixin.ChunkMapAccessor;
import com.ffsupver.createheat.registries.CHBlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class BlockEntityTicker {
    private static int cooldown;
    public static void registerEvent(ServerTickEvent.Post event){
        cooldown--;

        if (cooldown <= 0){
            event.getServer().getAllLevels().forEach(BlockEntityTicker::processLevelBlockEntities);
            cooldown = 20;
        }
    }

    private static void processLevelBlockEntities(ServerLevel level) {
        // 只处理已经加载的区块，避免加载未加载的区块
        if (level.getChunkSource().chunkMap instanceof ChunkMapAccessor chunkMapAccessor) {
            for (ChunkHolder chunkHolder : chunkMapAccessor.getChunksAccessor()) {
                LevelChunk chunk = chunkHolder.getTickingChunk();
                if (chunk != null && level.shouldTickBlocksAt(chunk.getPos().toLong())) {
                    processChunkBlockEntities(chunk, level);
                }
            }
        }
    }

    private static void processChunkBlockEntities(LevelChunk chunk, ServerLevel level) {
        chunk.getBlockEntities().forEach((pos, blockEntity) -> {
            CHBlockEntityTickers.tryTick(pos, level, blockEntity);
        });
    }
    @FunctionalInterface
    public interface Ticker {
        void tick(BlockPos pos, ServerLevel level, BlockEntity blockEntity);
    }

}
