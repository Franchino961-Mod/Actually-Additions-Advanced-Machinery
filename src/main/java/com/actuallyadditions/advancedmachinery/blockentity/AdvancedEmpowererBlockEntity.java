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
import net.minecraft.world.SimpleContainer;
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
    public static final int ENERGY_CAPACITY = 2000000;
    public static final int BASE_SPEED = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 6) return stack.getItem() == ModItems.SPEED_UPGRADE.get();
            if (slot == 7) return stack.getItem() == ModItems.EFFICIENCY_UPGRADE.get();
            return super.isItemValid(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= 6) return 4;
            return super.getSlotLimit(slot);
        }
    };

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY);

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public EnergyStorage getEnergyStorage() {
        return energy;
    }
    
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = BASE_SPEED;

    public AdvancedEmpowererBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_EMPOWERER.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> AdvancedEmpowererBlockEntity.this.progress;
                    case 1 -> AdvancedEmpowererBlockEntity.this.maxProgress;
                    case 2 -> AdvancedEmpowererBlockEntity.this.energy.getEnergyStored();
                    case 3 -> AdvancedEmpowererBlockEntity.this.energy.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> AdvancedEmpowererBlockEntity.this.progress = value;
                    case 1 -> AdvancedEmpowererBlockEntity.this.maxProgress = value;
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
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AdvancedEmpowererMenu(id, inventory, this, this.data);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        Optional<RecipeHolder<EmpowererRecipe>> recipe = getRecipe();
        if (recipe.isPresent()) {
            EmpowererRecipe r = recipe.get().value();
            this.maxProgress = getModifiedTime(r.getTime());
            int energyPerTick = getModifiedEnergy(r.getEnergyPerStand() * 4) / this.maxProgress;

            if (this.energy.getEnergyStored() >= energyPerTick) {
                this.energy.extractEnergy(energyPerTick, false);
                this.progress++;

                if (this.progress >= this.maxProgress) {
                    craftItem(r);
                    this.progress = 0;
                }
            }
        } else {
            this.progress = 0;
        }
    }

    private Optional<RecipeHolder<EmpowererRecipe>> getRecipe() {
        if (level == null) return Optional.empty();
        
        return level.getRecipeManager().getAllRecipesFor(ActuallyRecipes.Types.EMPOWERING.get())
                .stream()
                .filter(r -> r.value().matches(
                        inventory.getStackInSlot(0),
                        inventory.getStackInSlot(1),
                        inventory.getStackInSlot(2),
                        inventory.getStackInSlot(3),
                        inventory.getStackInSlot(4)
                ))
                .findFirst();
    }

    private void craftItem(EmpowererRecipe recipe) {
        ItemStack result = recipe.getOutput().copy();
        ItemStack currentOutput = inventory.getStackInSlot(5);

        if (currentOutput.isEmpty() || (ItemStack.isSameItem(currentOutput, result) && currentOutput.getCount() + result.getCount() <= result.getMaxStackSize())) {
            for (int i = 0; i < 5; i++) {
                inventory.extractItem(i, 1, false);
            }
            inventory.insertItem(5, result, false);
        }
    }

    private int getModifiedTime(int originalTime) {
        int speedUpgrades = getUpgradeCount(ModItems.SPEED_UPGRADE.get());
        return Math.max(20, originalTime - (speedUpgrades * 40));
    }

    private int getModifiedEnergy(int originalEnergy) {
        int efficiencyUpgrades = getUpgradeCount(ModItems.EFFICIENCY_UPGRADE.get());
        return Math.max(originalEnergy / 4, originalEnergy - (efficiencyUpgrades * (originalEnergy / 10)));
    }

    private int getUpgradeCount(net.minecraft.world.item.Item upgrade) {
        if (upgrade == ModItems.SPEED_UPGRADE.get()) return inventory.getStackInSlot(6).getCount();
        if (upgrade == ModItems.EFFICIENCY_UPGRADE.get()) return inventory.getStackInSlot(7).getCount();
        return 0;
    }

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
        energy.receiveEnergy(tag.getInt("Energy"), false);
        this.progress = tag.getInt("Progress");
    }
}
