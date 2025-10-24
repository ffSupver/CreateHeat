package com.ffsupver.createheat.block;

import com.ffsupver.createheat.util.BlockUtil;
import com.ffsupver.createheat.util.NbtUtil;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ffsupver.createheat.util.BlockUtil.AllDirectionOf;

public abstract class ConnectableBlockEntity<T extends ConnectableBlockEntity<T>> extends SmartBlockEntity {
    protected boolean isController;
    private BlockPos controllerPos;
    protected final Set<BlockPos> connectedBlocks = new HashSet<>();

    public ConnectableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controllerPos = getBlockPos();
    }

    public abstract boolean canConnect(ConnectableBlockEntity<?> toCheck);
    protected abstract T castToSubclass();

    protected void connectedNewBlock(BlockPos newPos,ConnectableBlockEntity<?> oldBlockEntity){
    }

    protected void mergeController(BlockPos oldControllerPos, ConnectableBlockEntity<?> oldControllerEntity, BlockPos newControllerPos, ConnectableBlockEntity<?> newControllerEntity){}
    /**
    Only called by a controller. Times of Calls equal to new controller count to create.
     */
    protected void switchToNewControllerWhenDestroy(BlockPos newPos,ConnectableBlockEntity<?> newControllerEntity){}

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {

        tag.putBoolean("is_controller",isController);
        if (isController){
            tag.put("connected", NbtUtil.writeBlockPosToNbtList(connectedBlocks));
        }else {
            tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
        }
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        isController = tag.getBoolean("is_controller");
        if (isController){
            connectedBlocks.clear();
            connectedBlocks.addAll(NbtUtil.readBlockPosFromNbtList(tag.getList("connected", Tag.TAG_COMPOUND)));
        }else {
            controllerPos = NBTHelper.readBlockPos(tag,"controller");
        }
        super.read(tag, registries, clientPacket);
    }

    public void checkNeighbour(){
        if (!isController){
            AtomicBoolean foundConnect = new AtomicBoolean(false);
            AllDirectionOf(getBlockPos(),checkPos->{
                if (getLevel().getBlockEntity(checkPos) instanceof ConnectableBlockEntity<?> neighbourEntity && canConnect(neighbourEntity)) {
                    this.isController = false;
                    if (!foundConnect.get()){
                        this.controllerPos = neighbourEntity.getControllerPos();
                        foundConnect.set(true);
                    }


                    ConnectableBlockEntity<T> controllerEntity = getControllerEntity();
                    if (controllerEntity != null){
                        controllerEntity.addConnectedPos(getBlockPos());
                        if (!neighbourEntity.getControllerPos().equals(controllerPos)){
                            if (neighbourEntity.getControllerEntity() != null){
                                mergeController(neighbourEntity.getControllerPos(),neighbourEntity.getControllerEntity(),controllerPos,controllerEntity);
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

    @Override
    public void destroy() {
        if (isController()){
            for (BlockPos pos : connectedBlocks){
                if (!pos.equals(getBlockPos()) && getLevel().getBlockEntity(pos) instanceof ConnectableBlockEntity<?> connectableBlockEntity && connectableBlockEntity.getControllerPos().equals(getBlockPos())){
                    connectableBlockEntity.isController = true;
                    connectableBlockEntity.walkAllBlocks(getBlockPos());
                    switchToNewControllerWhenDestroy(pos,connectableBlockEntity);
                }
            }
        }else {
            if (getLevel().getBlockEntity(controllerPos) instanceof ConnectableBlockEntity connectableBlockEntity){
                connectableBlockEntity.walkAllBlocks(getBlockPos());
            }
        }
        super.destroy();
    }

    public void walkAllBlocks(BlockPos exceptFor){
        Set<BlockPos> oldBlocks = Set.copyOf(connectedBlocks);
        connectedBlocks.clear();
        BlockUtil.walkAllBlocks(getBlockPos(),connectedBlocks, pos -> {
            if (!pos.equals(exceptFor) && getLevel().getBlockEntity(pos) instanceof ConnectableBlockEntity<?> connectableBlockEntity && canConnect(connectableBlockEntity)) {
                connectedNewBlock(pos, connectableBlockEntity);
                connectableBlockEntity.controllerPos = getBlockPos();
                return true;
            }else {
                return false;
            }
        });

        for (BlockPos pos : oldBlocks){
            if (!connectedBlocks.contains(pos) && getLevel().getBlockEntity(pos) instanceof ConnectableBlockEntity connectableBlockEntity && canConnect(connectableBlockEntity)) {
                if(!connectableBlockEntity.isController() && connectableBlockEntity.getControllerPos().equals(getBlockPos())){
                    connectableBlockEntity.isController = true;
                    connectableBlockEntity.walkAllBlocks(exceptFor);
                    switchToNewControllerWhenDestroy(pos,connectableBlockEntity);
                }
            }
        }

        notifyUpdate();
    }

    @SuppressWarnings("unchecked")
    public T getControllerEntity(){
        if (isController){
            return castToSubclass();
        }else if (controllerPos != null && getLevel().getBlockEntity(controllerPos) instanceof ConnectableBlockEntity controllerEntity && canConnect(controllerEntity)){
            return controllerEntity.isController() ? (T) controllerEntity : null;
        }else {
            return null;
        }
    }

    public void addConnectedPos(BlockPos pos){
        this.connectedBlocks.add(pos);
    }

    public boolean isController(){return isController;}

    public BlockPos getControllerPos(){return isController ? getBlockPos() : controllerPos;}
    public Set<BlockPos> getConnectedBlocks(){return isController ? connectedBlocks : getControllerEntity().getConnectedBlocks();}
}
