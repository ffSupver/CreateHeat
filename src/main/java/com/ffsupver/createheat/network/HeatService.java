package com.ffsupver.createheat.network;

import com.ffsupver.createheat.util.NbtUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HeatService {
    private static HeatServiceData serviceData;
    public static void onServerTickPost(ServerTickEvent.Post event){
        MinecraftServer server = event.getServer();
        if (serviceData == null){
            DimensionDataStorage dataStorage = server.overworld().getDataStorage();
           serviceData = dataStorage.get(HeatServiceData.factory(),"heat_service_data");
           if (serviceData == null){
               serviceData = new HeatServiceData(new HashMap<>());
               dataStorage.set("heat_service_data",serviceData);
           }
        }

        server.getAllLevels().forEach(serverLevel -> {
            serviceData.tick(serverLevel);
        });
    }

    public static class HeatServiceData extends SavedData {
        private final Map<ResourceKey<Level>, Set<HeatNetwork>> NETWORKS;

        public HeatServiceData(Map<ResourceKey<Level>, Set<HeatNetwork>> networks) {
            NETWORKS = networks;
        }

        public void tick(ServerLevel serverLevel){
            ResourceKey<Level> levelKey = serverLevel.dimension();
            Set<HeatNetwork> networks = NETWORKS.getOrDefault(levelKey,Set.of());

            boolean needSave = false;
            for (HeatNetwork network : networks){
                if(network.tick(serverLevel)){
                    needSave = true;
                }
            }

            if (needSave){
                this.setDirty();
            }
        }


        public static HeatServiceData load(CompoundTag tag, HolderLookup.Provider levelRegistry) {
            Map<ResourceKey<Level>, Set<HeatNetwork>> networkMap = NbtUtil.readMapFromNbtList(
                    tag.getList("heat_service", Tag.TAG_COMPOUND),
                    keyTag-> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(((CompoundTag)keyTag).getString("level_id"))),
                    vTag->Set.copyOf(NbtUtil.readListFromNbt(((ListTag)vTag),HeatNetwork::fromNbt))
                    );
            return new HeatServiceData(networkMap);
        }

        public static Factory<HeatServiceData> factory(){
            return new Factory<>(()->{
              throw new RuntimeException("No Heat Service");
            },HeatServiceData::load);
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.put("heat_service",NbtUtil.writeMapToNbtList(
                    NETWORKS,
                    levelKey-> {
                        CompoundTag keyTag = new CompoundTag();
                        keyTag.putString("level_id", levelKey.location().toString());
                        return keyTag;
                    },networkSet->NbtUtil.writeToNbtList(networkSet,HeatNetwork::toNbt)
                    ));
            return nbt;
        }
    }
}
