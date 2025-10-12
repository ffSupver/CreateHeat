package com.ffsupver.createheat.block.dragonFireInput;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.block.HeatProvider;
import com.iafenvoy.iceandfire.data.DragonType;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import com.iafenvoy.iceandfire.util.DragonTypeProvider;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlock.BURNING;

public class DragonFireInputBlockEntity extends SmartBlockEntity implements HeatProvider {
    public int lastDragonFlameTimer = 0;
    public boolean isHitByFrame;
    private static final int COOL_DOWN = 60;

    private int lastStage;

    private static final float RADIUS = 25.0F;
    public DragonFireInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Override
    public void tick() {
        super.tick();
        lureDragons();

        if (lastDragonFlameTimer > 0) {
            --lastDragonFlameTimer;
        }else if (lastDragonFlameTimer == 0){
            if (isHitByFrame){
                lastDragonFlameTimer = COOL_DOWN;
                isHitByFrame = false;
            }else {
                if (lastStage != 0) {
                    lastStage = 0;
                }
            }
            updateBurning();
        }
    }

    public void onHitByFrame(int stage){
        setLastStage(stage);
        isHitByFrame = true;
        lastDragonFlameTimer = Math.min(lastDragonFlameTimer + COOL_DOWN,COOL_DOWN * 5);

    }

    private void setLastStage(int stage){
        int tmpLastStage = lastStage;
        lastStage = stage;
        if (tmpLastStage != lastStage){
            setBurning(!getBlockState().getValue(BURNING));
        }
    }

    private void setBurning(boolean burning){
        getLevel().setBlock(getBlockPos(),getBlockState().setValue(BURNING,burning),3);
        notifyUpdate();
    }

    private void updateBurning(){
        setBurning(lastStage != 0);
    }



    protected void lureDragons() {
        Vec3 targetPosition = new Vec3((float)this.getBlockPos().getX() + 0.5F, (float)this.getBlockPos().getY() + 0.5F, (float)this.getBlockPos().getZ() + 0.5F);
        AABB searchArea = new AABB((double)this.worldPosition.getX() - RADIUS, (double)this.worldPosition.getY() - RADIUS, (double)this.worldPosition.getZ() - RADIUS, (double)this.worldPosition.getX() + RADIUS, (double)this.worldPosition.getY() + RADIUS, (double)this.worldPosition.getZ() + RADIUS);
        boolean dragonSelected = false;

        assert this.level != null;
        for(DragonBaseEntity dragon : this.level.getEntitiesOfClass(DragonBaseEntity.class, searchArea)) {
            if (!dragonSelected && dragon.dragonType.equals(this.getDragonType()) && canSeeInput(dragon,targetPosition)){
                if (dragon.burningTarget == null){
                    dragon.burningTarget = this.worldPosition;
                    dragonSelected = true;
                }
            }else if (dragon.burningTarget != null && dragon.burningTarget.equals(this.worldPosition)){
                dragon.burningTarget = null;
            }
        }
    }

    private boolean canSeeInput(DragonBaseEntity dragon, Vec3 target) {
        if (target != null) {
            assert this.level != null;

            HitResult rayTrace = this.level.clip(new ClipContext(dragon.getHeadPosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
            double distance = dragon.getHeadPosition().distanceTo(rayTrace.getLocation());
            return distance < (double)(10.0F + dragon.getBbWidth() * 2.0F);
        } else {
            return false;
        }
    }

    private DragonType getDragonType(){
        Block block = this.getBlockState().getBlock();
        DragonType type;
        if (block instanceof DragonTypeProvider provider){
            type = provider.getDragonType();
        }else {
            type = IafDragonTypes.FIRE;
        }
        return type;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        lastDragonFlameTimer = tag.getInt("frame_timer");
        isHitByFrame = tag.getBoolean("hit_by_frame");
        lastStage = tag.getInt("stage");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("frame_timer",lastDragonFlameTimer);
        tag.putBoolean("hit_by_frame",isHitByFrame);
        tag.putInt("stage",lastStage);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }


    @Override
    public int getHeatPerTick() {
        return getSupperHeatCount() * Config.HEAT_PER_SEETHING_BLAZE.get();
    }

    @Override
    public int getSupperHeatCount() {
        return lastStage * 2;
    }
}
