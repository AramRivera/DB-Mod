package com.dragonblock.client.overlay;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.gui.UiTheme;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Equipped techniques down the right edge of the screen. Ctrl + mouse wheel moves the selection; while
 * Ctrl is held (or right after scrolling) every slot shows its name, otherwise only the selected one does.
 * The selected slot shows its cost, the charge while the key is held, and a cooldown sweep.
 */
public class TechniqueBar implements LayeredDraw.Layer {
    public static final TechniqueBar INSTANCE = new TechniqueBar();
    public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/gui/techniques.png");
    private static final int SLOT = 22;

    private static long scrolledAt = 0;
    private float shownSelection = -1;
    private float labels = 0;

    public static void onScrolled() {
        scrolledAt = Util.getMillis();
    }

    /** Draws a technique's 16x16 icon from the atlas (8 icons per row). */
    public static void icon(GuiGraphics g, Technique tech, int x, int y) {
        int i = tech.ordinal();
        g.blit(ICONS, x, y, (i % 8) * 16, (i / 8) * 16, 16, 16, 128, 32);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        PlayerDBCData data = PlayerDBCData.get(mc.player);
        List<Technique> equipped = data.getEquipped();
        if (equipped.isEmpty() || !data.isRaceChosen()) return;
        Font font = mc.font;
        long now = Util.getMillis();

        int selected = data.getSelectedSlot();
        if (shownSelection < 0 || shownSelection >= equipped.size()) shownSelection = selected;
        shownSelection += (selected - shownSelection) * 0.3f;
        boolean expanded = Screen.hasControlDown() || now - scrolledAt < 2200;
        labels += ((expanded ? 1f : 0f) - labels) * 0.2f;

        int x = g.guiWidth() - SLOT - 6;
        int top = g.guiHeight() / 2 - equipped.size() * SLOT / 2;

        // Selection frame glides between slots
        int sy = top + Math.round(shownSelection * SLOT);
        g.fill(x - 2, sy - 2, x + SLOT, sy + SLOT - 2, 0xFF000000 | UiTheme.ORANGE);

        for (int i = 0; i < equipped.size(); i++) {
            Technique tech = equipped.get(i);
            int y = top + i * SLOT;
            boolean isSel = i == selected;
            g.fill(x, y, x + SLOT - 4, y + SLOT - 4, isSel ? 0xF0182038 : 0xC00C1120);
            icon(g, tech, x + 1, y + 1);

            float cd = AnimState.cooldown(tech);
            if (cd > 0) g.fill(x, y, x + SLOT - 4, y + Math.round((SLOT - 4) * cd), 0xB0000000);

            // Name tag to the left: always for the selected slot, for all of them while browsing
            float show = isSel ? 1f : labels * 0.85f;
            if (show > 0.05f) {
                String name = tech.getDisplayName();
                String cost = isSel ? "  " + (int) tech.getKiCost() + " ki" : "";
                int w = font.width(name) + font.width(cost) + 8;
                int alpha = (int) (show * 255);
                int lx = x - 6 - w + (int) ((1 - show) * 10);
                g.fill(lx, y + 1, x - 4, y + SLOT - 5, (int) (alpha * 0.75f) << 24 | 0x0C1120);
                g.drawString(font, name, lx + 4, y + 5, alpha << 24 | (isSel ? UiTheme.TEXT_HEADER : UiTheme.TEXT), false);
                if (isSel) {
                    g.drawString(font, cost, lx + 4 + font.width(name), y + 5, alpha << 24 | UiTheme.TEXT_DIM, false);
                    drawCharge(g, data, tech, lx, y + SLOT - 7, x - 4 - lx, now);
                }
            }
        }
        if (expanded) {
            String hint = "Ctrl + rueda";
            g.drawString(font, hint, x + SLOT - 4 - font.width(hint), top + equipped.size() * SLOT + 2,
                    (int) (labels * 200) << 24 | UiTheme.TEXT_DIM, false);
        }
    }

    /** Thin charge bar along the bottom of the selected name tag while the key is held. */
    private static void drawCharge(GuiGraphics g, PlayerDBCData data, Technique tech, int x, int y, int w, long now) {
        if (!data.isTechHeld() || tech.isInstant()) return;
        float c = data.getTechCharge();
        boolean ready = c + 1e-3f >= tech.getMinCharge();
        g.fill(x, y, x + w, y + 2, UiTheme.TRACK);
        g.fill(x, y, x + Math.round(w * c), y + 2, 0xFF000000 | (ready ? tech.colorFor(data.getTransformation()) : 0x707070));
        if (tech.getMinCharge() > 0) {
            int mark = x + Math.round(w * tech.getMinCharge());
            g.fill(mark, y - 1, mark + 1, y + 3, 0xFFFFFFFF);
        }
        if (c >= 1f && (now / 150) % 2 == 0) g.fill(x, y, x + w, y + 2, 0x60FFFFFF);
    }
}
