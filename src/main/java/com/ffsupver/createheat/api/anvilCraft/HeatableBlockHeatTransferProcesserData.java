package com.ffsupver.createheat.api.anvilCraft;

import com.ffsupver.createheat.api.BlockStateTester;
import com.ffsupver.createheat.compat.anvilCraft.AnvilCraft;
import com.ffsupver.createheat.util.DataUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public class HeatableBlockHeatTransferProcesserData {
    public static Codec<HeatableBlockHeatTransferProcesserData> CODEC = RecordCodecBuilder.create(i->i.group(
            Codec.list(HeatTierLineData.CODEC).fieldOf("heat_tier_line").forGetter(HeatableBlockHeatTransferProcesserData::heatTierLineBuilderData),
            Codec.list(BlockStateTester.CODEC).fieldOf("blocks").forGetter(HeatableBlockHeatTransferProcesserData::testers)
    ).apply(i, HeatableBlockHeatTransferProcesserData::new));


    private final List<HeatTierLineData> heatTierLineBuilderData;
    private final List<BlockStateTester> testers;
    private final HeatTierLine heatTierLine;

    public HeatableBlockHeatTransferProcesserData(List<HeatTierLineData> heatTierLineBuilderData, List<BlockStateTester> testers) {
        this.heatTierLineBuilderData = heatTierLineBuilderData;
        this.testers = testers;
        this.heatTierLine = buildHeatTierLine(heatTierLineBuilderData);
    }

    public List<BlockStateTester> testers(){
        return testers;
    }

    public List<HeatTierLineData> heatTierLineBuilderData() {
        return heatTierLineBuilderData;
    }

    public HeatTierLine heatTierLine(){
        return heatTierLine;
    }

    public static Optional<Holder.Reference<HeatableBlockHeatTransferProcesserData>> getFromBlockState(BlockState state, RegistryAccess registryAccess){
       return DataUtil.getLastMatchData(AnvilCraft.HEATABLE_BLOCK_HTP_DATA,registryAccess,h -> h.value().testers().stream().anyMatch(tester -> tester.test(state)));
    }

    private static HeatTierLine buildHeatTierLine(List<HeatTierLineData> dataList){
       HeatTierLine.LineBuilder builder = HeatTierLine.builder();
       dataList.forEach(data->builder.addPoint(data.toNext,data.heatTier,data.duration));
       return builder.build();
    }

    public record HeatTierLineData(HeatTier heatTier,int toNext,int duration){
        public static Codec<HeatTierLineData> CODEC = RecordCodecBuilder.create(i->i.group(
                HeatTier.LOWER_NAME_CODEC.fieldOf("heat_tier").forGetter(HeatTierLineData::heatTier),
                Codec.INT.optionalFieldOf("to_next",Integer.MAX_VALUE).forGetter(HeatTierLineData::toNext),
                Codec.INT.optionalFieldOf("duration",0).forGetter(HeatTierLineData::duration)
        ).apply(i,HeatTierLineData::new));
    }
}
