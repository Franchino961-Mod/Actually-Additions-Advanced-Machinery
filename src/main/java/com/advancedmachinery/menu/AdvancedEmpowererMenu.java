package com.advancedmachinery.menu;

import com.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.advancedmachinery.registration.ModItems;
import com.advancedmachinery.registration.ModMenuTypes;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
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
    // Usa un handler dummy vuoto se la BlockEntity non è ancora disponibile,
    // evitando di istanziare una BlockEntity con AIR come blockstate.
    public AdvancedEmpowererMenu(int containerId, Inventory playerInventory,
            net.minecraft.network.FriendlyByteBuf buf) {
        super(ModMenuTypes.ADVANCED_EMPOWERER.get(), containerId);
        this.level = playerInventory.player.level();

        BlockPos pos = buf.readBlockPos();
        BlockEntity be = this.level.getBlockEntity(pos);

        if (be instanceof AdvancedEmpowererBlockEntity emp) {
            this.blockEntity = emp;
            this.data = emp.getContainerData();
            addBlockEntitySlots(emp.getInventory());
        } else {
            // Race condition o chunk non caricato: usa handler/data dummy
            // Non istanziamo una BlockEntity fasulla — solo strutture leggere
            this.blockEntity = null;
            this.data = new SimpleContainerData(4);
            addBlockEntitySlots(new ItemStackHandler(8));
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(this.data);
    }

    // -----------------------------------------------------------------------
    // Slot layout GUI — coordinate dalla texture advanced_empowerer.png
    //
    // Indice Inv Posizione Ruolo
    // 0 0 ( 44, 24) Input 0 – centro-alto (croce: top)
    // 1 1 ( 14, 54) Input 1 – sinistra (croce: left)
    // 2 2 ( 44, 54) Input 2 – centro (croce: center / base)
    // 3 3 ( 74, 54) Input 3 – destra (croce: right)
    // 4 4 ( 44, 84) Input 4 – centro-basso (croce: bottom)
    // 5 5 (125, 54) Output (read-only)
    // 6 6 ( 98, 90) Speed Upgrade
    // 7 7 (116, 90) Efficiency Upgrade
    // 8–34 – Inventario player (3×9)
    // 35–43 – Hotbar player (9)
    // -----------------------------------------------------------------------
    private void addBlockEntitySlots(IItemHandler handler) {
        // Input a croce — coordinate dalla texture aggiornata
        addSlot(new SlotItemHandler(handler, 0, 44, 24)); // top
        addSlot(new SlotItemHandler(handler, 1, 14, 54)); // left
        addSlot(new SlotItemHandler(handler, 2, 44, 54)); // center (base)
        addSlot(new SlotItemHandler(handler, 3, 74, 54)); // right
        addSlot(new SlotItemHandler(handler, 4, 44, 84)); // bottom

        // Output — no inserimento manuale
        addSlot(new SlotItemHandler(handler, 5, 125, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Upgrade slots
        addSlot(new SlotItemHandler(handler, 6, 98, 90)); // Speed
        addSlot(new SlotItemHandler(handler, 7, 116, 90)); // Efficiency
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
    // Getter letti dal ContainerData
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
    // Slot macchina : 0–7 (0–4 input, 5 output, 6–7 upgrade)
    // Inventario : 8–34
    // Hotbar : 35–43
    // -----------------------------------------------------------------------
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == 5) {
            // Output → sposta nell'inventario (da fondo)
            if (!this.moveItemStackTo(stack, 8, 44, true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);

        } else if (index < 8) {
            // Input (0-4) o upgrade (6-7) → sposta nell'inventario
            if (!this.moveItemStackTo(stack, 8, 44, false))
                return ItemStack.EMPTY;

        } else {
            // Dall'inventario/hotbar → tenta lo slot corretto in macchina
            boolean moved = false;
            if (stack.getItem() == ModItems.SPEED_UPGRADE.get()) {
                moved = this.moveItemStackTo(stack, 6, 7, false);
            } else if (stack.getItem() == ModItems.EFFICIENCY_UPGRADE.get()) {
                moved = this.moveItemStackTo(stack, 7, 8, false);
            } else {
                // Tenta gli slot input (0–4)
                moved = this.moveItemStackTo(stack, 0, 5, false);
            }

            if (!moved) {
                // Fallback: sposta tra inventario principale e hotbar
                if (index < 35) {
                    if (!this.moveItemStackTo(stack, 35, 44, false))
                        return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(stack, 8, 35, false))
                        return ItemStack.EMPTY;
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
        // Se blockEntity è null (costruttore client dummy) la GUI non è valida
        if (this.blockEntity == null)
            return false;
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                blockEntity.getBlockState().getBlock());
    }

    public ContainerData getData() {
        return data;
    }

    public AdvancedEmpowererBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
