package com.dragonblock.client.gui;

import com.dragonblock.DragonBlockMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared palette with a Dragon Ball feel kept sober: deep night-blue panels, the classic gi orange as
 * the main accent, dragon-ball gold for highlights, and a star-ball emblem on headers.
 */
public final class UiTheme {
    public static final int PANEL = 0xF20C1222;
    public static final int PANEL_ALT = 0xFF131B30;
    public static final int ROW_HOVER = 0xFF1C2742;
    public static final int BORDER = 0xFF2A3556;
    public static final int BUTTON = 0xFF17203A;
    public static final int BUTTON_HOVER = 0xFF22304F;
    public static final int BUTTON_DISABLED = 0xFF111728;
    public static final int TEXT = 0xE2E5EE;
    public static final int TEXT_DIM = 0x8790AA;
    public static final int TEXT_HEADER = 0xFFF3DE;
    public static final int GOOD = 0x8FD19E;
    public static final int BAD = 0xE5776F;
    public static final int TRACK = 0xFF080B15;
    /** Gi orange, the signature accent. */
    public static final int ORANGE = 0xF28C28;
    /** Dragon ball gold. */
    public static final int GOLD = 0xFFC93C;

    public static final ResourceLocation EMBLEM = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/item/dragon_ball_4.png");

    private UiTheme() {}

    /** Panel header: orange-to-gold rule along the top edge and the four-star ball before the title. */
    public static void header(GuiGraphics g, int x, int y, int w, float reveal) {
        int len = (int) (w * reveal);
        for (int i = 0; i < len; i += 4) {
            g.fill(x + i, y, x + Math.min(len, i + 4), y + 2, 0xFF000000 | mix(ORANGE, GOLD, (float) i / w));
        }
        g.blit(EMBLEM, x + 7, y + 6, 0, 0, 12, 12, 12, 12);
    }

    public static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int gr = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | gr << 8 | bl;
    }
}
