package com.ffsupver.createheat.compat.ponder.scenes.iceAndFire;

import com.ffsupver.createheat.compat.ponder.scenes.ThermalBlockScene;
import com.iafenvoy.iceandfire.entity.FireDragonEntity;
import com.iafenvoy.iceandfire.particle.DragonFlameParticleType;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING;

public class DragonFireInputScenes {
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("ice_and_fire.use", "Using Dragon Fire Input");

        scene.scaleSceneView(0.5f);

        scene.configureBasePlate(0, 0, 13);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.world().showSection(util.select().fromTo(4,2,8,4,3,8),Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4,3,9,4,3,11),Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3,3,11,1,3,11),Direction.DOWN);

        scene.idle(10);

        BlockPos dragonFireInput = util.grid().at(4,2,8);

        scene.world().createEntity(level -> {
           FireDragonEntity fireDragon = new FireDragonEntity(IafEntities.FIRE_DRAGON.get(), level);
           fireDragon.setAgeInDays(50);
           BlockPos blockPos = util.grid().at(7,1,3);
           fireDragon.setPos(blockPos.getCenter());
           fireDragon.setInSittingPose(true);

           fireDragon.burningTarget = dragonFireInput;
           return fireDragon;
        });

        scene.overlay().showText(20)
                .text("Dragon Fire Input can turn dragon fire into heat")
                .pointAt(dragonFireInput.getCenter());

        scene.idle(30);

        scene.world().showSection(util.select().position(4,1,8),Direction.UP);
        scene.overlay().showText(20)
                .text("Place any Dragon Forge Bricks next to Dragon Fire Input to select dragon type you want")
                .pointAt(dragonFireInput.below().getCenter());

        scene.world().modifyEntities(FireDragonEntity.class,fireDragonEntity -> {
            fireDragonEntity.setInSittingPose(false);
            fireDragonEntity.getLookControl().setLookAt(dragonFireInput.getCenter());
            fireDragonEntity.updateBurnTarget();
        });


        int totalTickLeft = 100;
        dragonBrunTicks(scene,30,dragonFireInput,0F,30F / totalTickLeft);

        scene.world().showSection(util.select().fromTo(3,4,11,1,6,11),Direction.DOWN);

        for (int i = 0;i < 3;i++){
            int x = 1 + i;
            BlockPos basinPos = util.grid().at(x,4,11);

            scene.world().createItemEntity(util.vector().centerOf(basinPos.above(1)), util.vector().of(0, 0, 0), new ItemStack(Items.COPPER_INGOT));
            scene.world().createItemEntity(util.vector().centerOf(basinPos.above(1)), util.vector().of(0, 0, 0), new ItemStack(AllItems.ZINC_INGOT.asItem()));

            scene.world().modifyBlock(util.grid().at(x,3,11), ThermalBlockScene.setHeatLevel(SEETHING),false);
            scene.world().setKineticSpeed(util.select().position(x,6,11),(-(x % 2) * 2 + 1) * 64f);
            scene.world().modifyBlockEntity(util.grid().at(x,6,11), MechanicalMixerBlockEntity.class,MechanicalMixerBlockEntity::startProcessingBasin);
        }

        scene.overlay().showText(20)
                .text("Dragon Fire Input will provide heat")
                .pointAt(util.grid().at(1,6,11).getCenter());

        dragonBrunTicks(scene,70,dragonFireInput,30F / totalTickLeft,100F / totalTickLeft);

        scene.markAsFinished();
    }

    private static void dragonBrunTicks(CreateSceneBuilder scene,int ticks,BlockPos burnPos,float progressStart,float progressEnd){
        float totalBurnProgress = 40F * (progressEnd - progressStart);
        float baseBurnProgress = 40F * progressStart;
        for (int tick = 0;tick < ticks;tick++){
            float progress = baseBurnProgress + totalBurnProgress / ticks * tick;
            scene.world().modifyEntities(FireDragonEntity.class,fireDragon -> addFire(fireDragon,burnPos,progress));
            scene.idle(1);
        }
    }

    private static void addFire(FireDragonEntity fireDragon, BlockPos burnPos,float burnProgress){
        float burnX = burnPos.getX() + 0.5F;
        float burnY = burnPos.getY() + 0.5F;
        float burnZ = burnPos.getZ() + 0.5F;
        Vec3 headPos = fireDragon.getHeadPosition();
        double d2 = burnX - headPos.x;
        double d3 = burnY - headPos.y;
        double d4 = burnZ - headPos.z;
        double distance = Math.max((double)2.5F * Math.sqrt(fireDragon.distanceToSqr(burnX, burnY, burnZ)), (double)0.0F);
        double conqueredDistance = (double)burnProgress / (double)40.0F * distance;
        int increment = (int)Math.ceil(conqueredDistance / (double)100.0F);
//        int particleCount = fireDragon.getDragonStage() <= 3 ? 6 : 3;
        for(int i = 0; (double)i < conqueredDistance; i += increment) {
            double progressX = d2 * (double) ((float) i / (float) distance);
            double progressY = d3 * (double) ((float) i / (float) distance);
            double progressZ = d4 * (double) ((float) i / (float) distance);
            Vec3 velocity = new Vec3(progressX, progressY, progressZ);
            fireDragon.level().addParticle(new DragonFlameParticleType(fireDragon.getAgeScale()), headPos.x, headPos.y, headPos.z, velocity.x, velocity.y, velocity.z);
        }
//        fireDragon.level().addParticle(new DragonFlameParticleType(fireDragon.getAgeScale()), headPos.x, headPos.y, headPos.z, 0,0,0);

    }
}
