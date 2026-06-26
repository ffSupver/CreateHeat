package com.ffsupver.createheat.block.thermalBlock;

import net.minecraft.nbt.CompoundTag;

public class HeatStorage{
    private int capacity;
    private int amount;
    public HeatStorage(int capacity){
        this.capacity = capacity;
        this.amount = 0;
    }

    /**
     * Insert heat
     * @param heat heat to insert
     * @return heat left after insert
     */
    public int insert(int heat){
        int max = amount + heat;
        if (max > capacity){
            amount = capacity;
            return max - capacity;
        }else {
            amount = max;
            return 0;
        }
    }

    /**
     * Extract heat
     * @param heat heat to extract
     * @param simulate whether to simulate the extraction
     * @return heat left after extraction
     */
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

    public Snapshot snapshot(){
        return new Snapshot(capacity,amount);
    }

    @Override
    public String toString() {
        return "{"+amount+"/"+capacity+"}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        HeatStorage that = (HeatStorage) o;
        return capacity == that.capacity && amount == that.amount;
    }


    /**
     * Snapshot of HeatStorage. Only for display
     */
    public static record Snapshot(int capacity,int amount){
        public CompoundTag toNbt(){
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("capacity",capacity);
            nbt.putInt("amount",amount);
            return nbt;
        }
        public static Snapshot fromNbt(CompoundTag nbt){
            return new Snapshot(nbt.getInt("capacity"),nbt.getInt("amount"));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Snapshot snapshot = (Snapshot) o;
            return amount == snapshot.amount && capacity == snapshot.capacity;
        }
    }
}
