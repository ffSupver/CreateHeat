package com.ffsupver.createheat.block;

import com.ffsupver.createheat.network.DirectionalNetworkConnect;
import com.ffsupver.createheat.network.NetworkService;
import com.ffsupver.createheat.network.TickingBlockNetwork;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NetworkBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<NetworkBehaviour> TYPE = new BehaviourType<>();
    private final NetworkService.Services networkType;

    private UUID networkId;
    private BlockPos lastPos;
    private TickingBlockNetwork network;


    public NetworkBehaviour(SmartBlockEntity be, NetworkService.Services networkType) {
        super(be);
        this.networkType = networkType;
    }

    @Override
    public void tick() {
        super.tick();
        if (lastPos == null){
            lastPos = getPos();
        }

        if (!getPos().equals(lastPos)){
            if (network != null){
                network.removeBlock(lastPos);
            }
            networkId = null;
            network = null;
            lastPos = getPos();
        }

        // try to create network or find neighbor network
        if (networkId == null){
            networkId = NetworkService.addBlockToNetwork(getPos(), getWorld(),getNeighborNetworkId(), networkType);
        }

        // try to get network
        if (network == null) {
            network = NetworkService.getNetwork(getWorld(), networkId, networkType);
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        if (this.networkId != null){
            nbt.putUUID("network_id", this.networkId);
        }
        if (this.lastPos != null){
            nbt.put("last_pos", NbtUtil.blockPosToNbt(this.lastPos));
        }
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("network_id")){
            this.networkId = tag.getUUID("network_id");
        }
        if (tag.contains("last_pos")){
            this.lastPos = NbtUtil.blockPosFromNbt(tag.getCompound("last_pos"));
        }
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public TickingBlockNetwork getNetwork() {
        return network;
    }

    public Set<UUID> getNeighborNetworkId(){
        Set<UUID> neighborNetworkIds = new HashSet<>();
        BlockState thisBlockState = getWorld().getBlockState(getPos());
        DirectionalNetworkConnect directionalNetworkConnect;
        if (thisBlockState.getBlock() instanceof DirectionalNetworkConnect d){
            directionalNetworkConnect = d;
        } else {
            directionalNetworkConnect = null;
        }
        BlockUtil.AllDirectionOf(getPos(), (neighborPos,face)->{
            if (directionalNetworkConnect != null && !directionalNetworkConnect.canDirectionConnect(thisBlockState, face)){
                return;
            }
            BlockState neighborBlockState = getWorld().getBlockState(neighborPos);
            if (neighborBlockState.getBlock() instanceof DirectionalNetworkConnect neighborDirectionalNetworkConnect && !neighborDirectionalNetworkConnect.canDirectionConnect(neighborBlockState,face.getOpposite())){
                return;
            }

            NetworkBehaviour neighborNetworkBehaviour = NetworkBehaviour.get(getWorld(), neighborPos, NetworkBehaviour.TYPE);
            if (neighborNetworkBehaviour != null && neighborNetworkBehaviour.checkNetworkType(networkType) && !neighborNetworkBehaviour.isPosIdChanging()){
                neighborNetworkIds.add(neighborNetworkBehaviour.getNetworkId());
            }
        });
        return neighborNetworkIds;
    }

    public boolean isInSameNetwork(BlockPos checkPos){
        NetworkBehaviour checkBehaviour = BlockEntityBehaviour.get(getWorld(), checkPos, NetworkBehaviour.TYPE);
        return checkBehaviour != null && checkBehaviour.networkId != null && networkType.equals(checkBehaviour.networkType) && checkBehaviour.networkId.equals(networkId);
    }

    public boolean isPosIdChanging(){
        return lastPos != null && !lastPos.equals(getPos());
    }

    public void setNetworkId(UUID heatNetworkId) {
        this.networkId = heatNetworkId;
        this.network = null;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public boolean checkNetworkType(NetworkService.Services networkType){
        return this.networkType.equals(networkType);
    }
}
