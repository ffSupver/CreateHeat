package com.ffsupver.createheat.compat.anvilCraft;

import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.api.anvilCraft.HeatableBlockHeatTransferProcesserData;
import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.ffsupver.createheat.util.BlockUtil;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterInfo;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class HeatableBlockTransferProcesser extends HeatTransferProcesser {
    public static final ResourceLocation TYPE = CreateHeat.asResource("anvil_craft_heatable_block_regular");

    private BlockPos heatPos;
    private ResourceLocation id;
    private int count;
    private HeaterInfo<HeatableBlockTransferProcesser> info = null;
    protected HeatableBlockTransferProcesser() {
        super(TYPE);
    }

    @Override
    public boolean needHeat(Level level, BlockPos pos, @Nullable Direction face) {
        Optional<Holder.Reference<HeatableBlockHeatTransferProcesserData>> dataOptional = HeatableBlockHeatTransferProcesserData.getFromBlockState(level.getBlockState(pos),level.registryAccess());
        if(dataOptional.isPresent()){
            ResourceLocation lastId = id;
            heatPos = pos;
            HeatableBlockHeatTransferProcesserData data = dataOptional.get().value();
            id = dataOptional.get().getKey().location();
            if (lastId != null && !lastId.equals(id)){ //当检测条件不符合时切换
                HeaterManager.removeProducer(heatPos,level,info);
                return false;
            }
            if (info == null){
                info = createInfo(data.heatTierLine());
            }
            return true;
        }
        return false;
    }

    @Override
    public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide, int tickSkip) {
        count = heatProvide / tickSkip;
        HeaterManager.addProducer(heatPos,level,info);
    }

    public Set<BlockPos> getHeatPosSet() {
        return Set.of(heatPos);
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean shouldProcessEveryTick() {
        return false;
    }

    public static Optional<HeatableBlockTransferProcesser> getByLevelPos(Level level, BlockPos pos){
        AtomicReference<Optional<HeatableBlockTransferProcesser>> result = new AtomicReference<>(Optional.empty());
        BlockUtil.AllDirectionOf(pos,cPos->{
            if (level.getBlockEntity(cPos) instanceof ConnectableBlockEntity<?> cbe){
                ThermalBlockEntityBehaviour tBEB = ThermalBlockEntityBehaviour.getFromCBE(cbe);
                if (tBEB != null){
                    Optional<HeatTransferProcesser> hTPOp = tBEB.getHeatTransferProcesserByOther(pos);
                    if (hTPOp.isPresent() && hTPOp.get() instanceof HeatableBlockTransferProcesser hBTP ){
                        result.set(Optional.of(hBTP));
                    }
                }
            }
        },p->result.get().isPresent());
        return result.get();
    }

    public static HeaterInfo<HeatableBlockTransferProcesser> createInfo(HeatTierLine heatTierLine){
        return             new HeaterInfo<>(
                HeatableBlockTransferProcesser::getByLevelPos,
                HeatableBlockTransferProcesser::getHeatPosSet,
                heatTierLine,
                HeatableBlockTransferProcesser::getCount
        );
    }
}
