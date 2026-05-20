package com.actuallyadditions.advancedmachinery.block;

import com.actuallyadditions.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.actuallyadditions.advancedmachinery.registration.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class AdvancedEmpowererBlock extends BaseEntityBlock {

    public static final MapCodec<AdvancedEmpowererBlock> CODEC = simpleCodec(AdvancedEmpowererBlock::new);

    // Proprietà di orientamento orizzontale
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public AdvancedEmpowererBlock(Properties properties) {
        super(properties);
        // stato default: rivolto a NORD
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<AdvancedEmpowererBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Il blocco si orienta verso il player quando viene posizionato
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedEmpowererBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof AdvancedEmpowererBlockEntity empowerer) {
                player.openMenu(empowerer, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ADVANCED_EMPOWERER.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    // FIX CRITICO: droppa tutto l'inventario nel mondo quando il blocco viene rotto
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AdvancedEmpowererBlockEntity empowerer) {
                ItemStackHandler inv = empowerer.getInventory();
                for (int i = 0; i < inv.getSlots(); i++) {
                    // Tutti gli slot (0-7) vengono droppati correttamente
                    ItemStack stack = inv.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(
                                level,
                                pos.getX(), pos.getY(), pos.getZ(),
                                stack);
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}