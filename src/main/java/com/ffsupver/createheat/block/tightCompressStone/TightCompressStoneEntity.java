package com.ffsupver.createheat.block.tightCompressStone;

import com.ffsupver.createheat.block.ConnectableBlockEntity;
import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour.MAX_COOLDOWN;

public class TightCompressStoneEntity extends ConnectableBlockEntity<TightCompressStoneEntity> {
    private final StoneHeatStorage stoneHeatStorage = new StoneHeatStorage(new HashSet<>(Set.of(getBlockPos())));
    private final Map<BlockPos,Integer> superHeatTakenByTh = new HashMap<>();//存储:没有加热的->使用的超级加热数
    private final Map<BlockPos,Integer> superHeatProvideByTh = new HashMap<>();//存储:有加热的->所有超级加热数
    private final Map<BlockPos,Integer> superHeatActuallyProvideByTh = new HashMap<>();//存储:有加热的->剩余超级加热数 或 没有加热的->失效前的所有超级加热数
    private final Map<BlockPos,Integer> superHeatMergeCashe = new HashMap<>();
    private int coolDown = 0;
    private int lastAmount;

    private int maxSuperHeatHold;
    public TightCompressStoneEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (isController()){
            tag.put("shs",stoneHeatStorage.toNbt());
            tag.putInt("max_super_heat",maxSuperHeatHold);
            tag.put("taken_map", NbtUtil.writeMapToNbtList(superHeatTakenByTh,NbtUtil::blockPosToNbt,NbtUtil::intToNbt));
            tag.put("provide_map", NbtUtil.writeMapToNbtList(superHeatProvideByTh,NbtUtil::blockPosToNbt,NbtUtil::intToNbt));
            tag.put("a_provide_map", NbtUtil.writeMapToNbtList(superHeatActuallyProvideByTh,NbtUtil::blockPosToNbt,NbtUtil::intToNbt));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (isController()){
            stoneHeatStorage.fromNbt(tag.getCompound("shs"),getConnectedBlocks());
            maxSuperHeatHold = tag.getInt("max_super_heat");
            superHeatTakenByTh.clear();
            superHeatTakenByTh.putAll(NbtUtil.readMapFromNbtList(tag.getList("taken_map", Tag.TAG_COMPOUND),NbtUtil::blockPosFromNbt,NbtUtil::intFromNbt));
            superHeatProvideByTh.clear();
            superHeatProvideByTh.putAll(NbtUtil.readMapFromNbtList(tag.getList("provide_map", Tag.TAG_COMPOUND),NbtUtil::blockPosFromNbt,NbtUtil::intFromNbt));
            superHeatActuallyProvideByTh.clear();
            superHeatActuallyProvideByTh.putAll(NbtUtil.readMapFromNbtList(tag.getList("a_provide_map", Tag.TAG_COMPOUND),NbtUtil::blockPosFromNbt,NbtUtil::intFromNbt));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!isController()){
            return;
        }

        if (coolDown > 0){
            coolDown--;
        }else {
            coolDown = MAX_COOLDOWN;

            boolean removed = removeDisconnect();

            boolean change = stoneHeatStorage.checkSize(getLevel(),getConnectedBlocks());
            int superHeatCount = getSuperHeatBlockCount();
            stoneHeatStorage.setSuperHeatCount(superHeatCount);
            change = stoneHeatStorage.updateBlockState(getLevel()) || change;

            if (calculateSuperHeatCountActuallyProvide() > stoneHeatStorage.getSuperHeatCount()){
                reduceSuperCountActuallyProvideToSHSSuperC();
            }


            maxSuperHeatHold = stoneHeatStorage.getMaxSuperHeatCount();

            if (change || lastAmount != stoneHeatStorage.getAmount() || removed){
                notifyUpdate();
            }

            lastAmount = stoneHeatStorage.getAmount();
        }
    }

    private boolean removeDisconnect(){
        boolean removed;
        removed = removeCountMap(superHeatTakenByTh,getLevel(),getControllerEntity().stoneHeatStorage);
        removed = removeCountMap(superHeatProvideByTh,getLevel(),getControllerEntity().stoneHeatStorage) || removed;
        removed = removeCountMap(superHeatActuallyProvideByTh,getLevel(),getControllerEntity().stoneHeatStorage) || removed;
        return removed;
    }

