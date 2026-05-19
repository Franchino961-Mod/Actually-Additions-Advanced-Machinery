package com.actuallyadditions.advancedmachinery.client;

import com.actuallyadditions.advancedmachinery.AdvancedMachinery;
import com.actuallyadditions.advancedmachinery.menu.AdvancedEmpowererMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedEmpowererScreen extends AbstractContainerScreen<AdvancedEmpowererMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AdvancedMachinery.MODID, "textures/gui/advanced_empowerer.png");

    // GUI dimensions — 5 righe macchina + gap + inventario player
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 204;

    // Progress arrow: tra Centro (44,54) e Output (116,54)
    private static final int ARROW_X      = 62;  // bordo sinistro freccia
    private static final int ARROW_Y      = 56;  // top (allineata allo slot y=54)
    private static final int ARROW_WIDTH  = 50;  // larghezza massima
    private static final int ARROW_HEIGHT = 14;
    private static final int ARROW_U      = 176;
    private static final int ARROW_V      = 0;

    // Energy bar: colonna 8 intera (x=152, righe 0-4, totale 90px)
    private static final int ENERGY_X      = 152;
    private static final int ENERGY_Y_TOP  = 18;
    private static final int ENERGY_HEIGHT = 90;
    private static final int ENERGY_WIDTH  = 16;
    private static final int ENERGY_U      = 176;
    private static final int ENERGY_V      = 17;

    public AdvancedEmpowererScreen(AdvancedEmpowererMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 112; // 10px sopra y=122 (inizio inventario player)
    }


    @Override
    protected void init() {
        super.init();
        // Forza l'altezza della GUI dopo che AbstractContainerScreen la inizializza
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Draw base GUI background
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, leftPos, topPos);
        renderEnergyBar(guiGraphics, leftPos, topPos);
    }

    /**
     * Renders the progress arrow, filling left-to-right proportionally to progress.
     */
    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getMaxProgress() <= 0)
            return;
        int filled = (int) ((float) menu.getProgress() / menu.getMaxProgress() * ARROW_WIDTH);
        if (filled <= 0)
            return;
        guiGraphics.blit(TEXTURE,
                x + ARROW_X, y + ARROW_Y, // destination on screen
                ARROW_U, ARROW_V, // source UV in texture
                filled, ARROW_HEIGHT); // width/height to draw
    }

    /**
     * Renders the energy bar, filling bottom-to-top proportionally to stored
     * energy.
     */
    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getMaxEnergy() <= 0)
            return;
        int filled = (int) ((float) menu.getEnergy() / menu.getMaxEnergy() * ENERGY_HEIGHT);
        if (filled <= 0)
            return;
        int yOffset = ENERGY_HEIGHT - filled; // how many pixels from top are empty
        guiGraphics.blit(TEXTURE,
                x + ENERGY_X, y + ENERGY_Y_TOP + yOffset, // destination
                ENERGY_U, ENERGY_V + yOffset, // source UV (shift down to match)
                ENERGY_WIDTH, filled);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title centered, white
        int titleWidth = this.font.width(this.title);
        guiGraphics.drawString(this.font, this.title,
                (this.imageWidth - titleWidth) / 2,
                this.titleLabelY,
                0xFFFFFF, false);
        // "Inventory" label suppressed (clean look)
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
    }

    /** Shows "X / Y FE" tooltip when hovering the energy bar. */
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int barLeft = x + ENERGY_X;
        int barRight = barLeft + ENERGY_WIDTH;
        int barTop = y + ENERGY_Y_TOP;
        int barBottom = barTop + ENERGY_HEIGHT;

        if (mouseX >= barLeft && mouseX <= barRight
                && mouseY >= barTop && mouseY <= barBottom) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " FE"),
                    mouseX, mouseY);
        }
    }
}