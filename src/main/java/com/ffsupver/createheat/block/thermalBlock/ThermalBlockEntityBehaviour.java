package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.api.CustomHeater;
import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.block.tightCompressStone.TightCompressStoneEntity;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import joptsimple.internal.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.ffsupver.createheat.util.BlockUtil.AllDirectionOf;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class ThermalBlockEntityBehaviour extends BlockEntityBehaviour {

    public static final BehaviourType<ThermalBlockEntityBehaviour> TYPE = new BehaviourType<>();
    private int heat;
    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private int cooldown;
    public static final int MAX_COOLDOWN = 10;

    private final HeatStorage heatStorage = new HeatStorage(MAX_HEAT.get());
    private final ArrayList<BlockPos> stoneHeatStorages = new ArrayList<>();

    private final Map<BlockPos, HeatTransferProcesser> transferProcesserMap = new HashMap<>();

    private final HeatStorage displayHeatStorage = new HeatStorage(0);

    private final ConnectableBlockEntity<?> connectableBlockEntity;
    private final Predicate<ThermalBlockEntityBehaviour> canSuperHeat;
    private final Predicate<ThermalBlockEntityBehaviour> canHeat;

    public ThermalBlockEntityBehaviour(ConnectableBlockEntity<?> be,Predicate<ThermalBlockEntityBehaviour> canHeat,Predicate<ThermalBlockEntityBehaviour> canSuperHeat) {
        super(be);
        cooldown = 0;
        connectableBlockEntity = be;
        this.canHeat = canHeat;
        this.canSuperHeat = canSuperHeat;
    }

    public ThermalBlockEntityBehaviour(ConnectableBlockEntity<?> be){
        this(be,null,null);
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (isController()) {
            tag.putInt("heat",heat);
            tag.putInt("cooldown",cooldown);

            tag.put("heat_storage",heatStorage.toNbt());

            tag.put("shs", NbtUtil.writeBlockPosToNbtList(stoneHeatStorages));


            tag.put("transfer_processers",NbtUtil.writeMapToNbtList(
                    transferProcesserMap,
                    NbtUtil::blockPosToNbt,
                    CHHeatTransferProcessers::toNbt
            ));

            if (clientPacket){
                tag.put("display_heat",getAllHeatForDisplay().toNbt());
            }
        }
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        //getLevel return null
        if (isController()) {
            heat = tag.getInt("heat");
            cooldown = tag.getInt("cooldown");

            heatStorage.fromNbt(tag.getCompound("heat_storage"));

            stoneHeatStorages.clear();
            stoneHeatStorages.addAll(NbtUtil.readBlockPosFromNbtList(tag.getList("shs", Tag.TAG_COMPOUND)));

            transferProcesserMap.clear();
            transferProcesserMap.putAll(NbtUtil.readMapFromNbtList(
                    tag.getList("transfer_processers", Tag.TAG_COMPOUND),
                    NbtUtil::blockPosFromNbt,
                    CHHeatTransferProcessers::fromNbt
            ));

            if (clientPacket) {
                displayHeatStorage.fromNbt(tag.getCompound("display_heat"));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!isController()){
            return;
        }

        if (cooldown < 0){
            tickEveryConnectedBlock();
            cooldown = MAX_COOLDOWN;
        }else {
            cooldown--;
        }

        //处理需要每tick的传热处理器
        transferProcesserMap.forEach((hTPPos,hTP)->{
            if (hTP.shouldProcessEveryTick()){
                int heatProvider = calculateHeatProvide(hTPPos,this,1);
                hTP.acceptHeat(getLevel(),hTPPos,heatProvider,1);
            }
        });

    }


    public void tickEveryConnectedBlock(){
        if (getLevel().isClientSide()){
            return;
        }

        int tickSkip = MAX_COOLDOWN;

        Set<ThermalBlockEntityBehaviour> connectedBlockList = getConnectedBlocks().stream().map(
                pos -> BlockEntityBehaviour.get(getLevel().getBlockEntity(pos),TYPE)
        ).filter(Objects::nonNull).collect(Collectors.toSet());


        int lastHeat = heat;
        heat = 0;

        int lastHeatStored = heatStorage.getAmount();

        int superHeatCount = 0;


        //移除断开连接的或者不是控制器的储热器 检查每个储热器的方块状态
        boolean changeSHS = false;
        Iterator<BlockPos> sHSIterator = stoneHeatStorages.iterator();
        while (sHSIterator.hasNext()){
            BlockPos sHSController = sHSIterator.next();
            BlockEntity tightCSE = getLevel().getBlockEntity(sHSController);
            if (!(tightCSE instanceof TightCompressStoneEntity) || tightCSE instanceof TightCompressStoneEntity sHS && (!sHS.isController() || !sHS.isConnect(getConnectedBlocks()))) {
                sHSIterator.remove();
                changeSHS = true;

            }
        }


        //移除不合条件的传热处理器
        List<BlockPos> heatTransferProcesserToRemove = transferProcesserMap.entrySet().stream()
                .filter(e->!e.getValue().needHeat(getLevel(),e.getKey(),null) || !BlockUtil.isConnect(getConnectedBlocks(),Set.of(e.getKey())))
                .map(Map.Entry::getKey).toList();
        heatTransferProcesserToRemove.forEach(blockPos -> {
            transferProcesserMap.get(blockPos).onControllerRemove();
            transferProcesserMap.remove(blockPos);
        });

        //====加热部分====


        //计算加热数
        for (ThermalBlockEntityBehaviour thermalBlockEntity : connectedBlockList){
            if (thermalBlockEntity.getControllerEntity() != null) {
                HeatData heatBelow = thermalBlockEntity.genHeat();
                heat += heatBelow.heat * tickSkip;
                superHeatCount += heatBelow.superHeatCount;


                // 寻找新储热器,传热处理器
                AllDirectionOf(thermalBlockEntity.getBlockPos(), (checkPos, f) -> {
                    if (transferProcesserMap.containsKey(checkPos)){
                        return;  //避免重复搜索
                    }

                    Optional<HeatTransferProcesser> transferProcesserOp = CHHeatTransferProcessers.findProcesser(getLevel(), checkPos, f);
                    transferProcesserOp.ifPresent(hTP -> transferProcesserMap.putIfAbsent(checkPos, hTP));

                    if (getLevel().getBlockEntity(checkPos) instanceof TightCompressStoneEntity sHS) {
                        BlockPos sHSPosToAdd = sHS.getControllerPos();
                        if (!stoneHeatStorages.contains(sHSPosToAdd)) {
                            stoneHeatStorages.add(sHSPosToAdd);
                        }
                    }
                });
            }
        }

        boolean usingHeater = heat > 0;
        int superHeatCountFromSHS = releaseHeat();
        int superHeatCountFromHeater = superHeatCount;
        superHeatCount += superHeatCountFromSHS;



        //====耗热部分====
        for (ThermalBlockEntityBehaviour thermalBlockEntity : connectedBlockList){
            if (thermalBlockEntity.getControllerEntity() != null) {
                CostHeatResult costHeatResult = thermalBlockEntity.costHeat(heat, tickSkip, superHeatCount);
                heat = costHeatResult.heat;
                superHeatCount = costHeatResult.superHeatCount;
            }
        }

        //处理传热处理器
        transferProcesserMap.forEach((hTPPos, hTP) -> {
            if (!hTP.shouldProcessEveryTick()){
                int heatProvide = calculateHeatProvide(hTPPos, this, tickSkip);
                hTP.acceptHeat(getLevel(), hTPPos, heatProvide,tickSkip);
            }
        });


        storageHeat(superHeatCountFromHeater,superHeatCount,usingHeater); //检测SHS是否有方块变化
        if (changeSHS || heat != lastHeat || lastHeatStored != heatStorage.getAmount()){
            sendData();
        }
    }


    private HeatData genHeat() {
        BlockPos belowPos = getBlockPos().below();
        if (getControllerEntity().getConnectedBlocks().contains(belowPos)){ //防止加热自己
            return new HeatData(0,0);
        }
        Optional<Holder.Reference<CustomHeater>> customHeatOp = CustomHeater.getFromBlockState(getLevel().registryAccess(), getLevel().getBlockState(belowPos));
        if (getLevel().getBlockEntity(belowPos) instanceof HeatProvider provider){
            return new HeatData(provider.getHeatPerTick(),provider.getSupperHeatCount());
        }else if (customHeatOp.isPresent()){
            CustomHeater customHeater = customHeatOp.get().value();
            return new HeatData(customHeater.heatPerTick(),customHeater.superHeatCount());
        }else {
            BlazeBurnerBlock.HeatLevel heatLevelB = getHeatLevel(belowPos);
            int boilHeat = (int) BoilerHeater.findHeat(getLevel(), getBlockPos().below(), getLevel().getBlockState(getBlockPos().below())) + 1;

            int result = Math.max(getHeatPerTick(heatLevelB), boilHeat);
            if (!Config.ALLOW_PASSIVE_HEAT.get()) {
                result = result == 1 ? 0 : result;
            }
            return new HeatData(result, result >= 3 ? 1 : 0);
        }
    }

    private CostHeatResult costHeat(int heat, int tickSkip, int superHeat){
        if (!needToHeat()){
            setBlockHeat(NONE);
            return new CostHeatResult(heat,superHeat);
        }

        boolean canSuperHeat = onCanSuperHeaTest() && (Config.ALLOW_GENERATE_SUPER_HEAT.get() || superHeat > 0);

        if (canSuperHeat && heat >= calculateHeatCost(tickSkip, SEETHING)) {
            if (needToHeatUp(SEETHING)) {
                setBlockHeat(SEETHING);
            }
        }else if (heat >= calculateHeatCost(tickSkip,KINDLED)){
            if (needToHeatUp(KINDLED) || getHeatLevel().equals(SEETHING)) {
                setBlockHeat(KINDLED);
            }
        }else{
            setBlockHeat(NONE);
        }

        if (!Config.ALLOW_SUPER_HEAT_REPRODUCE.get() && getHeatLevel().equals(SEETHING)){
            superHeat--;
        }

        return new CostHeatResult(heat - calculateHeatCost(tickSkip,getHeatLevel()),superHeat);
    }

    //存储多余的热
    private void storageHeat(int superHeatCount,int superCountLeft,boolean usingHeater){
        if (heat > 0){
            int left = heat;
            for (BlockPos stoneHeatStoragePos : stoneHeatStorages){
                if (getLevel().getBlockEntity(stoneHeatStoragePos) instanceof TightCompressStoneEntity stoneHeatStorage) {
                    if (left > 0) {
                        int inserted = stoneHeatStorage.insert(left);
                        left -= inserted;
                    }

                    //记录超级加热数
                    if (!Config.ALLOW_GENERATE_SUPER_HEAT.get()) {
                        superHeatCount = stoneHeatStorage.addSuperHeatCount(superCountLeft,superHeatCount,usingHeater,getControllerPos());
                    }
                }
            }

            if (left > 0){
                heatStorage.insert(left);
            }
        }
    }

    //释放存储的热
    private int releaseHeat(){
        int superHeatCount = 0;
        if (heat <= 0){
            for (BlockPos stoneHeatStoragePos : stoneHeatStorages){
                if (getLevel().getBlockEntity(stoneHeatStoragePos) instanceof TightCompressStoneEntity stoneHeatStorage) {
                    int need = heatStorage.getCapacity() - heatStorage.getAmount();
                    if (need > 0) {
                        int toExtract = stoneHeatStorage.extract(need, true);
                        toExtract = heatStorage.insert(toExtract);
                        stoneHeatStorage.extract(toExtract, false);
                    }

                    //释放超级加热
                    if (!Config.ALLOW_GENERATE_SUPER_HEAT.get()) {
                        superHeatCount += stoneHeatStorage.releaseSuperHeatCount(getControllerPos());
                    }
                }
            }


            heat += heatStorage.extract(heatStorage.getAmount(),false);
        }
        return superHeatCount;
    }

    private boolean needToHeatUp(BlazeBurnerBlock.HeatLevel heatLevel){
        return needToHeat() && (getHeatLevel().equals(NONE) ||
                heatLevel.equals(SEETHING) && !getHeatLevel().equals(SEETHING));
    }

    private boolean needToHeat(){
        if (!onCanHeatTest()){
            return false;
        }

        AtomicBoolean findProcesser = new AtomicBoolean(false);
        List<BlockPos> transferProcesserKeys = List.copyOf(getControllerEntity().transferProcesserMap.keySet());
        AllDirectionOf(getBlockPos(),blockPos->{
            if (!findProcesser.get()){
                findProcesser.set(transferProcesserKeys.contains(blockPos));
            }
        });
        return needToHeatAbove() || findProcesser.get();
    }

    private boolean onCanHeatTest(){
       return onTest(canHeat,this);
    }

    private boolean onCanSuperHeaTest(){
        return onTest(canSuperHeat,this);
    }

    public Optional<HeatTransferProcesser> getHeatTransferProcesserByOther(BlockPos pos){
        return Optional.ofNullable(getControllerEntity().transferProcesserMap.get(pos));
    }

    public boolean needToHeatAbove(){
        boolean needToHeatAbove = getLevel().getBlockState(getBlockPos().above()).is(CHTags.BlockTag.SHOULD_HEAT);
        boolean needToHeatBoiler = getLevel().getBlockEntity(getBlockPos().above()) instanceof FluidTankBlockEntity fluidTankBlockEntity &&
                fluidTankBlockEntity.getControllerBE().boiler.attachedEngines > 0;
        return needToHeatAbove || needToHeatBoiler;
    }

    public BlazeBurnerBlock.HeatLevel getHeatLevel(BlockPos pos){
        BlockState blockState = getLevel().getBlockState(pos);
        return blockState.hasProperty(HEAT_LEVEL) ? blockState.getValue(HEAT_LEVEL) : NONE;
    }
    public BlazeBurnerBlock.HeatLevel getHeatLevel(){
        return getBlockState().getValue(HEAT_LEVEL);
    }

    public void setBlockHeat(BlazeBurnerBlock.HeatLevel heatLevel){
        getLevel().setBlock(getBlockPos(),getBlockState().setValue(HEAT_LEVEL, heatLevel), 3);
        notifyUpdate();
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalBlockEntityBehaviour controllerEntity = getControllerEntity();
        if (controllerEntity != null) {
            HeatStorage heatStorageForDisplay = controllerEntity.displayHeatStorage;
            if (heatStorageForDisplay != null) {
                tooltip.add(Component.literal(
                        Strings.repeat(' ', 4)
                ).append(
                        Component.translatable(
                                "createheat.gui.goggles.heat_amount",
                                heatStorageForDisplay.getAmount(),
                                heatStorageForDisplay.getCapacity()
                        )
                ));
                tooltip.add(Component.literal(
                        Strings.repeat(' ', 4)
                ).append(
                        Component.translatable(
                                "createheat.gui.goggles.heat_remain",
                                getControllerEntity().heat / MAX_COOLDOWN
                        )
                ));
                return true;
            }
        }
        return false;
    }

    protected void mergeController(BlockPos oldControllerPos, ThermalBlockEntityBehaviour oldControllerEntity, BlockPos newControllerPos, ThermalBlockEntityBehaviour newControllerEntity) {
        oldControllerEntity.switchStoneHeatStorageToNew(newControllerPos,newControllerEntity,true);
        oldControllerEntity.transferProcesserMap.forEach((p,hTP)->hTP.onControllerRemove());
    }

    protected void switchToNewControllerWhenDestroy(BlockPos newPos, ThermalBlockEntityBehaviour newControllerEntity) {
        switchStoneHeatStorageToNew(newPos, newControllerEntity, false);
    }

    @Override
    public void destroy() {
        transferProcesserMap.forEach((b,h)->h.onControllerRemove());
        super.destroy();
    }

    private void switchStoneHeatStorageToNew(BlockPos newControllerPos, ThermalBlockEntityBehaviour newControllerEntity, boolean merge){
        Iterator<BlockPos> shsPosIt = stoneHeatStorages.iterator();
        while (shsPosIt.hasNext()){
            BlockPos shsPos =shsPosIt.next();
            if (getLevel().getBlockEntity(shsPos) instanceof TightCompressStoneEntity sHSEntity){
                if (merge){
                    sHSEntity.switchTControllerTo(getControllerPos(), newControllerPos);
                    shsPosIt.remove();
                }else{
                    if ((!sHSEntity.isConnect(getConnectedBlocks()) || isController()) && sHSEntity.isConnect(newControllerEntity.getConnectedBlocks())) {
                        sHSEntity.switchTControllerTo(getControllerPos(), newControllerPos);
                        shsPosIt.remove();
                    }
                }
            }
        }
    }

    public void calculateHeatStorage(){
        heatStorage.setCapacity(getConnectedBlocks().size() * MAX_HEAT.get());
    }

    public HeatStorage getAllHeatForDisplay(){
        HeatStorage result = new HeatStorage(0);
        for (BlockPos sHSPos : stoneHeatStorages){
            if (getLevel().getBlockEntity(sHSPos) instanceof TightCompressStoneEntity sHS) {
                HeatStorage hs = sHS.getStoneHeatStorage();
                result.setCapacity(result.getCapacity() + hs.getCapacity());
                result.insert(hs.getAmount());
            }
        }
        result.setCapacity(result.getCapacity() + heatStorage.getCapacity());
        result.insert(heatStorage.getAmount());
        return result;
    }

    public HeatStorage getHeatStorage() {
        if (isController()){
            return heatStorage;
        }else {
            return getControllerEntity().getHeatStorage();
        }
    }

    private static int calculateHeatProvide(BlockPos cPos,ThermalBlockEntityBehaviour controller,int tickSkip){
        AtomicInteger heatAccept = new AtomicInteger();
        AllDirectionOf(cPos,pos -> {
            if (controller.getConnectedBlocks().contains(pos)){
                BlazeBurnerBlock.HeatLevel heatLevel = controller.getHeatLevel(pos);
                heatAccept.addAndGet(calculateHeatCost(tickSkip, heatLevel));
            }
        });
        return heatAccept.get();
    }


    public int getHeat() {
        return heat;
    }

    public boolean isConnectTo(StoneHeatStorage shs){
        return shs.isConnect(getControllerEntity().getConnectedBlocks());
    }


    public int getBlockSize(){
        return getConnectedBlocks().size();
    }


    private boolean isController(){
        return connectableBlockEntity.isController();
    }

    private BlockPos getControllerPos(){
        return connectableBlockEntity.getControllerPos();
    }

    private ThermalBlockEntityBehaviour getControllerEntity(){
        return BlockEntityBehaviour.get(connectableBlockEntity.getControllerEntity(),TYPE);
    }

    private Set<BlockPos> getConnectedBlocks(){
        return connectableBlockEntity.getConnectedBlocks();
    }

    private Level getLevel(){
        return connectableBlockEntity.getLevel();
    }

    private BlockPos getBlockPos(){
        return connectableBlockEntity.getBlockPos();
    }

    private BlockState getBlockState(){
        return connectableBlockEntity.getBlockState();
    }

    public void sendData(){
        connectableBlockEntity.sendData();
    }

    private void notifyUpdate(){
        connectableBlockEntity.notifyUpdate();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public static int calculateHeatCost(int tickSkip, BlazeBurnerBlock.HeatLevel heatLevel){
        return tickSkip * getHeatPerTick(heatLevel);
    }

    public static int getHeatPerTick(BlazeBurnerBlock.HeatLevel heatLevel){
        return  switch (heatLevel){
            case NONE -> 0;
            case SMOULDERING -> 1;
            case FADING, KINDLED -> Config.HEAT_PER_FADING_BLAZE.get();
            case SEETHING -> Config.HEAT_PER_SEETHING_BLAZE.get();
        };
    }

    public static ThermalBlockEntityBehaviour getFromCBE(ConnectableBlockEntity<?> be){
        return be.getBehaviour(TYPE);
    }

    public static boolean canConnect(ConnectableBlockEntity<?> toCheck) {
        return getFromCBE(toCheck) != null;
    }

    private static boolean onTest(Predicate<ThermalBlockEntityBehaviour> test,ThermalBlockEntityBehaviour behaviour){
        if (test == null){
            return true;
        }else {
            return test.test(behaviour);
        }
    }


    private record CostHeatResult(int heat, int superHeatCount){}


    private record HeatData(int heat,int superHeatCount){}
}
