package com.ffsupver.createheat.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class NetworkService {
    private static final Map<String,ServiceData<?>> SERVICE_DATA_MAP = new HashMap<>();

    public static void onServerTickPost(ServerTickEvent.Post event){
        MinecraftServer server = event.getServer();


        for (Services services : Services.values()){
            if (!SERVICE_DATA_MAP.containsKey(services.id)){
                DimensionDataStorage dataStorage = server.overworld().getDataStorage();
                ServiceData<?> serviceData = dataStorage.get((SavedData.Factory<ServiceData<?>>) (SavedData.Factory<?>) services.factoryFromNbt, services.id);
                if (serviceData == null){
                    serviceData = services.factory.get();
                    dataStorage.set(services.id, serviceData);
                }
                SERVICE_DATA_MAP.put(services.id, serviceData);
            }


            ServiceData<?> serviceData = SERVICE_DATA_MAP.get(services.id);
            server.getAllLevels().forEach(serviceData::tick);
        }
    }

    /**
     * On Minecraft Saved
     *  set serviceData null to prevent other level get the wrong network
     *
     */
    public static void onServerStop(ServerStoppingEvent event) {
        SERVICE_DATA_MAP.clear();
    }


    public static ServiceData<?> getService(Services services){
        return SERVICE_DATA_MAP.get(services.id);
    }

    public static boolean isLoad(Services services){
        return SERVICE_DATA_MAP.containsKey(services.id);
    }

    /**
     * Get Network from level and networkID
     * @param level should be server level
     * @param networkID network id
     * @param services network service type
     * @return
     */
    public static TickingBlockNetwork getNetwork(Level level, UUID networkID,Services services){
        if (networkID == null || !SERVICE_DATA_MAP.containsKey(services.id) || !(level instanceof ServerLevel serverLevel)){
            return null;
        }

        return SERVICE_DATA_MAP.get(services.id).getNetwork(serverLevel.dimension(),networkID);
    }

    /**
     * Add a Block to Network and try to merge with neighbor network
     * @param pos pos to add
     * @param level should be server level
     * @param allNeighborNetworkIDs all neighbor network id from the pos
     * @param services network service type
     * @return network id the pos added to
     */
    public static UUID addBlockToNetwork(BlockPos pos, Level level, Set<UUID> allNeighborNetworkIDs,Services services){
        if (!SERVICE_DATA_MAP.containsKey(services.id) || !(level instanceof ServerLevel serverLevel)){
            return null;
        }

        return SERVICE_DATA_MAP.get(services.id).addBlockToNetwork(pos,serverLevel,allNeighborNetworkIDs);
    }

    /**
     * Remove Block from Network, call when block removed
     **/
    public static void removeBlockFromNetwork(Level level, BlockPos pos, UUID networkID,Services services) {
        if (!SERVICE_DATA_MAP.containsKey(services.id) || !(level instanceof ServerLevel serverLevel)){
            return;
        }

        SERVICE_DATA_MAP.get(services.id).removeBlockFromNetwork(serverLevel,pos,networkID);
    }

    /**
     * On Network Split.
     * try to create new network for disconnected blocks
     * @param disconnectedBlocks disconnected blocks from original network
     * @param services network service type
     * @return new networks created from disconnected blocks
     */
    public static Set<? extends TickingBlockNetwork> onNetworkSplit(Set<BlockPos> disconnectedBlocks,ServerLevel level,Services services){
        if (!SERVICE_DATA_MAP.containsKey(services.id) || !(level instanceof ServerLevel serverLevel)){
            return Set.of();
        }

        return SERVICE_DATA_MAP.get(services.id).onNetworkSplit(disconnectedBlocks,level);
    }



    public enum Services{
        HEAT("heat_service_data", HeatService.HeatServiceData.factory(), HeatService.HeatServiceData::new),
        HEAT_STORAGE("heat_storage_service_data", HeatStorageService.HeatStorageServiceData.factory(),HeatStorageService.HeatStorageServiceData::new),;
        public final String id;
        private final SavedData.Factory<? extends ServiceData<? extends TickingBlockNetwork>> factoryFromNbt;
        private final Supplier<? extends ServiceData<? extends TickingBlockNetwork>> factory;
        Services(String id, SavedData.Factory<? extends ServiceData<? extends TickingBlockNetwork>> factoryFromNbt, Supplier<? extends ServiceData<? extends TickingBlockNetwork>> factory){
            this.id = id;
            this.factoryFromNbt = factoryFromNbt;
            this.factory = factory;
        }
    }
}
