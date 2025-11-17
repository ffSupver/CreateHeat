package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.api.CustomHeater;
import com.ffsupver.createheat.api.SimpleHeatProvider;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.item.thermalTool.ThermalToolPointLogic;
import com.ffsupver.createheat.item.thermalTool.ThermalToolPointServer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class CHHeatProviders {
    private static final CreateRegistrate REGISTRATE = CreateHeat.registrate();
    public static final ResourceKey<Registry<HeatFinder>> HEAT_FINDER_REGISTRY_KEY = REGISTRATE.makeRegistry(
            "heat_provider", RegistryBuilder::new
    );

    /** Register HeatFinder only with name space "createheat"
     * @param name path of the id, name space is "createheat"
     */
    public static void registerHeatFinder(String name, Supplier<HeatFinder> heatFinderSupplier){
        REGISTRATE.generic(name,HEAT_FINDER_REGISTRY_KEY, NonNullSupplier.of(heatFinderSupplier)).register();
    }
    public static void bootSetup(){
        registerHeatFinder("block_entity",()->(level, pos, state)->{
            if (level.getBlockEntity(pos) instanceof HeatProvider provider){
                return provider;
            }else {
                return null;
            }
        });
        registerHeatFinder("custom_heater",()->((level, pos, state) -> {
            Optional<Holder.Reference<CustomHeater>> customHeatOp = CustomHeater.getFromBlockState(level.registryAccess(),level.getBlockState(pos));
            return customHeatOp.<HeatProvider>map(Holder.Reference::value).orElse(null);
        }));
        registerHeatFinder("thermal_tool_point",()->(level, pos, state)->{
            ThermalToolPointLogic logic = ThermalToolPointServer.getPoint(level.dimension(),pos.above());
           if(ThermalToolPointLogic.HEAT_SOURCE.equals(logic)){
                return new SimpleHeatProvider(Config.HEAT_PER_FADING_BLAZE.get(),0);
           } else if (ThermalToolPointLogic.SUPER_HEAT_SOURCE.equals(logic)) {
               return new SimpleHeatProvider(Config.HEAT_PER_SEETHING_BLAZE.get(),1);
           }
           return null;
        });
    }
    public static Optional<HeatProvider> findHeatProvider(Level level, BlockPos pos, BlockState state){
       return REGISTRATE.getAll(HEAT_FINDER_REGISTRY_KEY).stream().map(s->s.get().getHeatProvider(level,pos,state))
               .filter(Objects::nonNull).findFirst();
    }

    @FunctionalInterface
    public interface HeatFinder{
        HeatProvider getHeatProvider(Level level, BlockPos pos, BlockState state);
    }
}
