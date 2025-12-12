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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CHHeatTransferProcessers {
    private static final CreateRegistrate REGISTRATE = CreateHeat.registrate();
    private static final Set<Predicate<BlockState>> NEED_TO_HEAT_UP_OPTIONAL = new HashSet<>();
    public static final ResourceKey<Registry<HeatTransferProcesserBuilder>> HEAT_PROCESSOR_REGISTRY_KEY = REGISTRATE.makeRegistry(
            "htp", RegistryBuilder::new
);


    public static void bootSetup(){
        registerHeatTransferProcesser(HeatRecipeTransferProcesser.TYPE.getPath(),()->HeatRecipeTransferProcesser::new);
        registerHeatTransferProcesser(OptionalNeedHeatUpBlockHTP.TYPE.getPath(),()->OptionalNeedHeatUpBlockHTP::new);
    }

    /** Register HeatTransferProcesser only with name space "createheat"
     * @param name path of the id, name space is "createheat"
     */
    public static void registerHeatTransferProcesser(String name,Supplier<HeatTransferProcesserBuilder> heatTransferProcesserBuilder){
        REGISTRATE.generic(name,HEAT_PROCESSOR_REGISTRY_KEY, NonNullSupplier.of(heatTransferProcesserBuilder)).register();
    }

    public static void registerOptionalNeedHeatBlock(Predicate<BlockState> tester){
        NEED_TO_HEAT_UP_OPTIONAL.add(tester);
    }

    public static Optional<HeatTransferProcesser> findProcesser(Level level, BlockPos blockPos, Direction face,int heat,int tickSkip,int superHeatCount){
       return REGISTRATE.getAll(HEAT_PROCESSOR_REGISTRY_KEY).stream().map(d->d.get().create())
               .filter(h->h.needHeat(level,blockPos,face,heat,tickSkip,superHeatCount)).findFirst();
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

    private static class OptionalNeedHeatUpBlockHTP extends HeatTransferProcesser {
        protected static ResourceLocation TYPE = CreateHeat.asResource("optional_need_heat_up");
        public OptionalNeedHeatUpBlockHTP() {
            super(TYPE);
        }
        @Override
        public boolean needHeat(Level level, BlockPos pos, @Nullable Direction face,int heat,int tickSkip,int superHeatCount) {
            boolean dirT = face == null || Direction.UP.equals(face);
            if (!dirT){
                return false;
            }
            BlockState state = level.getBlockState(pos);
            return NEED_TO_HEAT_UP_OPTIONAL.stream().anyMatch(sP->sP.test(state));
        }
        @Override
        public boolean shouldHeatAt(Direction face) {
            return face.equals(Direction.UP);
        }
        @Override
        public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide, int tickSkip,int superHeatCount) {}
        @Override
        public boolean shouldProcessEveryTick() {return false;}
    }

}
