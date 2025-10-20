package com.ffsupver.createheat.registries;

import com.ffsupver.createheat.api.BoilerUpdater;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;

public class CHBoilerUpdaters {
    private static final Set<BoilerUpdater.Tester> TESTERS = new HashSet<>();

    public static void registerBoilerUpdater(BoilerUpdater.Tester tester){
        TESTERS.add(tester);
    }

    public static boolean shouldUpdate(BlockPos belowPos, ServerLevel level){
        final boolean[] result = {false};
        TESTERS.forEach(tester -> result[0] = result[0] || tester.shouldUpdate(belowPos,level));
        return result[0];
    }
}
