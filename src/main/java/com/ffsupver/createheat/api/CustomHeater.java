package com.ffsupver.createheat.api;

import com.ffsupver.createheat.registries.CHDataKeys;
import com.ffsupver.createheat.util.BlockUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public record CustomHeater(BlockState heaterState, int heatPerTick, int superHeatCount) {
    public static Codec<CustomHeater> CODEC = RecordCodecBuilder.create(i->i.group(
            BlockState.CODEC.fieldOf("state").forGetter(CustomHeater::heaterState),
            Codec.INT.fieldOf("heat_per_tick").forGetter(CustomHeater::heatPerTick),
            Codec.INT.fieldOf("super_heat_count").forGetter(CustomHeater::superHeatCount)
    ).apply(i, CustomHeater::new));

    public static Optional<Holder.Reference<CustomHeater>> getFromBlockState(RegistryAccess registryAccess, BlockState state){
        return getAll(registryAccess).stream()
                .filter(customHeaterReference -> BlockUtil.checkState(state,customHeaterReference.value().heaterState))
                .findFirst();
    }
    public static List<Holder.Reference<CustomHeater>> getAll(RegistryAccess registryAccess){
        return registryAccess.lookupOrThrow(CHDataKeys.CUSTOM_HEATER).listElements().toList();
    }
}
