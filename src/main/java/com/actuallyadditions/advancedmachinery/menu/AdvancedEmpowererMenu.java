package com.actuallyadditions.advancedmachinery.menu;

import com.actuallyadditions.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.actuallyadditions.advancedmachinery.registration.ModItems;
import com.actuallyadditions.advancedmachinery.registration.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AdvancedEmpowererMenu extends AbstractContainerMenu {

    private final AdvancedEmpowererBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // -----------------------------------------------------------------------
    // ContainerData indices
    // 0 = progress
    // 1 = maxProgress
    // 2 = energyStored
    // 3 = maxEnergy
    // -----------------------------------------------------------------------

    // Costruttore SERVER (chiamato da BlockEntity.createMenu)
    public AdvancedEmpowererMenu(int containerId, Inventory playerInventory,
            AdvancedEmpowererBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.ADVANCED_EMPOWERER.get(), containerId);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addBlockEntitySlots(blockEntity.getInventory());
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    // Costruttore CLIENT (chiamato dal network)
    // FIX CRITICO: fallback sicuro invece di IllegalStateException
    public AdvancedEmpowererMenu(int containerId, Inventory playerInventory,
            net.minecraft.network.FriendlyByteBuf buf) {
        super(ModMenuTypes.ADVANCED_EMPOWERER.get(), containerId);
        this.level = playerInventory.player.level();

        BlockPos pos = buf.readBlockPos();
        BlockEntity be = this.level.getBlockEntity(pos);

        if (be instanceof AdvancedEmpowererBlockEntity emp) {
            this.blockEntity = emp;
            this.data = emp.getContainerData();
        } else {
            // Race condition o chunk non ancora caricato: usa dummy per evitare crash
            this.blockEntity = new AdvancedEmpowererBlockEntity(pos,
                    Blocks.AIR.defaultBlockState());
            this.data = new SimpleContainerData(4);
        }

        addBlockEntitySlots(this.blockEntity.getInventory());
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(this.data);
    }

    // -----------------------------------------------------------------------
    // Slot layout GUI (indici menu):
    // 0 – Input base (inv slot 0)
    // 1 – Input modifier 1 (inv slot 1)
    // 2 – Input modifier 2 (inv slot 2)
    // 3 – Output (read-only) (inv slot 5)
    // 4 – Speed Upgrade (inv slot 6)
    // 5 – Efficiency Upgrade (inv slot 7)
    // 6–32 – Inventario player (3×9)
    // 33–41 – Hotbar player (9)
    // -----------------------------------------------------------------------
    private void addBlockEntitySlots(IItemHandler handler) {
        addSlot(new SlotItemHandler(handler, 0, 56, 35));
        addSlot(new SlotItemHandler(handler, 1, 76, 35));
        addSlot(new SlotItemHandler(handler, 2, 96, 35));

        // Output — nessun inserimento manuale
        addSlot(new SlotItemHandler(handler, 5, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Upgrade slots
        addSlot(new SlotItemHandler(handler, 6, 56, 60)); // Speed
        addSlot(new SlotItemHandler(handler, 7, 76, 60)); // Efficiency
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 122 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 180));
        }
    }

    // -----------------------------------------------------------------------
    // Getter letti dal ContainerData — usati dalla Screen
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // Shift+Click
    // -----------------------------------------------------------------------
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 3) {
            // Output → sposta nell'inventario (da fondo)
            if (!this.moveItemStackTo(stack, 6, 42, true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);

        } else if (index < 6) {
            // Input (0-2) o upgrade (4-5) → sposta nell'inventario
            if (!this.moveItemStackTo(stack, 6, 42, false))
                return ItemStack.EMPTY;

        } else {
            // Dall'inventario/hotbar → tenta lo slot corretto in macchina
            boolean moved = false;
            if (stack.getItem() == ModItems.SPEED_UPGRADE.get()) {
                moved = this.moveItemStackTo(stack, 4, 5, false);
            } else if (stack.getItem() == ModItems.EFFICIENCY_UPGRADE.get()) {
                moved = this.moveItemStackTo(stack, 5, 6, false);
            } else {
                // Tenta gli slot input (0–2)
                moved = this.moveItemStackTo(stack, 0, 3, false);
            }

            if (!moved) {
                // Fallback: sposta tra inventario principale e hotbar
                if (index < 33) {
                    if (!this.moveItemStackTo(stack, 33, 42, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 6, 33, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount())
            return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                blockEntity.getBlockState().getBlock());
    }

    // Esposto per il costruttore client del dummy
    public ContainerData getData() {
        return data;
    }

    public AdvancedEmpowererBlockEntity getBlockEntity() {
        return blockEntity;
    }
}