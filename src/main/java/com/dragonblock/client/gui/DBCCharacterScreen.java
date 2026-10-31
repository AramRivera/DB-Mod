package com.dragonblock.client.gui;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SSelectRacePacket;
import com.dragonblock.common.network.packet.C2SUpgradeStatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DBCCharacterScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 220;

    public DBCCharacterScreen() {
        super(Component.literal("Dragon Block - Ficha de Personaje"));
    }

    @Override
    protected void init() {
        super.init();
        refreshButtons();
    }

    private void refreshButtons() {
        this.clearWidgets();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        PlayerDBCData data = PlayerDBCData.get(mc.player);

        // Change Race button
        this.addRenderableWidget(Button.builder(
                Component.literal("Cambiar"),
                btn -> {
                    Race[] races = Race.values();
                    int next = (data.getRace().ordinal() + 1) % races.length;
                    Race newRace = races[next];
                    data.setRace(newRace);
                    ModMessages.sendToServer(new C2SSelectRacePacket(newRace.name()));
                    refreshButtons();
                }
        ).bounds(left + PANEL_WIDTH - 72, top + 34, 60, 16)
         .tooltip(Tooltip.create(Component.literal("Cambiar a la siguiente raza")))
         .build());

        // Stat upgrade buttons [+]
        String[] stats = {"STR", "DEX", "CON", "WIL", "MND"};
        int startY = top + 102;

        for (int i = 0; i < stats.length; i++) {
            final String stat = stats[i];
            int cost = data.getUpgradeCost(stat);
            boolean canAfford = data.getTrainingPoints() >= cost;

            this.addRenderableWidget(Button.builder(
                    Component.literal("+"),
                    btn -> {
                        if (data.upgradeStat(stat)) {
                            ModMessages.sendToServer(new C2SUpgradeStatPacket(stat));
                            refreshButtons();
                        }
                    }
            ).bounds(left + PANEL_WIDTH - 32, startY + (i * 18), 20, 16)
             .tooltip(Tooltip.create(
                     Component.literal("Mejorar " + stat + " | Costo: " + cost + " TP (" + (canAfford ? "Disponible" : "Insuficiente") + ")")
             ))
             .build());
        }

        // Close button
        this.addRenderableWidget(Button.builder(
                Component.literal("Cerrar"),
                btn -> this.onClose()
        ).bounds(left + (PANEL_WIDTH - 70) / 2, top + PANEL_HEIGHT - 22, 70, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Clear non-blurred background tint
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        PlayerDBCData data = PlayerDBCData.get(mc.player);

        // Solid crisp panel background (no transparency blur leak)
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF14161D);
        // Outer Golden Border
        guiGraphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFE0A020);
        // Header separator
        guiGraphics.fill(left + 8, top + 24, left + PANEL_WIDTH - 8, top + 25, 0xFF353C4E);

        // Header Title
        guiGraphics.drawCenteredString(this.font, "§6§lDRAGON BLOCK §e§lESTADÍSTICAS", left + PANEL_WIDTH / 2, top + 8, 0xFFFFFF);

        // Player Info Section
        guiGraphics.drawString(this.font, "§7Guerrero: §f" + mc.player.getGameProfile().getName(), left + 12, top + 30, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "§7Raza: §b" + data.getRace().getDisplayName(), left + 12, top + 44, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "§7Estado: §a" + data.getTransformation().getDisplayName(), left + 12, top + 58, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "§7Poder de Pelea (BP): §e" + String.format("%,d", data.getBattlePower()), left + 12, top + 72, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "§7Puntos de Entrenamiento (TP): §a" + String.format("%,d", data.getTrainingPoints()), left + 12, top + 86, 0xFFFFFF, true);

        // Stats Divider
        guiGraphics.fill(left + 8, top + 98, left + PANEL_WIDTH - 8, top + 99, 0xFF353C4E);

        // Stats List
        int startY = top + 106;
        renderStatLine(guiGraphics, "Fuerza (STR):", data.getStr(), data.getEffectiveStr(), data.getUpgradeCost("STR"), left + 12, startY);
        renderStatLine(guiGraphics, "Destreza (DEX):", data.getDex(), data.getEffectiveDex(), data.getUpgradeCost("DEX"), left + 12, startY + 18);
        renderStatLine(guiGraphics, "Constitución (CON):", data.getCon(), data.getEffectiveCon(), data.getUpgradeCost("CON"), left + 12, startY + 36);
        renderStatLine(guiGraphics, "Voluntad (WIL):", data.getWil(), data.getEffectiveWil(), data.getUpgradeCost("WIL"), left + 12, startY + 54);
        renderStatLine(guiGraphics, "Mente (MND):", data.getMnd(), data.getMnd(), data.getUpgradeCost("MND"), left + 12, startY + 72);

        // Render widgets/buttons and tooltips
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatLine(GuiGraphics guiGraphics, String name, int base, int effective, int cost, int x, int y) {
        String text = "§f" + name + " §e" + base;
        if (effective != base) {
            text += " §7(§a" + effective + "§7)";
        }
        text += " §8[§6" + cost + " TP§8]";
        guiGraphics.drawString(this.font, text, x, y, 0xFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
