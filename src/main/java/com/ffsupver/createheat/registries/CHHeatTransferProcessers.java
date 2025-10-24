package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.recipe.HeatRecipeTransferProcesser;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Optional;
import java.util.function.Supplier;

public class CHHeatTransferProcessers {
    private static final CreateRegistrate REGISTRATE = CreateHeat.registrate();
    public static final ResourceKey<Registry<HeatTransferProcesserBuilder>> HEAT_PROCESSOR_REGISTRY_KEY = REGISTRATE.makeRegistry(
            "htp", RegistryBuilder::new
);


    public static void bootSetup(){
        registerHeatTransferProcesser(HeatRecipeTransferProcesser.TYPE.getPath(),()->HeatRecipeTransferProcesser::new);
    }

    /** Register HeatTransferProcesser only with name space "createheat"
     * @param name path of the id, name space is "createheat"
     */
    public static void registerHeatTransferProcesser(String name,Supplier<HeatTransferProcesserBuilder> heatTransferProcesserBuilder){
        REGISTRATE.generic(name,HEAT_PROCESSOR_REGISTRY_KEY, NonNullSupplier.of(heatTransferProcesserBuilder)).register();
    }

    public static Optional<HeatTransferProcesser> findProcesser(Level level, BlockPos blockPos, Direction face){
       return REGISTRATE.getAll(HEAT_PROCESSOR_REGISTRY_KEY).stream().map(d->d.get().create()).filter(h->h.needHeat(level,blockPos,face)).findFirst();
    }

    public static HeatTransferProcesser fromNbt(Tag tag){
        CompoundTag nbt = (CompoundTag) tag;
        if (nbt.contains("type", Tag.TAG_STRING)){
           Optional<HeatTransferProcesser> heatTransferProcesserOp = REGISTRATE.getAll(HEAT_PROCESSOR_REGISTRY_KEY)
                    .stream().filter(
                            rE->rE.getId().equals(ResourceLocation.parse(nbt.getString("type")))
                    ).map(d->d.get().create()).findFirst();
           if (heatTransferProcesserOp.isPresent()){
               HeatTransferProcesser heatTransferProcesser = heatTransferProcesserOp.get();
               heatTransferProcesser.fromNbt(nbt);
               return heatTransferProcesser;
           }
        }
        return null;
    }

    public static CompoundTag toNbt(HeatTransferProcesser heatTransferProcesser){
        if (heatTransferProcesser.shouldWriteAndReadFromNbt()){
            CompoundTag nbt = heatTransferProcesser.toNbt();
            nbt.putString("type",heatTransferProcesser.getTypeId().toString());
            return nbt;
        }
        return null;
    }

    @FunctionalInterface
    public interface HeatTransferProcesserBuilder{
        HeatTransferProcesser create();
    }
}
