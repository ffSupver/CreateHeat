package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.*;

public class ThermalBlockEntity extends SmartBlockEntity {
    private boolean isController;
    private BlockPos controllerPos;

    private final Set<BlockPos> connectedBlocks = new HashSet<>();

    private int heat;
    private static final Supplier<Integer> MAX_HEAT = () -> 50 * Config.HEAT_PER_FADING_BLAZE.get();
    private int cooldown;
    private static final int MAX_COOLDOWN = 10;

    private final HeatStorage heatStorage = new HeatStorage(MAX_HEAT.get());
    private final ArrayList<StoneHeatStorage> stoneHeatStorages = new ArrayList<>();

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

            tag.put("shs",NbtUtil.writeToNbtList(stoneHeatStorages, StoneHeatStorage::toNbt));
        }else {
            tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        isController = tag.getBoolean("is_controller");
        if (isController) {
            connectedBlocks.clear();
            connectedBlocks.addAll(NbtUtil.readBlockPosFromNbtList(tag.getList("connected", Tag.TAG_COMPOUND)));
            heat = tag.getInt("heat");
            cooldown = tag.getInt("cooldown");

            heatStorage.fromNbt(tag.getCompound("heat_storage"));

            stoneHeatStorages.clear();
            stoneHeatStorages.addAll(NbtUtil.readFromNbt(tag.getList("shs",Tag.TAG_COMPOUND),tS -> StoneHeatStorage.cFromNbt((CompoundTag) tS)));
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


        Set<ThermalBlockEntity> connectedBlockList = connectedBlocks.stream().map(
                pos -> getLevel().getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity ? thermalBlockEntity : null
        ).collect(Collectors.toSet());

        heat = 0;

        int superHeatCount = 0;


        //移除断开连接的或者空的储热器 检查每个储热器的方块状态
        Iterator<StoneHeatStorage> sHSIterator = stoneHeatStorages.iterator();
        while (sHSIterator.hasNext()){
            StoneHeatStorage sHS = sHSIterator.next();
            if (!sHS.isConnect(connectedBlocks)){
                sHSIterator.remove();
            }else {
                sHS.checkSize(getLevel());
                if (sHS.getCapacity() <= 0){
                    sHSIterator.remove();
                }
            }
        }

        //计算加热数
        for (ThermalBlockEntity thermalBlockEntity : connectedBlockList){
            int heatBelow = thermalBlockEntity.genHeat();
            heat += heatBelow * MAX_COOLDOWN;
            superHeatCount += (heatBelow >= 3)? 1 : 0;

            // 寻找新储热器
            for (Direction d : Direction.values()){
                BlockPos checkPos = thermalBlockEntity.getBlockPos().relative(d);
                if (StoneHeatStorage.isAvailableBlock(getLevel().getBlockState(checkPos))){
                   boolean foundIn = stoneHeatStorages.stream().anyMatch(sHs->sHs.hasPos(checkPos));
                    if (!foundIn){
                        StoneHeatStorage newShs = new StoneHeatStorage(new HashSet<>(Set.of(checkPos)));
                        newShs.checkSize(getLevel(),checkPos,true);
                        stoneHeatStorages.add(newShs);
                    }
                }
            }
        }



        releaseHeat();

        for (ThermalBlockEntity thermalBlockEntity : connectedBlockList){
            CostHeatResult costHeatResult = thermalBlockEntity.costHeat(heat,MAX_COOLDOWN,superHeatCount);
            heat = costHeatResult.heat;
            superHeatCount = costHeatResult.superHeatCount;
        }

        storageHeat();
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
        if (!needToHeatAbove()){
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
    private void storageHeat(){
        if (heat > 0){
            int left = heat;
            for (StoneHeatStorage stoneHeatStorage : stoneHeatStorages){
                if (left > 0){
                    int inserted = stoneHeatStorage.insert(left);
                    left -= inserted;
                }

                //更新方块状态
                stoneHeatStorage.updateBlockState(getLevel());
            }

            if (left > 0){
                heatStorage.insert(left);
            }
        }
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

    public boolean needToHeatUp(HeatLevel heatLevel){
        return needToHeatAbove() && (getHeatLevel().equals(NONE) ||
                heatLevel.equals(SEETHING) && !getHeatLevel().equals(SEETHING));
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

    public void checkNeighbour(){
        if (!isController){
            boolean foundConnect = false;
            for (Direction direction : Direction.values()) {
                if (getLevel().getBlockEntity(getBlockPos().relative(direction)) instanceof ThermalBlockEntity neighbourEntity) {
                    this.isController = false;
                    if (!foundConnect){
                        this.controllerPos = neighbourEntity.getControllerPos();
                        foundConnect = true;
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
            }

            if (!foundConnect){
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
        if (isController){
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

    public void walkAllBlocks(BlockPos exceptFor){
        Set<BlockPos> oldBlocks = Set.copyOf(connectedBlocks);
        connectedBlocks.clear();
        walkAllBlocks(getBlockPos(),connectedBlocks,pos -> {
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

    public static void walkAllBlocks(BlockPos startPos, Set<BlockPos> walkedBlockPos, Predicate<BlockPos> check) {
        // 如果当前位置已经遍历过，或者不满足条件，则返回
        if (walkedBlockPos.contains(startPos) || !check.test(startPos)) {
            return;
        }

        // 将当前位置添加到已遍历集合中
        walkedBlockPos.add(startPos);

        // 遍历六个方向（上、下、北、南、西、东）
        for (Direction direction : Direction.values()) {
            // 获取相邻方块的位置
            BlockPos neighborPos = startPos.relative(direction);

            // 递归遍历相邻方块
            walkAllBlocks(neighborPos, walkedBlockPos, check);
        }
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
        }else if (getLevel().getBlockEntity(controllerPos) instanceof ThermalBlockEntity controllerEntity){
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
    public static class HeatStorage{
        private int capacity;
        private int amount;
        public HeatStorage(int capacity){
            this.capacity = capacity;
            this.amount = 0;
        }

        public int insert(int heat){
            int max = amount + heat;
            if (max > capacity){
                amount = capacity;
                return heat - (max - capacity);
            }else {
                amount = max;
                return heat;
            }
        }

        public int extract(int heat,boolean simulate){
            int min = amount - heat;
            if (min < 0){
                if (!simulate){
                    amount = 0;
                }
                return heat + min;
            }else {
                if (!simulate){
                    amount = min;
                }
                return heat;
            }
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
            this.amount = Math.min(amount,capacity);
        }

        public int getCapacity() {
            return capacity;
        }

        public CompoundTag toNbt(){
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("capacity",capacity);
            nbt.putInt("amount",amount);
            return nbt;
        }

        public void fromNbt(CompoundTag nbt){
            this.capacity = nbt.getInt("capacity");
            this.amount = nbt.getInt("amount");
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "{"+amount+"/"+capacity+"}";
        }
    }

    private static class StoneHeatStorage extends HeatStorage{
        private static final Supplier<Integer> HEAT_PER_LAVA = ()->MAX_HEAT.get() * 10;
        private final HashSet<BlockPos> stonePosSet;

        public StoneHeatStorage(HashSet<BlockPos> stonePosSet) {
            super(calculateCapacity(stonePosSet));
            this.stonePosSet = stonePosSet;
        }

        private boolean checkSize(Level level,BlockPos startPos,boolean shouldSetAmountWithoutCheck){
            boolean modified = false;

            Set<BlockPos> oldSet = Set.copyOf(stonePosSet);
            stonePosSet.clear();
            AtomicInteger lavaCount = new AtomicInteger();
            AtomicInteger stoneCount = new AtomicInteger();

            walkAllBlocks(startPos,stonePosSet,b->{
               BlockState bsT = level.getBlockState(b);
               if (isAvailableLavaBlock(bsT)){
                   lavaCount.getAndIncrement();
               }else if (isAvailableTStoneBlock(bsT)) {
                   stoneCount.getAndIncrement();
               }
               return isAvailableBlock(bsT);
            });

            if (oldSet.size() != stonePosSet.size()){
                setCapacity(calculateCapacity(stonePosSet));
                modified = true;
            }

            int lC = lavaCount.get();
            int sC = stoneCount.get();
            int amountFromBlock = sC + lC > 0 ? lC * getCapacity() / (sC + lC) : 0;
            if (shouldSetAmountWithoutCheck){
                setAmount(amountFromBlock);
            }else {
                setAmount(Math.max(getAmount(),amountFromBlock));
            }

            return modified;
        }

        public boolean checkSize(Level level) {
            if (!stonePosSet.isEmpty()){
               return checkSize(level, stonePosSet.iterator().next(),false);
            }
            return false;
        }

        public boolean updateBlockState(Level level){
            int lavaCount = 0;
            int stoneCount = 0;
            Set<BlockPos> stoneBlockPosSet = new HashSet<>();
            Set<BlockPos> lavaBlockPosSet = new HashSet<>();
            for (BlockPos sPos : stonePosSet){
                if (isAvailableLavaBlock(level.getBlockState(sPos))){
                    lavaCount++;
                    lavaBlockPosSet.add(sPos);
                }else if (level.getBlockState(sPos).is(CHBlocks.TIGHT_COMPRESSED_STONE)){
                    stoneCount++;
                    stoneBlockPosSet.add(sPos);
                }
            }

            if (lavaCount + stoneCount == 0){
                return false;
            }

            int bCount = stoneCount + lavaCount;
            int aUp = getCapacity() * (lavaCount + 1) / bCount;
            int aDw = getCapacity() * lavaCount / bCount;
            boolean addTStone = aDw > getAmount();
            boolean removeTStone = aUp <= getAmount();
            if (addTStone){
                int countToAdd = (aDw - getAmount()) * bCount / getCapacity() + 1;
                List<BlockPos> lavaPosList = new  ArrayList<>(lavaBlockPosSet);
                for (int i = 0;i < Math.min(countToAdd,lavaPosList.size());i++){
                    level.setBlock(lavaPosList.get(i),CHBlocks.TIGHT_COMPRESSED_STONE.getDefaultState(),3);
                }
            }else if (removeTStone){
                int countToRemove = (getAmount() - aUp) * bCount / getCapacity() + 1;
                List<BlockPos> stonePosList = new  ArrayList<>(stoneBlockPosSet);
                for (int i = 0;i < Math.min(countToRemove,stonePosList.size());i++){
                    level.setBlock(stonePosList.get(i),Blocks.LAVA.defaultBlockState(),3);
                }
            }
            return addTStone || removeTStone;
        }

        public static boolean isAvailableBlock(BlockState bsT){
            return isAvailableLavaBlock(bsT) || isAvailableTStoneBlock(bsT);
        }

        public static boolean isAvailableLavaBlock(BlockState bsT){
            return bsT.is(Blocks.LAVA) && bsT.getValue(LiquidBlock.LEVEL) == 0;
        }

        public static boolean isAvailableTStoneBlock(BlockState bsT){
            return bsT.is(CHBlocks.TIGHT_COMPRESSED_STONE.get());
        }

        private static int calculateCapacity(Set<BlockPos> stonePosSet){
            return stonePosSet.size() * HEAT_PER_LAVA.get();
        }

        public void calculateAmountByBlock(Level level){
            for (BlockPos stonePos : stonePosSet){
                BlockState blockState = level.getBlockState(stonePos);
                setAmount(getAmount() + (isAvailableLavaBlock(blockState) ?
                    HEAT_PER_LAVA.get() : 0
                ));
            }
        }

        public boolean hasPos(BlockPos pos){
            return stonePosSet.contains(pos);
        }

        @Override
        public void fromNbt(CompoundTag nbt) {
            super.fromNbt(nbt);
            stonePosSet.clear();
            stonePosSet.addAll(NbtUtil.readBlockPosFromNbtList(nbt.getList("stone_pos",Tag.TAG_COMPOUND)));
        }

        public static StoneHeatStorage cFromNbt(CompoundTag tag){
            StoneHeatStorage r = new StoneHeatStorage(new HashSet<>());
            r.fromNbt(tag);
            return r;
        }

        @Override
        public CompoundTag toNbt() {
            CompoundTag nbt = super.toNbt();
            nbt.put("stone_pos",NbtUtil.writeBlockPosToNbtList(stonePosSet));
            return nbt;
        }

        public boolean isConnect(Set<BlockPos> posSet){
            for (BlockPos thisPos : stonePosSet){
                for (Direction d : Direction.values()){
                    for (BlockPos thPos : posSet) {
                        if (thisPos.relative(d).equals(thPos)){
                            return true;
                        }
                    }
                }
            }
            return false;
        }


    }
}
