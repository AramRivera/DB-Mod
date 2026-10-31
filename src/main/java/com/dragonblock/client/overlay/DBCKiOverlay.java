package com.dragonblock.client.overlay;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

public class DBCKiOverlay implements LayeredDraw.Layer {
    public static final DBCKiOverlay INSTANCE = new DBCKiOverlay();

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PlayerDBCData data = PlayerDBCData.get(mc.player);
        float currentKi = data.getCurrentKi();
        float maxKi = Math.max(1.0f, data.getMaxKi());
        float ratio = Math.min(1.0f, Math.max(0.0f, currentKi / maxKi));

        int x = 10;
        int y = 10;
        int barWidth = 115;
        int barHeight = 11;

        // Outer Dark Container
        guiGraphics.fill(x - 3, y - 3, x + barWidth + 3, y + barHeight + 25, 0xCC101015);
        // Subtle Border
        guiGraphics.renderOutline(x - 3, y - 3, barWidth + 6, barHeight + 28, 0xFF4080B0);

        // Ki Bar Background (Dark track)
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF1C1C24);

        // Dynamic Ki Bar Color
        int kiColor = data.isChargingKi() ? 0xFF00FFFF : 0xFF0099FF;
        if (data.getTransformation() == Transformation.SUPER_SAIYAN) {
            kiColor = 0xFFFFD700; // Gold
        } else if (data.getTransformation() == Transformation.KAIOKEN) {
            kiColor = 0xFFFF2233; // Red
        }

        int fillWidth = (int) (barWidth * ratio);
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + barHeight, kiColor);
        }

        // Ki Text inside or over the bar
        String kiText = String.format("Ki: %.0f/%.0f (%.0f%%)", currentKi, maxKi, ratio * 100.0f);
        guiGraphics.drawString(mc.font, kiText, x + 3, y + 2, 0xFFFFFF, true);

        // Battle Power (BP)
        String bpText = String.format("BP: %,d", data.getBattlePower());
        if (data.getTransformation() != Transformation.BASE) {
            bpText += " [" + data.getTransformation().getDisplayName() + "]";
        }
        guiGraphics.drawString(mc.font, bpText, x, y + barHeight + 4, 0xFFE040, true);

        // Race & Status
        String raceText = "Raza: §b" + data.getRace().getDisplayName();
        if (data.isChargingKi()) {
            raceText += " §a[Cargando]";
        } else if (data.isFlying()) {
            raceText += " §e[Volando]";
        }
        guiGraphics.drawString(mc.font, raceText, x, y + barHeight + 15, 0xDDDDDD, true);
    }
}
