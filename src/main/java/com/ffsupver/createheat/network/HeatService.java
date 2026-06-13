package com.ffsupver.createheat.network;

import com.ffsupver.createheat.block.thermalBlock.BaseThermalBlockBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

import static com.ffsupver.createheat.network.NetworkService.Services.HEAT;

public class HeatService {
    private static final String NAME = "heat_service";

    /**
     * Get Network from level and networkID
     * @param level should be server level
     * @param networkID network id
     * @return
     */
    public static HeatNetwork getNetwork(Level level, UUID networkID){
        if (NetworkService.getNetwork(level,networkID, HEAT) instanceof HeatNetwork heatNetwork){
            return heatNetwork;
        }
        return null;
    }

    /**
     * Add a Block to Network and try to merge with neighbor network
     * @param pos pos to add
     * @param level should be server level
     * @param allNeighborNetworkIDs all neighbor network id from the pos
     * @return network id the pos added to
     */
    public static UUID addBlockToNetwork(BlockPos pos,Level level,Set<UUID> allNeighborNetworkIDs){
        return NetworkService.addBlockToNetwork(pos,level,allNeighborNetworkIDs, HEAT);
    }

    /**
     * Remove Block from Network, call when block removed
     **/
    public static void removeBlockFromNetwork(Level level, BlockPos pos, UUID networkID) {
        NetworkService.removeBlockFromNetwork(level,pos,networkID,HEAT);
    }

    /**
     * On Heat Network Split.
     * try to create new network for disconnected blocks
     * @param disconnectedBlocks disconnected blocks from original network
     * @return new networks created from disconnected blocks
     */
    public static Set<? extends TickingBlockNetwork> onHeatNetworkSplit(Set<BlockPos> disconnectedBlocks,ServerLevel level){
        return NetworkService.onNetworkSplit(disconnectedBlocks,level,HEAT);
    }

    public static class HeatServiceData extends ServiceData<HeatNetwork> {
        private static final HeatServiceFactory FACTORY = new HeatServiceFactory();

        public HeatServiceData(Map<ResourceKey<Level>, Map<UUID,HeatNetwork>> networks) {
            super(networks,NAME);
        }


        @Override
        public HeatNetwork createNetwork(UUID networkID, Collection<BlockPos> originalPos) {
            return new HeatNetwork(networkID,new HashSet<>(originalPos));
        }

        @Override
        public void changeBlockNetwork(BlockPos pos, ServerLevel level, UUID newNetworkID) {
            BaseThermalBlockBehaviour baseThermalBlockBehaviour = BlockEntityBehaviour.get(level,pos,BaseThermalBlockBehaviour.TYPE);
            if (baseThermalBlockBehaviour != null){
                baseThermalBlockBehaviour.setHeatNetworkId(newNetworkID);
            }
        }

        public static Factory<HeatServiceData> factory(){
            return new Factory<>(()->{
              throw new RuntimeException("No Heat Service");
            },(tag,levelRegistry)->ServiceData.load(tag,levelRegistry,NAME,HeatNetwork::fromNbt,FACTORY)
            );
        }


        private final static class HeatServiceFactory implements ServiceFactory<HeatNetwork,HeatServiceData>{
            @Override
            public HeatServiceData create(Map<ResourceKey<Level>, Map<UUID, HeatNetwork>> network, String name) {
                return new HeatServiceData(network);
            }
        }
    }
}
