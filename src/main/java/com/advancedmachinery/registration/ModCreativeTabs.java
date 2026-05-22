package com.advancedmachinery.registration;

import com.advancedmachinery.AdvancedMachinery;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdvancedMachinery.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.advancedmachinery"))
            .icon(() -> ModItems.ADVANCED_EMPOWERER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ADVANCED_EMPOWERER.get());
                output.accept(ModItems.SPEED_UPGRADE.get());
                output.accept(ModItems.EFFICIENCY_UPGRADE.get());
            })
            .build());
}
