package com.actuallyadditions.advancedmachinery;

import com.actuallyadditions.advancedmachinery.registration.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ADVANCED_EMPOWERER.get(), (be, side) -> be.getInventory());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ADVANCED_EMPOWERER.get(), (be, side) -> be.getEnergyStorage());
    }
}