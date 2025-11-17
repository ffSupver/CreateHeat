package com.ffsupver.createheat.api;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.registries.CHDatapacks;
import com.ffsupver.createheat.util.HeatUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public record CustomHeater(BlockStateTester heaterState, int heatPerTick, int superHeatCount) implements HeatProvider {
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
        return registryAccess.lookupOrThrow(CHDatapacks.CUSTOM_HEATER).listElements().toList();
    }

    public static void registerBoilerHeater(RegistryAccess registryAccess){
        BoilerHeater.REGISTRY.registerProvider(SimpleRegistry.Provider.forBlockTag(CHTags.BlockTag.CUSTOM_BOILER_HEATER,new CustomBoilerHeater(registryAccess)));
    }

    @Override
    public int getHeatPerTick() {
        return heatPerTick;
    }

    @Override
    public int getSupperHeatCount() {
        return superHeatCount;
    }

    public static class CustomBoilerHeater implements BoilerHeater{
        private final RegistryAccess registryAccess;
        public CustomBoilerHeater(RegistryAccess registryAccess) {
            this.registryAccess = registryAccess;
        }

        public float getHeat (Level level, BlockPos pos, BlockState state){
            Optional<Holder.Reference<CustomHeater>> heaterOp = getFromBlockState(registryAccess, state);
            if (heaterOp.isPresent()) {
                return HeatUtil.toBoilerHeat(heaterOp.get().value().heatPerTick);
            } else {
                return BoilerHeater.NO_HEAT;
            }
        }
    }
}
