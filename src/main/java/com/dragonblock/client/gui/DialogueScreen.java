package com.dragonblock.client.gui;

import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SDialogueChoicePacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * A conversation: the character in 3D on the left, its words typed out in a box along the bottom, and
 * your replies as cards. Clicking while it is still typing shows the whole line at once.
 */
public class DialogueScreen extends Screen {
    private static final int OPTION_H = 16;

    private final int npcId;
    private final String speaker, text;
    private final List<String> options;
    private final long openedAt = Util.getMillis();
    private boolean skipped = false;

    private DialogueScreen(int npcId, String speaker, String text, List<String> options) {
        super(Component.literal(speaker));
        this.npcId = npcId;
        this.speaker = speaker;
        this.text = text;
        this.options = options;
    }

    /** From the server: a new line (or -1 to end the conversation). */
    public static void show(int npcId, String speaker, String text, List<String> options) {
        Minecraft mc = Minecraft.getInstance();
        if (npcId < 0) {
            if (mc.screen instanceof DialogueScreen) mc.setScreen(null);
            return;
        }
        mc.setScreen(new DialogueScreen(npcId, speaker, text, options));
    }

    private int shownChars() {
        return skipped ? text.length() : (int) Math.min(text.length(), (Util.getMillis() - openedAt) / 22);
    }

    private boolean typed() {
        return shownChars() >= text.length();
    }

    private int boxTop() { return height - 70 - options.size() * (OPTION_H + 3); }
    private int boxLeft() { return Math.max(10, width / 2 - 170); }
    private int boxWidth() { return Math.min(340, width - 20); }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, height / 3, width, height, 0x00000000, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
        int l = boxLeft(), w = boxWidth(), top = boxTop();

        // The character, life-size on the left
        if (mc.level != null && mc.level.getEntity(npcId) instanceof LivingEntity npc && npc != mc.player) {
            float scale = 1f / Math.max(1f, npc.getBbHeight() / 1.8f);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, l - 4, top - 120, l + 90, top + 10, (int) (55 * scale), 0.0625f, mouseX, mouseY, npc);
        }

        // Name tag and the line being spoken
        g.fill(l, top, l + w, top + 54, 0xE80C1222);
        g.renderOutline(l, top, w, 54, UiTheme.BORDER);
        UiTheme.header(g, l, top, w, 1f);
        g.drawString(font, speaker.toUpperCase(), l + 24, top + 9, UiTheme.GOLD, false);
        int y = top + 22;
        for (FormattedCharSequence line : font.split(Component.literal(text.substring(0, shownChars())), w - 16)) {
            g.drawString(font, line, l + 8, y, UiTheme.TEXT_HEADER, false);
            y += 10;
        }

        // Replies appear once the line is done
        if (!typed()) return;
        for (int i = 0; i < options.size(); i++) {
            int oy = top + 58 + i * (OPTION_H + 3);
            boolean hover = inside(mouseX, mouseY, l, oy, w, OPTION_H);
            g.fill(l, oy, l + w, oy + OPTION_H, hover ? UiTheme.BUTTON_HOVER : 0xE0131B30);
            g.fill(l, oy, l + (hover ? 4 : 2), oy + OPTION_H, 0xFF000000 | (hover ? UiTheme.GOLD : UiTheme.ORANGE));
            g.drawString(font, (i + 1) + ". " + options.get(i), l + 10, oy + 4, hover ? 0xFFFFFF : UiTheme.TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!typed()) {
            skipped = true;
            return true;
        }
        int l = boxLeft(), w = boxWidth(), top = boxTop();
        for (int i = 0; i < options.size(); i++) {
            if (inside(mouseX, mouseY, l, top + 58 + i * (OPTION_H + 3), w, OPTION_H)) {
                choose(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Number keys pick replies too. */
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        int index = key - 49; // GLFW '1'
        if (typed() && index >= 0 && index < options.size()) {
            choose(index);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void choose(int index) {
        ModMessages.sendToServer(new C2SDialogueChoicePacket(npcId, index));
    }

    /** Leaving the conversation also ends its protection on the server. */
    @Override
    public void onClose() {
        ModMessages.sendToServer(new C2SDialogueChoicePacket(npcId, -1));
        super.onClose();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
