package com.ffsupver.createheat.compat.sable;

import com.ffsupver.createheat.compat.CHModCompat;
import com.ffsupver.createheat.compat.Mods;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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
            Vector3d transPos = subLevel.logicalPose().transformPosition(
                    new Vector3d(worldPos.getX(), worldPos.getY(), worldPos.getZ())
            );
            HashSet<BlockPos> result = new HashSet<>();
            result.add(BlockPos.containing(transPos.x(), transPos.y(), transPos.z()));
            return result;
        }
        return Set.of();
    }

    @Override
    public Set<BlockPos> findHitBlockPos(Level level, BlockPos pos) {
        Set<BlockPos> result = new HashSet<>();
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(pos))) {
            Pose3d pose = subLevel.logicalPose();
            Vector3d transformedPos = pose.transformPositionInverse(new Vector3d(pos.getX(), pos.getY(), pos.getZ()));
            BlockPos transformedBlockPos = BlockPos.containing(transformedPos.x(), transformedPos.y(), transformedPos.z());
            result.add(transformedBlockPos);
        }
        return result;
    }
}
