package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.network.HeatNetwork;
import com.ffsupver.createheat.network.HeatService;
import com.ffsupver.createheat.registries.CHHeatProviders;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.HeatUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import joptsimple.internal.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.ffsupver.createheat.api.BoilerUpdater.getBoilerControllerBE;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class BaseThermalBlockBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<BaseThermalBlockBehaviour> TYPE = new BehaviourType<>();

    private UUID heatNetworkId;
    private HeatNetwork heatNetwork;
    private final BaseThermalBlockEntity1 thermalBlockEntity;
    private int litUpCooldown;

    // api
    private Predicate<BaseThermalBlockBehaviour> canSuperHeat;
    private Predicate<BaseThermalBlockBehaviour> canHeat;
    private Predicate<BaseThermalBlockBehaviour> canGenerateHeatIgnoreHTP;
    private Predicate<BaseThermalBlockBehaviour> shouldHeatUp;
    private Consumer<HeatLevel> onSetHeatLevel;

    // display
    private HeatStorage.Snapshot displayHeatStorage;
    private HeatUtil.HeatIOData displayHeatRemain;


    public BaseThermalBlockBehaviour(BaseThermalBlockEntity1 be) {
        super(be);
        thermalBlockEntity = be;
    }

    @Override
    public void tick() {
        super.tick();
        // try to create network or find neighbor network
        if (heatNetworkId == null){
            if (!thermalBlockEntity.getNeighborNetworkId().isEmpty()){
                this.heatNetworkId = HeatService.addBlockToNetwork(getPos(),getWorld(),thermalBlockEntity.getNeighborNetworkId());
            }
        }

        // try to get network
        if (heatNetwork == null) {
            heatNetwork = HeatService.getNetwork(getWorld(), heatNetworkId);
        }

        // tick with network
        if (heatNetwork != null){
            HeatUtil.HeatData networkRemainHeat = heatNetwork.getHeatDataLastTickRemain();

            // find transfer processor
            Set<HeatTransferProcesser> neighborTransferProcesser = new HashSet<>();
            BlockUtil.AllDirectionOf(getPos(),(checkPos,face)->{
                if (heatNetwork.getTransferProcesser(checkPos) == null){
                    Optional<HeatTransferProcesser> transferProcesserOp = CHHeatTransferProcessers.findProcesser(getWorld(),checkPos,face,networkRemainHeat.heat(),1,networkRemainHeat.superHeatCount());
                    transferProcesserOp.ifPresent(
                            heatTransferProcesser -> {
                                heatNetwork.addTransferProcesser(checkPos, heatTransferProcesser);
                                neighborTransferProcesser.add(heatNetwork.getTransferProcesser(checkPos));
                            }
                    );
                }else {
                    neighborTransferProcesser.add(heatNetwork.getTransferProcesser(checkPos));
                }
            });

            if (litUpCooldown > 0){
                litUpCooldown--;
            }

            HeatUtil.HeatData heatGenData = genHeat();
            checkHeatLevel(networkRemainHeat,neighborTransferProcesser);
            HeatUtil.HeatData heatCostData = getCurrentHeatCostData();

            heatNetwork.onBlockTick(getPos(),heatGenData,heatCostData);

            // send display data
            if (!getWorld().isClientSide()){
                HeatStorage.Snapshot newDisplayHeatStorage = heatNetwork.getDisplayHeatStorage();
                HeatUtil.HeatIOData newDisplayHeatRemain = heatNetwork.getDisplayHeatData();
//                System.out.println("new:"+newDisplayHeatStorage+":"+newDisplayHeatRemain+"   old:"+displayHeatStorage+":"+displayHeatRemain+" pos:"+getPos());
                boolean displayDataChanged = !newDisplayHeatRemain.equals(displayHeatRemain) || !newDisplayHeatStorage.equals(displayHeatStorage);
                displayHeatRemain = newDisplayHeatRemain;
                displayHeatStorage = newDisplayHeatStorage;
                if (displayDataChanged){
//                    System.out.println("displayDataChanged");
                    thermalBlockEntity.sendData();
                }
            }
        }
    }

    /** calculate heat cost by BlockState
     */
    public HeatUtil.HeatData getCurrentHeatCostData() {
       return new HeatUtil.HeatData(getHeatPerTick(getHeatLevel()),getHeatLevel().isAtLeast(SEETHING)? 1:0);
    }

    private HeatUtil.HeatData genHeat(){
        BlockPos belowPos = getPos().below();
        boolean avoidHTP = !onCanGenerateHeatIgnoreHTPTest() && getHeatTransferProcesserByOther(belowPos).isPresent();

        if (isInSameNetwork(belowPos) || avoidHTP){ //防止加热自己或者被处理的热源
            return HeatUtil.NO_HEAT_PROVIDE;
        }
        Optional<HeatProvider> heatProviderOp = CHHeatProviders.findHeatProvider(getWorld(),belowPos,getWorld().getBlockState(belowPos));
        if (heatProviderOp.isPresent()) {
            HeatProvider provider = heatProviderOp.get();
            return new HeatUtil.HeatData(provider.getHeatPerTick(), provider.getSupperHeatCount());
        }else {
            return HeatUtil.fromBoilerHeat(BoilerHeater.findHeat(getWorld(), getPos().below(), getWorld().getBlockState(getPos().below())));
        }
    }

    private void checkHeatLevel(HeatUtil.HeatData networkRemainHeat,Set<HeatTransferProcesser> neighborTransferProcesser){
        HeatLevel heatLevel = getHeatLevel();
        boolean haeEnoughHeat = getHeatPerTick(heatLevel) <= networkRemainHeat.heat() && (!heatLevel.isAtLeast(SEETHING) || networkRemainHeat.superHeatCount() >= 1);
        boolean hasHTPNeighbor = !neighborTransferProcesser.isEmpty();
        HeatLevel newHeatLevel = heatLevel;
        if (haeEnoughHeat) {
            if (needToHeat(hasHTPNeighbor)) {
                boolean canSuperHeat = onCanSuperHeatTest() && networkRemainHeat.heat() >= getHeatPerTick(SEETHING) && networkRemainHeat.superHeatCount() >= 1;

                if (canSuperHeat && needToHeatUp(SEETHING,hasHTPNeighbor)){
                    newHeatLevel = SEETHING;
                }else if (
                        networkRemainHeat.heat() >= getHeatPerTick(KINDLED) && // has enough heat for KINDLED
                                (needToHeatUp(KINDLED,hasHTPNeighbor) || heatLevel.isAtLeast(SEETHING) && !canSuperHeat) // NONE->KINDLED or SEETHING->KINDLED
                ){
                    newHeatLevel = KINDLED;
                }
            }else{
                newHeatLevel = NONE;
            }
        }else {
            if (heatLevel.isAtLeast(SEETHING) && networkRemainHeat.heat() >= getHeatPerTick(KINDLED)){
                newHeatLevel = KINDLED;
            }else {
                newHeatLevel = NONE;
            }
        }

        if (!heatLevel.equals(newHeatLevel)){
            setBlockHeat(newHeatLevel);
        }
    }

    /**
     * if this ThermalBlock need a higher HeatLevel.
     * e.g BlockState is KINDLED it returns true if input heatLevel is SEETHING, and it returns false if input heatLevel is KINDLED or lower.
     * @param heatLevel the highest HeatLevel this ThermalBlock can reach
     */
    private boolean needToHeatUp(HeatLevel heatLevel,boolean hasHTPNeighbor){
        return needToHeat(hasHTPNeighbor) && (getHeatLevel().equals(NONE) ||
                heatLevel.equals(SEETHING) && !getHeatLevel().equals(SEETHING));
    }

    /**
     * if this ThermalBlock need to turn into heating state
     */
    private boolean needToHeat(boolean hasHTPNeighbor){
        if (!onCanHeatTest()){
            return false;
        }

        return needToHeatAbove() || hasHTPNeighbor || onShouldHeatUp();
    }

    /**
     * if block above need to be heat
     * @return true if block above need to be heat
     */
    public boolean needToHeatAbove(){
        boolean needToHeatAbove = getWorld().getBlockState(getPos().above()).is(CHTags.BlockTag.SHOULD_HEAT);
        boolean needToHeatBoiler = getBoilerControllerBE(getWorld().getBlockEntity(getPos().above())).isPresent();
        return needToHeatAbove || needToHeatBoiler;
    }

    public void setBlockHeat(HeatLevel heatLevel){
        boolean canLitUp = litUpCooldown <= 0;
        boolean setSuccess = false;
        if (heatLevel.isAtLeast(KINDLED)){
            if (canLitUp){
                getWorld().setBlock(getPos(), getBlockState().setValue(HEAT_LEVEL, heatLevel), 3);
                litUpCooldown = 5;
                setSuccess = true;
            }
        }else {
            getWorld().setBlock(getPos(), getBlockState().setValue(HEAT_LEVEL, heatLevel), 3);
            setSuccess = true;
        }
        System.out.println("setBlockHeat:"+heatLevel+" pos:"+getPos()+" success:"+setSuccess);
        if (setSuccess && onSetHeatLevel != null){
            onSetHeatLevel.accept(heatLevel);
        }
        notifyUpdate();
    }

    private void notifyUpdate(){
        thermalBlockEntity.notifyUpdate();
    }

    /**
     * get heat level of this block
     * @return heat level of this block
     */
    public HeatLevel getHeatLevel(){
        return getBlockState().getValue(HEAT_LEVEL);
    }

    private boolean isInSameNetwork(BlockPos checkPos){
        BaseThermalBlockBehaviour checkBehaviour = BlockEntityBehaviour.get(getWorld(), checkPos, BaseThermalBlockBehaviour.TYPE);
        return checkBehaviour != null && checkBehaviour.getHeatNetworkId() == heatNetworkId;
    }

    public Optional<HeatTransferProcesser> getHeatTransferProcesserByOther(BlockPos belowPos) {
        if (heatNetwork != null){
            return Optional.ofNullable(heatNetwork.getTransferProcesser(belowPos));
        }
        return Optional.empty();
    }

    private boolean onShouldHeatUp(){
        return onTest(shouldHeatUp,this,false);
    }

    private boolean onCanGenerateHeatIgnoreHTPTest() {
        return onTest(canGenerateHeatIgnoreHTP,this,false);
    }
    private boolean onCanHeatTest() {
        return onTest(canHeat,this);
    }
    private boolean onCanSuperHeatTest() {
        return onTest(canSuperHeat,this);
    }

    public void setShouldHeatUp(Predicate<BaseThermalBlockBehaviour> shouldHeatUp) {
        this.shouldHeatUp = shouldHeatUp;
    }

    public void setCanGenerateHeatIgnoreHTP(Predicate<BaseThermalBlockBehaviour> canGenerateHeatIgnoreHTP) {
        this.canGenerateHeatIgnoreHTP = canGenerateHeatIgnoreHTP;
    }

    public void setCanHeat(Predicate<BaseThermalBlockBehaviour> canHeat) {
        this.canHeat = canHeat;
    }

    public void setCanSuperHeat(Predicate<BaseThermalBlockBehaviour> canSuperHeat) {
        this.canSuperHeat = canSuperHeat;
    }

    public void setOnSetHeatLevel(Consumer<HeatLevel> onSetHeatLevel) {
        this.onSetHeatLevel = onSetHeatLevel;
    }

    public BlockState getBlockState(){
        return getWorld().getBlockState(getPos());
    }


    public boolean isBurning() {
       return getHeatLevel().isAtLeast(KINDLED);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("network_id")){
            this.heatNetworkId = tag.getUUID("network_id");
        }

        if (tag.contains("display_heat_remain")){
            this.displayHeatRemain = HeatUtil.HeatIOData.fromNbt(tag.getCompound("display_heat_remain"));
        }
        if (tag.contains("display_heat_storage", Tag.TAG_COMPOUND)){
            displayHeatStorage = HeatStorage.Snapshot.fromNbt(tag.getCompound("display_heat_storage"));
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        if (this.heatNetworkId != null){
            nbt.putUUID("network_id", this.heatNetworkId);
        }


        if (displayHeatRemain != null){
            nbt.put("display_heat_remain", displayHeatRemain.toNbt());
        }
        if (displayHeatStorage != null){
            nbt.put("display_heat_storage", displayHeatStorage.toNbt());
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public void setHeatNetworkId(UUID heatNetworkId) {
        this.heatNetworkId = heatNetworkId;
        this.heatNetwork = null;
    }

    public UUID getHeatNetworkId() {
        return heatNetworkId;
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (displayHeatStorage != null) {
            tooltip.add(Component.literal(
                    Strings.repeat(' ', 4)
            ).append(
                    Component.translatable(
                            "createheat.gui.goggles.heat_amount",
                            displayHeatStorage.amount(),
                            displayHeatStorage.capacity()
                    )
            ));
            tooltip.add(Component.literal(
                    Strings.repeat(' ', 4)
            ).append(
                    Component.translatable(
                            "createheat.gui.goggles.heat_remain",
                            displayHeatRemain.inHeat()+"-"+displayHeatRemain.outHeat()+"="+displayHeatRemain.heatGen()+
                                    " / "+displayHeatRemain.inSuperHeatCount()+"-"+displayHeatRemain.outSuperHeatCount()+"="+displayHeatRemain.superHeatCountGen()
                    )
            ));
            return true;
        }

        return heatNetworkId != null;
    }

    /**
     * test a predicate
     * @param test the predicate
     * @param behaviour the behaviour
     * @param defaultResult the default result
     * @return the result of the predicate
     */
    private static boolean onTest(Predicate<BaseThermalBlockBehaviour> test, BaseThermalBlockBehaviour behaviour, boolean defaultResult){
        if (test == null){
            return defaultResult;
        }else {
            return test.test(behaviour);
        }
    }
    /**
     * test a predicate, default true
     * @param test the predicate
     * @param behaviour the behaviour
     * @return the result of the predicate
     */
    private static boolean onTest(Predicate<BaseThermalBlockBehaviour> test,BaseThermalBlockBehaviour behaviour){
        return onTest(test,behaviour,true);
    }

    public static int getHeatPerTick(HeatLevel heatLevel){
        return  switch (heatLevel){
            case NONE -> 0;
            case SMOULDERING -> 1;
            case FADING, KINDLED -> Config.HEAT_PER_FADING_BLAZE.get();
            case SEETHING -> Config.HEAT_PER_SEETHING_BLAZE.get();
        };
    }
}
