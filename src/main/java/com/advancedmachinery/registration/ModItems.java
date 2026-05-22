package com.advancedmachinery.registration;

import com.advancedmachinery.AdvancedMachinery;
import com.advancedmachinery.item.UpgradeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AdvancedMachinery.MODID);

    public static final DeferredItem<BlockItem> ADVANCED_EMPOWERER = ITEMS
            .registerSimpleBlockItem(ModBlocks.ADVANCED_EMPOWERER);

    // FIX: usa UpgradeItem invece di Item base, così i tooltip
    // definiti in en_us.json e it_it.json vengono effettivamente mostrati
    // quando il giocatore passa il mouse sull'oggetto nell'inventario.
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
            () -> new UpgradeItem(new Item.Properties(),
                    "tooltip.advancedmachinery.speed_upgrade"));

    public static final DeferredItem<Item> ENERGY_UPGRADE = ITEMS.register("energy_upgrade",
            () -> new UpgradeItem(new Item.Properties(),
                    "tooltip.advancedmachinery.energy_upgrade"));
}