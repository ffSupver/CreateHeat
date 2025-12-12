package com.ffsupver.createheat.block;

import com.ffsupver.createheat.block.thermalBlock.ThermalBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.ffsupver.createheat.util.BlockUtil.AllDirectionOf;
/** 同一位置多个控制器不同的热处理器自动合并成一个来处理
 * */

public abstract class MainHeatTransferProcesser extends HeatTransferProcesser{
    private boolean removed = false;
    protected boolean isMainProcesser = false;
    private MainHeatTransferProcesser mainProcesser = null;
    protected int acceptedHeat = 0;
    protected MainHeatTransferProcesser(ResourceLocation typeId) {
        super(typeId);
    }
    /** 判断是否可以传热
     *  Replace{@link com.ffsupver.createheat.block.HeatTransferProcesser#needHeat(Level, BlockPos, Direction, int, int, int)}
     * with this method instead of overriding it
     * */
    protected abstract boolean needHeatBefore(Level level,BlockPos pos,Direction face);
    /**主控处理器处理接受到的热
     * On main processor heated
     * */
    protected abstract void acceptHeatAsMain(Level level, BlockPos hTPPos, int heatProvide, int tickSkip);
    protected boolean isSameMHTP(MainHeatTransferProcesser testHTP){
        return getTypeId().equals(testHTP.getTypeId());
    }
    protected Optional<MainHeatTransferProcesser> findMainProcesser(Level level,BlockPos pos,Direction face){
        ConnectableBlockEntity<?> thBlockAttach = getAttachThermalBlock(level,pos,face);

        AtomicReference<Optional<MainHeatTransferProcesser>> oHRP = new AtomicReference<>(Optional.empty());
        AllDirectionOf(pos,
                (checkControllerPos,f)-> {
                    if (!f.equals(face.getOpposite()) && level.getBlockEntity(checkControllerPos) instanceof ConnectableBlockEntity<?> connectableBlockEntity) {
                        ThermalBlockEntityBehaviour otherTE = BlockEntityBehaviour.get(connectableBlockEntity,ThermalBlockEntityBehaviour.TYPE);
                        if (otherTE != null && !connectableBlockEntity.getControllerPos().equals(thBlockAttach.getControllerPos())) {
                            Optional<HeatTransferProcesser> otherHTP = otherTE.getHeatTransferProcesserByOther(pos);
                            if (otherHTP.isPresent() && otherHTP.get() instanceof MainHeatTransferProcesser hRTP
                                    && hRTP.isSameMHTP(this) && hRTP.isMainProcesser
                            ){
                                oHRP.set(Optional.of(hRTP));
                            }
                        }
                    }
                },
                //找到mainProcesser就终止
                c-> oHRP.get().isPresent() && oHRP.get().get() instanceof MainHeatTransferProcesser hRTP && hRTP.isMainProcesser
        );
        return oHRP.get();
    }

    @Override
    public boolean needHeat(Level level, BlockPos pos, @Nullable Direction face,int heat,int tickSkip,int superHeatCount) {
        boolean needHeat = needHeatBefore(level,pos,face);
        if (face != null && needHeat){
            Optional<MainHeatTransferProcesser> mainProcesserOptional = findMainProcesser(level,pos,face);
            if (mainProcesserOptional.isPresent()){
                this.mainProcesser = mainProcesserOptional.get();
            }else {
                this.isMainProcesser = true;
            }
        }
        return needHeat;
    }

    @Override
    public void onControllerRemove() {
        this.removed = true;
    }

    @Override
    public void acceptHeat(Level level, BlockPos hTPPos, int heatProvide, int tickSkip,int superHeatCount) {
            if (isMainProcesser){
                acceptHeatAsMain(level,hTPPos,heatProvide,tickSkip,acceptedHeat);
                acceptedHeat = 0;
            }else {
                if (mainProcesser.removed){ //主控被移除时
                    if (mainProcesser.mainProcesser == null) { //没有新主控时
                        mainProcesser.switchToNew(this);
                        acceptHeatAsMain(level, hTPPos, heatProvide, tickSkip,acceptedHeat);
                    }else {                 //已有新主控时
                        mainProcesser = mainProcesser.mainProcesser;
                        mainProcesser.mainGetHeat(heatProvide,tickSkip);
                    }
                }else {
                    mainProcesser.mainGetHeat(heatProvide, tickSkip);
                }
            }
    }

    protected void acceptHeatAsMain(Level level, BlockPos hTPPos, int heatProvide, int tickSkip, int originStore){
        int totalHeat = heatProvide + originStore;
        this.acceptHeatAsMain(level,hTPPos,totalHeat,tickSkip);
    }

    /** <p>不是主控的热处理器向主控的热处理器传热时调用主控的热处理器的这个方法</p>
     * This method of the main processor is called when a non-main processor transfers heat to the main processor.
     */
    protected void mainGetHeat(int heat,int tickSkip){
        this.acceptedHeat += heat;
    }

    protected void switchToNew(MainHeatTransferProcesser mHTP){
        mHTP.acceptedHeat = this.acceptedHeat;
        mHTP.mainProcesser = null;
        mHTP.isMainProcesser = true;
        this.mainProcesser = mHTP;
    }


    private static ConnectableBlockEntity<?> getAttachThermalBlock(Level level, BlockPos transferProcesserPos, Direction face){
        BlockPos thBlockPos = transferProcesserPos.relative(face.getOpposite());
        if (level.getBlockEntity(thBlockPos) instanceof ConnectableBlockEntity<?> connectableBlockEntity &&
                BlockEntityBehaviour.get(connectableBlockEntity, ThermalBlockEntityBehaviour.TYPE) != null){
            return connectableBlockEntity;
        }
        return null;
    }
}
