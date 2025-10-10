package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHBlocks;
import com.ffsupver.createheat.util.NbtUtil;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntity.MAX_HEAT;
import static com.ffsupver.createheat.util.BlockUtil.walkAllBlocks;

public class StoneHeatStorage extends HeatStorage{
    private static final Supplier<Integer> HEAT_PER_LAVA = ()->MAX_HEAT.get() * 10;
    public final HashSet<BlockPos> stonePosSet;
    private final BlockPos recordControllerPos;
    public boolean shouldChangeController = false;
    private BlockPos newController = null;
    private BlockCounts lastBlockCounts = new BlockCounts();


    public StoneHeatStorage(HashSet<BlockPos> stonePosSet,BlockPos recordController) {
        super(calculateCapacity(stonePosSet));
        this.stonePosSet = stonePosSet;
        this.recordControllerPos = recordController;
    }

    public StoneHeatStorage(StoneHeatStorage oldSHS,BlockPos newController){
        this(oldSHS.stonePosSet,newController);
        this.setCapacity(oldSHS.getCapacity());
        this.setAmount(oldSHS.getAmount());
    }

    public void disconnect(BlockPos controllerPos){
        if (controllerPos.equals(recordControllerPos)){
            this.shouldChangeController = true;
        }
    }

    private void tryChangeToNewController(BlockPos pos, Level level){
        if (shouldChangeController && newController == null && level.getBlockEntity(pos) instanceof ThermalBlockEntity thermalBlockEntity){
            if (thermalBlockEntity.getControllerEntity().acceptNewStoneHeatStorage(this)){
                newController = thermalBlockEntity.getControllerPos();
            }
        }
    }

    public Set<BlockPos> checkSize(Level level, BlockPos startPos, boolean shouldSetAmountWithoutCheck){
        Set<BlockPos> oldSet = Set.copyOf(stonePosSet);
        stonePosSet.clear();
        AtomicInteger lavaCount = new AtomicInteger();
        AtomicInteger stoneCount = new AtomicInteger();

        Set<BlockPos> controllerPos = new HashSet<>();

        walkAllBlocks(startPos,stonePosSet,b->{
            BlockState bsT = level.getBlockState(b);
            if (isAvailableLavaBlock(bsT)){
                lavaCount.getAndIncrement();
            }else if (isAvailableTStoneBlock(bsT)) {
                stoneCount.getAndIncrement();
            }
            if (level.getBlockEntity(b) instanceof ThermalBlockEntity connectTBE){
                controllerPos.add(connectTBE.getControllerPos());
            }
            return isAvailableBlock(bsT);
        });

        if (oldSet.size() != stonePosSet.size()){
            setCapacity(calculateCapacity(stonePosSet));
        }

        int lC = lavaCount.get();
        int sC = stoneCount.get();
        int amountFromBlock = sC + lC > 0 ? lC * getCapacity() / (sC + lC) : 0;
        if (shouldSetAmountWithoutCheck){
            setAmount(amountFromBlock);
        }else if (!lastBlockCounts.unInit() && lastBlockCounts.lava < lC){
            setAmount(Math.max(getAmount(),amountFromBlock));
        }

        lastBlockCounts = new BlockCounts(sC,lC);
        return controllerPos;
    }

    public boolean checkSize(Level level,BlockPos controllerPos) {
        if (!controllerPos.equals(recordControllerPos)){
            tryChangeToNewController(controllerPos,level);
            return true; //无论是否变化都向客户端发送数据
        }

        if (!stonePosSet.isEmpty()){
            int oldSize = stonePosSet.size();
            checkSize(level, stonePosSet.iterator().next(),false);
            return stonePosSet.size() != oldSize;
        }
        return false;
    }

    public boolean updateBlockState(Level level,BlockPos controllerPos){
        if (!controllerPos.equals(recordControllerPos)){
            tryChangeToNewController(controllerPos,level);
            return false;
        }
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
            List<BlockPos> lavaPosList = new ArrayList<>(lavaBlockPosSet);
            for (int i = 0;i < Math.min(countToAdd,lavaPosList.size());i++){
                level.setBlock(lavaPosList.get(i),CHBlocks.TIGHT_COMPRESSED_STONE.getDefaultState(),3);
            }
        }else if (removeTStone){
            int countToRemove = (getAmount() - aUp) * bCount / getCapacity() + 1;
            List<BlockPos> stonePosList = new  ArrayList<>(stoneBlockPosSet);
            for (int i = 0;i < Math.min(countToRemove,stonePosList.size());i++){
                level.setBlock(stonePosList.get(i), Blocks.LAVA.defaultBlockState(),3);
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
        stonePosSet.addAll(NbtUtil.readBlockPosFromNbtList(nbt.getList("stone_pos", Tag.TAG_COMPOUND)));
        shouldChangeController = nbt.getBoolean("should_change_c");
    }

    public static StoneHeatStorage cFromNbt(CompoundTag tag){
        StoneHeatStorage r = new StoneHeatStorage(new HashSet<>(), NBTHelper.readBlockPos(tag,"record_controller"));
        r.fromNbt(tag);
        return r;
    }

    public boolean shouldWriteToNbt(BlockPos pos){
        return pos.equals(recordControllerPos);
    }

    @Override
    public CompoundTag toNbt() {
        CompoundTag nbt = super.toNbt();
        nbt.put("stone_pos",NbtUtil.writeBlockPosToNbtList(stonePosSet));
        nbt.put("record_controller", NbtUtils.writeBlockPos(recordControllerPos));
        nbt.putBoolean("should_change_c",shouldChangeController);
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

    private static class BlockCounts{
        private Integer stone;
        private Integer lava;
        public BlockCounts(int stone,int lava){
            this.stone = stone;
            this.lava = lava;
        }
        public BlockCounts(){
        }

        public boolean unInit(){
            return stone == null || lava == null;
        }
    }
}
