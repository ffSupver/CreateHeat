package com.ffsupver.createheat.block.thermalBlock;

import com.ffsupver.createheat.registries.CHBlocks;
import com.simibubi.create.content.decoration.copycat.FilteredBlockAndTintGetter;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.simibubi.create.content.decoration.copycat.CopycatModel.MATERIAL_PROPERTY;
import static com.simibubi.create.content.decoration.copycat.CopycatModel.getModelOf;

public class CopycatThermalBlockModel extends BakedModelWrapperWithData {
    private static final ModelProperty<OcclusionData> OCCLUSION_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<ModelData> WRAPPED_DATA_PROPERTY = new ModelProperty<>();
    public CopycatThermalBlockModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        BlockState material = getMaterial(blockEntityData);
        if (material == null)
            return builder;

        builder.with(MATERIAL_PROPERTY, material);
         if(!(state.getBlock() instanceof CopycatThermalBlock block)){
             return builder;
         }

        OcclusionData occlusionData = new OcclusionData();
        gatherOcclusionData(world, pos, state, material, occlusionData);
        builder.with(OCCLUSION_PROPERTY, occlusionData);

        ModelData wrappedData = getModelOf(material).getModelData(
                new FilteredBlockAndTintGetter(world,
                        targetPos -> block.canConnectTextures(state)
                ),
                pos, material, ModelData.EMPTY);
        return builder.with(WRAPPED_DATA_PROPERTY, wrappedData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        return getCroppedQuads(state, side, rand, getMaterial(ModelData.EMPTY), ModelData.EMPTY,
                RenderType.solid());
    }

    private List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType) {
        BakedModel model = getModelOf(material);
        return model.getQuads(material, side, rand, wrappedData, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        //获取伪装方块
        BlockState material = getMaterial(extraData);
        if (material == null) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }
        //获取是否遮挡
        OcclusionData occlusionData = extraData.get(OCCLUSION_PROPERTY);
        if (occlusionData != null && occlusionData.isOccluded(side)) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }
        //连接
        ModelData wrappedData = extraData.get(WRAPPED_DATA_PROPERTY);
        if (wrappedData == null)
            wrappedData = ModelData.EMPTY;

        if (renderType != null && !Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(material)
                .getRenderTypes(material, rand, wrappedData)
                .contains(renderType)) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }

        List<BakedQuad> croppedQuads = getCroppedQuads(state,side,rand,material,wrappedData,renderType);

        // Rubidium兼容 好像没有阻挡,不需要?

        return croppedQuads;
    }

    /**From {@link com.simibubi.create.content.decoration.copycat.CopycatModel}
     * */
    private void gatherOcclusionData(BlockAndTintGetter world, BlockPos pos, BlockState state, BlockState material,
                                     OcclusionData occlusionData) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (Direction face : Iterate.directions) {


            BlockPos.MutableBlockPos neighbourPos = mutablePos.setWithOffset(pos, face);
            BlockState neighbourState = world.getBlockState(neighbourPos);
            if (state.supportsExternalFaceHiding()
                    && neighbourState.hidesNeighborFace(world, neighbourPos, state, face.getOpposite())) {
                occlusionData.occlude(face);
                continue;
            }

            if (!Block.shouldRenderFace(material, world, pos, face, neighbourPos))
                occlusionData.occlude(face);
        }
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState material = getMaterial(data);

        if (material == null)
            return super.getParticleIcon(data);

        ModelData wrappedData = data.get(WRAPPED_DATA_PROPERTY);
        if (wrappedData == null)
            wrappedData = ModelData.EMPTY;

        return getModelOf(material).getParticleIcon(wrappedData);
    }

    public static BlockState getMaterial(ModelData data) {
        BlockState material = data == null ? null : data.get(MATERIAL_PROPERTY);
        return material == null ? null : material.is(CHBlocks.COPYCAT_THERMAL_BLOCK.get()) ? null : material;
    }

    private static class OcclusionData {
        private final boolean[] occluded;

        public OcclusionData() {
            occluded = new boolean[6];
        }

        public void occlude(Direction face) {
            occluded[face.get3DDataValue()] = true;
        }

        public boolean isOccluded(Direction face) {
            return face == null ? false : occluded[face.get3DDataValue()];
        }
    }
}
