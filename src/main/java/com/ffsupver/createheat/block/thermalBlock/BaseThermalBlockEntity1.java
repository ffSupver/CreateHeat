package com.ffsupver.createheat.block.thermalBlock;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

public class BaseThermalBlockEntity1 extends SmartBlockEntity {
    private UUID heatNetworkId;
    public BaseThermalBlockEntity1(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void setHeatNetworkId(UUID heatNetworkId) {
        this.heatNetworkId = heatNetworkId;
    }

    public UUID getHeatNetworkId() {
        return heatNetworkId;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("network_id")){
            this.heatNetworkId = tag.getUUID("network_id");
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (this.heatNetworkId != null){
            tag.putUUID("network_id", this.heatNetworkId);
        }
    }
}
