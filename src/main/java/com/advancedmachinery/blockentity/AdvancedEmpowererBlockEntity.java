package com.advancedmachinery.blockentity;

import com.advancedmachinery.menu.AdvancedEmpowererMenu;
import com.advancedmachinery.registration.ModBlockEntities;
import com.advancedmachinery.registration.ModItems;
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
    // e capienza dinamica scalata con gli Energy Upgrade.
    // -------------------------------------------------------------------
    public class MutableEnergyStorage extends EnergyStorage {
        public MutableEnergyStorage(int capacity) {
            super(capacity);
        }

        @Override
        public int getMaxEnergyStored() {
            if (level != null && level.isClientSide) {
                return clientMaxEnergy;
            }
            int energyUpgrades = inventory.getStackInSlot(7).getCount();
            double multiplier = Math.pow(10.0, (double) energyUpgrades / 8.0);
            return (int) (ENERGY_CAPACITY * multiplier);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive())
                return 0;

            int maxEnergy = getMaxEnergyStored();
            int energyReceived = Math.min(maxEnergy - this.energy, Math.min(maxEnergy, maxReceive));
            if (!simulate) {
                this.energy += energyReceived;
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract())
                return 0;

            int maxEnergy = getMaxEnergyStored();
            int energyExtracted = Math.min(this.energy, Math.min(maxEnergy, maxExtract));
            if (!simulate) {
                this.energy -= energyExtracted;
            }
            return energyExtracted;
        }

        public void setStored(int amount) {
            this.energy = amount;
        }
    }

    // -------------------------------------------------------------------
    // Inventory layout (8 slot):
    // 0 – Input modifier 1 (croce: top     — coordinata GUI: 44, 24)
    // 1 – Input modifier 2 (croce: left    — coordinata GUI: 14, 54)
    // 2 – Input BASE       (croce: center  — coordinata GUI: 44, 54)
    // 3 – Input modifier 3 (croce: right   — coordinata GUI: 74, 54)
    // 4 – Input modifier 4 (croce: bottom  — coordinata GUI: 44, 84)
    // 5 – Output (read-only)
    // 6 – Speed Upgrade (max 8)
    // 7 – Energy Upgrade (max 8)
    //
    // NOTA: EmpowererRecipe.matches(base, m1, m2, m3, m4) richiede
    // esattamente 1 base + 4 modifier, tutti non-empty.
    // Slot 2 = base, slot 0,1,3,4 = modifier (AA gestisce internamente
    // tutte le permutazioni dei modifier).
    // -------------------------------------------------------------------
    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            recipeDirty = true;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 5 -> false; // output – no inserimento manuale
                case 6 -> stack.getItem() == ModItems.SPEED_UPGRADE.get();
                case 7 -> stack.getItem() == ModItems.ENERGY_UPGRADE.get();
                default -> true; // slot 0,1,2,3,4 → input liberi
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return (slot == 6 || slot == 7) ? 8 : super.getSlotLimit(slot);
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
    // Recipe cache
    // -------------------------------------------------------------------
    @Nullable
    private Optional<RecipeHolder<EmpowererRecipe>> cachedRecipe = null;
    private boolean recipeDirty = true;

    // ContainerData: 0=progress, 1=maxProgress, 2=energyStored, 3=maxEnergy
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = BASE_SPEED;
    private int clientMaxEnergy = ENERGY_CAPACITY;

    public AdvancedEmpowererBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_EMPOWERER.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energy.getEnergyStored() & 0xFFFF;
                    case 3 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energy.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energy.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> maxProgress = value;
                    case 2 -> {
                        int current = energy.getEnergyStored();
                        energy.setStored((current & 0xFFFF0000) | (value & 0xFFFF));
                    }
                    case 3 -> {
                        int current = energy.getEnergyStored();
                        energy.setStored(((value & 0xFFFF) << 16) | (current & 0xFFFF));
                    }
                    case 4 -> {
                        clientMaxEnergy = (clientMaxEnergy & 0xFFFF0000) | (value & 0xFFFF);
                    }
                    case 5 -> {
                        clientMaxEnergy = ((value & 0xFFFF) << 16) | (clientMaxEnergy & 0xFFFF);
                    }
                }
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    public ContainerData getContainerData() {
        return this.data;
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
        if (level == null || level.isClientSide)
            return;

        Optional<RecipeHolder<EmpowererRecipe>> recipeOpt = getRecipe();
        if (recipeOpt.isPresent()) {
            EmpowererRecipe recipe = recipeOpt.get().value();
            this.maxProgress = getModifiedTime(recipe.getTime());

            int energyPerTick = getEnergyPerTick(recipe);

            if (this.energy.getEnergyStored() >= energyPerTick) {

                if (this.progress >= this.maxProgress - 1 && !canCraft(recipe)) {
                    return;
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
    // Recipe matching
    // Slot 2 = base (centro), slot 0,1,3,4 = modifier
    // Il layout rispecchia esattamente la GUI: center = base item.
    // matches() richiede tutti e 4 i modifier non-empty → se uno slot è
    // vuoto la ricetta non matcha (comportamento corretto).
    // -------------------------------------------------------------------
    private Optional<RecipeHolder<EmpowererRecipe>> getRecipe() {
        if (level == null)
            return Optional.empty();

        if (recipeDirty || cachedRecipe == null) {
            ItemStack base = inventory.getStackInSlot(2); // center slot
            ItemStack m1   = inventory.getStackInSlot(0); // top
            ItemStack m2   = inventory.getStackInSlot(1); // left
            ItemStack m3   = inventory.getStackInSlot(3); // right
            ItemStack m4   = inventory.getStackInSlot(4); // bottom

            cachedRecipe = level.getRecipeManager()
                    .getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get())
                    .stream()
                    .filter(r -> r.value().matches(base, m1, m2, m3, m4))
                    .findFirst();
            recipeDirty = false;
        }
        return cachedRecipe;
    }

    private boolean canCraft(EmpowererRecipe recipe) {
        ItemStack result = recipe.getOutput().copy();
        ItemStack currentOut = inventory.getStackInSlot(5);
        return currentOut.isEmpty()
                || (ItemStack.isSameItem(currentOut, result)
                        && currentOut.getCount() + result.getCount() <= result.getMaxStackSize());
    }

    private void craftItem(EmpowererRecipe recipe) {
        ItemStack result = recipe.getOutput().copy();
        ItemStack currentOut = inventory.getStackInSlot(5);

        // Consuma base (slot 2) + tutti e 4 i modifier (slot 0,1,3,4)
        for (int i = 0; i < 5; i++) {
            inventory.extractItem(i, 1, false);
        }

        if (currentOut.isEmpty()) {
            inventory.setStackInSlot(5, result);
        } else {
            currentOut.grow(result.getCount());
        }
        setChanged();
    }

    // -------------------------------------------------------------------
    // Upgrade helpers
    // -------------------------------------------------------------------
    private int getModifiedTime(int original) {
        int speed = inventory.getStackInSlot(6).getCount();
        double speedMultiplier = Math.pow(10.0, (double) speed / 8.0);
        return Math.max(1, (int) Math.round(original / speedMultiplier));
    }

    private int getEnergyPerTick(EmpowererRecipe recipe) {
        int speed = inventory.getStackInSlot(6).getCount();
        int energyUpgrades = inventory.getStackInSlot(7).getCount();
        
        double baseEnergy = recipe.getEnergyPerStand() * 4.0;
        double baseTime = recipe.getTime();
        double baseUsage = baseEnergy / baseTime;
        
        double exponent = (2.0 * speed - energyUpgrades) / 8.0;
        double usage = baseUsage * Math.pow(10.0, exponent);
        return Math.max(1, (int) Math.round(usage));
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
        energy.setStored(tag.getInt("Energy"));
        this.progress = tag.getInt("Progress");
        this.recipeDirty = true;
    }
}
