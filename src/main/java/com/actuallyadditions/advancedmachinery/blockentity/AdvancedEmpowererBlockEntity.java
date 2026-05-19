package com.actuallyadditions.advancedmachinery.blockentity;

import com.actuallyadditions.advancedmachinery.menu.AdvancedEmpowererMenu;
import com.actuallyadditions.advancedmachinery.registration.ModBlockEntities;
import com.actuallyadditions.advancedmachinery.registration.ModItems;
import de.ellpeck.actuallyadditions.mod.crafting.ActuallyRecipes;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AdvancedEmpowererBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ENERGY_CAPACITY = 2_000_000;
    public static final int BASE_SPEED = 200;

    // -------------------------------------------------------------------
    // EnergyStorage estesa con setter diretto per il caricamento NBT
    // (Fix #5: evita duplicazione energia su loadAdditional doppio)
    // -------------------------------------------------------------------
    private static class MutableEnergyStorage extends EnergyStorage {
        public MutableEnergyStorage(int capacity) {
            super(capacity);
        }

        /** Imposta direttamente l'energia senza passare per receiveEnergy(). */
        public void setStored(int amount) {
            this.energy = Math.min(Math.max(0, amount), this.capacity);
        }
    }

    // -------------------------------------------------------------------
    // Inventory layout (8 slots total):
    // 0 – Input A
    // 1 – Input B
    // 2 – Input C
    // 3 – (riservato – NON accetta item finché non implementato)
    // 4 – (riservato – NON accetta item finché non implementato)
    // 5 – Output
    // 6 – Speed Upgrade (max stack 4)
    // 7 – Efficiency Upgrade (max stack 4)
    // -------------------------------------------------------------------
    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            recipeDirty = true; // Fix #8: invalida la cache ricetta
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 5    -> false; // output – no inserimento manuale
                case 6    -> stack.getItem() == ModItems.SPEED_UPGRADE.get();
                case 7    -> stack.getItem() == ModItems.EFFICIENCY_UPGRADE.get();
                default   -> true; // Slot 0, 1, 2, 3, 4 sono tutti validi come input
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return (slot == 6 || slot == 7) ? 4 : super.getSlotLimit(slot);
        }
    };

    private final MutableEnergyStorage energy = new MutableEnergyStorage(ENERGY_CAPACITY);

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public MutableEnergyStorage getEnergyStorage() {
        return energy;
    }

    // -------------------------------------------------------------------
    // Recipe cache (Fix #8): ricalcolata solo quando l'inventario cambia
    // -------------------------------------------------------------------
    @Nullable
    private Optional<RecipeHolder<EmpowererRecipe>> cachedRecipe = null;
    private boolean recipeDirty = true;

    // ContainerData esposta al menu/screen (4 interi)
    protected final ContainerData data;
    private int progress    = 0;
    private int maxProgress = BASE_SPEED;

    public AdvancedEmpowererBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_EMPOWERER.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energy.getEnergyStored();
                    case 3 -> energy.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // I valori 2/3 (energia) sono read-only lato client; 0/1 vengono
                // sincronizzati dal server attraverso addDataSlots() nel menu.
                switch (index) {
                    case 0 -> progress    = value;
                    case 1 -> maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.advancedmachinery.advanced_empowerer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AdvancedEmpowererMenu(id, inv, this, this.data);
    }

    // -------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        Optional<RecipeHolder<EmpowererRecipe>> recipeOpt = getRecipe();
        if (recipeOpt.isPresent()) {
            EmpowererRecipe recipe = recipeOpt.get().value();
            this.maxProgress = getModifiedTime(recipe.getTime());

            int energyPerTick = Math.max(1,
                    getModifiedEnergy(recipe.getEnergyPerStand() * 4) / this.maxProgress);

            if (this.energy.getEnergyStored() >= energyPerTick) {

                // Fix #6: se siamo all'ultimo tick e l'output è pieno,
                // NON consumare energia e NON azzerare il progress.
                if (this.progress >= this.maxProgress - 1 && !canCraft(recipe)) {
                    return; // attesa output – nessuno spreco di energia
                }

                this.energy.extractEnergy(energyPerTick, false);
                this.progress++;

                if (this.progress >= this.maxProgress) {
                    craftItem(recipe);
                    this.progress = 0;
                }
                setChanged();
            }
        } else {
            if (this.progress != 0) {
                this.progress = 0;
                setChanged();
            }
        }
    }

    // -------------------------------------------------------------------
    // Recipe matching – usa i 3 slot di input attivi (0, 1, 2)
    // Slot 3 e 4 passati come EMPTY per compatibilità con AA (5 ingredienti)
    // Fix #8: risultato cachato, ricalcolato solo se recipeDirty
    // -------------------------------------------------------------------
    private Optional<RecipeHolder<EmpowererRecipe>> getRecipe() {
        if (level == null) return Optional.empty();

        if (recipeDirty || cachedRecipe == null) {
            cachedRecipe = level.getRecipeManager()
                    .getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get())
                    .stream()
                    .filter(r -> r.value().matches(
                            inventory.getStackInSlot(0),
                            inventory.getStackInSlot(1),
                            inventory.getStackInSlot(2),
                            inventory.getStackInSlot(3),
                            inventory.getStackInSlot(4)
                    ))
                    .findFirst();
            recipeDirty = false;
        }
        return cachedRecipe;
    }

    /** Fix #6: verifica se c'è spazio nell'output prima di completare la craft. */
    private boolean canCraft(EmpowererRecipe recipe) {
        ItemStack result     = recipe.getOutput().copy();
        ItemStack currentOut = inventory.getStackInSlot(5);
        return currentOut.isEmpty()
                || (ItemStack.isSameItem(currentOut, result)
                        && currentOut.getCount() + result.getCount() <= result.getMaxStackSize());
    }

    private void craftItem(EmpowererRecipe recipe) {
        ItemStack result     = recipe.getOutput().copy();
        ItemStack currentOut = inventory.getStackInSlot(5);

        for (int i = 0; i < 5; i++) {
            inventory.extractItem(i, 1, false);
        }
        if (currentOut.isEmpty()) {
            inventory.setStackInSlot(5, result);
        } else {
            currentOut.grow(result.getCount());
        }
    }

    // -------------------------------------------------------------------
    // Upgrade helpers
    // -------------------------------------------------------------------
    private int getModifiedTime(int original) {
        int speed = inventory.getStackInSlot(6).getCount();
        return Math.max(20, original - speed * 40);
    }

    private int getModifiedEnergy(int original) {
        int efficiency = inventory.getStackInSlot(7).getCount();
        return Math.max(original / 4, original - efficiency * (original / 10));
    }

    // -------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        // Fix #5: usa setStored() per impostare direttamente il valore
        // senza sommare a quello già presente (idempotente).
        energy.setStored(tag.getInt("Energy"));
        this.progress = tag.getInt("Progress");
        this.recipeDirty = true; // invalida cache dopo il caricamento
    }
}