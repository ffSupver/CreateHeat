package com.ffsupver.createheat.block.tightCompressStone;

import com.ffsupver.createheat.block.thermalBlock.HeatStorage;
import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour.MAX_HEAT;
import static com.ffsupver.createheat.block.tightCompressStone.TightCompressStone.HEAT;
import static com.ffsupver.createheat.block.tightCompressStone.TightCompressStone.Heat.*;

public class StoneHeatStorage extends HeatStorage {
    private static final Supplier<Integer> HEAT_PER_LAVA = ()->MAX_HEAT.get() * 50;
    public final HashSet<BlockPos> stonePosSet;
    private BlockCounts lastBlockCounts = new BlockCounts();
    private int superHeatCount;


    public StoneHeatStorage(HashSet<BlockPos> stonePosSet) {
        super(calculateCapacity(stonePosSet));
        this.stonePosSet = stonePosSet;
    }



    public void checkSize(Level level,Set<BlockPos> newBlockPosSet, boolean shouldSetAmountWithoutCheck){
        Set<BlockPos> oldSet = Set.copyOf(stonePosSet);
        stonePosSet.clear();
        AtomicInteger regularHeatCount = new AtomicInteger();
        AtomicInteger stoneCount = new AtomicInteger();
        AtomicInteger superHeatCount = new AtomicInteger();


        newBlockPosSet.forEach(b->{
            BlockState bsT = level.getBlockState(b);
                if (isAvailableRegularHeatBlock(bsT)){
                regularHeatCount.getAndIncrement();
            }else if (isAvailableSuperHeatBlock(bsT)){
                superHeatCount.getAndIncrement();
            }else if (isAvailableTStoneBlock(bsT)) {
                stoneCount.getAndIncrement();
            }
            stonePosSet.add(b);
        });

        if (oldSet.size() != stonePosSet.size()){
            setCapacity(calculateCapacity(stonePosSet));
        }

        int lC = regularHeatCount.get();
        int sHC = superHeatCount.get();
        int sC = stoneCount.get();
        int amountFromBlock = sC + lC + sHC > 0 ? (lC + sHC) * getCapacity() / (sC + lC + sHC) : 0;
        if (shouldSetAmountWithoutCheck){
            setAmount(amountFromBlock);
        }else if (!lastBlockCounts.unInit() && (lastBlockCounts.regular + lastBlockCounts.superHeat) < (lC + sHC)){
            setAmount(Math.max(getAmount(),amountFromBlock));
        }

        lastBlockCounts = new BlockCounts(sC,lC,sHC);
    }

    public boolean checkSize(Level level,Set<BlockPos> newBlockPosSet) {
        if (!stonePosSet.isEmpty()){
            int oldSize = stonePosSet.size();
            checkSize(level, newBlockPosSet,false);
            return stonePosSet.size() != oldSize;
        }
        return false;
    }

    public boolean updateBlockState(Level level){
        if (getCapacity() == 0){
            return false;
        }
        int regularHeatCount = 0;
        int superHeatCount = 0;
        int stoneCount = 0;
        Set<BlockPos> stoneBlockPosSet = new HashSet<>();
        Set<BlockPos> regularHeatBlockPosSet = new HashSet<>();
        Set<BlockPos> superHeatBlockPosSet = new HashSet<>();
        for (BlockPos sPos : stonePosSet){
            if (isAvailableRegularHeatBlock(level.getBlockState(sPos))){
                regularHeatCount++;
                regularHeatBlockPosSet.add(sPos);
            }else if (isAvailableSuperHeatBlock(level.getBlockState(sPos))){
                superHeatCount++;
                superHeatBlockPosSet.add(sPos);
            }else if (level.getBlockState(sPos).is(CHBlocks.TIGHT_COMPRESSED_STONE)){
                stoneCount++;
                stoneBlockPosSet.add(sPos);
            }
        }

        if (regularHeatCount + stoneCount == 0){
            return false;
        }

        int bCount = stoneCount + regularHeatCount + superHeatCount;
        int heatCount = regularHeatCount + superHeatCount;
        int aUp = getCapacity() * (heatCount + 1) / bCount;
        int aDw = getCapacity() * heatCount / bCount;
        boolean addTStone = aDw > getAmount();
        boolean removeTStone = aUp <= getAmount();
        if (addTStone){
            int countToAdd = (aDw - getAmount()) * bCount / getCapacity() + 1;
            int leftToAdd = countToAdd;
            //先替换regularHeat
            List<BlockPos> regularHeatPosList = new ArrayList<>(regularHeatBlockPosSet);
            for (int i = 0;i < Math.min(countToAdd,regularHeatPosList.size());i++){
                BlockPos regularHeatPos = regularHeatPosList.get(i);
                setNoneHeatBlock(level,regularHeatPos);
                regularHeatBlockPosSet.remove(regularHeatPos);
                leftToAdd--;
            }

            //再替换superHeat
            if (leftToAdd > 0){
                List<BlockPos> superHeatPosList = new ArrayList<>(superHeatBlockPosSet);
                for (int i = 0;i < Math.min(leftToAdd,superHeatPosList.size());i++){
                    BlockPos superHeatPos = superHeatPosList.get(i);
                    setNoneHeatBlock(level,superHeatPos);
                    superHeatBlockPosSet.remove(superHeatPos);
                }
            }
        }else if (removeTStone){
            int countToRemove = (getAmount() - aUp) * bCount / getCapacity() + 1;
            List<BlockPos> stonePosList = new  ArrayList<>(stoneBlockPosSet);
            for (int i = 0;i < Math.min(countToRemove,stonePosList.size());i++){
                BlockPos newRegularPos = stonePosList.get(i);
                setRegularHeatBlock(level,newRegularPos);
                regularHeatBlockPosSet.add(newRegularPos);
            }
        }

        //更新superHeat数量
        superHeatCount = superHeatBlockPosSet.size();
        boolean canAddSuperHeat = canSuperHeat();
        boolean shouldAddSuperHeat = getSuperHeatCount() > superHeatCount;
        boolean shouldRemoveSuperHeat = getSuperHeatCount() < superHeatCount;
        boolean changedSuperHeat = false;
        if (canAddSuperHeat && shouldAddSuperHeat){
            int leftToAdd = getSuperHeatCount() - superHeatCount;
            List<BlockPos> regularHeatPosList = new ArrayList<>(regularHeatBlockPosSet);
            for (int i = 0;i < Math.min(leftToAdd,regularHeatPosList.size());i++){
                BlockPos superHeatToAdd = regularHeatPosList.get(i);
                setSuperHeatBlock(level,superHeatToAdd);
                superHeatBlockPosSet.add(superHeatToAdd);
                changedSuperHeat = true;
            }
        }else if (!canAddSuperHeat || shouldRemoveSuperHeat){
            int leftToRemove = canAddSuperHeat ? superHeatCount - getSuperHeatCount() : superHeatCount;
            List<BlockPos> superHeatPosList = new ArrayList<>(superHeatBlockPosSet);
            for (int i = 0;i < Math.min(leftToRemove,superHeatPosList.size());i++){
                BlockPos superHeatToRemove = superHeatPosList.get(i);
                setRegularHeatBlock(level,superHeatToRemove);
                superHeatBlockPosSet.remove(superHeatToRemove);
                changedSuperHeat = true;
            }
        }

        setSuperHeatCount(superHeatBlockPosSet.size());

        return addTStone || removeTStone || changedSuperHeat;
    }

