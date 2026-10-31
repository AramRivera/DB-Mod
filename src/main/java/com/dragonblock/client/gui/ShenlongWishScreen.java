package com.dragonblock.client.gui;

import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SShenlongWishPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ShenlongWishScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;
    private final int shenlongEntityId;

    public ShenlongWishScreen(int shenlongEntityId) {
        super(Component.literal("Deseos de Shenlong"));
        this.shenlongEntityId = shenlongEntityId;
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        String[] wishTitles = {
                "⚡ 50,000 Puntos de Entrenamiento (TP)",
                "⚔️ Fuerza y Voluntad Divina (+15 STR / +15 WIL)",
                "❤️ Vitalidad Suprema (+10 Corazones Permanentes)",
                "🌀 Control Absoluto del Ki (+Ki Máximo y 100% Recarga)",
                "🌰 Provisiones Sagradas (16 Semillas + Scouter + Pesas)",
                "✨ Despertar de Potencial (25k TP y Requisitos)"
        };

        String[] wishTooltips = {
                "Recibe 50,000 TP para mejorar tus atributos como desees.",
                "Otorga +15 puntos permanentes en Fuerza (STR) y Voluntad (WIL).",
                "Aumenta tu Constitución en +10 (+20 HP máximos permanentes).",
                "Aumenta tu Voluntad en +10 y restaura todo tu Ki al 100%.",
                "Otorga 16 Semillas del Ermitaño, un Scouter y Pesas de entrenamiento.",
                "Otorga 25,000 TP y asegura los atributos para transformarte."
        };

        int buttonWidth = PANEL_WIDTH - 24;
        int buttonHeight = 22;
        int startY = top + 56;

        for (int i = 0; i < wishTitles.length; i++) {
            final int wishIndex = i;
            this.addRenderableWidget(Button.builder(
                    Component.literal(wishTitles[i]),
                    btn -> {
                        ModMessages.sendToServer(new C2SShenlongWishPacket(wishIndex, this.shenlongEntityId));
                        this.onClose();
                    }
            ).bounds(left + 12, startY + (i * 26), buttonWidth, buttonHeight)
             .tooltip(Tooltip.create(Component.literal(wishTooltips[i])))
             .build());
        }

        // Cancel / Close
        this.addRenderableWidget(Button.builder(
                Component.literal("§7Pensarlo mejor (Cerrar)"),
                btn -> this.onClose()
        ).bounds(left + (PANEL_WIDTH - 120) / 2, top + PANEL_HEIGHT - 24, 120, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        // Solid Emerald/Dark Divine Box
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF0E1A14);
        // Golden Border
        guiGraphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFE0A020);
        guiGraphics.renderOutline(left + 2, top + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, 0xFF00AA44);

        // Header Title
        guiGraphics.drawCenteredString(this.font, "§a§l🐉 SHENLONG - EL DIOS DRAGÓN 🐉", left + PANEL_WIDTH / 2, top + 10, 0xFFFFFF);

        // Dialogue text
        guiGraphics.drawCenteredString(this.font, "§e\"¡Habla mortal! ¡Te concederé un solo deseo antes de partir!\"", left + PANEL_WIDTH / 2, top + 26, 0xFFDD44);
        guiGraphics.drawCenteredString(this.font, "§7Elige con sabiduría, las Esferas del Dragón se convertirán en piedra.", left + PANEL_WIDTH / 2, top + 39, 0xAAAAAA);

        // Render widgets
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
