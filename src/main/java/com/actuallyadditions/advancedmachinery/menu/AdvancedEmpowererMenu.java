package com.actuallyadditions.advancedmachinery.menu;

import com.actuallyadditions.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.actuallyadditions.advancedmachinery.registration.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AdvancedEmpowererMenu extends AbstractContainerMenu {
    private final AdvancedEmpowererBlockEntity blockEntity;
    private final ContainerData data;

    // Client constructor
    public AdvancedEmpowererMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (AdvancedEmpowererBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Common constructor
    public AdvancedEmpowererMenu(int id, Inventory inv, AdvancedEmpowererBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ADVANCED_EMPOWERER.get(), id);
        this.blockEntity = entity;
        this.data = data;

        checkContainerDataCount(data, 4);
        addDataSlots(data);

        // Input slots (0-4)
        addSlot(new SlotItemHandler(entity.getInventory(), 0, 80, 35)); // Base
        addSlot(new SlotItemHandler(entity.getInventory(), 1, 80, 10)); // Up
        addSlot(new SlotItemHandler(entity.getInventory(), 2, 80, 60)); // Down
        addSlot(new SlotItemHandler(entity.getInventory(), 3, 55, 35)); // Left
        addSlot(new SlotItemHandler(entity.getInventory(), 4, 105, 35)); // Right

        // Output slot (5)
        addSlot(new SlotItemHandler(entity.getInventory(), 5, 145, 35));

        // Upgrade slots (6-7)
        addSlot(new SlotItemHandler(entity.getInventory(), 6, 8, 60));  // Speed
        addSlot(new SlotItemHandler(entity.getInventory(), 7, 26, 60)); // Efficiency

        layoutPlayerInventorySlots(inv, 8, 84);
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getEnergy() {
        return data.get(2);
    }

    public int getMaxEnergy() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 8) { // Dal macchinario all'inventario
                if (!this.moveItemStackTo(itemstack1, 8, 44, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Dall'inventario al macchinario
                if (itemstack1.getItem() == com.actuallyadditions.advancedmachinery.registration.ModItems.SPEED_UPGRADE.get()) {
                    if (!this.moveItemStackTo(itemstack1, 6, 7, false)) return ItemStack.EMPTY;
                } else if (itemstack1.getItem() == com.actuallyadditions.advancedmachinery.registration.ModItems.EFFICIENCY_UPGRADE.get()) {
                    if (!this.moveItemStackTo(itemstack1, 7, 8, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(itemstack1, 0, 5, false)) { // Prova negli slot input
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
        return true;
    }

    private void layoutPlayerInventorySlots(Inventory inv, int x, int y) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inv, j + i * 9 + 9, x + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inv, i, x + i * 18, y + 58));
        }
    }
}
