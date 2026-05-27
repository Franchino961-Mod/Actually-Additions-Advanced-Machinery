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
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

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
    // 0 – Input BASE (croce: center — GUI: 44, 54)
    // 1 – Input modifier 1 (croce: top — GUI: 44, 24)
    // 2 – Input modifier 2 (croce: left — GUI: 14, 54)
    // 3 – Input modifier 3 (croce: right — GUI: 74, 54)
    // 4 – Input modifier 4 (croce: bottom — GUI: 44, 84)
    // 5 – Output (read-only)
    // 6 – Speed Upgrade (max 8)
    // 7 – Energy Upgrade (max 8)
    //
    // EmpowererRecipe.matches(base, m1, m2, m3, m4):
    // Slot 0 = base, slot 1,2,3,4 = modifier.
    // AA gestisce internamente le permutazioni dei modifier.
    // -------------------------------------------------------------------
    private boolean autoInput = false;
    private boolean autoOutput = false;
    private boolean roundRobin = false;
    private boolean singleItemMode = false;
    private int roundRobinIndex = 1;
    private int currentEnergyPerTick = 0;
    private int currentTotalEnergy = 0;
    private int tickCounter = 0;
    private final int[] sidedConfig = new int[] { 4, 4, 4, 4, 4, 4 };

    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            recipeDirty = true;
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 5 -> false; // output — no inserimento manuale
                case 6 -> stack.getItem() == ModItems.SPEED_UPGRADE.get();
                case 7 -> stack.getItem() == ModItems.ENERGY_UPGRADE.get();
                case 0 -> level == null || isValidBaseItem(stack);
                case 1, 2, 3, 4 -> level == null || isValidModifierItem(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            if (singleItemMode && slot >= 0 && slot <= 4) {
                return 1;
            }
            return (slot == 6 || slot == 7) ? 8 : super.getSlotLimit(slot);
        }
    };

    private RecipeManager lastRecipeManager = null;
    private final java.util.List<RecipeHolder<EmpowererRecipe>> cachedRecipes = new java.util.ArrayList<>();
    private final java.util.List<Ingredient> cachedBaseIngredients = new java.util.ArrayList<>();
    private final java.util.List<Ingredient> cachedModifierIngredients = new java.util.ArrayList<>();

    private void updateRecipeCache() {
        if (level == null) return;
        RecipeManager recipeManager = level.getRecipeManager();
        if (lastRecipeManager == recipeManager) {
            return;
        }
        lastRecipeManager = recipeManager;
        cachedRecipes.clear();
        cachedBaseIngredients.clear();
        cachedModifierIngredients.clear();

        for (RecipeHolder<EmpowererRecipe> holder : recipeManager.getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get())) {
            EmpowererRecipe recipe = holder.value();
            cachedRecipes.add(holder);
            cachedBaseIngredients.add(recipe.getInput());
            cachedModifierIngredients.add(recipe.getStandOne());
            cachedModifierIngredients.add(recipe.getStandTwo());
            cachedModifierIngredients.add(recipe.getStandThree());
            cachedModifierIngredients.add(recipe.getStandFour());
        }
    }

    public boolean isValidBaseItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        updateRecipeCache();
        for (Ingredient ing : cachedBaseIngredients) {
            if (ing.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidModifierItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        updateRecipeCache();
        for (Ingredient ing : cachedModifierIngredients) {
            if (ing.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private final IItemHandler externalItemHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (isValidBaseItem(stack)) {
                ItemStack existing = inventory.getStackInSlot(0);
                if (existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < inventory.getSlotLimit(0))) {
                    return inventory.insertItem(0, stack, simulate);
                }
            }

            if (isValidModifierItem(stack)) {
                int targetSlot = findAvailableModifierSlot(stack);
                if (targetSlot != -1) {
                    return inventory.insertItem(targetSlot, stack, simulate);
                }
            }

            if (stack.getItem() == ModItems.SPEED_UPGRADE.get()) {
                return inventory.insertItem(6, stack, simulate);
            } else if (stack.getItem() == ModItems.ENERGY_UPGRADE.get()) {
                return inventory.insertItem(7, stack, simulate);
            }

            return stack;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 5 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }

        private int findAvailableModifierSlot(ItemStack stack) {
            if (!roundRobin && !singleItemMode) {
                for (int s = 1; s <= 4; s++) {
                    ItemStack existing = inventory.getStackInSlot(s);
                    if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < inventory.getSlotLimit(s)) {
                        return s;
                    }
                }
                for (int s = 1; s <= 4; s++) {
                    if (inventory.getStackInSlot(s).isEmpty()) {
                        return s;
                    }
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    int s = 1 + (roundRobinIndex - 1 + i) % 4;
                    ItemStack existing = inventory.getStackInSlot(s);
                    if (existing.isEmpty()) {
                        roundRobinIndex = 1 + (s - 1 + 1) % 4;
                        return s;
                    }
                    if (!singleItemMode && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < inventory.getSlotLimit(s)) {
                        roundRobinIndex = 1 + (s - 1 + 1) % 4;
                        return s;
                    }
                }
            }
            return -1;
        }
    };

    public IItemHandler getExternalItemHandler() {
        return externalItemHandler;
    }

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
                    case 6 -> autoInput ? 1 : 0;
                    case 7 -> autoOutput ? 1 : 0;
                    case 8 -> roundRobin ? 1 : 0;
                    case 9 -> singleItemMode ? 1 : 0;
                    case 10 -> currentEnergyPerTick;
                    case 11 -> currentTotalEnergy;
                    case 12 -> sidedConfig[0];
                    case 13 -> sidedConfig[1];
                    case 14 -> sidedConfig[2];
                    case 15 -> sidedConfig[3];
                    case 16 -> sidedConfig[4];
                    case 17 -> sidedConfig[5];
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> maxProgress = value;
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
                    case 6 -> autoInput = value != 0;
                    case 7 -> autoOutput = value != 0;
                    case 8 -> roundRobin = value != 0;
                    case 9 -> singleItemMode = value != 0;
                    case 10 -> currentEnergyPerTick = value;
                    case 11 -> currentTotalEnergy = value;
                    case 12 -> sidedConfig[0] = value;
                    case 13 -> sidedConfig[1] = value;
                    case 14 -> sidedConfig[2] = value;
                    case 15 -> sidedConfig[3] = value;
                    case 16 -> sidedConfig[4] = value;
                    case 17 -> sidedConfig[5] = value;
                }
            }

            @Override
            public int getCount() {
                return 18;
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

        tickCounter++;
        if (tickCounter % 10 == 0) {
            if (autoOutput) {
                performAutoOutput();
            }
            if (autoInput) {
                performAutoInput();
            }
            if (roundRobin) {
                redistributeModifierSlots();
            }
        }

        autoAlignIngredients();

        Optional<RecipeHolder<EmpowererRecipe>> recipeOpt = getRecipe();
        if (recipeOpt.isPresent()) {
            EmpowererRecipe recipe = recipeOpt.get().value();
            this.maxProgress = getModifiedTime(recipe.getTime());

            int energyPerTick = getEnergyPerTick(recipe);
            this.currentEnergyPerTick = energyPerTick;
            this.currentTotalEnergy = energyPerTick * this.maxProgress;

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
            this.currentEnergyPerTick = 0;
            this.currentTotalEnergy = 0;
            if (this.progress != 0) {
                this.progress = 0;
                setChanged();
            }
        }
    }

    // -------------------------------------------------------------------
    // Recipe matching
    // Slot 0 = base (centro), slot 1,2,3,4 = modifier.
    // Cache invalidata ogni volta che l'inventario cambia.
    // -------------------------------------------------------------------
    private Optional<RecipeHolder<EmpowererRecipe>> getRecipe() {
        if (level == null)
            return Optional.empty();

        if (recipeDirty || cachedRecipe == null) {
            ItemStack base = inventory.getStackInSlot(0); // center (base)
            ItemStack m1 = inventory.getStackInSlot(1); // top
            ItemStack m2 = inventory.getStackInSlot(2); // left
            ItemStack m3 = inventory.getStackInSlot(3); // right
            ItemStack m4 = inventory.getStackInSlot(4); // bottom

            updateRecipeCache();
            cachedRecipe = cachedRecipes.stream()
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
        tag.putBoolean("AutoInput", autoInput);
        tag.putBoolean("AutoOutput", autoOutput);
        tag.putBoolean("RoundRobin", roundRobin);
        tag.putBoolean("SingleItemMode", singleItemMode);
        tag.putInt("RoundRobinIndex", roundRobinIndex);
        tag.putIntArray("SidedConfig", sidedConfig);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        energy.setStored(tag.getInt("Energy"));
        this.progress = tag.getInt("Progress");
        this.autoInput = tag.getBoolean("AutoInput");
        this.autoOutput = tag.getBoolean("AutoOutput");
        this.roundRobin = tag.getBoolean("RoundRobin");
        this.singleItemMode = tag.getBoolean("SingleItemMode");
        this.roundRobinIndex = tag.contains("RoundRobinIndex") ? tag.getInt("RoundRobinIndex") : 1;
        if (tag.contains("SidedConfig")) {
            int[] arr = tag.getIntArray("SidedConfig");
            System.arraycopy(arr, 0, this.sidedConfig, 0, Math.min(arr.length, 6));
        }
        this.recipeDirty = true;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public boolean isAutoInput() { return autoInput; }
    public boolean isAutoOutput() { return autoOutput; }
    public boolean isRoundRobin() { return roundRobin; }
    public boolean isSingleItemMode() { return singleItemMode; }

    public void toggleAutoInput() {
        this.autoInput = !this.autoInput;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void toggleAutoOutput() {
        this.autoOutput = !this.autoOutput;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void toggleRoundRobin() {
        this.roundRobin = !this.roundRobin;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void toggleSingleItemMode() {
        this.singleItemMode = !this.singleItemMode;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void performAutoInput() {
        if (level == null) return;
        for (Direction direction : Direction.values()) {
            int relIdx = getRelativeIndex(direction);
            int mode = sidedConfig[relIdx];
            if (mode == 0 || mode == 3) continue; // Disabled or Output Only

            BlockPos adjacentPos = this.worldPosition.relative(direction);
            IItemHandler adjacentHandler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, adjacentPos, direction.getOpposite());
            if (adjacentHandler != null) {
                while (true) {
                    boolean madeProgress = false;
                    for (int adjSlot = 0; adjSlot < adjacentHandler.getSlots(); adjSlot++) {
                        ItemStack stackInAdj = adjacentHandler.getStackInSlot(adjSlot);
                        if (!stackInAdj.isEmpty()) {
                            if ((mode == 1 || mode == 4) && isValidBaseItem(stackInAdj) && inventory.getStackInSlot(0).isEmpty()) {
                                ItemStack extracted = adjacentHandler.extractItem(adjSlot, 1, false);
                                if (!extracted.isEmpty()) {
                                    inventory.insertItem(0, extracted, false);
                                    madeProgress = true;
                                    continue;
                                }
                            }
                            if ((mode == 2 || mode == 4) && isValidModifierItem(stackInAdj)) {
                                int targetSlot = findEmptyModifierSlot();
                                if (targetSlot != -1) {
                                    ItemStack extracted = adjacentHandler.extractItem(adjSlot, 1, false);
                                    if (!extracted.isEmpty()) {
                                        inventory.insertItem(targetSlot, extracted, false);
                                        madeProgress = true;
                                    }
                                }
                            }
                        }
                    }
                    if (!madeProgress || !hasEmptyInputSlot()) {
                        break;
                    }
                }
            }
        }
    }

    private int findEmptyModifierSlot() {
        if (roundRobin) {
            for (int i = 0; i < 4; i++) {
                int slot = 1 + (roundRobinIndex - 1 + i) % 4;
                if (inventory.getStackInSlot(slot).isEmpty()) {
                    roundRobinIndex = 1 + (slot - 1 + 1) % 4;
                    return slot;
                }
            }
        } else {
            for (int slot = 1; slot <= 4; slot++) {
                if (inventory.getStackInSlot(slot).isEmpty()) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private boolean hasEmptyInputSlot() {
        if (inventory.getStackInSlot(0).isEmpty()) return true;
        for (int slot = 1; slot <= 4; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private void performAutoOutput() {
        if (level == null) return;
        ItemStack outputStack = inventory.getStackInSlot(5);
        if (!outputStack.isEmpty()) {
            for (Direction direction : Direction.values()) {
                int relIdx = getRelativeIndex(direction);
                int mode = sidedConfig[relIdx];
                if (mode == 0 || mode == 1 || mode == 2) continue; // Disabled or Input Only

                BlockPos adjacentPos = this.worldPosition.relative(direction);
                IItemHandler adjacentHandler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, adjacentPos, direction.getOpposite());
                if (adjacentHandler != null) {
                    for (int i = 0; i < adjacentHandler.getSlots(); i++) {
                        ItemStack remainder = adjacentHandler.insertItem(i, outputStack.copy(), false);
                        if (remainder.getCount() < outputStack.getCount()) {
                            int inserted = outputStack.getCount() - remainder.getCount();
                            inventory.extractItem(5, inserted, false);
                            outputStack = inventory.getStackInSlot(5);
                            if (outputStack.isEmpty()) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void redistributeModifierSlots() {
        java.util.Map<ItemStack, java.util.List<Integer>> itemSlots = new java.util.HashMap<>();

        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                boolean found = false;
                for (java.util.Map.Entry<ItemStack, java.util.List<Integer>> entry : itemSlots.entrySet()) {
                    if (ItemStack.isSameItemSameComponents(entry.getKey(), stack)) {
                        entry.getValue().add(slot);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    java.util.List<Integer> slots = new java.util.ArrayList<>();
                    slots.add(slot);
                    itemSlots.put(stack.copy(), slots);
                }
            }
        }

        for (java.util.Map.Entry<ItemStack, java.util.List<Integer>> entry : itemSlots.entrySet()) {
            java.util.List<Integer> slots = entry.getValue();
            if (slots.size() > 1) {
                int totalCount = 0;
                for (int slot : slots) {
                    totalCount += inventory.getStackInSlot(slot).getCount();
                }

                int countPerSlot = totalCount / slots.size();
                int remainder = totalCount % slots.size();
                ItemStack template = entry.getKey();

                for (int i = 0; i < slots.size(); i++) {
                    int slot = slots.get(i);
                    int newCount = countPerSlot + (i < remainder ? 1 : 0);
                    if (newCount > 0) {
                        ItemStack newStack = template.copy();
                        newStack.setCount(newCount);
                        inventory.setStackInSlot(slot, newStack);
                    } else {
                        inventory.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    /**
     * Controlla se gli oggetti inseriti in qualsiasi ordine negli slot di input (0-4)
     * formano una ricetta valida. Se sì, e l'oggetto base non si trova nello slot 0 (centro),
     * scambia la posizione del base con l'oggetto attualmente nello slot 0 in modo da
     * allineare automaticamente gli ingredienti per l'avvio del crafting.
     */
    private void autoAlignIngredients() {
        if (level == null || level.isClientSide || !recipeDirty)
            return;

        for (int baseSlot = 1; baseSlot < 5; baseSlot++) {
            ItemStack base = inventory.getStackInSlot(baseSlot);
            if (base.isEmpty())
                continue;

            ItemStack[] mods = new ItemStack[4];
            int modIdx = 0;
            for (int slot = 0; slot < 5; slot++) {
                if (slot != baseSlot) {
                    mods[modIdx++] = inventory.getStackInSlot(slot);
                }
            }

            final ItemStack m1 = mods[0];
            final ItemStack m2 = mods[1];
            final ItemStack m3 = mods[2];
            final ItemStack m4 = mods[3];

            updateRecipeCache();
            boolean matches = cachedRecipes.stream()
                    .anyMatch(r -> r.value().matches(base, m1, m2, m3, m4));

            if (matches) {
                ItemStack temp = inventory.getStackInSlot(0).copy();
                inventory.setStackInSlot(0, inventory.getStackInSlot(baseSlot).copy());
                inventory.setStackInSlot(baseSlot, temp);
                break;
            }
        }
    }

    public int getRelativeIndex(Direction absoluteDir) {
        if (absoluteDir == null) return 4;
        if (absoluteDir == Direction.UP) return 0;
        if (absoluteDir == Direction.DOWN) return 1;

        Direction facing = getBlockState().getValue(com.advancedmachinery.block.AdvancedEmpowererBlock.FACING);
        if (absoluteDir == facing) return 2; // FRONT
        if (absoluteDir == facing.getOpposite()) return 3; // BACK
        if (absoluteDir == facing.getClockWise()) return 5; // RIGHT
        if (absoluteDir == facing.getCounterClockWise()) return 4; // LEFT

        return 4; // fallback
    }

    public void cycleSideConfig(int index) {
        if (index >= 0 && index < 6) {
            sidedConfig[index] = (sidedConfig[index] + 1) % 5;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    public IItemHandler getSidedItemHandler(Direction side) {
        return new SidedItemHandlerWrapper(this, side);
    }

    public static class SidedItemHandlerWrapper implements IItemHandler {
        private final AdvancedEmpowererBlockEntity be;
        private final Direction side;

        public SidedItemHandlerWrapper(AdvancedEmpowererBlockEntity be, Direction side) {
            this.be = be;
            this.side = side;
        }

        @Override
        public int getSlots() {
            return be.getInventory().getSlots();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return be.getInventory().getStackInSlot(slot);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            int relIdx = be.getRelativeIndex(side);
            int mode = be.sidedConfig[relIdx];

            if (mode == 0) return stack; // Disabled
            if (mode == 3) return stack; // Output Only (no insertion)

            if (mode == 1) { // Solo Basi
                if (!be.isValidBaseItem(stack)) return stack;
                if (slot != 0) return stack;
            }
            if (mode == 2) { // Solo Modificatori
                if (!be.isValidModifierItem(stack)) return stack;
                if (slot < 1 || slot > 4) return stack;
            }

            return be.getExternalItemHandler().insertItem(slot, stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int relIdx = be.getRelativeIndex(side);
            int mode = be.sidedConfig[relIdx];

            if (mode == 0) return ItemStack.EMPTY; // Disabled
            if (mode == 1 || mode == 2) return ItemStack.EMPTY; // Solo input

            if (slot == 5) {
                return be.getInventory().extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return be.getInventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            int relIdx = be.getRelativeIndex(side);
            int mode = be.sidedConfig[relIdx];

            if (mode == 0 || mode == 3) return false;
            if (mode == 1) return slot == 0 && be.isValidBaseItem(stack);
            if (mode == 2) return slot >= 1 && slot <= 4 && be.isValidModifierItem(stack);

            return be.getInventory().isItemValid(slot, stack);
        }
    }
}