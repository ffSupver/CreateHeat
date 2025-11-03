package com.ffsupver.createheat.util;

import com.ffsupver.createheat.Config;
import com.simibubi.create.api.boiler.BoilerHeater;

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

    public record HeatData(int heat,int superHeatCount){}
}
