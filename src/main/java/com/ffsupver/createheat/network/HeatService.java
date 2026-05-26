package com.ffsupver.createheat.network;

import com.ffsupver.createheat.util.NbtUtil;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

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

    /**
     * Add Block to Network, call when block placed
     * @param pos pos to add
     * @param level should be server level
     * @param networkID id find from neighbor network,or null if not found
     * @return
     */
    public static UUID addBlockToNetwork(BlockPos pos,Level level,UUID networkID){
        if (serviceData == null || !(level instanceof ServerLevel serverLevel)){
            return null;
        }

        HeatNetwork heatNetwork = null;
        if (networkID != null){
            heatNetwork = serviceData.getNetwork(serverLevel.dimension(),networkID);
        }

        System.out.println("network:"+networkID+"n "+heatNetwork);
        if (heatNetwork == null){
            networkID = UUID.randomUUID();
            heatNetwork = new HeatNetwork(networkID,new HashSet<>(Set.of(pos)));
            serviceData.addNetwork(serverLevel.dimension(),heatNetwork);
        }else {
            heatNetwork.addBlock(pos);
        }
        System.out.println("network:"+networkID);
        serviceData.setDirty();

        return networkID;
    }

    /**
     * Remove Block from Network, call when block removed
     **/
    public static void removeBlockFromNetwork(Level level, BlockPos pos, UUID networkID) {
        if (serviceData == null || !(level instanceof ServerLevel serverLevel)){
            return;
        }

        HeatNetwork heatNetwork = serviceData.getNetwork(serverLevel.dimension(),networkID);
        System.out.println("removing block"+pos+" network:"+networkID+"n "+heatNetwork);
        if (heatNetwork != null){
            heatNetwork.removeBlock(pos);
            serviceData.setDirty();
        }
    }

    /**
     * On Minecraft Saved
     *  set serviceData null to prevent other level get the wrong network
     *
     */
    public static void onServerStop(ServerStoppingEvent event) {
        serviceData = null;
    }

    public static class HeatServiceData extends SavedData {
        private final Map<ResourceKey<Level>, Map<UUID,HeatNetwork>> NETWORKS;

        public HeatServiceData(Map<ResourceKey<Level>, Map<UUID,HeatNetwork>> networks) {
            NETWORKS = networks;
        }

        public void tick(ServerLevel serverLevel){
            ResourceKey<Level> levelKey = serverLevel.dimension();
            Map<UUID,HeatNetwork> networks = NETWORKS.getOrDefault(levelKey,Map.of());

            boolean needSave = false;
            Set<UUID> networkNeedToRemove = new HashSet<>();
            System.out.println("ticking:"+serverLevel.dimension()+" level:"+serverLevel);
            for (HeatNetwork network : networks.values()){
                System.out.println("levelKey:"+serverLevel.dimension()+"\nnetwork:"+network);
                if(network.tick(serverLevel)){
                    needSave = true;
                }
                if (network.shouldRemove()){
                    networkNeedToRemove.add(network.getNetworkID());
                    needSave = true;
                }
            }

            System.out.println("to Remove:"+networkNeedToRemove);
            networkNeedToRemove.forEach(networks::remove);

            if (needSave){
                this.setDirty();
            }
        }

        public HeatNetwork getNetwork(ResourceKey<Level> levelKey,UUID uuid){
            if (NETWORKS.containsKey(levelKey)){
                return NETWORKS.get(levelKey).get(uuid);
            }
            return null;
        }

        public void addNetwork(ResourceKey<Level> levelKey,HeatNetwork network){
            Map<UUID,HeatNetwork> levelNetworks =NETWORKS.getOrDefault(levelKey,new HashMap<>());
            levelNetworks.put(network.getNetworkID(),network);
            NETWORKS.put(levelKey,levelNetworks);
        }


        public static HeatServiceData load(CompoundTag tag, HolderLookup.Provider levelRegistry) {
            Map<ResourceKey<Level>, Map<UUID,HeatNetwork>> networkMap = NbtUtil.readMapFromNbtList(
                    tag.getList("heat_service", Tag.TAG_COMPOUND),
                    keyTag-> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(((CompoundTag)keyTag).getString("level_id"))),
                    vTag->new HashMap<>(NbtUtil.readMapFromNbtList(
                            ((ListTag)vTag),
                            uuidTag->((CompoundTag)uuidTag).getUUID("uuid"),
                            HeatNetwork::fromNbt
                    ))
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
                    },
                    networkMap->NbtUtil.writeMapToNbtList(
                            networkMap,uuid-> {
                                CompoundTag uuidTag = new CompoundTag();
                                uuidTag.putUUID("uuid",uuid);
                                return uuidTag;
                            },
                            HeatNetwork::toNbt
                    )
                    ));
            return nbt;
        }
    }
}
