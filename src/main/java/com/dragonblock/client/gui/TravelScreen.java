package com.dragonblock.client.gui;

import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2STravelPacket;
import com.dragonblock.common.world.Planet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** The ship's navigation console: a card per planet, the one you are on greyed out. */
public class TravelScreen extends Screen {
    private static final int W = 300, CARD_H = 40;
    private static final int[] PLANET_COLORS = {0x3F76E4, 0x4FC9A0, 0x7A5AD0};

    private final int shipId;
    private final long openedAt = Util.getMillis();

    public TravelScreen(int shipId) {
        super(Component.literal("Navegación"));
        this.shipId = shipId;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - (Planet.values().length * (CARD_H + 4) + 28)) / 2; }

    private Planet current() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : Planet.of(level.dimension());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float k = Mth.clamp((Util.getMillis() - openedAt) / 300f, 0f, 1f);
        g.fill(0, 0, width, height, (int) (0xB0 * k) << 24 | 0x02040C);
        int l = left(), t = top();
        g.fill(l, t, l + W, t + 22, UiTheme.PANEL);
        UiTheme.header(g, l, t, W, k);
        g.drawString(font, "CONSOLA DE NAVEGACIÓN", l + 24, t + 8, UiTheme.TEXT_HEADER, false);
        Planet here = current();
        Planet[] planets = Planet.values();
        for (int i = 0; i < planets.length; i++) {
            Planet p = planets[i];
            int y = t + 26 + i * (CARD_H + 4);
            boolean disabled = p == here;
            boolean hover = !disabled && inside(mouseX, mouseY, l, y, W, CARD_H);
            g.fill(l, y, l + W, y + CARD_H, disabled ? 0xE0101420 : hover ? UiTheme.BUTTON_HOVER : UiTheme.PANEL_ALT);
            // A little planet: a disc in its color with a highlight
            int cx = l + 22, cy = y + CARD_H / 2;
            for (int dy = -12; dy <= 12; dy++) {
                int half = (int) Math.sqrt(144 - dy * dy);
                g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFF000000 | (disabled ? 0x3A3E46 : PLANET_COLORS[i]));
            }
            g.fill(cx - 6, cy - 9, cx - 2, cy - 7, 0x80FFFFFF);
            g.drawString(font, p.getDisplayName() + (disabled ? "  (estás aquí)" : ""), l + 42, y + 5, disabled ? UiTheme.TEXT_DIM : UiTheme.TEXT_HEADER, false);
            int ly = y + 16;
            for (FormattedCharSequence line : font.split(Component.literal(p.getDescription()), W - 50)) {
                g.drawString(font, line, l + 42, ly, UiTheme.TEXT_DIM, false);
                ly += 10;
            }
        }
        g.drawCenteredString(font, "Elige destino · Esc para quedarte", width / 2, t + 30 + planets.length * (CARD_H + 4), UiTheme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Planet[] planets = Planet.values();
        for (int i = 0; i < planets.length; i++) {
            if (planets[i] != current() && inside(mouseX, mouseY, left(), top() + 26 + i * (CARD_H + 4), W, CARD_H)) {
                ModMessages.sendToServer(new C2STravelPacket(shipId, i));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
