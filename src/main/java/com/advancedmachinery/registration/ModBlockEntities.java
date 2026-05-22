package com.advancedmachinery.registration;

import com.advancedmachinery.AdvancedMachinery;
import com.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdvancedMachinery.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedEmpowererBlockEntity>> ADVANCED_EMPOWERER =
            BLOCK_ENTITIES.register("advanced_empowerer", () -> BlockEntityType.Builder.of(AdvancedEmpowererBlockEntity::new, ModBlocks.ADVANCED_EMPOWERER.get()).build(null));
}
