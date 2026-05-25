package com.advancedmachinery.client;

import com.advancedmachinery.AdvancedMachinery;
import com.advancedmachinery.menu.AdvancedEmpowererMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedEmpowererScreen extends AbstractContainerScreen<AdvancedEmpowererMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AdvancedMachinery.MODID, "textures/gui/advanced_empowerer.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 204;

    // Freccia di progresso:
    // La zona "vuota" (background bianco) è a x=90..124, y=54..70 nel GUI.
    // Sprite attivo (arancione, U=176 V=0): 22×16 px — freccia → con shaft + punta.
    // ARROW_X allineato al bordo sinistro della zona vuota (dopo slot 3 a
    // x=74+16=90).
    // ARROW_Y allineato al bordo superiore della zona vuota (top degli slot = 54).
    private static final int ARROW_X = 95; // bordo sinistro zona freccia
    private static final int ARROW_Y = 54; // bordo superiore zona freccia
    private static final int ARROW_WIDTH = 22; // larghezza sprite aggiornata
    private static final int ARROW_HEIGHT = 16; // altezza sprite aggiornata
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 0;

    // Energy bar: colonna destra (x=152)
    // Dimensioni identiche ad Actually Additions (EnergyDisplay):
    // background totale: W=18px, H=85px (x=151..168, y=17..101)
    // area fill interna: W=16px, H=83px (1px border su tutti i lati)
    // formula fill: filled = energy * 83 / maxEnergy (come AA)
    private static final int ENERGY_X = 152;
    private static final int ENERGY_Y_TOP = 18;
    private static final int ENERGY_HEIGHT = 83; // =85px totale -1px top border -1px bottom border
    private static final int ENERGY_WIDTH = 16;
    private static final int ENERGY_U = 176;
    private static final int ENERGY_V = 17;

    public AdvancedEmpowererScreen(AdvancedEmpowererMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 112;
    }

    @Override
    protected void init() {
        super.init();
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, leftPos, topPos);
        renderEnergyBar(guiGraphics, leftPos, topPos);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getMaxProgress() <= 0)
            return;
        int filled = (int) ((float) menu.getProgress() / menu.getMaxProgress() * ARROW_WIDTH);
        if (filled <= 0)
            return;
        guiGraphics.blit(TEXTURE,
                x + ARROW_X, y + ARROW_Y,
                ARROW_U, ARROW_V,
                filled, ARROW_HEIGHT);
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getMaxEnergy() <= 0)
            return;
        int filled = (int) ((float) menu.getEnergy() / menu.getMaxEnergy() * ENERGY_HEIGHT);
        if (filled <= 0)
            return;
        int yOffset = ENERGY_HEIGHT - filled;

        // Animazione arcobaleno: stessa tecnica di Actually Additions
        // (EnergyDisplay.draw).
        // getWheelColor(gameTime % 256) mappa il tempo di gioco sulla ruota dei colori
        // HSV
        // a saturazione e valore massimi. setShaderColor() tinge il blit con quel
        // colore.
        // Lo sprite della barra (U=176, V=17) è bianco per ricevere il colore puro.
        if (Minecraft.getInstance().level != null) {
            float[] rgb = getWheelColor(Minecraft.getInstance().level.getGameTime() % 256L);
            RenderSystem.setShaderColor(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 1.0f);
        }

        guiGraphics.blit(TEXTURE,
                x + ENERGY_X, y + ENERGY_Y_TOP + yOffset,
                ENERGY_U, ENERGY_V + yOffset,
                ENERGY_WIDTH, filled);

        // Ripristina il colore neutro (importante: senza questo, i blit successivi
        // sarebbero tinti)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // -------------------------------------------------------------------
    // Converte una posizione 0–255 nella ruota dei colori HSV (S=1, V=1).
    // Identica all'algoritmo di AssetUtil.getWheelColor() di Actually Additions.
    // -------------------------------------------------------------------
    private static float[] getWheelColor(long position) {
        float h = (position % 256L) / 256f * 6f; // hue 0..6
        int i = (int) h % 6;
        float f = h - (int) h; // parte frazionaria
        float q = (1f - f) * 255f; // fade-out
        float t = f * 255f; // fade-in
        return switch (i) {
            case 0 -> new float[] { 255f, t, 0f }; // red → yellow
            case 1 -> new float[] { q, 255f, 0f }; // yellow → green
            case 2 -> new float[] { 0f, 255f, t }; // green → cyan
            case 3 -> new float[] { 0f, q, 255f }; // cyan → blue
            case 4 -> new float[] { t, 0f, 255f }; // blue → magenta
            case 5 -> new float[] { 255f, 0f, q }; // magenta → red
            default -> new float[] { 255f, 255f, 255f };
        };
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        guiGraphics.drawString(this.font, this.title,
                (this.imageWidth - titleWidth) / 2,
                this.titleLabelY,
                0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
    }

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
