package com.ffsupver.createheat.compat.sable;

import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import org.joml.Vector3d;

import java.util.Set;

public class SableCompat implements CHModCompat {
    @Override
    public String getModId() {
        return Mods.ModIds.SABLE.ModId;
    }

    @Override
    public void init(IEventBus eventBus) {

    }

    @Override
    public Set<BlockPos> getGlobalBlockPos(Level level, BlockPos worldPos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, worldPos);
        if (subLevel != null) {
            Vector3d localPos = subLevel.logicalPose().transformPositionInverse(
                    new Vector3d(worldPos.getX(), worldPos.getY(), worldPos.getZ())
            );
            Vector3d worldPosition = subLevel.logicalPose().transformPosition(localPos);
            return Set.of(BlockPos.containing(worldPosition.x(), worldPosition.y(), worldPosition.z()));
        }
        return Set.of();
    }


}
