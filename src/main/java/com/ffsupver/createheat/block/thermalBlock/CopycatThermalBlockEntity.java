package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.Config;
import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

import java.util.Optional;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED;
import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING;
import static net.minecraft.world.level.block.Blocks.*;

public class CopycatThermalBlockEntity extends BaseThermalBlockEntity {
    private BlockState cashedState;
    private BlockState cashedMaterial;
    private BlockState material;
    private ItemStack itemStack;
    private final ItemStackHandler inputInventory;
    private final ItemStackHandler outputInventory;
    private final CopycatThermalBlockInv inv;
    private int processTime;
    public CopycatThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        material = CHBlocks.COPYCAT_THERMAL_BLOCK.getDefaultState();
        itemStack = ItemStack.EMPTY;
        inputInventory = new ItemStackHandler(1);
        outputInventory = new ItemStackHandler(1);
        inv = new CopycatThermalBlockInv();
    }

    public static void registerCapacity(RegisterCapabilitiesEvent capabilitiesEvent){
        capabilitiesEvent.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CHBlocks.COPYCAT_THERMAL_BLOCK_ENTITY.get(),
                (be,context)->be.inv
                );
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

        inputInventory.deserializeNBT(registries,tag.getCompound("input"));
        outputInventory.deserializeNBT(registries,tag.getCompound("output"));

        processTime = tag.getInt("process");

        if (clientPacket && preMaterial != material){
            redraw();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Material", NbtUtils.writeBlockState(material));
        tag.put("item",itemStack.saveOptional(registries));

        tag.put("input",inputInventory.serializeNBT(registries));
        tag.put("output",outputInventory.serializeNBT(registries));

        tag.putInt("process",processTime);
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

        // 熔炼物品
        if (needProcess() && getRecipe().isPresent()){
            AbstractCookingRecipe recipe = getRecipe().get().value();
            int cookingTime = recipe.getCookingTime();
            if (processTime >= cookingTime){
                processTime = 0;
                ItemStack resultItem = recipe.assemble(new SingleRecipeInput(inputInventory.getStackInSlot(0)),getLevel().registryAccess());
                ItemStack extractedCheck = inputInventory.extractItem(0,1,true);
                ItemStack left = outputInventory.insertItem(0,resultItem,true);
                if (left.isEmpty() && !extractedCheck.isEmpty()){
                    inputInventory.extractItem(0,1,false);
                    outputInventory.insertItem(0,resultItem,false);
                }
            }else {
                BlazeBurnerBlock.HeatLevel heatLevel = getThBehaviour().getHeatLevel();
                if (heatLevel.equals(KINDLED)){
                    processTime++;
                }else if (heatLevel.equals(SEETHING)){
                    processTime += Config.HEAT_PER_SEETHING_BLAZE.get() / Config.HEAT_PER_FADING_BLAZE.get();
                }
            }
        }else {
            processTime = 0;
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
    }

    public ItemStack removeMaterial(){
        if (hasMaterial()){
            this.material = CHBlocks.COPYCAT_THERMAL_BLOCK.getDefaultState();
            ItemStack result = itemStack;
            this.itemStack = ItemStack.EMPTY;
            return result;
        }
        dropInv();
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
        dropInv();
        super.destroy();
    }

    //伪装熔炉处理物品
    public boolean isFurnace(){
        return this.material.is(FURNACE) || this.material.is(BLAST_FURNACE) || this.material.is(SMOKER);
    }
    private void dropInv(){
        Containers.dropItemStack(getLevel(),getBlockPos().getX(),getBlockPos().getY(),getBlockPos().getZ(),inputInventory.extractItem(0,Integer.MAX_VALUE,false));
        Containers.dropItemStack(getLevel(),getBlockPos().getX(),getBlockPos().getY(),getBlockPos().getZ(),outputInventory.extractItem(0,Integer.MAX_VALUE,false));
    }

    private RecipeType<? extends AbstractCookingRecipe> getRecipeType(){
        if (isFurnace()){
            if (material.is(FURNACE)){
                return RecipeType.SMELTING;
            }else if (material.is(BLAST_FURNACE)){
                return RecipeType.BLASTING;
            }else if (material.is(SMOKER)){
                return RecipeType.SMOKING;
            }else {
                return null;
            }
        }
        return null;
    }
    private Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> getRecipe(ItemStack stack){
       return getLevel().getRecipeManager().getRecipeFor(getRecipeType(),new SingleRecipeInput(stack),getLevel());
    }

    private Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> getRecipe(){
        return getRecipe(inputInventory.getStackInSlot(0));
    }

    private boolean needProcess(){
        return isFurnace() && !inputInventory.getStackInSlot(0).isEmpty();
    }

    @Override
    protected void setUpThermalBlockEntityBehaviour(ThermalBlockEntityBehaviour thermalBlockEntityBehaviour) {
        thermalBlockEntityBehaviour.setShouldHeatUp(b->needProcess());
    }

    public class CopycatThermalBlockInv extends CombinedInvWrapper {
        public CopycatThermalBlockInv(){
            super(inputInventory,outputInventory);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isFurnace()){
                return stack;
            }
            if (outputInventory == getHandlerFromIndex(getIndexForSlot(slot))) { //不能从输出槽插入物品
                return stack;
            }
            if (getRecipe(stack).isEmpty()){ // 不可熔炼物品不能进入
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (inputInventory == getHandlerFromIndex(getIndexForSlot(slot))) { //不能从输入槽提取物品
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    }
}
