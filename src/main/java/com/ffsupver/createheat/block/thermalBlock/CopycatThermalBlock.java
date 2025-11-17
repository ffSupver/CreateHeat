package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.CHTags;
import com.ffsupver.createheat.registries.CHBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL;

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


        AtomicBoolean set = new AtomicBoolean(false);
        withBlockEntityDo(level,pos,copycatThermalBlockEntity -> {
           set.set(copycatThermalBlockEntity.setMaterial(material,stack));
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
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
            return switch (state.getValue(HEAT_LEVEL)) {
                case NONE -> 0;
                case SMOULDERING, FADING, KINDLED -> 4;
                case SEETHING -> 8;
            };
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
}
