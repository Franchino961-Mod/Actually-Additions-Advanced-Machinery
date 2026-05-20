package com.actuallyadditions.advancedmachinery.client.jei;

import com.actuallyadditions.advancedmachinery.AdvancedMachinery;
import com.actuallyadditions.advancedmachinery.menu.AdvancedEmpowererMenu;
import com.actuallyadditions.advancedmachinery.registration.ModBlocks;
import com.actuallyadditions.advancedmachinery.registration.ModMenuTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class AdvancedMachineryJEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.parse(AdvancedMachinery.MODID + ":jei_plugin");

    // "empowering" è l'ID usato da AA (corrisponde a
    // ActuallyRecipes.Types.EMPOWERING)
    private static final RecipeType<Object> TYPE_EMPOWERING = RecipeType.create("actuallyadditions", "empowering",
            Object.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                ModBlocks.ADVANCED_EMPOWERER.get().asItem().getDefaultInstance(),
                TYPE_EMPOWERING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Slot macchina: 8 slot totali (indici 0–7: 5 input, 1 output, 2 upgrade)
        // Inventario player: 36 slot (indici 8–43: 27 main + 9 hotbar)
        registration.addRecipeTransferHandler(
                AdvancedEmpowererMenu.class,
                ModMenuTypes.ADVANCED_EMPOWERER.get(),
                TYPE_EMPOWERING,
                0, 8, // slot macchina: da indice 0, count 8
                8, 36); // inventario player: da indice 8, count 36
    }
}