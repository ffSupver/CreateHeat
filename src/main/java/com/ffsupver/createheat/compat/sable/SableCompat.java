package com.ffsupver.createheat.compat.sable;

import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import org.joml.Vector3d;

import java.util.HashSet;
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
            Vector3d transPos = subLevel.logicalPose().transformPosition(
                    new Vector3d(worldPos.getX(), worldPos.getY(), worldPos.getZ())
            );
            Vec3 poj = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atLowerCornerOf(worldPos));
            HashSet<BlockPos> result = new HashSet<>();
            result.add(BlockPos.containing(worldPosition.x(), worldPosition.y(), worldPosition.z()));
            result.add(BlockPos.containing(transPos.x(), transPos.y(), transPos.z()));
            result.add(BlockPos.containing(poj.x(), poj.y(), poj.z()));
            return result;
        }
        return Set.of();
    }


}
