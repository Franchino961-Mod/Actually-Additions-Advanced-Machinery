package com.advancedmachinery.registration;

import com.advancedmachinery.AdvancedMachinery;
import com.advancedmachinery.menu.AdvancedEmpowererMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
            AdvancedMachinery.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedEmpowererMenu>> ADVANCED_EMPOWERER = MENUS
            .register("advanced_empowerer", () -> IMenuTypeExtension.create(AdvancedEmpowererMenu::new));
}
