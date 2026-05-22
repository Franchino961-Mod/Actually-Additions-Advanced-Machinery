package com.advancedmachinery.registration;

import com.advancedmachinery.AdvancedMachinery;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AdvancedMachinery.MODID);

    public static final DeferredItem<BlockItem> ADVANCED_EMPOWERER = ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_EMPOWERER);

    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.registerSimpleItem("speed_upgrade", new Item.Properties());
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE = ITEMS.registerSimpleItem("efficiency_upgrade", new Item.Properties());
}
