package com.ffsupver.createheat.compat.ponder.scenes.aeronautics;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import static com.ffsupver.createheat.compat.ponder.scenes.ThermalBlockScene.setHeatLevel;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING;

public class AeronauticsSimulateScene {
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("aeronautics.use", "Using Thermal Block");

        scene.configureBasePlate(0,0,7);
        BlockPos blazePos = util.grid().at(4,1,3);
        scene.world().modifyBlock(blazePos,setHeatLevel(KINDLED),false);

        scene.world().showSection(util.select().everywhere(), Direction.DOWN);

        scene.idle(10);

        BlockPos enginePos = util.grid().at(1,3,3);
        scene.overlay().showText(20).placeNearTarget()
                .text("Heat Engine")
                .pointAt(enginePos.getCenter())
                .attachKeyFrame();

        scene.idle(40);

        scene.world().modifyBlock(blazePos,setHeatLevel(SEETHING),false);
        scene.world().modifyBlock(util.grid().at(1,2,3),setHeatLevel(SEETHING),false);

        scene.world().modifyBlockEntity(enginePos, PortableEngineBlockEntity.class, be -> {
            be.setSuperHeated(true);
        });
        scene.world().multiplyKineticSpeed(util.select().everywhere(), 2);


        scene.overlay().showText(20).placeNearTarget()
                .text("Super Heat Engine")
                .pointAt(enginePos.getCenter())
                .attachKeyFrame();

        scene.idle(40);

        scene.markAsFinished();
    }
}
