package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHIcons;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class SmartThermalBlockEntity extends BaseThermalBlockEntity implements IHaveGoggleInformation {
    private ScrollOptionBehaviour<MaxHeatSelections> maxHeatLevelSelections;
    public SmartThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        ThermalBlockEntityBehaviour thermalBlockEntityBehaviour = new ThermalBlockEntityBehaviour(this);
        thermalBlockEntityBehaviour.setCanHeat(tBEB->canHeat(KINDLED));
        thermalBlockEntityBehaviour.setCanSuperHeat(tBEB->canHeat(SEETHING));
        thermalBlockEntityBehaviour.setCanGenerateHeatIgnoreHTP(tBEB->canGenerateHeatIgnoreHTP());
        behaviours.add(thermalBlockEntityBehaviour);
        maxHeatLevelSelections =new ScrollOptionBehaviour<>(
                MaxHeatSelections.class,
                Component.translatable("createheat.smart_thermal_block.selection_mode.max_heat"),
                this, new CenteredSideValueBoxTransform()
        );
        behaviours.add(maxHeatLevelSelections);
    }



    private boolean canHeat(BlazeBurnerBlock.HeatLevel heatLevel){
        BlazeBurnerBlock.HeatLevel maxHeatLevel = this.maxHeatLevelSelections.get().getHeatLevel();
        return heatLevel.ordinal() <= maxHeatLevel.ordinal(); //KINDLED是普通里面最大的
    }

    private boolean canGenerateHeatIgnoreHTP(){
        return this.maxHeatLevelSelections.get().equals(MaxHeatSelections.NONE_HEAT);
    }

    private enum MaxHeatSelections implements INamedIconOptions{
        NONE_HEAT("none", CHIcons.I_HEAT_LEVEL_NONE, NONE),
        KINDLED_HEAT("kindled",CHIcons.I_HEAT_LEVEL_KINDLED, KINDLED),
        SEETHING_HEAT("seething",CHIcons.I_HEAT_LEVEL_SEETHING, SEETHING);
        private final String name;
        private final CHIcons icon;
        private final BlazeBurnerBlock.HeatLevel heatLevel;

        MaxHeatSelections(String name, CHIcons icon, BlazeBurnerBlock.HeatLevel heatLevel) {
            this.name = name;
            this.icon = icon;
            this.heatLevel = heatLevel;
        }


        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return "createheat.smart_thermal_block.selection_mode."+name;
        }

        public BlazeBurnerBlock.HeatLevel getHeatLevel() {
            return heatLevel;
        }
    }
}
