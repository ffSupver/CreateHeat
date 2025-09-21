package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.item.ThermalTool;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public class CHItems {
    private static final CreateRegistrate REGISTRATE = CreateHeat.registrate();

    static {
        REGISTRATE.setCreativeTab(CHCreativeTab.MAIN_TAB);
    }
    public static final ItemEntry<ThermalTool> THERMAL_TOOL = REGISTRATE
        .item("thermal_tool",ThermalTool::new)
        .properties(p->p.food(new FoodProperties(2,2f,true,10f, Optional.of(Items.DIAMOND.getDefaultInstance()),
                List.of(new FoodProperties.PossibleEffect(()-> new MobEffectInstance(MobEffects.LEVITATION,20),0.8f))
        )).stacksTo(1))
        .register();

public static void register() {
}
}
