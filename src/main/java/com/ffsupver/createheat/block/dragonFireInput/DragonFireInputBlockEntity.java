package com.ffsupver.createheat.block.dragonFireInput;

import com.ffsupver.createheat.api.iceAndFire.DragonHeater;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.network.HeatNetwork;
import com.ffsupver.createheat.network.NetworkService;
import com.ffsupver.createheat.util.HeatUtil;
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

import java.util.*;

import static com.ffsupver.createheat.block.dragonFireInput.DragonFireInputBlock.BURNING;
import static com.ffsupver.createheat.util.BlockUtil.isInDistance;

public class DragonFireInputBlockEntity extends SmartBlockEntity {
    public int lastDragonFlameTimer = 0;
    public boolean isHitByFrame;
    private static final int COOL_DOWN = 60;

    private int lastStage;
    private DragonType dragonType;
    private DragonHeater dragonHeater;
    private UUID lastDragonUUID;

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
        }else {
            dragonHeater = null; // remove the old dragon heater
        }

        updateBurning();

        // try gen heat to network
        if(getNetworkBehaviour().getNetwork() instanceof HeatNetwork heatNetwork){
            HeatProvider heatProvider = getHeatProvider();
            HeatUtil.HeatData heatGen = new HeatUtil.HeatData(heatProvider.getHeatPerTick(),heatProvider.getSupperHeatCount());
            heatNetwork.onBlockTick(getBlockPos(),heatGen,HeatUtil.NO_HEAT_PROVIDE,Set.of());
        }
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

    public Set<DragonBaseEntity> getDragonsInRange(BlockPos pos) {
        Set<DragonBaseEntity> dragonsInRange = new HashSet<>();
            BlockPos worldPosition = pos.immutable(); // why?
            AABB searchArea = new AABB((double)worldPosition.getX() - RADIUS, (double)worldPosition.getY() - RADIUS, (double)worldPosition.getZ() - RADIUS, (double)worldPosition.getX() + RADIUS, (double)worldPosition.getY() + RADIUS, (double)worldPosition.getZ() + RADIUS);
            dragonsInRange.addAll(Set.copyOf(this.level.getEntitiesOfClass(DragonBaseEntity.class, searchArea)));
        return dragonsInRange;
    }


    protected void lureDragons(DragonType dragonType) {
        boolean canLungType = canLungDragon(dragonType,this.level);

        boolean dragonSelected = false;

        DragonBaseEntity backUpDragon = null;
        BlockPos finalTargetPos = null;

            BlockPos logicPos = getBlockPos();
            Set<DragonBaseEntity> dragons = getDragonsInRange(logicPos);
            Vec3 targetPosition = new Vec3((float)logicPos.getX() + 0.5F, (float)logicPos.getY() + 0.5F, (float)logicPos.getZ() + 0.5F);
            for (DragonBaseEntity dragon : dragons){
                boolean noTarget = dragon.burningTarget == null;
                boolean isLastOne = dragon.getUUID().equals(this.lastDragonUUID);
                boolean targetThis = !noTarget && dragon.burningTarget.equals(logicPos);

                if (canLungType && assembled() && dragon.dragonType.equals(dragonType) && (dragon.isChained() || dragon.isTame()) && canSeeInput(dragon, targetPosition) && (noTarget || isLastOne)) {
                    if (backUpDragon == null) {
                        backUpDragon = dragon;
                        finalTargetPos = logicPos;
                    }

                    if (isLastOne) {
                        if (!noTarget && !targetThis) {
                            lastDragonUUID = null;
                        } else {
                            backUpDragon = dragon;
                            dragonSelected = true;
                            finalTargetPos = logicPos;
                        }
                    }
                } else if (targetThis) {
                    dragon.burningTarget = null;
                }
            }

        if (backUpDragon != null){
            backUpDragon.burningTarget = finalTargetPos;
            if (!dragonSelected){
                this.lastDragonUUID = backUpDragon.getUUID();
            }
        }
    }

    private boolean canSeeInput(DragonBaseEntity dragon, Vec3 target) {
        if (level == null){
            return false;
        }
        if (target != null) {
            HitResult rayTrace = level.clip(new ClipContext(dragon.getHeadPosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
            return isInDistance(rayTrace.getLocation(),dragon.getHeadPosition(),dragon.level(), 10.0F + dragon.getBbWidth() * 2.0F);
        } else {
            return false;
        }
    }

    // dragon fire breath try hitting on DragonFireInput
    public static void hitDragonFireInput(Level level, DragonBaseEntity dragon, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DragonFireInputBlockEntity dragonFireInputBlockEntity) {
            dragonFireInputBlockEntity.onHitByFrame(dragon.getDragonStage());
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
        if (tag.contains("last_dragon", CompoundTag.TAG_INT_ARRAY)){
            lastDragonUUID = tag.getUUID("last_dragon");
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
        if (lastDragonUUID != null){
            tag.putUUID("last_dragon",lastDragonUUID);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        NetworkBehaviour networkBehaviour = new NetworkBehaviour(this, NetworkService.Services.HEAT);
        behaviours.add(networkBehaviour);
    }

    public HeatProvider getHeatProvider(){
        if (!assembled() || dragonHeater == null || !getBurning()){
            return DragonHeater.NO_HEAT_PROVIDER;
        }
        return dragonHeater.heatProviderByStage().apply(lastStage);
    }

//    @Override
//    public int getHeatPerTick() {
//        return getHeatProvider().getHeatPerTick();
//    }
//
//    @Override
//    public int getSupperHeatCount() {
//        return getHeatProvider().getSupperHeatCount();
//    }

    public NetworkBehaviour getNetworkBehaviour(){
        return getBehaviour(NetworkBehaviour.TYPE);
    }
}
