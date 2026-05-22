package com.actuallyadditions.advancedmachinery.client.jei;

import com.actuallyadditions.advancedmachinery.AdvancedMachinery;
import com.actuallyadditions.advancedmachinery.menu.AdvancedEmpowererMenu;
import com.actuallyadditions.advancedmachinery.registration.ModBlocks;
import com.actuallyadditions.advancedmachinery.registration.ModMenuTypes;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class AdvancedMachineryJEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.parse(AdvancedMachinery.MODID + ":jei_plugin");

    // Usiamo la stessa istanza RecipeType registrata da AA nel suo JEI plugin.
    // Dalla decompilazione del JAR di AA, il tipo viene creato con:
    //   RecipeType.create("actuallyadditions", "empowerer", EmpowererRecipe.class)
    // e viene esposto come campo pubblico statico "EMPOWERER" in JEIActuallyAdditionsPlugin.
    // Referenziare questo campo garantisce che JEI usi lo stesso oggetto singleton
    // e colleghi correttamente il nostro catalizzatore alle ricette di AA.
    private static RecipeType<EmpowererRecipe> getEmpowererType() {
        return JEIActuallyAdditionsPlugin.EMPOWERER;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Registra l'Advanced Empowerer come catalizzatore per le ricette
        // actuallyadditions:empowerer — JEI mostrerà il nostro blocco
        // nel tooltip "Can be made in:" insieme all'Empowerer classico.
        registration.addRecipeCatalyst(
                ModBlocks.ADVANCED_EMPOWERER.get().asItem().getDefaultInstance(),
                getEmpowererType());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Slot macchina: 8 slot totali (indici 0–7: 5 input, 1 output, 2 upgrade)
        // Inventario player: 36 slot (indici 8–43: 27 main + 9 hotbar)
        registration.addRecipeTransferHandler(
                AdvancedEmpowererMenu.class,
                ModMenuTypes.ADVANCED_EMPOWERER.get(),
                getEmpowererType(),
                0, 8, // slot macchina: da indice 0, count 8
                8, 36); // inventario player: da indice 8, count 36
    }
}