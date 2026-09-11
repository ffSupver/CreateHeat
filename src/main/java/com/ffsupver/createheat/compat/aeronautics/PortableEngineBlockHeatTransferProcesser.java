package com.ffsupver.createheat.compat.aeronautics;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class PortableEngineBlockHeatTransferProcesser extends HeatTransferProcesser {
    public static final ResourceLocation TYPE = CreateHeat.asResource("portable_engine");
    public PortableEngineBlockHeatTransferProcesser() {
        super(TYPE);
    }
    private int heatAccepted;

    @Override
    public boolean needHeat(Level level, BlockPos pos, @Nullable Direction face, int heat, int tickSkip, int superHeatCount) {
        if ((face == null || Direction.UP.equals(face)) && level.getBlockEntity(pos) instanceof PortableEngineBlockEntity){
            return true;
        }
        return false;
    }

    @Override
    public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide, int tickSkip, int superHeatCount) {
        heatAccepted = heatAccepted + heatProvide;
        if(heatAccepted > 0 && level.getBlockEntity(hTPPos) instanceof PortableEngineBlockEntity portableEngineBlockEntity){
            boolean superHeating = superHeatCount > 0;
            int shouldCostHeat = superHeating ? Config.HEAT_PER_SEETHING_BLAZE.get() : Config.HEAT_PER_FADING_BLAZE.get();
            int burnTime = portableEngineBlockEntity.getCurrentBurnTime();

            // Should add burn time this accept, prepare more heat for first accept
            boolean shouldAddThisAccept = burnTime > 0 && heatAccepted >= shouldCostHeat * 3 || burnTime <= 0 && heatAccepted >= shouldCostHeat * 4;
            if (shouldAddThisAccept){
                int shouldAddTime = heatAccepted / shouldCostHeat;
                heatAccepted = heatAccepted % shouldCostHeat;
                if (portableEngineBlockEntity.isSuperHeated() != superHeating){
                    portableEngineBlockEntity.setSuperHeated(superHeating);
                }
                portableEngineBlockEntity.setCurrentBurnTime(burnTime + shouldAddTime);
                portableEngineBlockEntity.sendData();
            }
        }
    }

    @Override
    public boolean shouldProcessEveryTick() {
        return false;
    }

    @Override
    public Set<Direction> canTransferHeatFrom() {
        return Set.of(Direction.UP);
    }
}
