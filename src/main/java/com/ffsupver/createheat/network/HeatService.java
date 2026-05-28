package com.ffsupver.createheat.network;

import com.ffsupver.createheat.block.thermalBlock.BaseThermalBlockBehaviour;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
     * Get Network from level and networkID
     * @param level should be server level
     * @param networkID network id
     * @return
     */
    public static HeatNetwork getNetwork(Level level, UUID networkID){
        if (networkID == null || serviceData == null || !(level instanceof ServerLevel serverLevel)){
            return null;
        }

        return serviceData.getNetwork(serverLevel.dimension(),networkID);
    }

    /**
     * Add a Block to Network and try to merge with neighbor network
     * @param pos pos to add
     * @param level should be server level
     * @param allNeighborNetworkIDs all neighbor network id from the pos
     * @return network id the pos added to
     */
    public static UUID addBlockToNetwork(BlockPos pos,Level level,Set<UUID> allNeighborNetworkIDs){
        if (serviceData == null || !(level instanceof ServerLevel serverLevel)){
            return null;
        }

        System.out.println("AddAndMerge:"+pos+" ids:"+allNeighborNetworkIDs);
        if (allNeighborNetworkIDs.isEmpty()){
            return addBlockToNetwork(pos,level,(UUID)null);
        }else if (allNeighborNetworkIDs.size() == 1){
            return addBlockToNetwork(pos,level,allNeighborNetworkIDs.iterator().next());
        }else {
            Set<HeatNetwork> neighborNetworks = new HashSet<>();
            for (UUID networkID : allNeighborNetworkIDs){
                HeatNetwork network = getNetwork(serverLevel,networkID);
                if (network != null){
                    neighborNetworks.add(network);
                }
            }

            UUID finalNetworkId = mergeNetwork(serverLevel,neighborNetworks);
            return addBlockToNetwork(pos,level,finalNetworkId);
        }
    }

    /**
     * Merge multiple networks into one
     * @param networks networks to merge
     * @return network id of the final network
     */
    public static UUID mergeNetwork(ServerLevel serverLevel,Set<HeatNetwork> networks){
        if (networks.isEmpty()){
            return null;
        }if(networks.size() == 1){
            return networks.iterator().next().getNetworkID();
        }

        Iterator<HeatNetwork> iterator = networks.iterator();
        HeatNetwork finalNetwork = iterator.next();
        while (iterator.hasNext()){
            HeatNetwork network = iterator.next();
            network.mergeSelfTo(serverLevel,finalNetwork);
        }

        return finalNetwork.getNetworkID();
    }

    /**
     * Add a single Block to Network
     * @param pos pos to add
     * @param level should be server level
     * @param networkID id find from neighbor network,or null if not found
     * @return network id the pos added to
     */
    public static UUID addBlockToNetwork(BlockPos pos,Level level,UUID networkID){
        if (serviceData == null || !(level instanceof ServerLevel serverLevel)){
            return null;
        }

        HeatNetwork heatNetwork = getNetwork(level,networkID);

        System.out.println("AddBlock network:"+networkID+"n "+heatNetwork);
        if (heatNetwork == null){
            networkID = UUID.randomUUID();
            heatNetwork = new HeatNetwork(networkID,new HashSet<>(Set.of(pos)));
            serviceData.addNetwork(serverLevel.dimension(),heatNetwork);
        }else {
            heatNetwork.addBlock(pos,true);
        }
        System.out.println("AddBlock network:"+networkID);
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

        HeatNetwork heatNetwork = getNetwork(serverLevel,networkID);
        System.out.println("removing block"+pos+" network:"+networkID+"n "+heatNetwork);
        if (heatNetwork != null){
            heatNetwork.removeBlock(pos);
            serviceData.setDirty();
        }
    }

    /**
     * On Heat Network Split.
     * try to create new network for disconnected blocks
     * @param disconnectedBlocks disconnected blocks from original network
     * @return new networks created from disconnected blocks
     */
    public static Set<HeatNetwork> onHeatNetworkSplit(Set<BlockPos> disconnectedBlocks,ServerLevel level){
        if (disconnectedBlocks.isEmpty()){
            return Set.of();
        }

        Set<HeatNetwork> newNetworks = new HashSet<>();
        Set<BlockPos> remainingBlocks = new HashSet<>(disconnectedBlocks);
        while (!remainingBlocks.isEmpty()){
            Set<BlockPos> connectedBlocks = new HashSet<>();
            BlockUtil.walkAllBlocks(remainingBlocks.iterator().next(),connectedBlocks,remainingBlocks::contains);
            HeatNetwork newNetwork = new HeatNetwork(UUID.randomUUID(),connectedBlocks);
            for (BlockPos pos : connectedBlocks){
                BaseThermalBlockBehaviour baseThermalBlockBehaviour = BlockEntityBehaviour.get(level,pos,BaseThermalBlockBehaviour.TYPE);
                if (baseThermalBlockBehaviour != null){
                    baseThermalBlockBehaviour.setHeatNetworkId(newNetwork.getNetworkID());
                }
            }
            newNetworks.add(newNetwork);
            serviceData.addNetwork(level.dimension(),newNetwork); // will serviceData is null ?
            remainingBlocks.removeAll(connectedBlocks);
        }
        System.out.println("disconnectedBlocks:"+disconnectedBlocks+"\nnew networks:"+newNetworks);

        return newNetworks;
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
        private final Map<ResourceKey<Level>,Set<HeatNetwork>> networkMapToAddNextTick = new HashMap<>();

        public HeatServiceData(Map<ResourceKey<Level>, Map<UUID,HeatNetwork>> networks) {
            NETWORKS = networks;
        }

        public void tick(ServerLevel serverLevel){
            ResourceKey<Level> levelKey = serverLevel.dimension();
            boolean needSave = false;
            if (!NETWORKS.containsKey(levelKey)){
                NETWORKS.put(levelKey,new HashMap<>());
            }
            Map<UUID,HeatNetwork> networks = NETWORKS.get(levelKey);


            //add network in networkMapToAddNextTick
            needSave = !networkMapToAddNextTick.isEmpty();
            System.out.println("adding network:"+networkMapToAddNextTick+" levelKey:"+levelKey);
            networkMapToAddNextTick.getOrDefault(levelKey,Set.of()).forEach(heatNetwork -> {
                networks.put(heatNetwork.getNetworkID(),heatNetwork);
            });
            System.out.println("networks:"+networks+" levelKey:"+levelKey);
            networkMapToAddNextTick.remove(levelKey);

            // tick each network
            Set<UUID> networkNeedToRemove = new HashSet<>();
            System.out.println("ticking:"+serverLevel.dimension()+" level:"+serverLevel);
            for (HeatNetwork network : networks.values()){
                System.out.println("network T:"+network);
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
            Set<HeatNetwork> levelNetworks = networkMapToAddNextTick.getOrDefault(levelKey,new HashSet<>());
            levelNetworks.add(network);
            networkMapToAddNextTick.put(levelKey,levelNetworks);
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
