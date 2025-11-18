package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.registries.CHBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static net.minecraft.world.level.block.CommandBlock.FACING;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

public class CopycatThermalBlock extends BaseThermalBlock<CopycatThermalBlockEntity>{

    public CopycatThermalBlock(Properties properties) {
        super(properties, CopycatThermalBlockEntity.class, ()->CHBlocks.COPYCAT_THERMAL_BLOCK_ENTITY.get());

    }

    @OnlyIn(Dist.CLIENT)
    public boolean canConnectTextures(BlockState state) {
        return true;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        AtomicReference<ItemStack> stack = new AtomicReference<>(ItemStack.EMPTY);
        withBlockEntityDo(context.getLevel(),context.getClickedPos(),copycatThermalBlockEntity -> {
            stack.set(copycatThermalBlockEntity.removeMaterial());
        });
        if (stack.get().isEmpty()){
            return super.onWrenched(state, context);
        }else {
            ItemStack returnItem = stack.get();
            Player player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                player.getInventory().placeItemBackInInventory(returnItem);
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty() && player.isShiftKeyDown()){
           BlockState material = getMaterial(level,pos);
           if (material.hasProperty(LIT)){
               withBlockEntityDo(level,pos,copycatThermalBlockEntity ->
                       copycatThermalBlockEntity.forceSetMaterial(material.setValue(LIT,!material.getValue(LIT)))
               );
               return ItemInteractionResult.SUCCESS;
           }
        }

        if (!(stack.getItem() instanceof BlockItem bi)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Block block = bi.getBlock();
        BlockState material = block.defaultBlockState();
        if (material.is(CHTags.BlockTag.COPYCAT_THERMAL_BLOCK_DENY) ||
                 !material.isCollisionShapeFullBlock(level,pos)
                        || !material.canOcclude() || !material.getRenderShape().equals(RenderShape.MODEL)
        ){
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (material.hasProperty(FACING)){
            material = material.setValue(FACING,player.getNearestViewDirection().getOpposite());
        }
        if (material.hasProperty(AXIS)){
            material = material.setValue(AXIS,player.getNearestViewDirection().getAxis());
        }


        AtomicBoolean set = new AtomicBoolean(false);
        BlockState finalMaterial = material;
        withBlockEntityDo(level,pos, copycatThermalBlockEntity -> {
           set.set(copycatThermalBlockEntity.setMaterial(finalMaterial,stack));
        });
        if (set.get()){
            if (!player.isCreative()){
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hitResult);
        if (result.equals(InteractionResult.PASS)) {
            return executeWithMaterial(
                    state, level, pos,
                    (m, s, l, p) -> m.useWithoutItem((Level) l,player,hitResult),
                    () -> result
            );
        }else {
            return result;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        ModelData modelData = level.getModelData(pos);
        if (modelData == ModelData.EMPTY){
            return getMaterial(level,pos);
        }
        BlockState fromModel = CopycatThermalBlockModel.getMaterial(modelData);
        return fromModel == null ? state : fromModel;
    }

    public static BlockState getMaterial(BlockGetter level,BlockPos pos){
        if (level.getBlockEntity(pos) instanceof CopycatThermalBlockEntity copycatThermalBlockEntity){
            return copycatThermalBlockEntity.getMaterial();
        }
        return CHBlocks.COPYCAT_THERMAL_BLOCK.getDefaultState();
    }

    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        AuxiliaryLightManager lightManager = level.getAuxLightManager(pos);
        if (lightManager != null)
            return lightManager.getLightAt(pos);

        return super.getLightEmission(state, level, pos);
    }

    @OnlyIn(Dist.CLIENT)
    public static BlockColor wrappedColor() {
        return new WrappedBlockColor();
    }

    @OnlyIn(Dist.CLIENT)
    public static class WrappedBlockColor implements BlockColor {

        @Override
        public int getColor(BlockState pState, @Nullable BlockAndTintGetter pLevel, @Nullable BlockPos pPos,
                            int pTintIndex) {
            if (pLevel == null || pPos == null)
                return GrassColor.get(0.5D, 1.0D);
            BlockState material = getMaterial(pLevel,pPos);
            if (material.getBlock() instanceof CopycatThermalBlock){
                return 0x000000;
            }
            return Minecraft.getInstance()
                    .getBlockColors()
                    .getColor(getMaterial(pLevel, pPos), pLevel, pPos, pTintIndex);
        }
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return executeWithMaterial(state,level,pos,
                (m,s,l,p)->m.getExplosionResistance(l,p,explosion),
                ()->super.getExplosionResistance(state,level, pos,explosion)
        );
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        return executeWithMaterial(state1,level,pos,
                (m,s,l,p)->m.addLandingEffects((ServerLevel) l,p,state2,entity,numberOfParticles),
                ()->super.addLandingEffects(state1, level, pos,state2,entity,numberOfParticles)
        );
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
        return executeWithMaterial(state,level,pos,
                (m,s,l,p)->m.addRunningEffects((Level) l,p,entity),
                ()->super.addRunningEffects(state, level, pos,entity)
        );
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return executeWithMaterial(state,level,pos,
                (m,s,l,p)->m.getEnchantPowerBonus((LevelReader) l,p),
                ()->super.getEnchantPowerBonus(state, level, pos)
        );
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return executeWithMaterial(state,level,pos,
                (m,s,l,p)->m.canEntityDestroy(l,p,entity),
                ()->super.canEntityDestroy(state, level, pos, entity)
        );
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        executeWithMaterial(state,level,pos,
                ((material, s, l, p) -> {
                    material.getBlock().fallOn((Level) l, s, p, entity, fallDistance);
                    return null;
                }),
                ()-> {
                    super.fallOn(level, state, pos, entity, fallDistance);
                    return null;
                }
        );
    }

    @Override
    public boolean isBurning(BlockState state, BlockGetter level, BlockPos pos) {
        return executeWithMaterial(state, level, pos,
                (material, s, l, p) -> {
                    if (material.is(CHTags.BlockTag.HEAT_ENTITY_ABOVE)){
                        return super.isBurning(state,level,pos);
                    }else {
                        return material.getBlock().isBurning(s, l, p);
                    }
                },
                () -> super.isBurning(state, level, pos)
        );
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.is(CHTags.BlockTag.HEAT_ENTITY_ABOVE)){
            super.stepOn(level,pos,state,entity);
        }else {
            executeWithMaterial(state, level, pos,
                    (material, s, l, p) -> {
                        if (material.is(CHTags.BlockTag.HEAT_ENTITY_ABOVE)) {
                            super.stepOn(level, pos, state, entity);
                        }else {
                            material.getBlock().stepOn((Level) l, p, s, entity);
                        }
                        return null;
                    },
                    () -> {
                        super.stepOn(level, pos, state, entity);
                        return null;
                    }
            );
        }
    }


    /**模仿伪装方块方法
     * void返回null
     * */
    private <T> T executeWithMaterial(BlockState state, BlockGetter level, BlockPos pos,
                                      MaterialFunction<T> materialFunction, Supplier<T> fallback) {
        BlockState material = getMaterial(level, pos);
        if (material != null && !(material.getBlock() instanceof CopycatThermalBlock)) {
            return materialFunction.apply(material, state, level, pos);
        }
        return fallback.get();
    }

    @FunctionalInterface
    private interface MaterialFunction<T> {
        T apply(BlockState material, BlockState state, BlockGetter level, BlockPos pos);
    }
}
