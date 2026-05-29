package com.ffsupver.createheat.util;

import com.ffsupver.createheat.Config;
import com.simibubi.create.api.boiler.BoilerHeater;
import net.minecraft.nbt.CompoundTag;

public final class HeatUtil {
    public static HeatData NO_HEAT_PROVIDE = new HeatData(0,0);
    public static float toBoilerHeat(float heatPerTick){
        float result = heatPerTick / Config.HEAT_PER_FADING_BLAZE.get(); //1代表HeatLevel.FADING给锅炉的热量
        result = result > 0 ? result < 1 ? BoilerHeater.PASSIVE_HEAT : result : result;  //将0-1之间的锅炉热量设置为被动,否则无法被锅炉使用
        return result;
    }

    public static HeatData fromBoilerHeat(float boilerHeat){
        if (boilerHeat == BoilerHeater.NO_HEAT || !Config.ALLOW_PASSIVE_HEAT.get() && boilerHeat == 0) {
           return NO_HEAT_PROVIDE;
        }
        int heatProvide = 1;
        int superHeatCount = 0;
        if (boilerHeat != 0){
            superHeatCount = (int) (boilerHeat / 2);
            int leftHeat = (int) (boilerHeat % 2);
            heatProvide = superHeatCount * Config.HEAT_PER_SEETHING_BLAZE.get() + leftHeat * Config.HEAT_PER_FADING_BLAZE.get();
        }
        return new HeatData(heatProvide,superHeatCount);
    }

    public record HeatData(int heat,int superHeatCount){
        public HeatData merge(HeatData other){
            return new HeatData(this.heat + other.heat,this.superHeatCount + other.superHeatCount);
        }
        public CompoundTag toNbt(){
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("heat",heat);
            nbt.putInt("super_heat_count",superHeatCount);
            return nbt;
        }

        public static HeatData fromNbt(CompoundTag nbt){
            return new HeatData(nbt.getInt("heat"),nbt.getInt("super_heat_count"));
        }

        public HeatData sub(HeatData heatData) {
            return new HeatData(this.heat - heatData.heat,this.superHeatCount - heatData.superHeatCount);
        }
    }

    public record HeatIOData(HeatData in,HeatData out) {
        public CompoundTag toNbt(){
            CompoundTag nbt = new CompoundTag();
            nbt.put("in",in.toNbt());
            nbt.put("out",out.toNbt());
            return nbt;
        }
        public static HeatIOData fromNbt(CompoundTag nbt){
            return new HeatIOData(HeatData.fromNbt(nbt.getCompound("in")),HeatData.fromNbt(nbt.getCompound("out")));
        }

        public int inHeat(){
            return in.heat;
        }
        public int outHeat(){
            return out.heat;
        }
        public int inSuperHeatCount(){
            return in.superHeatCount;
        }
        public int outSuperHeatCount(){
            return out.superHeatCount;
        }
        public int heatGen(){
            return in.heat - out.heat;
        }
        public int superHeatCountGen(){
            return in.superHeatCount - out.superHeatCount;
        }
    }
}
