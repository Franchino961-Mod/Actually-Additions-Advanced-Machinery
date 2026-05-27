package com.advancedmachinery;

import com.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import com.advancedmachinery.client.AdvancedEmpowererScreen;
import com.advancedmachinery.registration.*;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AdvancedMachinery.MODID)
public class AdvancedMachinery {
    public static final String MODID = "advancedmachinery";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public AdvancedMachinery(IEventBus modEventBus, Dist dist) {
        LOGGER.info("Advanced Machinery initializing...");

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerNetworking);

        // Registrazione della GUI screen direttamente sul modEventBus.
        // Questo è il pattern moderno raccomandato in NeoForge 1.21.1,
        // che evita l'uso di @EventBusSubscriber con bus=Bus.MOD (deprecato).
        // Il controllo dist == Dist.CLIENT assicura che la registrazione
        // avvenga solo lato client — lato server la classe AdvancedEmpowererScreen
        // non esiste e non deve essere caricata.
        if (dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
        }

        LOGGER.info("Advanced Machinery registration complete!");
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ADVANCED_EMPOWERER.get(), AdvancedEmpowererScreen::new);
    }

    private void registerNetworking(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(
            com.advancedmachinery.network.ToggleAutoSettingPayload.TYPE,
            com.advancedmachinery.network.ToggleAutoSettingPayload.STREAM_CODEC,
            com.advancedmachinery.network.ToggleAutoSettingPayload::handle
        );
    }

    /**
     * Registra le capability con un wrapper "sided" per l'inventario.
     *
     * Regole di accesso per lato:
     * NULL / UP / laterali → slot input 0-4 (inserimento ingredienti)
     * DOWN → slot output 5 (solo estrazione prodotto)
     * Slot upgrade 6-7 → mai accessibili dall'automazione esterna
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
     * Layout slot:
     * 0-4 → input (modifier 1-4 + base)
     * 5 → output (sola estrazione)
     * 6-7 → upgrade (non automatizzabili)
     */
    private static IItemHandler getSidedInventory(AdvancedEmpowererBlockEntity be, Direction side) {
        if (side == null) {
            return be.getExternalItemHandler();
        }

        return be.getSidedItemHandler(side);
    }
}