    private int getSuperHeatBlockCount(){
        int bC = 0;
        for (Map.Entry<BlockPos,Integer> entry : superHeatActuallyProvideByTh.entrySet()){
            bC += superHeatProvideByTh.containsKey(entry.getKey()) ? superHeatProvideByTh.get(entry.getKey()) : entry.getValue();
        }
        return bC;
    }

    public boolean isConnect(Set<BlockPos> posSet){
        return getControllerEntity() != null && getControllerEntity().stoneHeatStorage.isConnect(posSet);
    }

    public int insert(int toInsert){
        return getControllerEntity() == null ? toInsert : getControllerEntity().stoneHeatStorage.insert(toInsert);
    }

    public int extract(int toExtract,boolean simulate){
        return getControllerEntity() == null ? 0 : getControllerEntity().stoneHeatStorage.extract(toExtract,simulate);
    }

    public int addSuperHeatCount(int count,int total,boolean usingHeater,BlockPos controllerPos){
        if (isController()) {
            if (usingHeater){

                int lastProvide;

                if (superHeatProvideByTh.containsKey(controllerPos)){
                    lastProvide = superHeatProvideByTh.get(controllerPos);
                    //热源超级加热数减少时
                    if (total < lastProvide){
                        superHeatProvideByTh.replace(controllerPos, total);
                    }
                }
                int nowProvider = superHeatProvideByTh.getOrDefault(controllerPos,0);


                int totalProvide = calculateSuperHeatCountProvide();
                int leftToHold = stoneHeatStorage.getMaxSuperHeatCount() - totalProvide + nowProvider;

                int addProvide = Math.min(total,leftToHold);

                updateSuperCountActuallyProvide(controllerPos,count);

                updateSuperCountProvide(controllerPos,addProvide);
                return total - addProvide;
            }else {
                if (superHeatTakenByTh.containsKey(controllerPos)){
                    int oldTaken = superHeatTakenByTh.get(controllerPos);
                    if (count > oldTaken) {
                        superHeatTakenByTh.remove(controllerPos);
                        return count - oldTaken;
                    }else {
                        superHeatTakenByTh.replace(controllerPos,oldTaken - count);
                        return 0;
                    }
                }else {
                    return count;
                }
            }
        }else {
            return getControllerEntity() == null ? usingHeater ? total : count : getControllerEntity().addSuperHeatCount(count,total,usingHeater,controllerPos);
        }
    }

    public int releaseSuperHeatCount(BlockPos controllerPos){
        if (isController()){
            updateSuperCountTaken(controllerPos,0);//重置使用数
            int totalLeft = calculateSuperHeatCountActuallyProvide() - calculateSuperHeatCountTaken();
            updateSuperCountTaken(controllerPos, totalLeft);
            if (superHeatMergeCashe.containsKey(controllerPos)){
                updateCountMap(superHeatActuallyProvideByTh,controllerPos,superHeatMergeCashe.get(controllerPos));
                superHeatMergeCashe.remove(controllerPos);
            }else if (superHeatProvideByTh.containsKey(controllerPos)) {
                superHeatActuallyProvideByTh.replace(controllerPos, superHeatProvideByTh.get(controllerPos));
                reduceSuperCountActuallyProvideToSHSSuperC();
                superHeatProvideByTh.remove(controllerPos);
            }
            return totalLeft;
        }else {
            return getControllerEntity() == null ? 0 : getControllerEntity().releaseSuperHeatCount(controllerPos);
        }
    }

    public void switchTControllerTo(BlockPos oldPos,BlockPos newPos){
        mergeCountMap(superHeatTakenByTh,newPos,oldPos);
        mergeCountMap(superHeatActuallyProvideByTh,newPos,oldPos);
        updateCountMap(superHeatProvideByTh,newPos,superHeatActuallyProvideByTh.get(newPos));
        superHeatMergeCashe.replace(newPos,superHeatActuallyProvideByTh.get(newPos));
        notifyUpdate();
    }

    private void updateSuperCountProvide(BlockPos pos,int count){
        updateCountMap(superHeatProvideByTh,pos,count);
    }

