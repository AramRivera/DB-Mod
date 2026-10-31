package com.dragonblock.client.overlay;

import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.fx.TransformFx;
import com.dragonblock.client.gui.UiTheme;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/** Compact top-left HUD: form, Ki bar, power level and contextual charge bars. */
public class DBCKiOverlay implements LayeredDraw.Layer {
    public static final DBCKiOverlay INSTANCE = new DBCKiOverlay();
    private static final int BAR_W = 128;

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PlayerDBCData data = PlayerDBCData.get(mc.player);
        Font font = mc.font;
        renderTransformFlash(g, mc, data);
        if (mc.gameMode != null && mc.gameMode.canHurtPlayer()) renderHealthBar(g, mc, data);
        Transformation form = data.getTransformation();
        int accent = 0xFF000000 | form.getAuraColor();
        int x = 8, y = 8;

        g.fill(x - 4, y - 4, x + BAR_W + 4, y + panelHeight(data), 0x90000000);
        g.fill(x - 4, y - 4, x - 2, y + panelHeight(data), accent);

        g.drawString(font, form.getDisplayName().toUpperCase(), x, y, form.getAuraColor(), false);
        String power = String.format("%,d", data.getBattlePower());
        g.drawString(font, power, x + BAR_W - font.width(power), y, UiTheme.TEXT_HEADER, false);
        y += 11;

        // Ki bar with 10% ticks
        float ratio = Math.min(1f, data.getCurrentKi() / Math.max(1f, data.getMaxKi()));
        g.fill(x, y, x + BAR_W, y + 5, UiTheme.TRACK);
        g.fill(x, y, x + (int) (BAR_W * ratio), y + 5, data.isChargingKi() ? 0xFFFFFFFF : accent);
        for (int i = 1; i < 10; i++) g.fill(x + BAR_W * i / 10, y, x + BAR_W * i / 10 + 1, y + 5, 0x60000000);
        y += 7;
        g.drawString(font, String.format("KI  %.0f / %.0f", data.getCurrentKi(), data.getMaxKi()), x, y, UiTheme.TEXT_DIM, false);
        String state = data.isGuarding() ? "GUARDIA" : data.isChargingKi() ? "CARGANDO" : data.isFlying() ? "VUELO" : "";
        g.drawString(font, state, x + BAR_W - font.width(state), y, UiTheme.TEXT, false);
        y += 11;

        if (data.isTransformHeld()) {
            Transformation target = data.getSelectedForm();
            y = chargeBar(g, font, x, y, target.getDisplayName(), data.getTransformCharge(), 0xFF000000 | target.getAuraColor());
        }
        if (data.isWaveHeld()) {
            y = chargeBar(g, font, x, y, "Onda de ki", data.getWaveCharge(), accent);
        }
        if (form != Transformation.BASE) {
            chargeBar(g, font, x, y, "Maestría", data.getMastery(form) / 100f, 0xFF8090A0);
        }
    }

    /** Full-screen flash right after your own transformation completes, strongest for cataclysm forms. */
    private static void renderTransformFlash(GuiGraphics g, Minecraft mc, PlayerDBCData data) {
        float t = AnimState.progress(mc.player, S2CAnimPacket.Anim.TRANSFORM, TransformFx.BURST_TICKS,
                mc.getTimer().getGameTimeDeltaPartialTick(false));
        if (t < 0 || t > 0.5f) return;
        Transformation form = data.getTransformation();
        float strength = switch (form.getSequence()) {
            case CATACLYSM -> 0.85f;
            case DIVINE -> 0.45f;
            case STRAIN -> 0.35f;
            default -> 0.6f;
        };
        int alpha = (int) (255 * strength * (1f - t / 0.5f));
        int rgb = form.getSequence() == Transformation.Sequence.DIVINE ? 0xFFFFFF : form.getAuraColor();
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | rgb);
    }

    /**
     * Body pool bar drawn over the vanilla heart row (vanilla health only mirrors the pool, so the hearts
     * are always a single row and this panel fully covers them, including their low-health jiggle).
     */
    private static void renderHealthBar(GuiGraphics g, Minecraft mc, PlayerDBCData data) {
        int x = g.guiWidth() / 2 - 91;
        int y = g.guiHeight() - 39;
        int w = 81;
        float ratio = Math.min(1f, data.getBody() / Math.max(1f, data.getMaxBody()));
        boolean hurt = mc.player.hurtTime > 0;

        g.fill(x - 1, y - 3, x + w + 1, y + 10, 0xFF0C0D10);
        g.renderOutline(x - 1, y - 3, w + 2, 13, hurt ? 0xFFE04040 : UiTheme.BORDER);
        g.fill(x + 1, y - 1, x + w - 1, y + 8, UiTheme.TRACK);
        int fill = (int) ((w - 2) * ratio);
        g.fillGradient(x + 1, y - 1, x + 1 + fill, y + 8, ratio < 0.25f ? 0xFFFF4A3A : 0xFFE0383A, 0xFF8A1420);
        g.fill(x + 1, y - 1, x + 1 + fill, y, 0x50FFFFFF); // top highlight

        String text = String.format("%,.0f", data.getBody());
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 0.5f, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(mc.font, text, -mc.font.width(text) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    private static int panelHeight(PlayerDBCData data) {
        int h = 32;
        if (data.isTransformHeld()) h += 14;
        if (data.isWaveHeld()) h += 14;
        if (data.getTransformation() != Transformation.BASE) h += 14;
        return h;
    }

    private static int chargeBar(GuiGraphics g, Font font, int x, int y, String label, float value, int color) {
        g.drawString(font, label, x, y, UiTheme.TEXT_DIM, false);
        String pct = (int) (value * 100) + "%";
        g.drawString(font, pct, x + BAR_W - font.width(pct), y, UiTheme.TEXT, false);
        g.fill(x, y + 9, x + BAR_W, y + 11, UiTheme.TRACK);
        g.fill(x, y + 9, x + (int) (BAR_W * value), y + 11, color);
        return y + 14;
    }
}
