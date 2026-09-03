package com.ffsupver.createheat.network;

import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.function.Function;

public abstract class ServiceData<T extends TickingBlockNetwork> extends SavedData {
    protected final Map<ResourceKey<Level>, Map<UUID,T>> NETWORKS;
    protected final Map<ResourceKey<Level>, Set<T>> networkMapToAddNextTick = new HashMap<>();
    private final String name;

    public ServiceData(Map<ResourceKey<Level>, Map<UUID, T>> networks,String name) {
        this.name = name;
        NETWORKS = networks;
    }

    public void tick(ServerLevel serverLevel){
        ResourceKey<Level> levelKey = serverLevel.dimension();
        boolean needSave = false;
        if (!NETWORKS.containsKey(levelKey)){
            NETWORKS.put(levelKey,new HashMap<>());
        }
        Map<UUID,T> networks = NETWORKS.get(levelKey);


        //add network in networkMapToAddNextTick
        needSave = !networkMapToAddNextTick.isEmpty();
        networkMapToAddNextTick.getOrDefault(levelKey,Set.of()).forEach(network -> {
            networks.put(network.getNetworkID(),network);
        });
        networkMapToAddNextTick.remove(levelKey);

        // tick each network
        Set<UUID> networkNeedToRemove = new HashSet<>();
        for (T network : networks.values()){
            if(network.tick(serverLevel)){
                needSave = true;
            }
            if (network.shouldRemove()){
                networkNeedToRemove.add(network.getNetworkID());
                needSave = true;
            }
        }

        networkNeedToRemove.forEach(networks::remove);

        if (needSave){
            this.setDirty();
        }
    }

    public T getNetwork(ResourceKey<Level> levelKey,UUID uuid){
        if (NETWORKS.containsKey(levelKey)){
            if (NETWORKS.get(levelKey).containsKey(uuid)){
                return NETWORKS.get(levelKey).get(uuid);
            }
        }
        if (networkMapToAddNextTick.containsKey(levelKey)){
            Optional<T> networkOp = networkMapToAddNextTick.get(levelKey).stream().filter(network -> network.getNetworkID().equals(uuid)).findAny();
            if (networkOp.isPresent()){
                return networkOp.get();
            }
        }
        return null;
    }

    /**
     * Get all network id
     * @return all network id
     */
    public Map<ResourceKey<Level>, Set<UUID>> getAllNetworkId(){
        Map<ResourceKey<Level>, Set<UUID>> allNetworkId = new HashMap<>();
        for (Map.Entry<ResourceKey<Level>, Map<UUID, T>> entry : NETWORKS.entrySet()) {
            allNetworkId.put(entry.getKey(), entry.getValue().keySet());
        }
        return allNetworkId;
    }

    /**
     * Add a new network to be added next tick
     * @param levelKey level of the network
     * @param network the network to be added
     */
    public void addNetwork(ResourceKey<Level> levelKey,T network){
        Set<T> levelNetworks = networkMapToAddNextTick.getOrDefault(levelKey,new HashSet<>());
        levelNetworks.add(network);
        networkMapToAddNextTick.put(levelKey,levelNetworks);
    }


    /**
     * Add a single Block to Network
     * @param pos pos to add
     * @param level should be server level
     * @param networkID id find from neighbor network,or null if not found
     * @return network id the pos added to
     */
    public UUID addBlockToNetwork(BlockPos pos, ServerLevel level, UUID networkID){
        T network = getNetwork(level.dimension(),networkID);

        if (network == null){
            networkID = UUID.randomUUID();
            network = createNetwork(networkID,Set.of(pos));
            addNetwork(level.dimension(),network);
        }else {
            network.addBlock(pos,true);
        }
        setDirty();

        return networkID;
    }

    /**
     * Add a Block to Network and try to merge with neighbor network
     * @param pos pos to add
     * @param serverLevel should be server level
     * @param allNeighborNetworkIDs all neighbor network id from the pos
     * @return network id the pos added to
     */
    public UUID addBlockToNetwork(BlockPos pos,ServerLevel serverLevel,Set<UUID> allNeighborNetworkIDs){
        if (allNeighborNetworkIDs.isEmpty()){
            return addBlockToNetwork(pos,serverLevel,(UUID)null);
        }else if (allNeighborNetworkIDs.size() == 1){
            return addBlockToNetwork(pos,serverLevel,allNeighborNetworkIDs.iterator().next());
        }else {
            Set<T> neighborNetworks = new HashSet<>();
            for (UUID networkID : allNeighborNetworkIDs){
                T network = getNetwork(serverLevel.dimension(),networkID);
                if (network != null){
                    neighborNetworks.add(network);
                }
            }

            UUID finalNetworkId = mergeNetwork(serverLevel,neighborNetworks);
            return addBlockToNetwork(pos,serverLevel,finalNetworkId);
        }
    }

