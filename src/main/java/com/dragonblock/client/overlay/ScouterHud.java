package com.dragonblock.client.overlay;

import com.dragonblock.client.anim.AnimState;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.item.ScouterItem;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The scouter in front of your left eye: a green glass lens with scan lines, a reticle on what you look
 * at, a power reading that counts up digit by digit (with a beep when it locks on), and an arrow toward
 * the strongest ki around you. When it overloads, the lens cracks and flashes.
 */
public class ScouterHud implements LayeredDraw.Layer {
    public static final ScouterHud INSTANCE = new ScouterHud();
    private static final int GREEN = 0x5CFF7A;

    private int lockedId = -1;
    private double shownPower = 0;
    private long lockedAt = 0;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float broke = AnimState.progress(mc.player, S2CAnimPacket.Anim.SCOUTER_BREAK, 50, partial);
        if (broke >= 0) {
            renderCrack(g, broke);
            return;
        }
        PlayerDBCData data = PlayerDBCData.get(mc.player);
        if (!data.wears(ScouterItem.ID)) {
            lockedId = -1;
            return;
        }
        Font font = mc.font;
        long now = Util.getMillis();
        int w = g.guiWidth(), h = g.guiHeight();
        int lx = (int) (w * 0.04f), ly = (int) (h * 0.14f), lw = (int) (w * 0.3f), lh = (int) (h * 0.36f);

        // Lens: tinted glass, a bright rim, a moving scan line and a faint grid
        g.fill(lx, ly, lx + lw, ly + lh, 0x2210FF50);
        g.renderOutline(lx, ly, lw, lh, 0x9040FF70);
        g.fill(lx + 2, ly + 2, lx + lw - 2, ly + 3, 0x5080FFA0);
        int scan = ly + (int) ((now / 12) % lh);
        g.fill(lx, scan, lx + lw, scan + 1, 0x4060FF80);
        for (int gx = lx + 12; gx < lx + lw; gx += 12) g.fill(gx, ly + lh - 6, gx + 1, ly + lh - 2, 0x5040FF70);

        // Reticle and reading on whatever is under the crosshair
        LivingEntity target = ScouterItem.lookedAt(mc.player, 64);
        int cx = w / 2, cy = h / 2;
        if (target != null) {
            if (target.getId() != lockedId) {
                lockedId = target.getId();
                shownPower = 0;
                lockedAt = now;
                mc.player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.4f, 1.8f);
            }
            long power = ScouterItem.readPower(target);
            shownPower += (power - shownPower) * 0.12;
            boolean counting = Math.abs(power - shownPower) > Math.max(1, power * 0.01);
            long shown = counting ? (long) shownPower + (now / 40) % 10 : power; // digits flicker while counting

            int s = 14 + (int) (4 * Math.max(0, 1 - (now - lockedAt) / 250f)); // the brackets snap in
            int c = 0xD0000000 | GREEN;
            bracket(g, cx - s, cy - s, 1, 1, c);
            bracket(g, cx + s, cy - s, -1, 1, c);
            bracket(g, cx - s, cy + s, 1, -1, c);
            bracket(g, cx + s, cy + s, -1, -1, c);

            int tx = lx + 6, ty = ly + 8;
            text(g, font, "OBJETIVO", tx, ty, 0x90000000 | GREEN, 0.75f);
            text(g, font, font.plainSubstrByWidth(target.getName().getString(), (int) ((lw - 12) / 1.0f)), tx, ty + 8, 0xFF000000 | GREEN, 1f);
            text(g, font, "PODER DE PELEA", tx, ty + 22, 0x90000000 | GREEN, 0.75f);
            text(g, font, String.format("%,d", shown), tx, ty + 30, 0xFF000000 | (power > data.getBattlePower() * 5 ? 0xFF6050 : GREEN), 1.5f);
            text(g, font, String.format("DIST  %.0f m", mc.player.distanceTo(target)), tx, ty + 46, 0x90000000 | GREEN, 0.75f);
        } else {
            lockedId = -1;
            text(g, font, (now / 500) % 2 == 0 ? "BUSCANDO..." : "BUSCANDO", lx + 6, ly + 8, 0x90000000 | GREEN, 0.75f);
        }
        renderStrongest(g, font, mc, lx, ly, lw, lh);
    }

    /** Arrow at the bottom of the lens toward the strongest other fighter within 128 blocks. */
    private static void renderStrongest(GuiGraphics g, Font font, Minecraft mc, int lx, int ly, int lw, int lh) {
        Player best = null;
        long bestPower = 0;
        for (Player p : mc.level.players()) {
            if (p == mc.player || p.distanceTo(mc.player) > 128) continue;
            long power = PlayerDBCData.get(p).getBattlePower();
            if (power > bestPower) {
                bestPower = power;
                best = p;
            }
        }
        if (best == null) return;
        double dx = best.getX() - mc.player.getX(), dz = best.getZ() - mc.player.getZ();
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz)) - mc.player.getYRot();
        int ax = lx + 14, ay = ly + lh - 18;
        g.pose().pushPose();
        g.pose().translate(ax, ay, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mth.wrapDegrees(bearing))); // up = straight ahead
        for (int i = 0; i < 6; i++) g.fill(-i / 2 - 1, -6 + i, i / 2 + 1, -5 + i, 0xE0000000 | GREEN); // arrow head
        g.fill(-1, 0, 1, 6, 0xE0000000 | GREEN);
        g.pose().popPose();
        text(g, font, String.format("KI %,d · %d m", bestPower, (int) Math.sqrt(dx * dx + dz * dz)), ax + 12, ay - 3, 0xB0000000 | GREEN, 0.75f);
    }

    private static void renderCrack(GuiGraphics g, float t) {
        int w = g.guiWidth(), h = g.guiHeight();
        int lx = (int) (w * 0.04f), ly = (int) (h * 0.14f), lw = (int) (w * 0.3f), lh = (int) (h * 0.36f);
        if (t < 0.08f) g.fill(0, 0, w, h, 0xA0FFFFFF); // the burst
        int alpha = (int) (220 * (1 - t));
        java.util.Random rnd = new java.util.Random(42);
        int ox = lx + lw / 2, oy = ly + lh / 2;
        for (int i = 0; i < 9; i++) {
            float x = ox, y = oy;
            double a = rnd.nextDouble() * Math.PI * 2;
            for (int k = 0; k < 7; k++) {
                a += (rnd.nextDouble() - 0.5) * 0.9;
                float nx = x + (float) Math.cos(a) * 9, ny = y + (float) Math.sin(a) * 9;
                for (int s = 0; s < 9; s++) {
                    int px = (int) Mth.lerp(s / 9f, x, nx), py = (int) Mth.lerp(s / 9f, y, ny);
                    g.fill(px, py, px + 1, py + 1, alpha << 24 | 0xE8FFF0);
                }
                x = nx;
                y = ny;
            }
        }
        g.fill(lx, ly, lx + lw, ly + lh, (alpha / 4) << 24 | 0x80FF90);
    }

    private static void bracket(GuiGraphics g, int x, int y, int sx, int sy, int color) {
        g.fill(Math.min(x, x + sx * 6), y, Math.max(x, x + sx * 6) + 1, y + 1, color);
        g.fill(x, Math.min(y, y + sy * 6), x + 1, Math.max(y, y + sy * 6) + 1, color);
    }

    private static void text(GuiGraphics g, Font font, String s, int x, int y, int argb, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, s, 0, 0, argb, false);
        g.pose().popPose();
    }
}
