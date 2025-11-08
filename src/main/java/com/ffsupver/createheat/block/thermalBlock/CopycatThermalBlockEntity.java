package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

public class CopycatThermalBlockEntity extends BaseThermalBlockEntity{
    private BlockState material;
    private ItemStack itemStack;
    public CopycatThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        material = CHBlocks.COPYCAT_THERMAL_BLOCK.getDefaultState();
        itemStack = ItemStack.EMPTY;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(CopycatModel.MATERIAL_PROPERTY,getMaterial())
                .build();
    }

    public boolean hasMaterial(){
        return !this.material.is(CHBlocks.COPYCAT_THERMAL_BLOCK);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        material = NbtUtils.readBlockState(blockHolderGetter(), tag.getCompound("Material"));
        itemStack = ItemStack.parseOptional(registries,tag.getCompound("item"));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Material", NbtUtils.writeBlockState(material));
        tag.put("item",itemStack.saveOptional(registries));
    }

    public boolean setMaterial(BlockState material,ItemStack stack) {
        if (!hasMaterial() && !material.is(CHBlocks.COPYCAT_THERMAL_BLOCK.get())){
            this.material = material;
            this.itemStack = stack.copyWithCount(1);
            notifyUpdate();
            return true;
        }else {
            return false;
        }
    }

    public ItemStack removeMaterial(){
        if (hasMaterial()){
            this.material = CHBlocks.COPYCAT_THERMAL_BLOCK.getDefaultState();
            ItemStack result = itemStack;
            this.itemStack = ItemStack.EMPTY;
            notifyUpdate();
            return result;
        }
        return ItemStack.EMPTY;
    }

    public BlockState getMaterial() {
        return material;
    }

    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        redraw();
    }

    private void redraw() {
        if (!isVirtual()) {
            requestModelDataUpdate();
        }
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
//            updateLight(); 没有透光的方块
        }
    }

    @Override
    public void destroy() {
        Vec3 itemDropPos = getBlockPos().getCenter();
        if (!itemStack.isEmpty() && getLevel() != null){
            Containers.dropItemStack(getLevel(), itemDropPos.x(), itemDropPos.y(), itemDropPos.z(), itemStack);
        }
        super.destroy();
    }
}
