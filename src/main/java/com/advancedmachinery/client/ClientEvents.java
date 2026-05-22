package com.actuallyadditions.advancedmachinery.client;

import com.actuallyadditions.advancedmachinery.AdvancedMachinery;
import com.actuallyadditions.advancedmachinery.registration.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AdvancedMachinery.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ADVANCED_EMPOWERER.get(), AdvancedEmpowererScreen::new);
    }
}
