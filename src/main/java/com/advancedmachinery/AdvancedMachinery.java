package com.advancedmachinery;

import com.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.advancedmachinery.registration.*;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AdvancedMachinery.MODID)
public class AdvancedMachinery {
    public static final String MODID = "advancedmachinery";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public AdvancedMachinery(IEventBus modEventBus) {
        LOGGER.info("Advanced Machinery initializing...");

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);

        LOGGER.info("Advanced Machinery registration complete!");
    }

    /**
     * Registra le capability con un wrapper "sided" per l'inventario.
     *
     * FIX: prima tutti gli 8 slot erano esposti da ogni lato, permettendo
     * a tramogge e pipe di rubare gli upgrade (slot 6-7) o gli ingredienti
     * a metà lavorazione. Ora le regole sono:
     *
     * NULL (automazione generica, es. pipe mod) → solo slot input (0-4)
     * DOWN (tramoggia sotto) → solo slot output (5)
     * UP / laterali → solo slot input (0-4)
     *
     * Gli slot upgrade (6-7) non sono mai accessibili dall'automazione.
     * L'energia rimane accessibile da tutti i lati (comportamento standard FE).
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ADVANCED_EMPOWERER.get(),
                (be, side) -> getSidedInventory(be, side));

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ADVANCED_EMPOWERER.get(),
                (be, side) -> be.getEnergyStorage());
    }

    /**
     * Restituisce una vista dell'inventario limitata al range di slot
     * appropriato per la direzione richiesta.
     *
     * Layout slot dell'inventario:
     * 0-4 → input (modifier 1-4 + base)
     * 5 → output (sola estrazione)
     * 6-7 → upgrade (non automatizzabili)
     *
     * @param be   la BlockEntity
     * @param side la direzione da cui arriva l'accesso (null = nessuna direzione
     *             specifica)
     * @return un IItemHandler che espone solo i slot consentiti per quel lato
     */
    private static IItemHandler getSidedInventory(AdvancedEmpowererBlockEntity be, Direction side) {
        ItemStackHandler inv = be.getInventory();

        if (side == Direction.DOWN) {
            // Da sotto: solo estrazione dal slot di output (slot 5)
            return new RangedWrapper(inv, 5, 6);
        }

        // Da sopra, lati, o null (pipe/conduit generici):
        // solo inserimento negli slot di input (slot 0-4).
        // Nota: RangedWrapper non blocca l'estrazione — ma isItemValid(5)
        // restituisce false per output, quindi l'insert è già bloccato
        // dalla BlockEntity; l'estrazione dai slot 0-4 è consentita
        // (utile per sistemi che rifiutano l'item e lo reimmettono).
        return new RangedWrapper(inv, 0, 5);
    }
}