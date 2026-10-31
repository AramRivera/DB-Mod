package com.dragonblock.client.overlay;

import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.fx.TransformFx;
import com.dragonblock.client.gui.UiTheme;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD: form panel with smoothly animated Ki bar (top-left), health bar over the heart row with a
 * "ghost" segment for recent damage, a charge ring around the crosshair while powering up, a title
 * card when a transformation completes, and floating TP gains.
 */
public class DBCKiOverlay implements LayeredDraw.Layer {
    public static final DBCKiOverlay INSTANCE = new DBCKiOverlay();
    private static final int BAR_W = 128;

    private float shownKi = -1, shownBody = -1, ghostBody = -1;
    private long ghostHoldUntil = 0;
    private int lastTp = -1;
    private Transformation lastForm = Transformation.BASE;
    private long titleAt = 0;
    private final List<long[]> tpPopups = new ArrayList<>(); // {amount, startMillis}
    private static long whiteOutAt = 0;

    /** Someone's Taiyoken caught your eyes. */
    public static void whiteOut() {
        whiteOutAt = Util.getMillis();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PlayerDBCData data = PlayerDBCData.get(mc.player);
        long now = Util.getMillis();
        Font font = mc.font;
        trackChanges(data, now);
        renderLetterbox(g, deltaTracker);
        renderTransformFlash(g, mc, data, deltaTracker);
        renderWhiteOut(g, now);
        if (mc.gameMode != null && mc.gameMode.canHurtPlayer()) renderHealthBar(g, mc, data, now);
        renderPanel(g, font, data, now);
        renderChargeRing(g, font, data, now);
        renderTitle(g, font, data, now);
        renderTpPopups(g, font, now);
    }

    /** Detects TP gains and completed transformations from the synced data. */
    private void trackChanges(PlayerDBCData data, long now) {
        int tp = data.getTrainingPoints();
        if (lastTp >= 0 && tp > lastTp) tpPopups.add(new long[]{tp - lastTp, now});
        lastTp = tp;
        Transformation form = data.getTransformation();
        if (form != lastForm && form != Transformation.BASE) titleAt = now;
        lastForm = form;
        tpPopups.removeIf(p -> now - p[1] > 1400);
    }

    private static float ease(float current, float target, float speed) {
        return current < 0 ? target : current + (target - current) * speed;
    }

    // ------------------------------------------------------------------ top-left panel

    private void renderPanel(GuiGraphics g, Font font, PlayerDBCData data, long now) {
        Transformation form = data.getTransformation();
        int accent = 0xFF000000 | form.getAuraColor();
        int x = 8, y = 8;
        int h = panelHeight(data);

        g.fill(x - 4, y - 4, x + BAR_W + 4, y + h, 0xA00A0F1E);
        g.fill(x - 4, y - 4, x - 2, y + h, accent);
        g.fill(x - 2, y + h - 1, x + BAR_W + 4, y + h, 0x60000000 | UiTheme.ORANGE);
        g.blit(UiTheme.EMBLEM, x, y - 1, 0, 0, 9, 9, 9, 9);

        g.drawString(font, form.getDisplayName().toUpperCase(), x + 12, y, form == Transformation.BASE ? UiTheme.GOLD : form.getAuraColor(), false);
        String power = String.format("%,d", data.getBattlePower());
        g.drawString(font, power, x + BAR_W - font.width(power), y, UiTheme.TEXT_HEADER, false);
        y += 11;

        // Ki bar: eases toward the real value, glows and shimmers while charging
        float maxKi = Math.max(1f, data.getMaxKi());
        shownKi = ease(shownKi, data.getCurrentKi(), 0.2f);
        float ratio = Mth.clamp(shownKi / maxKi, 0f, 1f);
        boolean charging = data.isChargingKi() || data.isTransformHeld();
        if (charging) {
            int glow = (int) (0x30 + 0x30 * (0.5f + 0.5f * Mth.sin(now / 120f)));
            g.fill(x - 1, y - 1, x + BAR_W + 1, y + 6, glow << 24 | form.getAuraColor());
        }
        g.fill(x, y, x + BAR_W, y + 5, UiTheme.TRACK);
        int fill = (int) (BAR_W * ratio);
        g.fillGradient(x, y, x + fill, y + 5, accent, 0xFF000000 | darken(form.getAuraColor()));
        if (charging && fill > 6) {
            int shine = (int) ((now / 6) % (fill + 20)) - 10;
            g.fill(Math.max(x, x + shine), y, Math.min(x + fill, x + shine + 6), y + 5, 0x70FFFFFF);
        }
        for (int i = 1; i < 10; i++) g.fill(x + BAR_W * i / 10, y, x + BAR_W * i / 10 + 1, y + 5, 0x50000000);
        y += 7;
        g.drawString(font, String.format("KI  %.0f / %.0f", data.getCurrentKi(), maxKi), x, y, UiTheme.TEXT_DIM, false);
        String state = data.isTransformHeld() ? "CARGANDO" : data.isGuarding() ? "GUARDIA" : data.isChargingKi() ? "CONCENTRANDO"
                : data.isFlying() ? "VUELO" : "";
        g.drawString(font, state, x + BAR_W - font.width(state), y, UiTheme.TEXT, false);
        y += 11;

        if (form != Transformation.BASE) chargeBar(g, font, x, y, "Maestría", data.getMastery(form) / 100f, 0xFF000000 | UiTheme.GOLD);
    }

