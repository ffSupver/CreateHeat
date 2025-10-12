package com.ffsupver.createheat.util;

import com.ffsupver.createheat.Config;
import com.simibubi.create.api.boiler.BoilerHeater;

public final class HeatUtil {
    public static float toBoilerHeat(float heatPerTick){
        float result = heatPerTick / Config.HEAT_PER_FADING_BLAZE.get(); //1代表HeatLevel.FADING给锅炉的热量
        result = result > 0 ? result < 1 ? BoilerHeater.PASSIVE_HEAT : result : result;  //将0-1之间的锅炉热量设置为被动,否则无法被锅炉使用
        return result;
    }
}
