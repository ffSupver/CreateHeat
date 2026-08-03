package com.ffsupver.createheat.network;

import com.ffsupver.createheat.block.NetworkBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

public class HeatStorageService {
    private static final String NAME = "heat_storage_service";

    public static class HeatStorageServiceData extends ServiceData<HeatStorageNetwork>{
        private static final HeatStorageServiceFactory FACTORY = new HeatStorageServiceFactory();

        public HeatStorageServiceData(Map<ResourceKey<Level>, Map<UUID, HeatStorageNetwork>> networks) {
            super(networks, NAME);
        }

        public HeatStorageServiceData() {
            this(new HashMap<>());
        }

        public static Factory<HeatStorageServiceData> factory(){
            return new Factory<>(()->{
                throw new RuntimeException("No Heat Service");
            },(tag,levelRegistry)->ServiceData.load(tag,levelRegistry,NAME,HeatStorageNetwork::fromNbt,FACTORY)
            );
        }

        @Override
        public HeatStorageNetwork createNetwork(UUID networkID, Collection<BlockPos> originalPos) {
            return new HeatStorageNetwork(networkID,new HashSet<>(originalPos));
        }

        @Override
        public void changeBlockNetwork(BlockPos pos, ServerLevel level, UUID newNetworkID) {
            NetworkBehaviour networkBehaviour = NetworkBehaviour.get(level,pos,NetworkBehaviour.TYPE);
            if (networkBehaviour != null && networkBehaviour.checkNetworkType(NetworkService.Services.HEAT_STORAGE)){
                networkBehaviour.setNetworkId(newNetworkID);
            }
//            HeatStorageBehaviour heatStorageBehaviour = BlockEntityBehaviour.get(level,pos,HeatStorageBehaviour.TYPE);
//            if (heatStorageBehaviour != null){
//                heatStorageBehaviour.setNetworkId(newNetworkID);
//            }
        }


        private final static class HeatStorageServiceFactory implements ServiceFactory<HeatStorageNetwork,HeatStorageServiceData>{
            @Override
            public HeatStorageServiceData create(Map<ResourceKey<Level>, Map<UUID, HeatStorageNetwork>> network, String name) {
                return new HeatStorageServiceData(network);
            }
        }
    }
}
