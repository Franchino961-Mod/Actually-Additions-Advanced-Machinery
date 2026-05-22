package com.advancedmachinery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Classe base per gli upgrade dell'Advanced Empowerer.
 * Mostra automaticamente un tooltip tradotto quando il giocatore
 * passa il mouse sull'oggetto nell'inventario.
 *
 * La chiave di traduzione viene letta dai file lang (en_us.json, it_it.json)
 * e visualizzata in grigio sotto il nome dell'oggetto.
 */
public class UpgradeItem extends Item {

    private final String tooltipKey;

    public UpgradeItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.translatable(this.tooltipKey)
                        .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}