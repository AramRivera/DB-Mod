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
        int barWidth = 110;
        int barHeight = 9;

        // Background box
        guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xAA000000);
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF1A1A1A);

        // Ki Bar
        int fillWidth = (int) (barWidth * ratio);
        int kiColor = data.isChargingKi() ? 0xFF00E5FF : 0xFF0088FF;
        if (data.getTransformation() == Transformation.SUPER_SAIYAN) {
            kiColor = 0xFFFFCC00;
        } else if (data.getTransformation() == Transformation.KAIOKEN) {
            kiColor = 0xFFFF2222;
        }

        guiGraphics.fill(x, y, x + fillWidth, y + barHeight, kiColor);

        // Text: Ki: 100 / 100
        String kiText = String.format("Ki: %.0f / %.0f (%.0f%%)", currentKi, maxKi, ratio * 100.0f);
        guiGraphics.drawString(mc.font, kiText, x + 3, y + 1, 0xFFFFFF, true);

        // Text: Battle Power / Form below bar
        String bpText = String.format("BP: %,d", data.getBattlePower());
        if (data.getTransformation() != Transformation.BASE) {
            bpText += " [" + data.getTransformation().getDisplayName() + "]";
        }
        guiGraphics.drawString(mc.font, bpText, x, y + barHeight + 4, 0xFFE040, true);

        // Race tag
        String raceText = "Raza: " + data.getRace().getDisplayName();
        guiGraphics.drawString(mc.font, raceText, x, y + barHeight + 14, 0xAAAAAA, true);
    }
}
