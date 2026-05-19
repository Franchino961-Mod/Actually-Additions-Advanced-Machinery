package com.actuallyadditions.advancedmachinery.menu;

import com.actuallyadditions.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.actuallyadditions.advancedmachinery.registration.ModBlocks;
import com.actuallyadditions.advancedmachinery.registration.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AdvancedEmpowererMenu extends AbstractContainerMenu {
    private final AdvancedEmpowererBlockEntity blockEntity;
    private final ContainerData data;

    // Client constructor — safe null/type check before cast
    public AdvancedEmpowererMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, resolveBlockEntity(inv, extraData.readBlockPos()), new SimpleContainerData(4));
    }

    private static AdvancedEmpowererBlockEntity resolveBlockEntity(Inventory inv, BlockPos pos) {
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof AdvancedEmpowererBlockEntity aebe) return aebe;
        throw new IllegalStateException("Expected AdvancedEmpowererBlockEntity at " + pos + ", got: " + be);
    }

    // Common constructor
    public AdvancedEmpowererMenu(int id, Inventory inv, AdvancedEmpowererBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ADVANCED_EMPOWERER.get(), id);
        this.blockEntity = entity;
        this.data = data;

        checkContainerDataCount(data, 4);
        addDataSlots(data);

        // -----------------------------------------------------------
        // Layout griglia chest-vanilla (x=8+col*18, y=18+row*18):
        // Inv slot 0 → Centro    (grid 20 → col2,row2) → (44, 54)
        // Inv slot 1 → Alto      (grid  2 → col2,row0) → (44, 18)
        // Inv slot 2 → Destra    (grid 22 → col4,row2) → (80, 54)
        // Inv slot 3 → Basso     (grid 38 → col2,row4) → (44, 90)
        // Inv slot 4 → Sinistra  (grid 18 → col0,row2) → ( 8, 54)
        // Inv slot 5 → Output    (grid 24 → col6,row2) → (116,54)
        // Inv slot 6 → Speed Up  (grid 41 → col5,row4) → (98, 90)
        // Inv slot 7 → Effic. Up (grid 42 → col6,row4) → (116,90)
        // -----------------------------------------------------------
        addSlot(new SlotItemHandler(entity.getInventory(), 0,  44, 54)); // Centro
        addSlot(new SlotItemHandler(entity.getInventory(), 1,  44, 18)); // Alto
        addSlot(new SlotItemHandler(entity.getInventory(), 2,  80, 54)); // Destra
        addSlot(new SlotItemHandler(entity.getInventory(), 3,  44, 90)); // Basso
        addSlot(new SlotItemHandler(entity.getInventory(), 4,   8, 54)); // Sinistra

        // Output slot (5)
        addSlot(new SlotItemHandler(entity.getInventory(), 5, 116, 54));

        // Upgrade slots (6-7)
        addSlot(new SlotItemHandler(entity.getInventory(), 6,  98, 90)); // Speed
        addSlot(new SlotItemHandler(entity.getInventory(), 7, 116, 90)); // Efficiency

        // Player inventory — inizia dopo le 5 righe macchina (y=108) + gap 14px = y=122
        // Hotbar a y=122+58=180; GUI height totale = 204px
        layoutPlayerInventorySlots(inv, 8, 122);
    }


    public int getProgress()    { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getEnergy()      { return data.get(2); }
    public int getMaxEnergy()   { return data.get(3); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 8) { // Dal macchinario all'inventario player
                if (!this.moveItemStackTo(itemstack1, 8, 44, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Dall'inventario player al macchinario
                if (itemstack1.getItem() == com.actuallyadditions.advancedmachinery.registration.ModItems.SPEED_UPGRADE.get()) {
                    if (!this.moveItemStackTo(itemstack1, 6, 7, false)) return ItemStack.EMPTY;
                } else if (itemstack1.getItem() == com.actuallyadditions.advancedmachinery.registration.ModItems.EFFICIENCY_UPGRADE.get()) {
                    if (!this.moveItemStackTo(itemstack1, 7, 8, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(itemstack1, 0, 5, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player,
                ModBlocks.ADVANCED_EMPOWERER.get());
    }

    private void layoutPlayerInventorySlots(Inventory inv, int x, int y) {
        // 3 righe inventario (slot 9-35)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inv, j + i * 9 + 9, x + j * 18, y + i * 18));
            }
        }
        // Hotbar (slot 0-8), 58px sotto l'inizio dell'inventario
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inv, i, x + i * 18, y + 58));
        }
    }
}