    private static int panelHeight(PlayerDBCData data) {
        int h = 32;
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

    private static int darken(int rgb) {
        return ((rgb >> 16 & 255) * 6 / 10) << 16 | ((rgb >> 8 & 255) * 6 / 10) << 8 | (rgb & 255) * 6 / 10;
    }

    // ------------------------------------------------------------------ health

    /**
     * Body pool bar drawn over the vanilla heart row (vanilla health only mirrors the pool, so hearts are
     * a single row and this panel covers them). Damage leaves a pale "ghost" that drains after a moment.
     */
    private void renderHealthBar(GuiGraphics g, Minecraft mc, PlayerDBCData data, long now) {
        int x = g.guiWidth() / 2 - 91;
        int y = g.guiHeight() - 39;
        int w = 81;
        float max = Math.max(1f, data.getMaxBody());
        float body = data.getBody();
        if (ghostBody < body || ghostBody < 0) ghostBody = body;
        if (shownBody >= 0 && body < shownBody - 0.01f) ghostHoldUntil = now + 500;
        shownBody = ease(shownBody, body, 0.3f);
        if (now > ghostHoldUntil) ghostBody = ease(ghostBody, body, 0.08f);

        boolean hurt = mc.player.hurtTime > 0;
        x += hurt ? (int) (Mth.sin(now / 25f) * 1.5f) : 0;
        float ratio = Mth.clamp(shownBody / max, 0f, 1f);
        float ghost = Mth.clamp(ghostBody / max, 0f, 1f);

        g.fill(x - 1, y - 3, x + w + 1, y + 10, 0xFF0C0D10);
        g.renderOutline(x - 1, y - 3, w + 2, 13, hurt ? 0xFFE04040 : UiTheme.BORDER);
        g.fill(x + 1, y - 1, x + w - 1, y + 8, UiTheme.TRACK);
        g.fill(x + 1, y - 1, x + 1 + (int) ((w - 2) * ghost), y + 8, 0xFFE8D0C0);
        int fill = (int) ((w - 2) * ratio);
        boolean low = ratio < 0.25f;
        int top = low && Mth.sin(now / 150f) > 0 ? 0xFFFF6A50 : 0xFFE0383A;
        g.fillGradient(x + 1, y - 1, x + 1 + fill, y + 8, top, 0xFF8A1420);
        g.fill(x + 1, y - 1, x + 1 + fill, y, 0x50FFFFFF);

        String text = String.format("%,.0f", body);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 0.5f, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(mc.font, text, -mc.font.width(text) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ transformation feedback

    /** Segmented ring around the crosshair that fills while you hold the transform key. */
    private static void renderChargeRing(GuiGraphics g, Font font, PlayerDBCData data, long now) {
        if (!data.isTransformHeld()) return;
        Transformation target = data.getSelectedForm();
        float charge = data.getTransformCharge();
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int segments = 24;
        float radius = 16 + 2 * Mth.sin(now / 90f) * charge;
        for (int i = 0; i < segments; i++) {
            double a = -Math.PI / 2 + Math.PI * 2 * i / segments;
            int px = cx + (int) (Math.cos(a) * radius), py = cy + (int) (Math.sin(a) * radius);
            boolean lit = (float) i / segments < charge;
            g.fill(px - 1, py - 1, px + 2, py + 2, lit ? 0xFF000000 | target.getAuraColor() : 0x50FFFFFF);
        }
        String name = target.getDisplayName();
        g.drawString(font, name, cx - font.width(name) / 2, cy + 24, target.getAuraColor(), true);
    }

    /** Big form name that punches in and fades out after a transformation. */
    private void renderTitle(GuiGraphics g, Font font, PlayerDBCData data, long now) {
        float t = (now - titleAt) / 1800f;
        if (titleAt == 0 || t > 1f) return;
        Transformation form = data.getTransformation();
        float scale = 3f - 1.2f * Mth.clamp(t * 6f, 0f, 1f);
        int alpha = (int) (255 * (t < 0.7f ? 1f : 1f - (t - 0.7f) / 0.3f));
        if (alpha < 8) return;
        String name = form.getDisplayName().toUpperCase();
        g.pose().pushPose();
        g.pose().translate(g.guiWidth() / 2f, g.guiHeight() * 0.3f, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, name, -font.width(name) / 2, -4, alpha << 24 | form.getAuraColor(), true);
        g.pose().popPose();
        int subAlpha = (int) (alpha * Mth.clamp(t * 4f - 0.4f, 0f, 1f));
        if (subAlpha > 8) {
            g.drawCenteredString(font, form.getEssence(), g.guiWidth() / 2, (int) (g.guiHeight() * 0.3f) + 18,
                    subAlpha << 24 | 0xD8DCE3);
        }
    }

    private void renderTpPopups(GuiGraphics g, Font font, long now) {
        int x = 8 + BAR_W + 10, y = 14;
        for (long[] p : tpPopups) {
            float t = (now - p[1]) / 1400f;
            int alpha = (int) (255 * (1f - t));
            if (alpha < 8) continue;
            g.drawString(font, "+" + p[0] + " TP", x, y - (int) (t * 12), alpha << 24 | UiTheme.GOOD, true);
            y += 10;
        }
    }

    /** Black cinema bars while the cinematic camera runs. */
    private static void renderLetterbox(GuiGraphics g, DeltaTracker delta) {
        float k = com.dragonblock.client.fx.CinematicCamera.amount(delta.getGameTimeDeltaPartialTick(false));
        if (k <= 0) return;
        int bar = (int) (g.guiHeight() * 0.11f * k);
        g.fill(0, 0, g.guiWidth(), bar, 0xFF000000);
        g.fill(0, g.guiHeight() - bar, g.guiWidth(), g.guiHeight(), 0xFF000000);
    }

    private static void renderWhiteOut(GuiGraphics g, long now) {
        float t = (now - whiteOutAt) / 2500f;
        if (whiteOutAt == 0 || t > 1f) return;
        int alpha = (int) (255 * (t < 0.3f ? 1f : 1f - (t - 0.3f) / 0.7f));
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0xFFFFF4);
    }

    /** Full-screen flash right after your own transformation completes, strongest for cataclysm forms. */
    private static void renderTransformFlash(GuiGraphics g, Minecraft mc, PlayerDBCData data, DeltaTracker delta) {
        float t = AnimState.progress(mc.player, S2CAnimPacket.Anim.TRANSFORM, TransformFx.BURST_TICKS,
                delta.getGameTimeDeltaPartialTick(false));
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
}