    private boolean canSuperHeat(){
        return getAmount() * 2 > getCapacity();
    }

    /**
     * Only call after calling {@link #checkSize(Level, Set, boolean)} or
     * when {@link BlockCounts#unInit()} returns true
     */
    public int getSuperHeatCount(){
        return superHeatCount;
    }

    /**
     * Only call after calling {@link #checkSize(Level, Set, boolean)} or
     * when {@link BlockCounts#unInit()} returns true
     */
    public int getMaxSuperHeatCount(){
        if (!canSuperHeat()){
            return 0;
        }
        return lastBlockCounts.unInit() ? getAmount() * stonePosSet.size() / getCapacity() : (lastBlockCounts.regular + lastBlockCounts.superHeat) / 2;
    }


    public void setSuperHeatCount(int superHeatCount){
        this.superHeatCount = Math.min(getMaxSuperHeatCount(),superHeatCount);
    }

    public static boolean isAvailableBlock(BlockState bsT){
        return isAvailableRegularHeatBlock(bsT) || isAvailableTStoneBlock(bsT) || isAvailableSuperHeatBlock(bsT);
    }

    public static boolean isAvailableRegularHeatBlock(BlockState bsT){
        return bsT.is(CHBlocks.TIGHT_COMPRESSED_STONE) && bsT.getValue(HEAT).equals(REGULAR_HEAT);
    }

    private static void setNoneHeatBlock(Level level,BlockPos pos){
        level.setBlock(pos, CHBlocks.TIGHT_COMPRESSED_STONE.getDefaultState().setValue(HEAT,NONE),3);
    }

    public static boolean isAvailableSuperHeatBlock(BlockState bsT){
        return bsT.is(CHBlocks.TIGHT_COMPRESSED_STONE) && bsT.getValue(HEAT).equals(SUPER_HEAT);
    }

    private static void setRegularHeatBlock(Level level,BlockPos pos){
        level.setBlock(pos, CHBlocks.TIGHT_COMPRESSED_STONE.getDefaultState().setValue(HEAT,REGULAR_HEAT),3);
    }

    private static void setSuperHeatBlock(Level level,BlockPos pos){
        level.setBlock(pos, CHBlocks.TIGHT_COMPRESSED_STONE.getDefaultState().setValue(HEAT,SUPER_HEAT),3);
    }

    public static boolean isAvailableTStoneBlock(BlockState bsT){
        return bsT.is(CHBlocks.TIGHT_COMPRESSED_STONE.get());
    }

    private static int calculateCapacity(Set<BlockPos> stonePosSet){
        return stonePosSet.size() * HEAT_PER_LAVA.get();
    }


    public void fromNbt(CompoundTag nbt,Set<BlockPos> stonePS) {
        this.fromNbt(nbt);
        stonePosSet.clear();
        stonePosSet.addAll(stonePS);
        superHeatCount = nbt.getInt("super_heat_count");
    }



    @Override
    public CompoundTag toNbt() {
        CompoundTag nbt = super.toNbt();
        nbt.putInt("super_heat_count", superHeatCount);
        return nbt;
    }

    public boolean isConnect(Set<BlockPos> posSet){
        return BlockUtil.isConnect(stonePosSet,posSet);
    }

    private static class BlockCounts{
        private Integer stone;
        private Integer regular;
        private Integer superHeat;
        public BlockCounts(int stone,int regular,int superHeat){
            this.stone = stone;
            this.regular = regular;
            this.superHeat = superHeat;
        }
        public BlockCounts(){
        }

        public boolean unInit(){
            return stone == null || regular == null || superHeat == null;
        }

        @Override
        public String toString() {
            return "BlockCounts{" +
                    "stone=" + stone +
                    ", regular=" + regular +
                    ", superHeat=" + superHeat +
                    '}';
        }
    }
}
