package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.block.HeatTransferProcesser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CHHeatTransferProcessers {
    private static final List<HeatTransferProcesser> HEAT_TRANSFER_PROCESSER = new ArrayList<>();

    public static void bootSetup(){
    }

    public static void registerHeatTransferProcesser(HeatTransferProcesser heatTransferProcesser){
        HEAT_TRANSFER_PROCESSER.add(heatTransferProcesser);
    }

    public static Optional<HeatTransferProcesser> findProcesser(Level level, BlockPos blockPos, Direction face){
       return HEAT_TRANSFER_PROCESSER.stream().filter(h->h.needHeat(level,blockPos,face)).findFirst();
    }
}
