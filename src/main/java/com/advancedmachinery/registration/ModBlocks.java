package com.actuallyadditions.advancedmachinery.registration;

import com.actuallyadditions.advancedmachinery.AdvancedMachinery;
import com.actuallyadditions.advancedmachinery.block.AdvancedEmpowererBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AdvancedMachinery.MODID);

    public static final DeferredBlock<AdvancedEmpowererBlock> ADVANCED_EMPOWERER = BLOCKS.registerBlock("advanced_empowerer",
            AdvancedEmpowererBlock::new, BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops());
}
