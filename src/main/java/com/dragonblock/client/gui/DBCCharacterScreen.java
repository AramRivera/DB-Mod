package com.dragonblock.client.gui;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SSelectRacePacket;
import com.dragonblock.common.network.packet.C2SUpgradeStatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DBCCharacterScreen extends Screen {
    private static final int PANEL_WIDTH = 250;
    private static final int PANEL_HEIGHT = 210;

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
        ).bounds(left + PANEL_WIDTH - 65, top + 34, 55, 16).build());

        // Stat upgrade buttons
        String[] stats = {"STR", "DEX", "CON", "WIL", "MND"};
        int startY = top + 94;

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
            ).bounds(left + PANEL_WIDTH - 30, startY + (i * 18), 18, 16)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.literal("Mejorar " + stat + " (Costo: " + cost + " TP)")
                    ))
                    .build());
        }

        // Close button
        this.addRenderableWidget(Button.builder(
                Component.literal("Cerrar"),
                btn -> this.onClose()
        ).bounds(left + (PANEL_WIDTH - 60) / 2, top + PANEL_HEIGHT - 22, 60, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Darkened background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        PlayerDBCData data = PlayerDBCData.get(mc.player);

        // Main Panel Box
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE141419);
        // Golden Border
        guiGraphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFE0A020);

        // Header Title
        guiGraphics.drawCenteredString(this.font, "§6§lDRAGON BLOCK §e§lESTADÍSTICAS", left + PANEL_WIDTH / 2, top + 8, 0xFFFFFF);

        // Character Info
        guiGraphics.drawString(this.font, "§7Luchador: §f" + mc.player.getGameProfile().getName(), left + 12, top + 24, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7Raza: §b" + data.getRace().getDisplayName(), left + 12, top + 38, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7Estado: §a" + data.getTransformation().getDisplayName(), left + 12, top + 52, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7Poder de Pelea (BP): §e" + String.format("%,d", data.getBattlePower()), left + 12, top + 66, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "§7Puntos de Entrenamiento (TP): §a" + String.format("%,d", data.getTrainingPoints()), left + 12, top + 79, 0xFFFFFF, false);

        // Divider
        guiGraphics.fill(left + 10, top + 90, left + PANEL_WIDTH - 10, top + 91, 0xFF555555);

        // Stats Labels & Values
        int startY = top + 98;
        renderStatLine(guiGraphics, "Fuerza (STR):", data.getStr(), data.getEffectiveStr(), data.getUpgradeCost("STR"), left + 12, startY);
        renderStatLine(guiGraphics, "Destreza (DEX):", data.getDex(), data.getEffectiveDex(), data.getUpgradeCost("DEX"), left + 12, startY + 18);
        renderStatLine(guiGraphics, "Constitución (CON):", data.getCon(), data.getEffectiveCon(), data.getUpgradeCost("CON"), left + 12, startY + 36);
        renderStatLine(guiGraphics, "Voluntad (WIL):", data.getWil(), data.getEffectiveWil(), data.getUpgradeCost("WIL"), left + 12, startY + 54);
        renderStatLine(guiGraphics, "Mente (MND):", data.getMnd(), data.getMnd(), data.getUpgradeCost("MND"), left + 12, startY + 72);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatLine(GuiGraphics guiGraphics, String name, int base, int effective, int cost, int x, int y) {
        String text = "§f" + name + " §e" + base;
        if (effective != base) {
            text += " §7(§a" + effective + "§7)";
        }
        text += " §8[Costo: §6" + cost + " TP§8]";
        guiGraphics.drawString(this.font, text, x, y, 0xFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
