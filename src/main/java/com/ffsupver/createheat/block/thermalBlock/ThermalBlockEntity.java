package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.recipe.HeatRecipe;
import com.ffsupver.createheat.registries.CHRecipes;
import com.ffsupver.createheat.util.BlockPosUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import joptsimple.internal.Strings;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.ffsupver.createheat.util.BlockPosUtil.AllDirectionOf;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class ThermalBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private boolean isController;
    private BlockPos controllerPos;

    private final Set<BlockPos> connectedBlocks = new HashSet<>();

    private int heat;
    public static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private int cooldown;
    private static final int MAX_COOLDOWN = 10;

    private final HeatStorage heatStorage = new HeatStorage(MAX_HEAT.get());
    private final ArrayList<StoneHeatStorage> stoneHeatStorages = new ArrayList<>();
    private final ArrayList<StoneHeatStorage> stoneHeatStoragesToAddLater = new ArrayList<>();

    private final Map<BlockPos,HeatRecipeProcessing> recipeProcessingMap = new HashMap<>();

    private final HeatStorage displayHeatStorage = new HeatStorage(0);

    public ThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controllerPos = null;
        cooldown = 0;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("is_controller",isController);
        if (isController) {
            tag.put("connected",NbtUtil.writeBlockPosToNbtList(connectedBlocks));
            tag.putInt("heat",heat);
            tag.putInt("cooldown",cooldown);

            tag.put("heat_storage",heatStorage.toNbt());

            tag.put("shs",NbtUtil.writeToNbtList(stoneHeatStorages, shs->{
                if (shs.shouldWriteToNbt(getControllerPos())){
                    return shs.toNbt();
                }else {
                    return null;
                }
            }));

            tag.put("recipe",NbtUtil.writeMapToNbtList(
                    recipeProcessingMap,
                    NbtUtil::blockPosToNbt,
                    hRP->hRP.toNbt(getBlockPos()))
            );

//            if (clientPacket){
                tag.put("display_heat",getAllHeatForDisplay().toNbt());
//            }
        }else {
            tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        //getLevel return null
        super.read(tag, registries, clientPacket);
        isController = tag.getBoolean("is_controller");
        if (isController) {
            connectedBlocks.clear();
            connectedBlocks.addAll(NbtUtil.readBlockPosFromNbtList(tag.getList("connected", Tag.TAG_COMPOUND)));
            heat = tag.getInt("heat");
            cooldown = tag.getInt("cooldown");

            heatStorage.fromNbt(tag.getCompound("heat_storage"));

            stoneHeatStorages.clear();
            stoneHeatStorages.addAll(NbtUtil.readListFromNbt(tag.getList("shs",Tag.TAG_COMPOUND), tS -> StoneHeatStorage.cFromNbt((CompoundTag) tS)));

            recipeProcessingMap.clear();
            recipeProcessingMap.putAll(NbtUtil.readMapFromNbtList(
                    tag.getList("recipe",Tag.TAG_COMPOUND),
                    NbtUtil::blockPosFromNbt,
                    t->HeatRecipeProcessing.fromNbt((CompoundTag) t,getBlockPos()))
            );

//            if (clientPacket){
              displayHeatStorage.fromNbt(tag.getCompound("display_heat"));
//            }
        }else {
            controllerPos = NBTHelper.readBlockPos(tag,"controller");
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!isController){
            return;
        }

        if (cooldown < 0){
            tickEveryConnectedBlock();
            cooldown = MAX_COOLDOWN;
        }else {
            cooldown--;
        }


    }

    public void tickEveryConnectedBlock(){
        if (getLevel().isClientSide()){
            return;
        }

        int tickSkip = MAX_COOLDOWN;

        Set<ThermalBlockEntity> connectedBlockList = connectedBlocks.stream().map(
                pos -> getLevel().getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity ? thermalBlockEntity : null
        ).collect(Collectors.toSet());

        heat = 0;

        int superHeatCount = 0;


        //移除断开连接的或者空的储热器 检查每个储热器的方块状态
        boolean changeSHS = false;
        Iterator<StoneHeatStorage> sHSIterator = stoneHeatStorages.iterator();
        while (sHSIterator.hasNext()){
            StoneHeatStorage sHS = sHSIterator.next();
            if (!sHS.isConnect(connectedBlocks)){
                sHS.disconnect(getControllerPos());
                sHSIterator.remove();
                changeSHS = true;
            }else {
                changeSHS = changeSHS || sHS.checkSize(getLevel(),getControllerPos());
                if (sHS.getCapacity() <= 0 || sHS.shouldChangeController){
                    sHSIterator.remove();
                    changeSHS = true;
                }
            }
        }
        //添加失去控制器的储热器
        stoneHeatStorages.addAll(stoneHeatStoragesToAddLater);
        stoneHeatStoragesToAddLater.clear();


        //移除已经完成的配方
        List<BlockPos> processPosToRemove = recipeProcessingMap.entrySet().stream()
                .filter(entry->entry.getValue().finished)
                .map(Map.Entry::getKey).toList();
        processPosToRemove.forEach(recipeProcessingMap::remove);

        //====加热部分====


        //计算加热数  寻找配方
        List<HeatRecipeProcessing> hRPL = new ArrayList<>();
        for (ThermalBlockEntity thermalBlockEntity : connectedBlockList){
            hRPL.addAll(thermalBlockEntity.getRecipes());

            int heatBelow = thermalBlockEntity.genHeat();
            heat += heatBelow * tickSkip;
            superHeatCount += (heatBelow >= 3)? 1 : 0;

            // 寻找新储热器
            for (Direction d : Direction.values()){
                BlockPos checkPos = thermalBlockEntity.getBlockPos().relative(d);
                if (StoneHeatStorage.isAvailableBlock(getLevel().getBlockState(checkPos))){
                   boolean foundIn = isPosInStoneHeatStorage(checkPos);
                    if (!foundIn){
                        StoneHeatStorage newShs = new StoneHeatStorage(new HashSet<>(Set.of(checkPos)),getControllerPos());
                        //储热器连接的控制器
                        Set<BlockPos> connectControllerPosSet = newShs.checkSize(getLevel(),checkPos,true);
                        boolean foundOldSHS = false; //其他控制器是否已经有该储热器
                        for (BlockPos otherControllerPos : connectControllerPosSet){
                            if (!otherControllerPos.equals(getControllerPos()) && getLevel().getBlockEntity(otherControllerPos) instanceof ThermalBlockEntity t){
                                ThermalBlockEntity otherControllerBE = t.getControllerEntity();
                                Optional<StoneHeatStorage> oldSHSOp = otherControllerBE.findOldStoneHeatStorage(newShs);
                                if (oldSHSOp.isPresent()){
                                    stoneHeatStorages.add(oldSHSOp.get());
                                    foundOldSHS = true;
                                    break;
                                }
                            }
                        }

                        if (!foundOldSHS){
                            stoneHeatStorages.add(newShs);
                        }

                        changeSHS = true;
                    }
                }
            }
        }
        releaseHeat();

        //添加找到的新配方
        checkAddHeatRecipeProcessing(hRPL);


        //====耗热部分====

        for (ThermalBlockEntity thermalBlockEntity : connectedBlockList){
            CostHeatResult costHeatResult = thermalBlockEntity.costHeat(heat,tickSkip,superHeatCount);
            heat = costHeatResult.heat;
            superHeatCount = costHeatResult.superHeatCount;
        }

        //处理配方->周围方块转换完毕 已经计算过耗热
        recipeProcessingMap.values().forEach(rP->{
            rP.addHeat(this,tickSkip);
            heat += rP.process(getBlockPos(),getLevel(),tickSkip);
        });


       boolean storageUpdate = storageHeat();
        if (changeSHS || storageUpdate){
            sendData();
        }
    }

    private void checkAddHeatRecipeProcessing(List<HeatRecipeProcessing> hRPL) {
        for (HeatRecipeProcessing h : hRPL){
            if (!recipeProcessingMap.containsKey(h.processPos)){
                recipeProcessingMap.put(h.processPos,h);
            }
        }
    }

    public int genHeat(){
        HeatLevel heatLevelB = getHeatLevel(getBlockPos().below());
        int boilHeat = (int) BoilerHeater.findHeat(getLevel(),getBlockPos().below(),getLevel().getBlockState(getBlockPos().below())) + 1;
        int result = Math.max(getHeatPerTick(heatLevelB),boilHeat);
        if (Config.ALLOW_PASSIVE_HEAT.get()){
            return  result;
        }else {
            return  result == 1 ? 0 : result;
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
    private boolean storageHeat(){
        boolean blockUpdate = false;
        if (heat > 0){
            int left = heat;
            for (StoneHeatStorage stoneHeatStorage : stoneHeatStorages){
                if (left > 0){
                    int inserted = stoneHeatStorage.insert(left);
                    left -= inserted;
                }

                //更新方块状态
                blockUpdate = blockUpdate || stoneHeatStorage.updateBlockState(getLevel(),getControllerPos());
            }

            if (left > 0){
                heatStorage.insert(left);
            }
        }
        return blockUpdate;
    }

    //释放存储的热
    private void releaseHeat(){
        if (heat <= 0){
            for (StoneHeatStorage stoneHeatStorage : stoneHeatStorages){
                int need = heatStorage.getCapacity() - heatStorage.getAmount();
                if (need > 0){
                    int toExtract = stoneHeatStorage.extract(need, true);
                    toExtract = heatStorage.insert(toExtract);
                    stoneHeatStorage.extract(toExtract, false);
                }
            }


            heat += heatStorage.extract(heatStorage.getAmount(),false);
        }
    }

    private boolean isPosInStoneHeatStorage(BlockPos checkPos){
        if (isController()){
           return stoneHeatStorages.stream().anyMatch(sHs -> sHs.hasPos(checkPos));
        }else {
            return getControllerEntity().isPosInStoneHeatStorage(checkPos);
        }
    }

    private Optional<StoneHeatStorage> findOldStoneHeatStorage(StoneHeatStorage newSHS){
        if (isController()){
           return stoneHeatStorages.stream()
                   .filter(
                           oldSHS-> !oldSHS.stonePosSet.isEmpty() &&
                                   newSHS.stonePosSet.contains(oldSHS.stonePosSet.iterator().next())
                   ).findFirst();
        }else {
            return getControllerEntity().findOldStoneHeatStorage(newSHS);
        }
    }

    public boolean acceptNewStoneHeatStorage(StoneHeatStorage oldSHS){
        if (isController()){
            if (oldSHS.isConnect(connectedBlocks)) {
                stoneHeatStoragesToAddLater.add(new StoneHeatStorage(oldSHS, getControllerPos()));
                return true;
            }
            return false;
        }else {
            return getControllerEntity().acceptNewStoneHeatStorage(oldSHS);
        }
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
        return needToHeatAbove() || canProcessRecipe(getBlockPos());
    }

    private List<HeatRecipeProcessing> getRecipes(){
        List<HeatRecipeProcessing> rPList = new ArrayList<>();
        AllDirectionOf(getBlockPos(),checkPos->{
            BlockState checkState = getLevel().getBlockState(checkPos);
            HeatRecipe.HeatRecipeTester tester = new HeatRecipe.HeatRecipeTester(checkState);
            Optional<RecipeHolder<HeatRecipe>> optionalRH = getLevel().getRecipeManager().getRecipeFor(CHRecipes.HEAT_RECIPE.get(),tester,getLevel());
            if (optionalRH.isPresent()){
                AtomicReference<Optional<HeatRecipeProcessing>> oHRP = new AtomicReference<>(Optional.empty());
                AllDirectionOf(checkPos,
                        checkControllerPos-> {
                            if (!checkControllerPos.equals(getBlockPos()) && getLevel().getBlockEntity(checkControllerPos) instanceof ThermalBlockEntity otherTE) {
                                if (!otherTE.getControllerPos().equals(getControllerPos())) {
                                    oHRP.set(otherTE.getRecipeByOther(checkPos));
                                }
                            }
                        },
                        c->oHRP.get().isPresent() //找到就终止
                );
                HeatRecipeProcessing hRP = oHRP.get().orElse(new HeatRecipeProcessing(checkPos,getControllerPos(), optionalRH.get()));
                rPList.add(hRP);
            }
        });
        return rPList;
    }

    private Optional<HeatRecipeProcessing> getRecipeByOther(BlockPos pos){
        return Optional.ofNullable(getControllerEntity().recipeProcessingMap.get(pos));
    }

    public boolean needToHeatAbove(){
        boolean needToHeatAbove = getLevel().getBlockState(getBlockPos().above()).is(CHTags.BlockTag.SHOULD_HEAT);
        boolean needToHeatBoiler = getLevel().getBlockEntity(getBlockPos().above()) instanceof FluidTankBlockEntity fluidTankBlockEntity &&
                fluidTankBlockEntity.getControllerBE().boiler.attachedEngines > 0;
        return needToHeatAbove || needToHeatBoiler;
    }


    public boolean canProcessRecipe(BlockPos pos){
        if (isController()) {
            AtomicBoolean c = new AtomicBoolean(false);
            AllDirectionOf(pos,checkPos->{
               c.set(recipeProcessingMap.containsKey(checkPos));
            },b->c.get());
            return c.get();
        }else {
           return getControllerEntity().canProcessRecipe(getBlockPos());
        }
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

    public void checkNeighbour(){
        if (!isController){
            AtomicBoolean foundConnect = new AtomicBoolean(false);
            AllDirectionOf(getBlockPos(),checkPos->{
                if (getLevel().getBlockEntity(checkPos) instanceof ThermalBlockEntity neighbourEntity) {
                    this.isController = false;
                    if (!foundConnect.get()){
                        this.controllerPos = neighbourEntity.getControllerPos();
                        foundConnect.set(true);
                    }


                    ThermalBlockEntity controllerEntity = getControllerEntity();
                    if (controllerEntity != null){
                        controllerEntity.addConnectedPos(getBlockPos());
                        if (!neighbourEntity.getControllerPos().equals(controllerPos)){
                            if (neighbourEntity.getControllerEntity() != null){
                                neighbourEntity.getControllerEntity().isController = false;
                            }
                            controllerEntity.walkAllBlocks(null);
                        }
                    }
                }
            });

            if (!foundConnect.get()){
                this.isController = true;
                this.controllerPos = getBlockPos();
                this.addConnectedPos(this.getBlockPos());
            }
        }
    }

    public void addConnectedPos(BlockPos pos){
        this.connectedBlocks.add(pos);
        calculateHeatStorage();
    }

    @Override
    public void destroy() {
        if (isController()){
            stoneHeatStorages.forEach(sHS->sHS.disconnect(getBlockPos()));
            for (BlockPos pos : connectedBlocks){
                if (!pos.equals(getBlockPos()) && getLevel().getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity){
                    thermalBlockEntity.isController = true;
                    thermalBlockEntity.walkAllBlocks(getBlockPos());
                    break;
                }
            }
        }else {
            if (getLevel().getBlockEntity(controllerPos) instanceof ThermalBlockEntity thermalBlockEntity){
                thermalBlockEntity.walkAllBlocks(getBlockPos());
            }
        }
        super.destroy();
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
                return true;
            }
        }
        return false;
    }

    public void walkAllBlocks(BlockPos exceptFor){
        Set<BlockPos> oldBlocks = Set.copyOf(connectedBlocks);
        connectedBlocks.clear();
        BlockPosUtil.walkAllBlocks(getBlockPos(),connectedBlocks, pos -> {
            if (!pos.equals(exceptFor) && getLevel().getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity) {
                thermalBlockEntity.controllerPos = getBlockPos();
                return true;
            }else {
                return false;
            }
        });

        for (BlockPos pos : oldBlocks){
            if (!connectedBlocks.contains(pos) && getLevel().getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity) {
                if(!thermalBlockEntity.isController() && thermalBlockEntity.getControllerPos().equals(getBlockPos())){
                    thermalBlockEntity.isController = true;
                    thermalBlockEntity.walkAllBlocks(exceptFor);
                }
            }
        }

        calculateHeatStorage();

        notifyUpdate();
    }

    private void calculateHeatStorage(){
        heatStorage.setCapacity(connectedBlocks.size() * MAX_HEAT.get());
    }




    public HeatStorage getAllHeatForDisplay(){
        HeatStorage result = new HeatStorage(0);
        for (HeatStorage hs : stoneHeatStorages){
            result.setCapacity(result.getCapacity() + hs.getCapacity());
            result.insert(hs.getAmount());
        }
        result.setCapacity(result.getCapacity() + heatStorage.getCapacity());
        result.insert(heatStorage.getAmount());
        return result;
    }


    public ThermalBlockEntity getControllerEntity(){
        if (isController){
            return this;
        }else if (controllerPos != null && getLevel().getBlockEntity(controllerPos) instanceof ThermalBlockEntity controllerEntity){
            return controllerEntity;
        }else {
            return null;
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public boolean isController(){return isController;}
    public BlockPos getControllerPos(){return isController ? getBlockPos() : controllerPos;}
    public Set<BlockPos> getConnectedBlocks(){return isController ? connectedBlocks : getControllerEntity().getConnectedBlocks();}

    public int getHeat() {
        return heat;
    }

    public HeatStorage getHeatStorage() {
        return heatStorage;
    }

    private record CostHeatResult(int heat, int superHeatCount){}


    private static class HeatRecipeProcessing{
        private final BlockPos processPos;
        private BlockPos recordControllerPos;
        private RecipeHolder<HeatRecipe> recipe;
        private int heatAccept;
        private int heatGot;
        private boolean finished;
        private String recipeId;

        private HeatRecipeProcessing(BlockPos processPos, BlockPos recordControllerPos, RecipeHolder<HeatRecipe> recipe,int heatGot) {
            this.processPos = processPos;
            this.recordControllerPos = recordControllerPos;
            this.recipe = recipe;
            this.heatAccept = 0;
            this.heatGot = heatGot;
        }

        private HeatRecipeProcessing(BlockPos processPos, BlockPos recordControllerPos, RecipeHolder<HeatRecipe> recipe) {
            this(processPos,recordControllerPos,recipe,0);
        }

        private HeatRecipeProcessing(BlockPos processPos, BlockPos recordControllerPos,String recipeId,int heatGot){
            this(processPos,recordControllerPos, (RecipeHolder<HeatRecipe>) null,heatGot);
            this.recipeId = recipeId;
        }

        private void addHeat(ThermalBlockEntity controller,int tickSkip){
            AllDirectionOf(processPos,pos -> {
                if (controller.getConnectedBlocks().contains(pos)){
                    HeatLevel heatLevel = controller.getHeatLevel(pos);
                    this.heatAccept += controller.calculateHeatCost(tickSkip,heatLevel);
                }
            });
        }

        private int process(BlockPos controllerPos,Level level,int tickSkip){
            if (!controllerPos.equals(recordControllerPos)){
                checkRecordController(level,controllerPos);
                return 0;
            }

            if (recipe == null){
                initRecipe(level.getRecipeManager());
            }

            //检测输入方块是否正确
            if (!checkInputBlock(level)){
                this.finished = true;
            }

            HeatRecipe heatRecipe = recipe.value();
            int tmpHeatAccept = heatAccept;
            heatAccept = 0;

            if (finished || tmpHeatAccept < heatRecipe.getMinHeatPerTick() * tickSkip){
                return tmpHeatAccept;
            }

            int heatFinal = heatGot + tmpHeatAccept;

            if (heatFinal >= heatRecipe.getHeatCost()){
                heatGot = heatRecipe.getHeatCost();
                this.finished = true;
                level.setBlock(processPos,heatRecipe.getOutputBlock(),3);
                return heatFinal - heatRecipe.getHeatCost();
            }else {
                heatGot = heatFinal;
                return 0;
            }
        }

        private void initRecipe(RecipeManager recipeManager){
            Optional<RecipeHolder<HeatRecipe>> recipeOp = recipeManager.getAllRecipesFor(CHRecipes.HEAT_RECIPE.get())
                    .stream().filter(h->h.id().toString().equals(recipeId)).findFirst();
            recipe = recipeOp.orElse(null);
            if (recipe == null){
                this.finished = true;
            }
        }

        private void checkRecordController(Level level,BlockPos newPos){
           if (!(level.getBlockEntity(recordControllerPos) instanceof ThermalBlockEntity)){
               recordControllerPos = newPos;
           }
        }

        private boolean checkInputBlock(Level level){
            //recipe 已经初始化
            HeatRecipe.HeatRecipeTester tester = new HeatRecipe.HeatRecipeTester(level.getBlockState(processPos));
            Optional<RecipeHolder<HeatRecipe>> heatRecipeRecipeHolderOp = level.getRecipeManager().getRecipeFor(CHRecipes.HEAT_RECIPE.get(),tester,level);
            if (heatRecipeRecipeHolderOp.isPresent()){
               RecipeHolder<HeatRecipe> heatRecipeH = heatRecipeRecipeHolderOp.get();
               return heatRecipeH.id().equals(recipe.id());
            }
            return false;
        }

        private CompoundTag toNbt(BlockPos controllerPos){
            if (controllerPos.equals(recordControllerPos)){
                CompoundTag nbt = new CompoundTag();
                nbt.put("process_pos",NbtUtils.writeBlockPos(processPos));
                nbt.putInt("heat_got",heatGot);
                nbt.putString("id", recipe == null ? recipeId : recipe.id().toString());

                return nbt;
            }
            return null;
        }

        private static HeatRecipeProcessing fromNbt(CompoundTag nbt, BlockPos recordControllerPos){
            BlockPos processPos = NBTHelper.readBlockPos(nbt,"process_pos");
            int heatGot = nbt.getInt("heat_got");
            String recipeId = nbt.getString("id");
            return new HeatRecipeProcessing(processPos, recordControllerPos, recipeId,heatGot);
        }
    }
}
