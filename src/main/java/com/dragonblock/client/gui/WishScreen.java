package com.dragonblock.client.gui;

import com.dragonblock.common.entity.ShenlongEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SWishPacket;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * The dragon speaks (typed out letter by letter) and the summoner picks one wish from a column of cards.
 * Closing it is harmless: using a ball again reopens it while the dragon waits.
 */
public class WishScreen extends Screen {
    private static final String LINE = "Has reunido las siete esferas. Habla, y te concederé un deseo. Solo uno.";
    private static final int W = 300, CARD_H = 28;
    private static final int DRAGON_GREEN = 0x5CD07A;

    private final int entityId;
    private final long openedAt = Util.getMillis();

    public WishScreen(int entityId) {
        super(Component.literal("Deseo"));
        this.entityId = entityId;
    }

    private int left() { return (width - W) / 2; }
    private int cardsTop() { return height / 2 - 30; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float k = Mth.clamp((Util.getMillis() - openedAt) / 400f, 0f, 1f);
        g.fillGradient(0, 0, width, height, (int) (0x90 * k) << 24 | 0x02140A, (int) (0xC0 * k) << 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        long since = Util.getMillis() - openedAt;
        int l = left(), top = cardsTop() - 74;

        // The dragon's words, typed out
        g.fill(l, top, l + W, top + 60, 0xE0081410);
        g.renderOutline(l, top, W, 60, 0xFF000000 | DRAGON_GREEN);
        UiTheme.header(g, l, top, W, 1f);
        g.drawString(font, "DRAGÓN ETERNO", l + 24, top + 9, DRAGON_GREEN, false);
        int shown = (int) Math.min(LINE.length(), since / 35);
        int y = top + 24;
        for (FormattedCharSequence line : font.split(Component.literal(LINE.substring(0, shown)), W - 20)) {
            g.drawString(font, line, l + 10, y, UiTheme.TEXT_HEADER, false);
            y += 10;
        }

        // Wishes, sliding in one after another once the dragon has spoken
        ShenlongEntity.Wish[] wishes = ShenlongEntity.Wish.values();
        for (int i = 0; i < wishes.length; i++) {
            float appear = Mth.clamp((since - LINE.length() * 35L - i * 90L) / 200f, 0f, 1f);
            if (appear <= 0) continue;
            int x = l + (int) ((1 - appear) * 30), cy = cardsTop() + i * (CARD_H + 4);
            boolean hover = inside(mouseX, mouseY, l, cy, W, CARD_H);
            int alpha = (int) (appear * 255);
            g.fill(x, cy, x + W, cy + CARD_H, (int) (alpha * 0.9f) << 24 | (hover ? 0x1C2742 : 0x131B30));
            g.fill(x, cy, x + (hover ? 4 : 2), cy + CARD_H, alpha << 24 | (hover ? UiTheme.GOLD : UiTheme.ORANGE));
            g.drawString(font, wishes[i].line, x + 12, cy + 5, alpha << 24 | UiTheme.TEXT_HEADER, false);
            g.drawString(font, wishes[i].effect, x + 12, cy + 16, alpha << 24 | UiTheme.TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ShenlongEntity.Wish[] wishes = ShenlongEntity.Wish.values();
        long since = Util.getMillis() - openedAt;
        for (int i = 0; i < wishes.length; i++) {
            boolean visible = since > LINE.length() * 35L + i * 90L + 100;
            if (visible && inside(mouseX, mouseY, left(), cardsTop() + i * (CARD_H + 4), W, CARD_H)) {
                ModMessages.sendToServer(new C2SWishPacket(entityId, i));
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
