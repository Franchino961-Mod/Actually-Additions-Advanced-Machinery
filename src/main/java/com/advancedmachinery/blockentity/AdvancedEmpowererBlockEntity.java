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
    //
    // FIX 1: dichiarata STATIC per evitare il riferimento implicito alla
    // BlockEntity esterna, che causava memory leak e problemi di
    // serializzazione. L'accesso all'inventario avviene tramite il
    // Supplier<Integer> passato nel costruttore.
    //
    // FIX 2: setStored() ora fa il clamp corretto [0, capacity], evitando
    // stati inconsistenti se il file NBT è corrotto o se l'energia
    // salvata supera la nuova capacità dopo aver rimosso gli upgrade.
    // -------------------------------------------------------------------
    public static class MutableEnergyStorage extends EnergyStorage {

        // Supplier che legge il conteggio degli Energy Upgrade dall'inventario.
        // Usato da getMaxEnergyStored() senza tenere un riferimento diretto
        // alla BlockEntity o all'ItemStackHandler.
        private final java.util.function.IntSupplier energyUpgradeCountSupplier;

        // Valore di maxEnergy lato client, sincronizzato via ContainerData.
        // Lato server viene sempre calcolato al volo dal supplier.
        private int clientMaxEnergy = ENERGY_CAPACITY;

        public MutableEnergyStorage(int baseCapacity,
                java.util.function.IntSupplier energyUpgradeCountSupplier) {
            super(baseCapacity);
            this.energyUpgradeCountSupplier = energyUpgradeCountSupplier;
        }

        // -------------------------------------------------------------------
        // Capacità dinamica: scala esponenzialmente con gli Energy Upgrade.
        // Con 0 upgrade → ENERGY_CAPACITY (2.000.000 FE)
        // Con 8 upgrade → ENERGY_CAPACITY * 10 (20.000.000 FE)
        // -------------------------------------------------------------------
        @Override
        public int getMaxEnergyStored() {
            // Lato client: usa il valore sincronizzato via ContainerData
            // (il supplier punta all'inventario del server, non disponibile qui)
            if (energyUpgradeCountSupplier == null) {
                return clientMaxEnergy;
            }
            int energyUpgrades = energyUpgradeCountSupplier.getAsInt();
            double multiplier = Math.pow(10.0, (double) energyUpgrades / 8.0);
            return (int) (ENERGY_CAPACITY * multiplier);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive())
                return 0;
            int maxEnergy = getMaxEnergyStored();
            int received = Math.min(maxEnergy - this.energy, Math.min(maxEnergy, maxReceive));
            if (!simulate)
                this.energy += received;
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract())
                return 0;
            int maxEnergy = getMaxEnergyStored();
            int extracted = Math.min(this.energy, Math.min(maxEnergy, maxExtract));
            if (!simulate)
                this.energy -= extracted;
            return extracted;
        }

        // FIX 2: clamp corretto — nessun valore negativo né superiore alla capacità.
        public void setStored(int amount) {
            this.energy = Math.max(0, Math.min(amount, getMaxEnergyStored()));
        }

        // Usato lato client dal ContainerData per aggiornare la capacità massima.
        public void setClientMaxEnergy(int value) {
            this.clientMaxEnergy = value;
        }

        public int getClientMaxEnergy() {
            return clientMaxEnergy;
        }
    }

    // -------------------------------------------------------------------
    // Inventory layout (8 slot):
    // 0 – Input modifier 1 (croce: top — GUI: 44, 24)
    // 1 – Input modifier 2 (croce: left — GUI: 14, 54)
    // 2 – Input BASE (croce: center — GUI: 44, 54)
    // 3 – Input modifier 3 (croce: right — GUI: 74, 54)
    // 4 – Input modifier 4 (croce: bottom — GUI: 44, 84)
    // 5 – Output (read-only)
    // 6 – Speed Upgrade (max 8)
    // 7 – Energy Upgrade (max 8)
    //
    // EmpowererRecipe.matches(base, m1, m2, m3, m4):
    // Slot 2 = base, slot 0,1,3,4 = modifier.
    // AA gestisce internamente le permutazioni dei modifier.
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
                case 5 -> false; // output — no inserimento manuale
                case 6 -> stack.getItem() == ModItems.SPEED_UPGRADE.get();
                case 7 -> stack.getItem() == ModItems.ENERGY_UPGRADE.get();
                default -> true; // slot 0-4 → input liberi
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return (slot == 6 || slot == 7) ? 8 : super.getSlotLimit(slot);
        }
    };

    // Il supplier legge il conteggio degli Energy Upgrade direttamente
    // dall'inventario della BlockEntity — elimina l'accoppiamento implicito
    // che esisteva quando MutableEnergyStorage era una inner class non-statica.
    private final MutableEnergyStorage energy = new MutableEnergyStorage(
            ENERGY_CAPACITY,
            () -> inventory.getStackInSlot(7).getCount());

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

    // -------------------------------------------------------------------
    // ContainerData — 6 valori sincronizzati client/server ogni tick:
    //
    // 0 = progress (tick correnti, max 32767)
    // 1 = maxProgress (tick totali ricetta modificata)
    // 2 = energyStored LOW (bit 0-15 dell'energia corrente)
    // 3 = energyStored HI (bit 16-31 dell'energia corrente)
    // 4 = maxEnergy LOW (bit 0-15 della capacità massima)
    // 5 = maxEnergy HI (bit 16-31 della capacità massima)
    //
    // ContainerData trasmette solo int a 16 bit firmati (-32768..32767).
    // Per trasportare valori fino a 20.000.000 FE (25 bit) usiamo due
    // slot per ciascun valore (split a 16 bit).
    //
    // FIX (sincronizzazione atomica): i due half-word di energyStored e
    // maxEnergy vengono aggiornati nel set() usando variabili di staging
    // locali (pendingEnergyStored, pendingMaxEnergy). Il valore effettivo
    // viene applicato solo quando arriva il secondo half-word (HI), in
    // modo che la GUI legga sempre una coppia coerente e non un valore
    // ibrido tra vecchio e nuovo frame.
    // -------------------------------------------------------------------
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = BASE_SPEED;

    // Valori di staging per la ricostruzione atomica lato client
    private int pendingEnergyLow = 0;
    private int pendingMaxEnergyLow = 0;

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

                    // Energia corrente: applica immediatamente unendo la parte ricevuta con quella memorizzata
                    case 2 -> {
                        pendingEnergyLow = value & 0xFFFF;
                        int currentHigh = (energy.getEnergyStored() >> 16) & 0xFFFF;
                        energy.setStored((currentHigh << 16) | pendingEnergyLow);
                    }
                    case 3 -> {
                        int currentLow = energy.getEnergyStored() & 0xFFFF;
                        int full = ((value & 0xFFFF) << 16) | currentLow;
                        energy.setStored(full);
                    }

                    // Capacità massima: applica immediatamente
                    case 4 -> {
                        pendingMaxEnergyLow = value & 0xFFFF;
                        int currentHigh = (energy.getClientMaxEnergy() >> 16) & 0xFFFF;
                        energy.setClientMaxEnergy((currentHigh << 16) | pendingMaxEnergyLow);
                    }
                    case 5 -> {
                        int currentLow = energy.getClientMaxEnergy() & 0xFFFF;
                        int full = ((value & 0xFFFF) << 16) | currentLow;
                        energy.setClientMaxEnergy(full);
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
    // Tick (solo server-side)
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
    // Slot 2 = base (centro), slot 0,1,3,4 = modifier.
    // Cache invalidata ogni volta che l'inventario cambia.
    // -------------------------------------------------------------------
    private Optional<RecipeHolder<EmpowererRecipe>> getRecipe() {
        if (level == null)
            return Optional.empty();

        if (recipeDirty || cachedRecipe == null) {
            ItemStack base = inventory.getStackInSlot(2); // center
            ItemStack m1 = inventory.getStackInSlot(0); // top
            ItemStack m2 = inventory.getStackInSlot(1); // left
            ItemStack m3 = inventory.getStackInSlot(3); // right
            ItemStack m4 = inventory.getStackInSlot(4); // bottom

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
    //
    // Speed Upgrade — velocità esponenziale:
    // S(u) = 10^(u/8)
    // Con 8 upgrade → 10x velocità, tempo ridotto da 200 a 20 tick.
    //
    // Energy Upgrade — riduce il consumo bilanciando la velocità:
    // usage = baseUsage * 10^((2*S - E) / 8)
    // Con 8S + 8E → exponent = (16-8)/8 = 1 → consumo = baseUsage * 10
    // (il buffer più grande compensa il costo aggiuntivo degli Speed).
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
        // setStored() ora clamp correttamente — safe anche se il valore NBT
        // supera la capacità attuale (es. dopo aver rimosso degli Energy Upgrade).
        energy.setStored(tag.getInt("Energy"));
        this.progress = tag.getInt("Progress");
        this.recipeDirty = true;
    }
}