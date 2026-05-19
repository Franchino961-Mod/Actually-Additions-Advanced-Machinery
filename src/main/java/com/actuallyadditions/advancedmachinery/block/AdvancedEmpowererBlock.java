package com.actuallyadditions.advancedmachinery.block;

import com.actuallyadditions.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.actuallyadditions.advancedmachinery.registration.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AdvancedEmpowererBlock extends BaseEntityBlock {

    public static final MapCodec<AdvancedEmpowererBlock> CODEC = simpleCodec(AdvancedEmpowererBlock::new);

    public AdvancedEmpowererBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AdvancedEmpowererBlock> codec() {
        return CODEC;
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
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
}