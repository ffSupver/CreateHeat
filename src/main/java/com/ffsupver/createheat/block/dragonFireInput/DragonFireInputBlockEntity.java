package com.ffsupver.createheat.block.dragonFireInput;

import com.ffsupver.createheat.api.iceAndFire.DragonHeater;
import com.ffsupver.createheat.block.HeatProvider;
import com.iafenvoy.iceandfire.data.DragonType;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafRegistries;
import com.iafenvoy.iceandfire.util.DragonTypeProvider;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlock.BURNING;

public class DragonFireInputBlockEntity extends SmartBlockEntity implements HeatProvider {
    public int lastDragonFlameTimer = 0;
    public boolean isHitByFrame;
    private static final int COOL_DOWN = 60;

    private int lastStage;
    private DragonType dragonType;
    private DragonHeater dragonHeater;

    private static final float RADIUS = 25.0F;
    public DragonFireInputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    @Override
    public void tick() {
        super.tick();
        assert this.level != null;


        checkAssembled();
        lureDragons(dragonType);
        if (!assembled()){
            lastStage = 0;
            lastDragonFlameTimer = 0;
            if (getBurning()){
                setBurning(false);
            }
            return;
        }





        RegistryAccess registryAccess = level.registryAccess();
        Optional<Holder.Reference<DragonHeater>> dragonHeaterReferenceOp = DragonHeater.getFromDragonType(registryAccess,dragonType);
        if (dragonHeaterReferenceOp.isPresent()) {
            dragonHeater = dragonHeaterReferenceOp.get().value();

            if (lastDragonFlameTimer > 0) {
                --lastDragonFlameTimer;
            } else if (lastDragonFlameTimer == 0) {
                if (isHitByFrame) {
                    lastDragonFlameTimer = COOL_DOWN;
                    isHitByFrame = false;
                } else {
                    if (lastStage != 0) {
                        lastStage = 0;
                    }
                }
            }
        }

        updateBurning();
    }

    private void checkAssembled(){
        DragonType dragonTypeToCheck = null;
        for (Direction facing : Direction.values()){
            BlockPos pos = this.getBlockPos().relative(facing);

            assert this.level != null;

            BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof DragonTypeProvider provider) {
                dragonTypeToCheck = provider.getDragonType();
                break;
            }
        }
        dragonType = dragonTypeToCheck;
    }

    private boolean assembled(){
        return dragonType != null;
    }

    private boolean canLungDragon(DragonType dragonType, Level level){
       return DragonHeater.getFromDragonType(level.registryAccess(),dragonType).isPresent();
    }

    public void onHitByFrame(int stage){
        if (assembled()){
            setLastStage(stage);
            isHitByFrame = true;
            lastDragonFlameTimer = Math.min(lastDragonFlameTimer + COOL_DOWN, COOL_DOWN * 5);
            notifyUpdate();
        }
    }

    private void setLastStage(int stage){
        int tmpLastStage = lastStage;
        lastStage = stage;
        if (tmpLastStage != lastStage){
            setBurning(!getBurning());
        }
    }

    private void setBurning(boolean burning){
        getLevel().setBlock(getBlockPos(),getBlockState().setValue(BURNING,burning),3);
        notifyUpdate();
    }

    private boolean getBurning(){
        return getBlockState().getValue(BURNING);
    }

    private void updateBurning(){
        boolean newBurning = lastStage != 0;
        if (getBurning() != newBurning){
            setBurning(newBurning);
        }
    }



    protected void lureDragons(DragonType dragonType) {
        boolean canLungType = canLungDragon(dragonType,this.level);

        Vec3 targetPosition = new Vec3((float)this.getBlockPos().getX() + 0.5F, (float)this.getBlockPos().getY() + 0.5F, (float)this.getBlockPos().getZ() + 0.5F);
        AABB searchArea = new AABB((double)this.worldPosition.getX() - RADIUS, (double)this.worldPosition.getY() - RADIUS, (double)this.worldPosition.getZ() - RADIUS, (double)this.worldPosition.getX() + RADIUS, (double)this.worldPosition.getY() + RADIUS, (double)this.worldPosition.getZ() + RADIUS);
        boolean dragonSelected = false;


        for(DragonBaseEntity dragon : this.level.getEntitiesOfClass(DragonBaseEntity.class, searchArea)) {
            if (canLungType && assembled() && !dragonSelected && dragon.dragonType.equals(dragonType) && canSeeInput(dragon,targetPosition)){
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

    @Override
    public void destroy() {
        lureDragons(null); //清空附近的龙的目标
        super.destroy();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        lastDragonFlameTimer = tag.getInt("frame_timer");
        isHitByFrame = tag.getBoolean("hit_by_frame");
        lastStage = tag.getInt("stage");
        if (tag.contains("dragon_type", CompoundTag.TAG_STRING)){
            dragonType = IafRegistries.DRAGON_TYPE.get(ResourceLocation.parse(tag.getString("dragon_type")));
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("frame_timer",lastDragonFlameTimer);
        tag.putBoolean("hit_by_frame",isHitByFrame);
        tag.putInt("stage",lastStage);
        if (dragonType != null){
            tag.putString("dragon_type", IafRegistries.DRAGON_TYPE.getKey(dragonType).toString());
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    private HeatProvider getHeatProvider(){
        if (!assembled() || dragonHeater == null){
            return DragonHeater.NO_HEAT_PROVIDER;
        }
        return dragonHeater.heatProviderByStage().apply(lastStage);
    }

    @Override
    public int getHeatPerTick() {
        return getHeatProvider().getHeatPerTick();
    }

    @Override
    public int getSupperHeatCount() {
        return getHeatProvider().getSupperHeatCount();
    }
}
