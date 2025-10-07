package com.ffsupver.createheat.api;

import com.ffsupver.createheat.registries.CHDataKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public record CustomHeater(BlockStateTester heaterState, int heatPerTick, int superHeatCount) {
    public static Codec<CustomHeater> CODEC = RecordCodecBuilder.create(i->i.group(
            BlockStateTester.CODEC.fieldOf("block").forGetter(CustomHeater::heaterState),
            Codec.INT.fieldOf("heat_per_tick").forGetter(CustomHeater::heatPerTick),
            Codec.INT.fieldOf("super_heat_count").forGetter(CustomHeater::superHeatCount)
    ).apply(i, CustomHeater::new));

    public static Optional<Holder.Reference<CustomHeater>> getFromBlockState(RegistryAccess registryAccess, BlockState state){
        List<Holder.Reference<CustomHeater>> allList = getAll(registryAccess);
        return allList.stream()
                .filter(customHeaterReference -> customHeaterReference.value().heaterState.test(state))
                .findFirst();
    }
    public static List<Holder.Reference<CustomHeater>> getAll(RegistryAccess registryAccess){
        return registryAccess.lookupOrThrow(CHDataKeys.CUSTOM_HEATER).listElements().toList();
    }
}