    /**
     * Merge multiple networks into one
     * @param networks networks to merge
     * @return network id of the final network
     */
    public UUID mergeNetwork(ServerLevel serverLevel,Set<T> networks){
        if (networks.isEmpty()){
            return null;
        }if(networks.size() == 1){
            return networks.iterator().next().getNetworkID();
        }

        Iterator<T> iterator = networks.iterator();
        T finalNetwork = iterator.next();
        while (iterator.hasNext()){
            T network = iterator.next();
            network.mergeSelfTo(serverLevel,finalNetwork);
        }

        return finalNetwork.getNetworkID();
    }

    /**
     * Remove Block from Network, call when block removed
     **/
    public void removeBlockFromNetwork(ServerLevel serverLevel, BlockPos pos, UUID networkID) {
        T network = getNetwork(serverLevel.dimension(),networkID);
        if (network != null){
            network.removeBlock(pos);
            setDirty();
        }
    }

    /**
     * On Network Split.
     * try to create new network for disconnected blocks
     * @param disconnectedBlocks disconnected blocks from original network
     * @return new networks created from disconnected blocks
     */
    public Set<T> onNetworkSplit(Set<BlockPos> disconnectedBlocks,ServerLevel level){
        if (disconnectedBlocks.isEmpty()){
            return Set.of();
        }

        Set<T> newNetworks = new HashSet<>();
        Set<BlockPos> remainingBlocks = new HashSet<>(disconnectedBlocks);
        while (!remainingBlocks.isEmpty()){
            Set<BlockPos> connectedBlocks = new HashSet<>();
            BlockUtil.walkAllBlocks(remainingBlocks.iterator().next(),connectedBlocks,remainingBlocks::contains);
            T newNetwork = createNetwork(UUID.randomUUID(),connectedBlocks);
            for (BlockPos pos : connectedBlocks){
                changeBlockNetwork(pos,level,newNetwork.getNetworkID());
            }
            newNetworks.add(newNetwork);
            addNetwork(level.dimension(),newNetwork);
            remainingBlocks.removeAll(connectedBlocks);
        }

        return newNetworks;
    }

    /**
     * To create a new network with correct type.
     * @param networkID id of new network
     * @param originalPos BlockPosSet should be added to new network
     * @return the new network
     */
    public abstract T createNetwork(UUID networkID,Collection<BlockPos> originalPos);

    /**
     * To change ID in the block that will be added to new network.
     * @param pos pos of the block whose network ID should be changed
     * @param newNetworkID the new network ID should be set into the block.
     */
    public abstract void changeBlockNetwork(BlockPos pos,ServerLevel level,UUID newNetworkID);
    

    public static <T extends TickingBlockNetwork,S extends ServiceData<T>> S load(CompoundTag tag, HolderLookup.Provider levelRegistry,String name,Function<Tag, T> networkFactory,ServiceFactory<T,S> serviceFactory) {
        Map<ResourceKey<Level>, Map<UUID,T>> networkMap = NbtUtil.readMapFromNbtList(
                tag.getList(name, Tag.TAG_COMPOUND),
                keyTag-> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(((CompoundTag)keyTag).getString("level_id"))),
                vTag->new HashMap<>(NbtUtil.readMapFromNbtList(
                        ((ListTag)vTag),
                        uuidTag->((CompoundTag)uuidTag).getUUID("uuid"),
                        networkFactory
                ))
        );
        return serviceFactory.create(networkMap,name);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.put(name,NbtUtil.writeMapToNbtList(
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
                        T::toNbt
                )
        ));
        return nbt;
    }

    @FunctionalInterface
    public interface ServiceFactory<T extends TickingBlockNetwork,S extends ServiceData<T>>{
        S create(Map<ResourceKey<Level>, Map<UUID,T>> network,String name);
    }
}
