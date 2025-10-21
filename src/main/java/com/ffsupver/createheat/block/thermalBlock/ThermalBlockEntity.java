package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.CreateHeat;
import com.ffsupver.createheat.api.CustomHeater;
import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.HeatProvider;
import com.ffsupver.createheat.block.HeatTransferProcesser;
import com.ffsupver.createheat.block.tightCompressStone.TightCompressStoneEntity;
import com.ffsupver.createheat.recipe.HeatRecipe;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.registries.CHHeatTransferProcessers;
import com.ffsupver.createheat.registries.CHRecipes;
import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import joptsimple.internal.Strings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.ffsupver.createheat.util.BlockUtil.AllDirectionOf;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class ThermalBlockEntity extends ConnectableBlockEntity<ThermalBlockEntity> implements IHaveGoggleInformation {

    private int heat;
    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private int cooldown;
    public static final int MAX_COOLDOWN = 10;

    private final HeatStorage heatStorage = new HeatStorage(MAX_HEAT.get());
    private final ArrayList<BlockPos> stoneHeatStorages = new ArrayList<>();

    private final Map<BlockPos,HeatTransferProcesser> transferProcesserMap = new HashMap<>();

    private final HeatStorage displayHeatStorage = new HeatStorage(0);

    public ThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        cooldown = 0;
    }

    @Override
    public boolean canConnect(ConnectableBlockEntity toCheck) {
        return toCheck instanceof ThermalBlockEntity;
    }

    @Override
    protected ThermalBlockEntity castToSubclass() {
        return this;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (isController()) {
            tag.putInt("heat",heat);
            tag.putInt("cooldown",cooldown);

            tag.put("heat_storage",heatStorage.toNbt());

            tag.put("shs",NbtUtil.writeBlockPosToNbtList(stoneHeatStorages));


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
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        //getLevel return null
        super.read(tag, registries, clientPacket);
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

        Set<ThermalBlockEntity> connectedBlockList = connectedBlocks.stream().map(
                pos -> getLevel().getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity ? thermalBlockEntity : null
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
            if (!(tightCSE instanceof TightCompressStoneEntity) || tightCSE instanceof TightCompressStoneEntity sHS && (!sHS.isController() || !sHS.isConnect(connectedBlocks))) {
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
        for (ThermalBlockEntity thermalBlockEntity : connectedBlockList){
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
        for (ThermalBlockEntity thermalBlockEntity : connectedBlockList){
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
                hTP.acceptHeat(level, hTPPos, heatProvide,tickSkip);
            }
        });


       storageHeat(superHeatCountFromHeater,superHeatCount,usingHeater); //检测SHS是否有方块变化
        if (changeSHS || heat != lastHeat || lastHeatStored != heatStorage.getAmount()){
            sendData();
        }
    }


    private HeatData genHeat() {
        BlockPos belowPos = getBlockPos().below();
        if (getControllerEntity().getControllerEntity().getConnectedBlocks().contains(belowPos)){ //防止加热自己
            return new HeatData(0,0);
        }
        Optional<Holder.Reference<CustomHeater>> customHeatOp = CustomHeater.getFromBlockState(getLevel().registryAccess(), getLevel().getBlockState(belowPos));
        if (getLevel().getBlockEntity(belowPos) instanceof HeatProvider provider){
           return new HeatData(provider.getHeatPerTick(),provider.getSupperHeatCount());
        }else if (customHeatOp.isPresent()){
            CustomHeater customHeater = customHeatOp.get().value();
            return new HeatData(customHeater.heatPerTick(),customHeater.superHeatCount());
        }else {
            HeatLevel heatLevelB = getHeatLevel(belowPos);
            int boilHeat = (int) BoilerHeater.findHeat(getLevel(), getBlockPos().below(), getLevel().getBlockState(getBlockPos().below())) + 1;

            int result = Math.max(getHeatPerTick(heatLevelB), boilHeat);
            if (!Config.ALLOW_PASSIVE_HEAT.get()) {
                result = result == 1 ? 0 : result;
            }
            return new HeatData(result, result >= 3 ? 1 : 0);
        }
    }

    private CostHeatResult costHeat(int heat,int tickSkip,int superHeat){
        if (!needToHeat()){
            setBlockHeat(NONE);
            return new CostHeatResult(heat,superHeat);
        }

        boolean canSuperHeat = Config.ALLOW_GENERATE_SUPER_HEAT.get() || superHeat > 0;

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


    public int calculateHeatCost(int tickSkip,HeatLevel heatLevel){
        return tickSkip * getHeatPerTick(heatLevel);
    }

    public int getHeatPerTick(HeatLevel heatLevel){
        return  switch (heatLevel){
            case NONE -> 0;
            case SMOULDERING -> 1;
            case FADING, KINDLED -> Config.HEAT_PER_FADING_BLAZE.get();
            case SEETHING -> Config.HEAT_PER_SEETHING_BLAZE.get();
        };
    }

    private boolean needToHeatUp(HeatLevel heatLevel){
        return needToHeat() && (getHeatLevel().equals(NONE) ||
                heatLevel.equals(SEETHING) && !getHeatLevel().equals(SEETHING));
    }

    private boolean needToHeat(){
        AtomicBoolean findProcesser = new AtomicBoolean(false);
        List<BlockPos> transferProcesserKeys = List.copyOf(getControllerEntity().transferProcesserMap.keySet());
        AllDirectionOf(getBlockPos(),blockPos->{
            if (!findProcesser.get()){
                findProcesser.set(transferProcesserKeys.contains(blockPos));
            }
        });
        return needToHeatAbove() || findProcesser.get();
    }

    private Optional<HeatTransferProcesser> getHeatTransferProcesserByOther(BlockPos pos){
        return Optional.ofNullable(getControllerEntity().transferProcesserMap.get(pos));
    }

    public boolean needToHeatAbove(){
        boolean needToHeatAbove = getLevel().getBlockState(getBlockPos().above()).is(CHTags.BlockTag.SHOULD_HEAT);
        boolean needToHeatBoiler = getLevel().getBlockEntity(getBlockPos().above()) instanceof FluidTankBlockEntity fluidTankBlockEntity &&
                fluidTankBlockEntity.getControllerBE().boiler.attachedEngines > 0;
        return needToHeatAbove || needToHeatBoiler;
    }

    public HeatLevel getHeatLevel(BlockPos pos){
        BlockState blockState = getLevel().getBlockState(pos);
        return blockState.hasProperty(HEAT_LEVEL) ? blockState.getValue(HEAT_LEVEL) : NONE;
    }
    public HeatLevel getHeatLevel(){
        return getBlockState().getValue(HEAT_LEVEL);
    }

    public void setBlockHeat(HeatLevel heatLevel){
        getLevel().setBlock(getBlockPos(),getBlockState().setValue(HEAT_LEVEL, heatLevel), 3);
        notifyUpdate();
    }


    @Override
    public void addConnectedPos(BlockPos pos) {
        super.addConnectedPos(pos);
        calculateHeatStorage();
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ThermalBlockEntity controllerEntity = getControllerEntity();
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

    @Override
    public void walkAllBlocks(BlockPos exceptFor) {
        super.walkAllBlocks(exceptFor);
        calculateHeatStorage();
    }


    @Override
    protected void mergeController(BlockPos oldControllerPos, ConnectableBlockEntity<?> oldControllerEntity, BlockPos newControllerPos, ThermalBlockEntity newControllerEntity) {
        ((ThermalBlockEntity)oldControllerEntity).switchStoneHeatStorageToNew(newControllerPos,newControllerEntity,true);
    }

    @Override
    protected void switchToNewControllerWhenDestroy(BlockPos newPos, ConnectableBlockEntity<?> newControllerEntity) {
        switchStoneHeatStorageToNew(newPos, (ThermalBlockEntity) newControllerEntity, false);
    }

    @Override
    public void destroy() {
        transferProcesserMap.forEach((b,h)->h.onControllerRemove());
        super.destroy();
    }

    private void switchStoneHeatStorageToNew(BlockPos newControllerPos, ThermalBlockEntity newControllerEntity, boolean merge){
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

    private void calculateHeatStorage(){
        heatStorage.setCapacity(connectedBlocks.size() * MAX_HEAT.get());
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


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean isConnectTo(StoneHeatStorage shs){
        return shs.isConnect(getControllerEntity().connectedBlocks);
    }



    public int getHeat() {
        return heat;
    }

    public HeatStorage getHeatStorage() {
        if (isController()){
            return heatStorage;
        }else {
            return getControllerEntity().getHeatStorage();
        }
    }

    private static int calculateHeatProvide(BlockPos cPos,ThermalBlockEntity controller,int tickSkip){
        AtomicInteger heatAccept = new AtomicInteger();
        AllDirectionOf(cPos,pos -> {
            if (controller.getConnectedBlocks().contains(pos)){
                HeatLevel heatLevel = controller.getHeatLevel(pos);
                heatAccept.addAndGet(controller.calculateHeatCost(tickSkip, heatLevel));
            }
        });
        return heatAccept.get();
    }

    private record CostHeatResult(int heat, int superHeatCount){}


    public static class HeatRecipeTransferProcesser extends HeatTransferProcesser{
        public static final ResourceLocation TYPE = CreateHeat.asResource("heat_recipe");
        private boolean finished = false;
        private boolean removed = false;
        private boolean isMainProcesser = false;
        private int acceptedHeat = 0;
        private int totalHeat = 0;
        private BlockPos fr = null;
        private HeatRecipeTransferProcesser mainProcesser = null;

        public HeatRecipeTransferProcesser() {
            super(TYPE);
        }

        @Override
        public boolean needHeat(Level level, BlockPos checkPos, @Nullable Direction face) {
                Optional<RecipeHolder<HeatRecipe>> optionalRH = getRecipe(level,checkPos);
                if (optionalRH.isPresent()){
                    if (face == null){
                        return !finished;
                    }

                    ThermalBlockEntity thBlockAttach = getAttachThermalBlock(level,checkPos,face);
                    AtomicReference<Optional<HeatTransferProcesser>> oHRP = new AtomicReference<>(Optional.empty());
                    AllDirectionOf(checkPos,
                            (checkControllerPos,f)-> {
                                if (!f.equals(face.getOpposite()) && level.getBlockEntity(checkControllerPos) instanceof ThermalBlockEntity otherTE) {
                                    if (!otherTE.getControllerPos().equals(thBlockAttach.getControllerPos())) {
                                        Optional<HeatTransferProcesser> otherHTP = otherTE.getHeatTransferProcesserByOther(checkPos);
                                        if (otherHTP.isPresent() && otherHTP.get() instanceof HeatRecipeTransferProcesser hRTP && hRTP.isMainProcesser){
                                            oHRP.set(otherHTP);
                                        }
                                    }
                                }
                            },
                            //找到mainProcesser就终止
                            c-> oHRP.get().isPresent() && oHRP.get().get() instanceof HeatRecipeTransferProcesser hRTP && hRTP.isMainProcesser
                    );

                    if (oHRP.get().isEmpty()){
                        this.isMainProcesser = true;
                        this.mainProcesser = null;
                    }else if (oHRP.get().get() instanceof HeatRecipeTransferProcesser mainRecipeP){
                        this.mainProcesser = mainRecipeP.isMainProcesser ? mainRecipeP : mainRecipeP.mainProcesser;
                    }

                    this.fr = checkPos.relative(face.getOpposite());

                    return true;
                }else {
                    return false;
                }
        }

        @Override
        public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide,int tickSkip) {
            if (isMainProcesser){
                acceptHeatAsMainP(level,hTPPos,heatProvide,tickSkip);
            }else {
                if (mainProcesser.removed){
                    //转换到新的mainProcesser
                    if (mainProcesser.mainProcesser == null){
                        mainProcesser.switchToNew(this);
                        acceptHeatAsMainP(level,hTPPos,heatProvide,tickSkip);
                    }else {
                        mainProcesser = mainProcesser.mainProcesser;
                        mainProcesser.mainAcceptHeat(heatProvide);
                    }
                }else {
                    mainProcesser.mainAcceptHeat(heatProvide);
                }
            }
        }

        private void mainAcceptHeat(int heat){
            this.acceptedHeat += heat;
        }

        @Override
        public boolean shouldProcessEveryTick() {
            return false;
        }

        @Override
        public boolean shouldWriteAndReadFromNbt() {
            return isMainProcesser;
        }

        @Override
        public void onControllerRemove() {
            this.removed = true;
        }

        @Override
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("total",totalHeat);
            nbt.putInt("accepted",acceptedHeat);
            nbt.putBoolean("is_main",isMainProcesser);
            return nbt;
        }

        @Override
        public void fromNbt(CompoundTag nbt) {
            totalHeat = nbt.getInt("total");
            acceptedHeat = nbt.getInt("accepted");
            isMainProcesser = nbt.getBoolean("is_main");
        }

        private void acceptHeatAsMainP(Level level, BlockPos hTPPos, int heatProvide, int tickSkip){
            Optional<RecipeHolder<HeatRecipe>> optionalRH =  getRecipe(level,hTPPos);
            if (optionalRH.isPresent()){
                HeatRecipe recipe = optionalRH.get().value();
                acceptedHeat += heatProvide;
                if (acceptedHeat >= recipe.getMinHeatPerTick() * tickSkip){
                    totalHeat += acceptedHeat;
                    if (totalHeat >= recipe.getHeatCost()){
                        doneRecipe(level,recipe,hTPPos);
                    }
                }
            }
            acceptedHeat = 0;
        }

        private void doneRecipe(Level level, HeatRecipe heatRecipe, BlockPos processPos) {
            BlockState outputState = heatRecipe.getOutputBlock();
            if (outputState.getFluidState().is(FluidTags.WATER) && level.dimensionType().ultraWarm()) {
                level.destroyBlock(processPos, false);
            } else {
                level.setBlock(processPos, heatRecipe.getOutputBlock(), 3);
            }
            this.finished = true;
        }

        private void switchToNew(HeatRecipeTransferProcesser newTransferP){
            this.mainProcesser = newTransferP;
            newTransferP.isMainProcesser = true;
            newTransferP.mainProcesser = null;
            newTransferP.totalHeat = this.totalHeat;
            newTransferP.acceptedHeat = this.acceptedHeat;
        }

        private static ThermalBlockEntity getAttachThermalBlock(Level level, BlockPos transferProcesserPos, Direction face){
            BlockPos thBlockPos = transferProcesserPos.relative(face.getOpposite());
            return level.getBlockEntity(thBlockPos, CHBlocks.THERMAL_BLOCK_ENTITY.get()).orElse(null);
        }

        private static Optional<RecipeHolder<HeatRecipe>> getRecipe(Level level,BlockPos checkPos){
            BlockState checkState = level.getBlockState(checkPos);
            HeatRecipe.HeatRecipeTester tester = new HeatRecipe.HeatRecipeTester(checkState);
            return level.getRecipeManager().getRecipeFor(CHRecipes.HEAT_RECIPE.get(),tester,level);
        }
    }

    private record HeatData(int heat,int superHeatCount){}
}
