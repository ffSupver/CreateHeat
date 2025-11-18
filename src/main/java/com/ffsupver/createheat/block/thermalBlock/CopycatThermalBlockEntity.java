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
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

public class CopycatThermalBlockEntity extends BaseThermalBlockEntity{
    private BlockState cashedState;
    private BlockState cashedMaterial;
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
        BlockState preMaterial = material;
        material = NbtUtils.readBlockState(blockHolderGetter(), tag.getCompound("Material"));
        itemStack = ItemStack.parseOptional(registries,tag.getCompound("item"));

        if (clientPacket && preMaterial != material){
            redraw();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Material", NbtUtils.writeBlockState(material));
        tag.put("item",itemStack.saveOptional(registries));
    }

    @Override
    public void tick() {
        super.tick();

        if (!getBlockState().equals(cashedState)){
            cashedState = getBlockState();
            updateLight();
        }

        if (!material.equals(cashedMaterial)){
            cashedMaterial = material;
            redraw();
        }
    }

    public boolean setMaterial(BlockState material, ItemStack stack) {
        if (!hasMaterial() && !material.is(CHBlocks.COPYCAT_THERMAL_BLOCK.get())){
            forceSetMaterial(material,stack);
            return true;
        }else {
            return false;
        }
    }

    public void forceSetMaterial(BlockState material) {
        this.material = material;
    }

    public void forceSetMaterial(BlockState material, ItemStack stack){
        this.material = material;
        this.itemStack = stack.copyWithCount(1);
//        notifyUpdate();
    }

    public ItemStack removeMaterial(){
        if (hasMaterial()){
            this.material = CHBlocks.COPYCAT_THERMAL_BLOCK.getDefaultState();
            ItemStack result = itemStack;
            this.itemStack = ItemStack.EMPTY;
//            notifyUpdate();
            return result;
        }
        return ItemStack.EMPTY;
    }

    public BlockState getMaterial() {
        return material;
    }

    @Override
    public void notifyUpdate() {
        redraw();
        super.notifyUpdate();
    }

    private void redraw() {
        if (!isVirtual()) {
            requestModelDataUpdate();
        }
        if (level != null) {
            updateLight(); //更新发光
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
    }

    private void updateLight() {
        if (level != null) {
            AuxiliaryLightManager lightManager = level.getAuxLightManager(getBlockPos());
            if (lightManager != null) {
                int blockLight = material.is(CHBlocks.COPYCAT_THERMAL_BLOCK) ? 1 : material.getLightEmission(level, getBlockPos());
                int workLight = switch (getBlockState().getValue(HEAT_LEVEL)) {
                    case NONE -> 0;
                    case SMOULDERING, FADING, KINDLED -> 4;
                    case SEETHING -> 8;
                };
                int r = Math.min(15, blockLight + workLight);
                lightManager.setLightAt(getBlockPos(),r);
            }
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
