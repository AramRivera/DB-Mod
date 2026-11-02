package com.dragonblock.client.combat;

import com.dragonblock.client.gui.UiTheme;
import com.dragonblock.common.data.PlayerDBCData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 2D Combat HUD card displayed when a target is locked on.
 * Shows target identity, distance in meters, and an animated health/body bar.
 */
public enum LockOnOverlay implements LayeredDraw.Layer {
    INSTANCE;

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        if (!LockOnManager.isLocked()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        LivingEntity target = LockOnManager.getTarget();
        if (target == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int w = 150;
        int h = 24;
        int x = (sw - w) / 2;
        int y = 12;

        // Background card
        g.fill(x, y, x + w, y + h, UiTheme.PANEL);
        g.fill(x, y, x + w, y + 1, UiTheme.BORDER);
        g.fill(x, y + h - 1, x + w, y + h, UiTheme.BORDER);
        g.fill(x, y, x + 1, y + h, UiTheme.BORDER);
        g.fill(x + w - 1, y, x + w, y + h, UiTheme.BORDER);

        // Header status indicator
        String distStr = String.format("%.1fm", mc.player.distanceTo(target));
        g.drawString(mc.font, "LOCKED", x + 5, y + 3, UiTheme.GOLD, false);
        g.drawString(mc.font, distStr, x + w - mc.font.width(distStr) - 5, y + 3, UiTheme.TEXT_DIM, false);

        // Target Name (truncated if necessary)
        String name = target.getDisplayName().getString();
        if (mc.font.width(name) > 85) {
            name = mc.font.plainSubstrByWidth(name, 80) + "...";
        }
        g.drawString(mc.font, name, x + 44, y + 3, UiTheme.TEXT_HEADER, false);

        // Health bar track
        int barX = x + 4;
        int barY = y + 14;
        int barW = w - 8;
        int barH = 5;
        g.fill(barX, barY, barX + barW, barY + barH, UiTheme.TRACK);

        // Calculate health percentage (checks PlayerDBCData if the target is a player)
        float hpRatio;
        if (target instanceof Player p) {
            PlayerDBCData data = PlayerDBCData.get(p);
            hpRatio = Math.max(0f, Math.min(1f, (float) (data.getBody() / data.getMaxBody())));
        } else {
            hpRatio = Math.max(0f, Math.min(1f, target.getHealth() / target.getMaxHealth()));
        }

        int fillW = (int) (barW * hpRatio);
        if (fillW > 0) {
            int hpColor = hpRatio > 0.5f ? UiTheme.GOOD : (hpRatio > 0.25f ? UiTheme.GOLD : UiTheme.BAD);
            g.fill(barX, barY, barX + fillW, barY + barH, hpColor);
        }

        if (!target.onGround() && mc.player.distanceTo(target) > 3.0) {
            String chasePrompt = "[ ⚡ CHASE (R) ]";
            int cx = (sw - mc.font.width(chasePrompt)) / 2;
            g.drawString(mc.font, chasePrompt, cx, y + h + 2, UiTheme.ORANGE, false);
        }
    }
}
