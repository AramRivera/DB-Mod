package com.dragonblock.client.overlay;

import com.dragonblock.common.entity.SpaceshipEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;

import java.util.Random;

/** Hyperspace: star streaks rushing past and a deep blue wash while your ship jumps between planets. */
public class WarpOverlay implements LayeredDraw.Layer {
    public static final WarpOverlay INSTANCE = new WarpOverlay();

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof SpaceshipEntity ship)) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float age = ship.stateAge() + partial;
        float k = switch (ship.getState()) {
            case LIFTOFF -> Math.max(0f, (age - 25f) / 25f);
            case LANDING -> Math.max(0f, 1f - age / 25f);
            default -> 0f;
        };
        if (k <= 0) return;
        k = Math.min(1f, k);
        int w = g.guiWidth(), h = g.guiHeight(), cx = w / 2, cy = h / 2;
        g.fill(0, 0, w, h, (int) (k * 200) << 24 | 0x040A24);
        float reach = Math.max(cx, cy) * 1.5f;
        Random rnd = new Random(42);
        var vc = g.bufferSource().getBuffer(RenderType.gui());
        var m = g.pose().last().pose();
        float time = (mc.level.getGameTime() + partial) * 0.08f;
        for (int i = 0; i < 140; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float phase = (rnd.nextFloat() + time * (0.6f + rnd.nextFloat())) % 1f;
            float r0 = reach * phase * phase, r1 = r0 + reach * 0.25f * k * phase;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a), wdt = 0.4f + 1.2f * phase;
            int color = (int) (k * 230 * phase) << 24 | (rnd.nextInt(3) == 0 ? 0x9FC8FF : 0xFFFFFF);
            float x0 = cx + cos * r0, y0 = cy + sin * r0, x1 = cx + cos * r1, y1 = cy + sin * r1;
            float nx = -sin * wdt, ny = cos * wdt;
            for (int side = 0; side < 2; side++) {
                float s = side == 0 ? 1 : -1; // both windings, in case the gui type culls
                vc.addVertex(m, x0 - nx * s, y0 - ny * s, 0).setColor(color);
                vc.addVertex(m, x0 + nx * s, y0 + ny * s, 0).setColor(color);
                vc.addVertex(m, x1 + nx * s, y1 + ny * s, 0).setColor(color);
                vc.addVertex(m, x1 - nx * s, y1 - ny * s, 0).setColor(color);
            }
        }
        g.flush();
    }
}
