package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.network.HeatNetwork;
import com.ffsupver.createheat.network.HeatService;
import com.ffsupver.createheat.registries.CHHeatProviders;
import com.ffsupver.createheat.util.HeatUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import joptsimple.internal.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BaseThermalBlockBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<BaseThermalBlockBehaviour> TYPE = new BehaviourType<>();

    private UUID heatNetworkId;
    private HeatNetwork heatNetwork;
    private final BaseThermalBlockEntity1 thermalBlockEntity;

    // display
    private HeatStorage.Snapshot displayHeatStorage;
    private HeatUtil.HeatData displayHeatRemain;

    public BaseThermalBlockBehaviour(BaseThermalBlockEntity1 be) {
        super(be);
        thermalBlockEntity = be;
    }

    @Override
    public void tick() {
        super.tick();
        if (heatNetworkId == null){
            if (!thermalBlockEntity.getNeighborNetworkId().isEmpty()){
                this.heatNetworkId = HeatService.addBlockToNetwork(getPos(),getWorld(),thermalBlockEntity.getNeighborNetworkId());
            }
        }

        if (heatNetwork == null) {
            heatNetwork = HeatService.getNetwork(getWorld(), heatNetworkId);
        }

        if (heatNetwork != null){
            HeatUtil.HeatData heatData = genHeat();

            heatNetwork.onBlockTick(getPos(),heatData);

            if (!getWorld().isClientSide()){
                HeatStorage.Snapshot newDisplayHeatStorage = heatNetwork.getDisplayHeatStorage();
                HeatUtil.HeatData newDisplayHeatRemain = heatNetwork.getDisplayHeatData();
//                System.out.println("new:"+newDisplayHeatStorage+":"+newDisplayHeatRemain+"   old:"+displayHeatStorage+":"+displayHeatRemain+" pos:"+getPos());
                boolean displayDataChanged = !newDisplayHeatRemain.equals(displayHeatRemain) || !newDisplayHeatStorage.equals(displayHeatStorage);
                displayHeatRemain = newDisplayHeatRemain;
                displayHeatStorage = newDisplayHeatStorage;
                if (displayDataChanged){
//                    System.out.println("displayDataChanged");
                    thermalBlockEntity.sendData();
                }
            }
        }
    }

    private HeatUtil.HeatData genHeat(){
        BlockPos belowPos = getPos().below();
        boolean avoidHTP = !onCanGenerateHeatIgnoreHTPTest() && getHeatTransferProcesserByOther(belowPos).isPresent();
        BaseThermalBlockBehaviour belowBehaviour = BlockEntityBehaviour.get(getWorld(), belowPos, BaseThermalBlockBehaviour.TYPE);
        if (belowBehaviour != null && belowBehaviour.getHeatNetworkId() == heatNetworkId || avoidHTP){ //防止加热自己或者被处理的热源
            return HeatUtil.NO_HEAT_PROVIDE;
        }
        Optional<HeatProvider> heatProviderOp = CHHeatProviders.findHeatProvider(getWorld(),belowPos,getWorld().getBlockState(belowPos));
        if (heatProviderOp.isPresent()) {
            HeatProvider provider = heatProviderOp.get();
            return new HeatUtil.HeatData(provider.getHeatPerTick(), provider.getSupperHeatCount());
        }else {
            return HeatUtil.fromBoilerHeat(BoilerHeater.findHeat(getWorld(), getPos().below(), getWorld().getBlockState(getPos().below())));
        }
    }

    private Optional<Object> getHeatTransferProcesserByOther(BlockPos belowPos) {
        return Optional.empty();
    }

    private boolean onCanGenerateHeatIgnoreHTPTest() {
        return true;
    }


    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("network_id")){
            this.heatNetworkId = tag.getUUID("network_id");
        }

        if (tag.contains("display_heat_remain")){
            this.displayHeatRemain = HeatUtil.HeatData.fromNbt(tag.getCompound("display_heat_remain"));
        }
        if (tag.contains("display_heat_storage", Tag.TAG_COMPOUND)){
            displayHeatStorage = HeatStorage.Snapshot.fromNbt(tag.getCompound("display_heat_storage"));
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        if (this.heatNetworkId != null){
            nbt.putUUID("network_id", this.heatNetworkId);
        }


        if (displayHeatRemain != null){
            nbt.put("display_heat_remain", displayHeatRemain.toNbt());
        }
        if (displayHeatStorage != null){
            nbt.put("display_heat_storage", displayHeatStorage.toNbt());
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

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (displayHeatStorage != null) {
            tooltip.add(Component.literal(
                    Strings.repeat(' ', 4)
            ).append(
                    Component.translatable(
                            "createheat.gui.goggles.heat_amount",
                            displayHeatStorage.amount(),
                            displayHeatStorage.capacity()
                    )
            ));
            tooltip.add(Component.literal(
                    Strings.repeat(' ', 4)
            ).append(
                    Component.translatable(
                            "createheat.gui.goggles.heat_remain",
                            displayHeatRemain.heat()+" / "+displayHeatRemain.superHeatCount()
                    )
            ));
            return true;
        }

        return heatNetworkId != null;
    }
}
