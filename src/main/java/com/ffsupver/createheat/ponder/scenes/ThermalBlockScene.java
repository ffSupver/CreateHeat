package com.ffsupver.createheat.ponder.scenes;

import com.ffsupver.createheat.block.thermalBlock.ThermalBlock;
import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.List;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class ThermalBlockScene {
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("thermal_block.use", "Using Thermal Block");

        scene.configureBasePlate(0, 0, 7);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().showSection(util.select().fromTo(0,1,0,6,4,6),Direction.DOWN);
        BlockPos blazePos = util.grid().at(4,1,2);
        scene.world().modifyBlock(blazePos, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED),false);


        scene.idle(10);

        scene.overlay().showText(20).placeNearTarget()
                        .text("Heat goes through Thermal Block")
                .pointAt(util.grid().at(4,3,2).getBottomCenter());

        List<Vec3i> pathList = List.of(
                new Vec3i(4,2,2),
                new Vec3i(4,2,3),
                new Vec3i(4,2,4),
                new Vec3i(4,3,4),
                new Vec3i(3,3,4),
                new Vec3i(2,3,4),
                new Vec3i(2,4,4),
                new Vec3i(2,4,3),
                new Vec3i(2,4,2)
        );

        for (Vec3i pathV : pathList){
            Selection p = util.select().position(pathV.getX(),pathV.getY(),pathV.getZ());
            scene.overlay().showOutline(PonderPalette.GREEN,p,p,6);
            scene.idle(6);
        }

        scene.idle(10);

        BlockPos basinPos = util.grid().at(2,5,2);
        scene.world().modifyBlock(basinPos.below(), b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED),false);
        scene.world().showSection(util.select().position(2,5,2),Direction.DOWN);

        scene.scaleSceneView(0.8f);

        scene.overlay().showText(15).placeNearTarget()
                .text("Thermal Block provide heat for machines above")
                .pointAt(basinPos.getBottomCenter());

        scene.idle(5);

        scene.world().createItemEntity(util.vector().centerOf(basinPos.above(1)), util.vector().of(0, 0, 0), new ItemStack(Items.COPPER_INGOT));
        scene.idle(5);
        scene.world().createItemEntity(util.vector().centerOf(basinPos.above(1)), util.vector().of(0, 0, 0), new ItemStack(AllItems.ZINC_INGOT.asItem()));

        scene.idle(5);

        scene.world().showSection(util.select().layer(7),Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(2,7,2),64f);

        scene.idle(5);

        scene.world().modifyBlockEntity(util.grid().at(2,7,2), MechanicalMixerBlockEntity.class, MechanicalMixerBlockEntity::startProcessingBasin);

        scene.idle(80);

        scene.world().createItemOnBeltLike(util.grid().at(1,4,2),Direction.DOWN,new ItemStack(AllItems.BRASS_INGOT.asItem()));

        scene.idle(10);

        scene.addKeyframe();

        scene.world().modifyBlock(blazePos, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING),false);
        scene.world().modifyBlock(basinPos.below(), b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING),false);

        scene.overlay().showText(15).placeNearTarget()
                .text("Super heat")
                .pointAt(basinPos.getBottomCenter());

        scene.idle(20);

        scene.markAsFinished();
    }

    public static void storage(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("thermal_block.storage", "Storge Heat Using Thermal Block");
        scene.configureBasePlate(0,0,7);
        scene.world().showSection(util.select().everywhere(),Direction.UP);

        BlockPos blazePos1 = util.grid().at(4,1,1);
        BlockPos blazePos2 = util.grid().at(4,1,2);
        BlockPos thermalBlockPos = util.grid().at(4,2,2);
        scene.world().modifyBlock(blazePos1, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING),false);
        scene.world().modifyBlock(blazePos2, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING),false);
        scene.world().modifyBlock(thermalBlockPos, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED),false);

        scene.idle(10);

        scene.overlay().showText(15).placeNearTarget()
                .text("Some passive heat sources can generate regular heat source")
                .attachKeyFrame()
                .pointAt(blazePos2.getBottomCenter());

        scene.idle(20);

        scene.world().modifyBlock(blazePos1, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED),false);
        scene.world().modifyBlock(blazePos2, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED),false);
        scene.world().modifyBlock(thermalBlockPos, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING),false);

        scene.overlay().showText(15).placeNearTarget()
                .text("Some regular heat sources can generate super heat source")
                .attachKeyFrame()
                .pointAt(blazePos2.getBottomCenter());

        scene.idle(20);

        scene.world().modifyBlock(blazePos2, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING),false);

        scene.overlay().showText(15).placeNearTarget()
                .text("When the provided heat exceed the consumed heat")
                .pointAt(blazePos2.getBottomCenter());

        scene.idle(30);

        BlockPos lavaPos = util.grid().at(2,1,4);
        for (int i = 0;i < 3;i++){
            scene.world().setBlock(lavaPos.above(i), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 0), false);
            scene.idle(3);
        }

        scene.overlay().showText(15).placeNearTarget()
                .text("The excess heat will be stored")
                .pointAt(lavaPos.getBottomCenter());

        scene.idle(30);

        scene.world().hideSection(util.select().position(blazePos1),Direction.DOWN);
        scene.world().hideSection(util.select().position(blazePos2),Direction.DOWN);

        scene.overlay().showText(15).placeNearTarget()
                .text("When no heat provided")
                .pointAt(lavaPos.getBottomCenter());

        scene.idle(30);

        scene.world().modifyBlock(thermalBlockPos, b->b.setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE),false);
        for (int i = 0;i < 3;i++) {
            scene.world().setBlock(lavaPos.above(i), CHBlocks.TIGHT_COMPRESSED_STONE.getDefaultState(), false);
            scene.idle(3);
        }
        scene.overlay().showText(15).placeNearTarget()
                .text("The stored heat will be released")
                .pointAt(lavaPos.getBottomCenter());

        scene.idle(30);

        scene.markAsFinished();
    }
}
