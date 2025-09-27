package com.ffsupver.createheat.block.thermalBlock;

import net.minecraft.nbt.CompoundTag;

public class HeatStorage{
    private int capacity;
    private int amount;
    public HeatStorage(int capacity){
        this.capacity = capacity;
        this.amount = 0;
    }

    public int insert(int heat){
        int max = amount + heat;
        if (max > capacity){
            amount = capacity;
            return heat - (max - capacity);
        }else {
            amount = max;
            return heat;
        }
    }

    public int extract(int heat,boolean simulate){
        int min = amount - heat;
        if (min < 0){
            if (!simulate){
                amount = 0;
            }
            return heat + min;
        }else {
            if (!simulate){
                amount = min;
            }
            return heat;
        }
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.amount = Math.min(amount,capacity);
    }

    public int getCapacity() {
        return capacity;
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("capacity",capacity);
        nbt.putInt("amount",amount);
        return nbt;
    }

    public void fromNbt(CompoundTag nbt){
        this.capacity = nbt.getInt("capacity");
        this.amount = nbt.getInt("amount");
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "{"+amount+"/"+capacity+"}";
    }
}