    private void updateSuperCountActuallyProvide(BlockPos pos, int count){
        updateCountMap(superHeatActuallyProvideByTh,pos,count);
        reduceSuperCountActuallyProvideToSHSSuperC();
    }

    private void reduceSuperCountActuallyProvideToSHSSuperC(){
        reduceCountMapTo(superHeatActuallyProvideByTh,stoneHeatStorage.getSuperHeatCount());
    }

    private void updateSuperCountTaken(BlockPos pos,int count){
        updateCountMap(superHeatTakenByTh,pos,count);
    }

    //更新工程师护目镜显示
    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        Set<BlockPos> connectThBlockPos = new HashSet<>();
        connectThBlockPos.addAll(superHeatTakenByTh.keySet());
        connectThBlockPos.addAll(superHeatProvideByTh.keySet());
        connectThBlockPos.addAll(superHeatActuallyProvideByTh.keySet());
        for (BlockPos thPos : connectThBlockPos){
            if (getLevel().getBlockEntity(thPos) instanceof ConnectableBlockEntity<?> connectableBlockEntity){
                ThermalBlockEntityBehaviour thermalBlockEntityBehaviour = ThermalBlockEntityBehaviour.getFromCBE(connectableBlockEntity);
                if (thermalBlockEntityBehaviour != null){
                    thermalBlockEntityBehaviour.sendData();
                }
            }
        }
    }

    private static void updateCountMap(Map<BlockPos,Integer> toUpdate, BlockPos pos, int count){
        if (toUpdate.containsKey(pos)){
            toUpdate.replace(pos, count);
        }else {
            toUpdate.put(pos,count);
        }
    }

    private static void mergeCountMap(Map<BlockPos,Integer> toUpdate, BlockPos pos, BlockPos oldPos){
        int count = toUpdate.getOrDefault(oldPos,0) + toUpdate.getOrDefault(pos,0);
        updateCountMap(toUpdate,pos,count);
        toUpdate.remove(oldPos);
    }

    private static int calculateTotalCountMap(Map<BlockPos,Integer> toSum){
        int taken = 0;
        for (Integer t : toSum.values()){
            taken += t;
        }
        return taken;
    }

    private static void reduceCountMapTo(Map<BlockPos,Integer> toReduce,int reduceTo){
        int currentSum = 0;
        for (Map.Entry<BlockPos,Integer> e : toReduce.entrySet()){
            int t = e.getValue();
            if (currentSum + t < reduceTo) {
                currentSum += t;
            }else {
                toReduce.replace(e.getKey(),reduceTo - currentSum);
                currentSum = reduceTo;
            }
        }
    }

    private static boolean removeCountMap(Map<BlockPos,Integer> toRemove, Level level,StoneHeatStorage stoneHeatStorage){
        List<BlockPos> toRemoveList = new ArrayList<>();
        for (BlockPos key : toRemove.keySet()){
            boolean shouldNotRemove = false;
            if (level.getBlockEntity(key) instanceof ConnectableBlockEntity<?> connectableBlockEntity && connectableBlockEntity.isController()){
                ThermalBlockEntityBehaviour tBEB = ThermalBlockEntityBehaviour.getFromCBE(connectableBlockEntity);
                if (tBEB != null && tBEB.isConnectTo(stoneHeatStorage)){
                    shouldNotRemove = true;
                }
            }
            if (!shouldNotRemove){
                toRemoveList.add(key);
            }
        }
        toRemoveList.forEach(toRemove::remove);
        return !toRemoveList.isEmpty();
    }



    private int calculateSuperHeatCountTaken(){
       return calculateTotalCountMap(superHeatTakenByTh);
    }

    private int calculateSuperHeatCountProvide(){
        return calculateTotalCountMap(superHeatProvideByTh);
    }

    private int calculateSuperHeatCountActuallyProvide(){
        return calculateTotalCountMap(superHeatActuallyProvideByTh);
    }

    public StoneHeatStorage getStoneHeatStorage() {
        return stoneHeatStorage;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public boolean canConnect(ConnectableBlockEntity toCheck) {
        return toCheck instanceof TightCompressStoneEntity;
    }

    @Override
    protected TightCompressStoneEntity castToSubclass() {
        return this;
    }
}
