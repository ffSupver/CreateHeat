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
    private BlockPos lastRememberedPos;
    protected boolean markUpdateForPos;
    protected final Set<BlockPos> connectedBlocks = new HashSet<>();

    public ConnectableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controllerPos = getBlockPos();
        lastRememberedPos = getBlockPos();
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
        tag.put("last_remembered_pos",NbtUtil.blockPosToNbt(lastRememberedPos));
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
        lastRememberedPos = NbtUtil.blockPosFromNbt(tag.getCompound("last_remembered_pos"));
        if (isController){
            connectedBlocks.clear();
            connectedBlocks.addAll(NbtUtil.readBlockPosFromNbtList(tag.getList("connected", Tag.TAG_COMPOUND)));
        }else {
            controllerPos = NBTHelper.readBlockPos(tag,"controller");
        }
        super.read(tag, registries, clientPacket);
    }

    @Override
    public void tick() {
        super.tick();

        // on pos change
        if (markUpdateForPos){
            lastRememberedPos = getBlockPos();
            if (isController){
                walkAllBlocks(null);
                markUpdateForPos = false;
            }else {
                T oldControllerEntity = getControllerEntity();
                if (oldControllerEntity != null) {
                    if (!oldControllerEntity.markUpdateForPos) {
                        oldControllerEntity.walkAllBlocks(null); // remove markUpdateForPos if oldControllerEntity touch this block

                        if (markUpdateForPos) { // if not connect to oldControllerEntity
                            this.isController = true;
                            this.walkAllBlocks(null);
                            markUpdateForPos = false;
                        }
                    }
                }else{ // missing original controller
                    AtomicBoolean foundConnect = new AtomicBoolean(false);
                    // try attach to neighbor controller
                    BlockUtil.AllDirectionOf(getBlockPos(),pos->{
                        if (getLevel().getBlockEntity(pos) instanceof ConnectableBlockEntity<?> neighbourEntity && canConnect(neighbourEntity) && !neighbourEntity.markUpdateForPos) {
                            this.controllerPos = neighbourEntity.getControllerPos();
                            foundConnect.set(true);
                        }
                    });
                    // neighbor controller not found
                    if (!foundConnect.get()){
                        // This block becomes a new controller and establishes its own network
                        this.isController = true;
                        this.controllerPos = getBlockPos();
                        this.walkAllBlocks(null); // original controller will be merged if transferred to here and touched by this step
                        markUpdateForPos = false;
                    }
                }
            }
        }

        // check pos change
        if (!getBlockPos().equals(lastRememberedPos)) {
            markUpdateForPos = true;
        }
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
                connectableBlockEntity.setControllerPos(getBlockPos());
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


    // set by walkAllBlocks, remove markUpdateForPos flag when set
    public void setControllerPos(BlockPos controllerPos) {
        if (isController){
            if(!controllerPos.equals(getBlockPos()) && getLevel().getBlockEntity(controllerPos) instanceof ConnectableBlockEntity<?> connectableBlockEntity && canConnect(connectableBlockEntity)) {
                this.isController = false;
                mergeController(getBlockPos(), this, controllerPos, connectableBlockEntity);
                this.controllerPos = controllerPos;
            }
        }else {
            this.controllerPos = controllerPos;
            markUpdateForPos = false;
        }
    }

    public void addConnectedPos(BlockPos pos){
        this.connectedBlocks.add(pos);
    }

    public boolean isController(){return isController;}

    public BlockPos getControllerPos(){return isController ? getBlockPos() : controllerPos;}
    public Set<BlockPos> getConnectedBlocks(){return isController ? connectedBlocks : getControllerEntity().getConnectedBlocks();}
}
