package com.ffsupver.createheat.block.tightCompressStone;

import com.ffsupver.createheat.block.thermalBlock.HeatStorage;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import static com.ffsupver.createheat.block.tightCompressStone.TightCompressStone1.HEAT;
import static com.ffsupver.createheat.block.tightCompressStone.TightCompressStone1.Heat.*;
import static com.ffsupver.createheat.network.HeatNetwork.MAX_HEAT;

public class HeatStorageBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<HeatStorageBehaviour> TYPE = new BehaviourType<>();

    private UUID networkId;
    private SuperHeatStorage superHeatStorage = new SuperHeatStorage(MAX_HEAT.get()*200);


    public HeatStorageBehaviour(SmartBlockEntity be) {
        super(be);
    }

    @Override
    public void tick() {
        super.tick();

        TightCompressStone1.Heat newHeat = getHeat();
        if (this.superHeatStorage.superAmount > 0){
            newHeat = SUPER_HEAT;
        }else if (this.superHeatStorage.getAmount() > 0){
            newHeat = REGULAR_HEAT;
        }else {
            newHeat = NONE;
        }
        if (newHeat != getHeat()){
            setHeat(newHeat);
        }
    }

    public TightCompressStone1.Heat getHeat(){
        return getBlockState().getValue(HEAT);
    }

    public void setHeat(TightCompressStone1.Heat heat){
        getWorld().setBlock(getPos(),getBlockState().setValue(HEAT, heat),3);
    }

    public BlockState getBlockState() {
        return getWorld().getBlockState(getPos());
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        networkId = nbt.getUUID("NetworkId");
        superHeatStorage.fromNbt(nbt.getCompound("super_storage"));
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putUUID("NetworkId", networkId);
        nbt.put("super_storage",superHeatStorage.toNbt());
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public static class SuperHeatStorage extends HeatStorage{
        private int superCapacity;
        private int superAmount;

        public SuperHeatStorage(int capacity) {
            super(capacity);
            superCapacity = capacity;
        }

        @Override
        public CompoundTag toNbt() {
            CompoundTag nbt = super.toNbt();
            nbt.putInt("super_capacity",superCapacity);
            nbt.putInt("super_amount",superAmount);
            return nbt;
        }

        @Override
        public void fromNbt(CompoundTag nbt) {
            super.fromNbt(nbt);
            superCapacity = nbt.getInt("super_capacity");
            superAmount = nbt.getInt("super_amount");
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && superCapacity == ((SuperHeatStorage) o).superCapacity && superAmount == ((SuperHeatStorage) o).superAmount;
        }

        @Override
        public String toString() {
            return super.toString()+"--super{"+superAmount+"/"+superCapacity+"}";
        }
    }
}
