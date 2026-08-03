package com.ffsupver.createheat.block.dragonFireInput;

import com.ffsupver.createheat.block.NetworkBehaviour;
import com.ffsupver.createheat.compat.iceAndFire.IceAndFire;
import com.ffsupver.createheat.network.HeatService;
import com.iafenvoy.iceandfire.data.DragonType;
import com.iafenvoy.iceandfire.item.block.util.DragonProof;
import com.iafenvoy.iceandfire.registry.IafDragonTypes;
import com.iafenvoy.iceandfire.util.DragonTypeProvider;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;
import java.util.UUID;

public class DragonFireInputBlock extends Block implements DragonProof, DragonTypeProvider, IBE<DragonFireInputBlockEntity>, IWrenchable {
    public static final Property<Boolean> BURNING = BooleanProperty.create("burning");
    public DragonFireInputBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BURNING,false));
    }


    @Override
    public Class<DragonFireInputBlockEntity> getBlockEntityClass() {
        return DragonFireInputBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DragonFireInputBlockEntity> getBlockEntityType() {
        return IceAndFire.DRAGON_FIRE_INPUT_BLOCK_ENTITY.get();
    }

    @Override
    public DragonType getDragonType() {
        return IafDragonTypes.FIRE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BURNING);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(BURNING) ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.getBlock().equals(newState.getBlock())){ // destroy
            withBlockEntityDo(level,pos,dragonFireInputBlockEntity -> {
                NetworkBehaviour networkBehaviour = dragonFireInputBlockEntity.getNetworkBehaviour();
                if (networkBehaviour != null){
                    HeatService.removeBlockFromNetwork(level,pos,networkBehaviour.getNetworkId());
                }
            });
        }
        IBE.onRemove(state,level,pos,newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.getBlock().equals(state.getBlock())) { // new block
            withBlockEntityDo(level, pos, dragonFireInputBlockEntity -> {
                NetworkBehaviour networkBehaviour = dragonFireInputBlockEntity.getNetworkBehaviour();
                if (networkBehaviour != null){
                    Set<UUID> neighborNetworkId = networkBehaviour.getNeighborNetworkId();
                    networkBehaviour.setNetworkId(HeatService.addBlockToNetwork(pos, level, neighborNetworkId));
                }
            });
        }
    }
}
