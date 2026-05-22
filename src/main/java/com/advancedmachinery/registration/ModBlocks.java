package com.advancedmachinery.registration;

import com.advancedmachinery.AdvancedMachinery;
import com.advancedmachinery.block.AdvancedEmpowererBlock;
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
