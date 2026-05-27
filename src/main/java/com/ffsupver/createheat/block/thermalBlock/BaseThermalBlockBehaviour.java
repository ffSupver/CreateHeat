package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.network.HeatNetwork;
import com.ffsupver.createheat.network.HeatService;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class BaseThermalBlockBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<BaseThermalBlockBehaviour> TYPE = new BehaviourType<>();

    private UUID heatNetworkId;
    private HeatNetwork heatNetwork;
    private BaseThermalBlockEntity1 thermalBlockEntity;

    public BaseThermalBlockBehaviour(BaseThermalBlockEntity1 be) {
        super(be);
        thermalBlockEntity = be;
    }

    @Override
    public void tick() {
        super.tick();
        if (heatNetworkId == null){
            if (thermalBlockEntity.getNeighborNetworkId() != null){
                this.heatNetworkId = HeatService.addBlockToNetwork(getPos(),getWorld(),thermalBlockEntity.getNeighborNetworkId());
            }
        }

        if (heatNetwork == null) {
            heatNetwork = HeatService.getNetwork(getWorld(), heatNetworkId);
        }

        if (heatNetwork != null){
            heatNetwork.onBlockTick(getPos());
        }
    }


    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("network_id")){
            this.heatNetworkId = tag.getUUID("network_id");
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        if (this.heatNetworkId != null){
            nbt.putUUID("network_id", this.heatNetworkId);
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public void setHeatNetworkId(UUID heatNetworkId) {
        this.heatNetworkId = heatNetworkId;
        this.heatNetwork = null;
    }

    public UUID getHeatNetworkId() {
        return heatNetworkId;
    }
}
