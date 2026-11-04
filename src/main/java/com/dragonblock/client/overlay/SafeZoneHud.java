package com.dragonblock.client.overlay;

import com.dragonblock.client.gui.UiTheme;
import com.dragonblock.common.world.SafeZones;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/** A quiet badge at the top while you stand where players can't fight; it fades in and out at the border. */
public class SafeZoneHud implements LayeredDraw.Layer {
    public static final SafeZoneHud INSTANCE = new SafeZoneHud();
    private static final String LABEL = "ZONA SEGURA  ·  SIN PVP";

    private long checkedAt = -100;
    private boolean safe;
    private float shown;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        long now = mc.level.getGameTime();
        if (now - checkedAt >= 10 || now < checkedAt) { // twice a second is plenty for a border you walk across
            checkedAt = now;
            safe = SafeZones.isSafe(mc.level, mc.player.position());
        }
        shown += ((safe ? 1f : 0f) - shown) * 0.08f;
        if (shown < 0.03f) return;
        int alpha = (int) (shown * 255) << 24;
        int w = mc.font.width(LABEL) + 16, x = (g.guiWidth() - w) / 2, y = 4;
        g.fill(x, y, x + w, y + 14, (int) (shown * 0xC0) << 24 | 0x0C1222);
        g.fill(x, y + 13, x + w, y + 14, alpha | 0x8FD19E);
        g.drawString(mc.font, LABEL, x + 8, y + 3, alpha | UiTheme.GOOD, false);
    }
}
