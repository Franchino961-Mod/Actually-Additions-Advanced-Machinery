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

    private boolean showSidedConfig = false;

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

        // Pulsanti di configurazione automazione (Round Robin e Single Item Mode spostati a y=24)
        drawAutoButton(guiGraphics, leftPos + 100, topPos + 24, menu.isRoundRobin(), "R", mouseX, mouseY);
        drawAutoButton(guiGraphics, leftPos + 114, topPos + 24, menu.isSingleItemMode(), "1", mouseX, mouseY);

        // Pulsante di toggle per la configurazione dei lati (C)
        drawAutoButton(guiGraphics, leftPos + 134, topPos + 90, showSidedConfig, "C", mouseX, mouseY);

        // Disegno del pannello laterale se showSidedConfig è attivo
        if (showSidedConfig) {
            // Sfondo pannello laterale a sinistra
            guiGraphics.fill(leftPos - 60, topPos + 18, leftPos, topPos + 118, 0xFF2D2D2D);
            guiGraphics.fill(leftPos - 60, topPos + 18, leftPos - 59, topPos + 118, 0xFF3C3C3C); // Bordo sinistro
            guiGraphics.fill(leftPos - 60, topPos + 18, leftPos, topPos + 19, 0xFF3C3C3C); // Bordo superiore
            guiGraphics.fill(leftPos - 60, topPos + 117, leftPos, topPos + 118, 0xFF3C3C3C); // Bordo inferiore

            // 6 bottoni dei lati a croce spiegata (U, D, F, B, L, R)
            drawColorButton(guiGraphics, leftPos - 36, topPos + 26, menu.getSideConfig(0), "U", mouseX, mouseY);
            drawColorButton(guiGraphics, leftPos - 50, topPos + 40, menu.getSideConfig(4), "L", mouseX, mouseY);
            drawColorButton(guiGraphics, leftPos - 36, topPos + 40, menu.getSideConfig(2), "F", mouseX, mouseY);
            drawColorButton(guiGraphics, leftPos - 22, topPos + 40, menu.getSideConfig(5), "R", mouseX, mouseY);
            drawColorButton(guiGraphics, leftPos - 36, topPos + 54, menu.getSideConfig(1), "D", mouseX, mouseY);
            drawColorButton(guiGraphics, leftPos - 36, topPos + 68, menu.getSideConfig(3), "B", mouseX, mouseY);

            // Auto Input (I) e Auto Output (O) posizionati in fondo al pannello
            drawAutoButton(guiGraphics, leftPos - 42, topPos + 90, menu.isAutoInput(), "I", mouseX, mouseY);
            drawAutoButton(guiGraphics, leftPos - 24, topPos + 90, menu.isAutoOutput(), "O", mouseX, mouseY);
        }
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
        renderAutomationTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int barLeft = x + ENERGY_X;
        int barRight = barLeft + ENERGY_WIDTH;
        int barTop = y + ENERGY_Y_TOP;
        int barBottom = barTop + ENERGY_HEIGHT;

        if (mouseX >= barLeft && mouseX <= barRight && mouseY >= barTop && mouseY <= barBottom) {
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(Component.translatable("gui.advancedmachinery.energy_tooltip.title"));
            tooltip.add(Component.translatable("gui.advancedmachinery.energy_tooltip.value", formatEnergy(menu.getEnergy()), formatEnergy(menu.getMaxEnergy())));

            int rfPerTick = menu.getEnergyPerTick();
            int totalCost = menu.getTotalEnergyCost();
            if (totalCost > 0) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.advancedmachinery.energy_tooltip.recipe_cost", formatEnergy(totalCost)));
                tooltip.add(Component.translatable("gui.advancedmachinery.energy_tooltip.usage", formatEnergy(rfPerTick)));
            }

            int speedUp = menu.getSpeedUpgradeCount();
            int energyUp = menu.getEnergyUpgradeCount();
            if (speedUp > 0 || energyUp > 0) {
                tooltip.add(Component.literal(""));
                if (speedUp > 0) {
                    double speedMultiplier = Math.pow(10.0, (double) speedUp / 8.0);
                    int timeReduction = (int) Math.round((1.0 - 1.0 / speedMultiplier) * 100.0);
                    tooltip.add(Component.translatable("gui.advancedmachinery.energy_tooltip.speed_upgrade", timeReduction));
                }
                if (energyUp > 0) {
                    double costMultiplier = Math.pow(10.0, - (double) energyUp / 8.0);
                    int costReduction = (int) Math.round((1.0 - costMultiplier) * 100.0);
                    tooltip.add(Component.translatable("gui.advancedmachinery.energy_tooltip.energy_upgrade", costReduction));
                }
            }

            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderAutomationTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component activeText = Component.translatable("gui.advancedmachinery.status.active");
        Component inactiveText = Component.translatable("gui.advancedmachinery.status.inactive");

        // R (Round Robin) - ora a y=24
        if (isHovering(100, 24, 12, 12, mouseX, mouseY)) {
            Component status = menu.isRoundRobin() ? activeText : inactiveText;
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(Component.translatable("gui.advancedmachinery.tooltip.round_robin", status));
            tooltip.add(Component.translatable("gui.advancedmachinery.tooltip.round_robin.desc.1"));
            tooltip.add(Component.translatable("gui.advancedmachinery.tooltip.round_robin.desc.2"));
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
        // 1 (Single Item Mode) - ora a y=24
        if (isHovering(114, 24, 12, 12, mouseX, mouseY)) {
            Component status = menu.isSingleItemMode() ? activeText : inactiveText;
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(Component.translatable("gui.advancedmachinery.tooltip.single_item", status));
            tooltip.add(Component.translatable("gui.advancedmachinery.tooltip.single_item.desc.1"));
            tooltip.add(Component.translatable("gui.advancedmachinery.tooltip.single_item.desc.2"));
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
        // C (Configuratore Lati)
        if (isHovering(134, 90, 12, 12, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.advancedmachinery.tooltip.configurator"), mouseX, mouseY);
        }

        if (showSidedConfig) {
            // Auto Input (I)
            if (isHovering(-42, 90, 12, 12, mouseX, mouseY)) {
                Component status = menu.isAutoInput() ? activeText : inactiveText;
                guiGraphics.renderTooltip(this.font, Component.translatable("gui.advancedmachinery.tooltip.auto_input", status), mouseX, mouseY);
            }
            // Auto Output (O)
            if (isHovering(-24, 90, 12, 12, mouseX, mouseY)) {
                Component status = menu.isAutoOutput() ? activeText : inactiveText;
                guiGraphics.renderTooltip(this.font, Component.translatable("gui.advancedmachinery.tooltip.auto_output", status), mouseX, mouseY);
            }

            // 6 Lati
            if (isHovering(-36, 26, 12, 12, mouseX, mouseY)) {
                renderSideTooltip(guiGraphics, "gui.advancedmachinery.side.up", menu.getSideConfig(0), mouseX, mouseY);
            }
            if (isHovering(-36, 54, 12, 12, mouseX, mouseY)) {
                renderSideTooltip(guiGraphics, "gui.advancedmachinery.side.down", menu.getSideConfig(1), mouseX, mouseY);
            }
            if (isHovering(-36, 40, 12, 12, mouseX, mouseY)) {
                renderSideTooltip(guiGraphics, "gui.advancedmachinery.side.front", menu.getSideConfig(2), mouseX, mouseY);
            }
            if (isHovering(-36, 68, 12, 12, mouseX, mouseY)) {
                renderSideTooltip(guiGraphics, "gui.advancedmachinery.side.back", menu.getSideConfig(3), mouseX, mouseY);
            }
            if (isHovering(-50, 40, 12, 12, mouseX, mouseY)) {
                renderSideTooltip(guiGraphics, "gui.advancedmachinery.side.left", menu.getSideConfig(4), mouseX, mouseY);
            }
            if (isHovering(-22, 40, 12, 12, mouseX, mouseY)) {
                renderSideTooltip(guiGraphics, "gui.advancedmachinery.side.right", menu.getSideConfig(5), mouseX, mouseY);
            }
        }
    }

    private void renderSideTooltip(GuiGraphics guiGraphics, String sideTranslationKey, int mode, int mouseX, int mouseY) {
        Component sideName = Component.translatable(sideTranslationKey);
        Component modeName = Component.translatable(getModeKey(mode));
        guiGraphics.renderTooltip(this.font, Component.translatable("gui.advancedmachinery.tooltip.side_format", sideName, modeName), mouseX, mouseY);
    }

    private String getModeKey(int mode) {
        return switch (mode) {
            case 0 -> "gui.advancedmachinery.mode.disabled";
            case 1 -> "gui.advancedmachinery.mode.base";
            case 2 -> "gui.advancedmachinery.mode.modifier";
            case 3 -> "gui.advancedmachinery.mode.output";
            case 4 -> "gui.advancedmachinery.mode.any";
            default -> "gui.advancedmachinery.mode.any";
        };
    }

    private String formatEnergy(int energy) {
        if (energy >= 1_000_000) {
            return String.format("%.2fM", (double) energy / 1_000_000.0);
        } else if (energy >= 1_000) {
            return String.format("%.1fK", (double) energy / 1_000.0);
        } else {
            return String.valueOf(energy);
        }
    }

    private void drawAutoButton(GuiGraphics g, int x, int y, boolean active, String label, int mouseX, int mouseY) {
        int size = 12;
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        int bg = active ? 0xFF2E8B57 : 0xFF708090; 
        if (hovered) {
            bg = active ? 0xFF3CB371 : 0xFF8795A5;
        }

        g.fill(x, y, x + size, y + size, 0xFF3C3C3C);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, bg);
        
        int labelColor = active ? 0xFFFFFFFF : 0xFFDDDDDD;
        g.drawString(this.font, label, x + 3, y + 2, labelColor, false);
    }

    private void drawColorButton(GuiGraphics g, int x, int y, int mode, String label, int mouseX, int mouseY) {
        int size = 12;
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        int bg = switch (mode) {
            case 0 -> 0xFF708090; // Disabled: Gray
            case 1 -> 0xFF1F75FE; // Base Input: Blue
            case 2 -> 0xFFD4AF37; // Modifier Input: Yellow
            case 3 -> 0xFF2E8B57; // Output Only: Green
            case 4 -> 0xFF8F00FF; // Any: Purple
            default -> 0xFF708090;
        };
        
        if (hovered) {
            int r = (bg >> 16) & 0xFF;
            int gr = (bg >> 8) & 0xFF;
            int b = bg & 0xFF;
            r = Math.min(255, r + 30);
            gr = Math.min(255, gr + 30);
            b = Math.min(255, b + 30);
            bg = 0xFF000000 | (r << 16) | (gr << 8) | b;
        }

        g.fill(x, y, x + size, y + size, 0xFF3C3C3C);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, bg);
        g.drawString(this.font, label, x + 3, y + 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        // C (Configuratore Lati)
        if (mouseX >= (double) (x + 134) && mouseX < (double) (x + 134 + 12) && mouseY >= (double) (y + 90) && mouseY < (double) (y + 90 + 12)) {
            showSidedConfig = !showSidedConfig;
            return true;
        }

        // R (Round Robin) - y=24
        if (mouseX >= (double) (x + 100) && mouseX < (double) (x + 100 + 12) && mouseY >= (double) (y + 24) && mouseY < (double) (y + 24 + 12)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 2));
            return true;
        }
        // 1 (Single Item Mode) - y=24
        if (mouseX >= (double) (x + 114) && mouseX < (double) (x + 114 + 12) && mouseY >= (double) (y + 24) && mouseY < (double) (y + 24 + 12)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 3));
            return true;
        }

        if (showSidedConfig) {
            // Auto Input (I)
            if (mouseX >= (double) (x - 42) && mouseX < (double) (x - 42 + 12) && mouseY >= (double) (y + 90) && mouseY < (double) (y + 90 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 0));
                return true;
            }
            // Auto Output (O)
            if (mouseX >= (double) (x - 24) && mouseX < (double) (x - 24 + 12) && mouseY >= (double) (y + 90) && mouseY < (double) (y + 90 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 1));
                return true;
            }

            // U (Up) -> 4
            if (mouseX >= (double) (x - 36) && mouseX < (double) (x - 36 + 12) && mouseY >= (double) (y + 26) && mouseY < (double) (y + 26 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 4));
                return true;
            }
            // L (Left) -> 8
            if (mouseX >= (double) (x - 50) && mouseX < (double) (x - 50 + 12) && mouseY >= (double) (y + 40) && mouseY < (double) (y + 40 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 8));
                return true;
            }
            // F (Front) -> 6
            if (mouseX >= (double) (x - 36) && mouseX < (double) (x - 36 + 12) && mouseY >= (double) (y + 40) && mouseY < (double) (y + 40 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 6));
                return true;
            }
            // R (Right) -> 9
            if (mouseX >= (double) (x - 22) && mouseX < (double) (x - 22 + 12) && mouseY >= (double) (y + 40) && mouseY < (double) (y + 40 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 9));
                return true;
            }
            // D (Down) -> 5
            if (mouseX >= (double) (x - 36) && mouseX < (double) (x - 36 + 12) && mouseY >= (double) (y + 54) && mouseY < (double) (y + 54 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 5));
                return true;
            }
            // B (Back) -> 7
            if (mouseX >= (double) (x - 36) && mouseX < (double) (x - 36 + 12) && mouseY >= (double) (y + 68) && mouseY < (double) (y + 68 + 12)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.advancedmachinery.network.ToggleAutoSettingPayload(menu.getBlockPos(), 7));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
