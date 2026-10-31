package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Hand-built additive geometry for auras, lightning, ki spheres and beams. */
public final class EnergyRender {
    public static final ResourceLocation AURA_TEX = tex("aura");
    public static final ResourceLocation KI_TEX = tex("ki");
    private static final int FULL_BRIGHT = 0xF000F0;

    private EnergyRender() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/" + name + ".png");
    }

    /**
     * Flame aura around a standing body, in model space (y down, feet at y=1.5). {@code size} scales it with
     * the player's power: three nested shells with rising flame tongues, loose wisps, and a ground ring.
     */
    public static void aura(PoseStack pose, MultiBufferSource buffers, float time, int rgb, float intensity,
                            float size, boolean groundRing) {
        float scroll = (time * 0.05f) % 1f;
        VertexConsumer vc = buffers.getBuffer(RenderType.energySwirl(AURA_TEX, time * 0.006f % 1f, -scroll));
        PoseStack.Pose p = pose.last();
        shell(p, vc, time, scaleColor(rgb, intensity * 0.38f), 1.05f * size, size, 0.0f, 0.55f);
        shell(p, vc, time, scaleColor(rgb, intensity * 0.65f), 0.82f * size, size, 0.37f, 0.4f);
        shell(p, vc, time, scaleColor(mix(rgb, 0xFFFFFF, 0.45f), intensity * 0.55f), 0.58f * size, size * 0.92f, 0.71f, 0.25f);
        wisps(p, vc, time, rgb, intensity, size);
        if (groundRing) groundRing(p, vc, time, rgb, intensity, size);
    }

    private static void shell(PoseStack.Pose p, VertexConsumer vc, float time, int color, float radius, float size,
                              float uShift, float tongueAmount) {
        final int sides = 24;
        final int rings = 14;
        for (int r = 0; r < rings; r++) {
            float t0 = (float) r / rings;
            float t1 = (float) (r + 1) / rings;
            for (int s = 0; s < sides; s++) {
                float a0 = Mth.TWO_PI * s / sides;
                float a1 = Mth.TWO_PI * (s + 1) / sides;
                float u0 = (float) s / sides * 3f + uShift;
                float u1 = (float) (s + 1) / sides * 3f + uShift;
                int low = r == 0 ? 0xFF000000 : color; // fade in at the feet
                quad(vc, p, flame(a0, t0, radius, size, time, tongueAmount), flame(a1, t0, radius, size, time, tongueAmount),
                        flame(a1, t1, radius, size, time, tongueAmount), flame(a0, t1, radius, size, time, tongueAmount),
                        u0, u1, t0 * 1.5f, t1 * 1.5f, low, r == rings - 1 ? 0xFF000000 : color);
            }
        }
    }

    /**
     * Point on a flame shell. t=0 at the feet, t=1 at the tip. The silhouette bulges around the chest and
     * breaks into moving tongues toward the top, which also stretch the tip upward.
     */
    private static Vec3 flame(float angle, float t, float radius, float size, float time, float tongueAmount) {
        float bulge = (float) Math.sin(Math.PI * Math.min(1f, 0.15f + t * 1.05f));
        float profile = (0.42f + 0.68f * bulge) * (1f - t * t * 0.9f);
        float tongue = tongueAmount * (float) (
                Math.pow(Math.max(0, Math.sin(angle * 5 + time * 0.31f + t * 4f)), 4) * Math.pow(t, 1.4)
                + 0.6 * Math.pow(Math.max(0, Math.sin(angle * 9 - time * 0.47f + t * 7f)), 6) * t);
        float flicker = 1f + 0.06f * Mth.sin(time * 0.9f + angle * 4f + t * 9f);
        float r = radius * profile * flicker * (1f + tongue * 0.5f);
        float height = 2.6f * (0.8f + 0.2f * size);
        float y = 1.6f - t * height - tongue * 0.9f * t * size;
        return new Vec3(Mth.cos(angle) * r, y, Mth.sin(angle) * r);
    }

    /** Loose flame wisps that peel off the aura and rise above the head before fading. */
    private static void wisps(PoseStack.Pose p, VertexConsumer vc, float time, int rgb, float intensity, float size) {
        for (int i = 0; i < 12; i++) {
            float life = (time * 0.035f + i * 0.137f) % 1f;
            float angle = i * 2.39996f + Mth.sin(time * 0.02f + i) * 0.4f;
            float rad = 0.62f * size * (1f - life * 0.5f);
            float x = Mth.cos(angle) * rad, z = Mth.sin(angle) * rad;
            float y = 1.0f - life * 2.9f * size;
            float h = 0.75f * size * (1f - life);
            float w = 0.14f * size * (1f - life * 0.6f);
            int c = scaleColor(rgb, intensity * 0.7f * (1f - life));
            int tip = 0xFF000000;
            quad(vc, p, new Vec3(x - w, y, z), new Vec3(x + w, y, z), new Vec3(x, y - h, z), new Vec3(x, y - h, z),
                    0f, 1f, 0f, 1f, c, tip);
            quad(vc, p, new Vec3(x, y, z - w), new Vec3(x, y, z + w), new Vec3(x, y - h, z), new Vec3(x, y - h, z),
                    0f, 1f, 0f, 1f, c, tip);
        }
    }

    /** Pressure ring rolling outward on the ground while powering up. */
    private static void groundRing(PoseStack.Pose p, VertexConsumer vc, float time, int rgb, float intensity, float size) {
        final int sides = 32;
        for (int k = 0; k < 2; k++) {
            float life = (time * 0.05f + k * 0.5f) % 1f;
            float inner = (0.5f + life * 2.2f) * size;
            float outer = inner + 0.25f * size;
            int c = scaleColor(rgb, intensity * 0.6f * (1f - life));
            for (int s = 0; s < sides; s++) {
                float a0 = Mth.TWO_PI * s / sides, a1 = Mth.TWO_PI * (s + 1) / sides;
                quad(vc, p,
                        new Vec3(Mth.cos(a0) * inner, 1.48f, Mth.sin(a0) * inner), new Vec3(Mth.cos(a1) * inner, 1.48f, Mth.sin(a1) * inner),
                        new Vec3(Mth.cos(a1) * outer, 1.48f, Mth.sin(a1) * outer), new Vec3(Mth.cos(a0) * outer, 1.48f, Mth.sin(a0) * outer),
                        0f, 1f, 0f, 0.3f, c, 0xFF000000);
            }
        }
    }

    /** Aura size from power: grows with the logarithm of battle power and with the form multiplier. */
    public static float auraSize(long battlePower, float formMultiplier) {
        float fromPower = 0.16f * (float) Math.log10(Math.max(1.0, battlePower / 100.0));
        return Mth.clamp(1.0f + fromPower + 0.05f * (formMultiplier - 1f), 1.0f, 2.4f);
    }

    /** Jagged bolts crackling around the body. Re-rolled every 2 ticks. */
    public static void lightning(PoseStack pose, MultiBufferSource buffers, int seed, int rgb) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose p = pose.last();
        Random rnd = new Random(seed);
        int bolts = 2 + rnd.nextInt(2);
        for (int b = 0; b < bolts; b++) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float x = Mth.cos(a) * 0.45f, z = Mth.sin(a) * 0.45f;
            float y = 1.3f - rnd.nextFloat() * 0.6f;
            for (int i = 0; i < 6; i++) {
                float nx = x + (rnd.nextFloat() - 0.5f) * 0.35f;
                float ny = y - 0.18f - rnd.nextFloat() * 0.2f;
                float nz = z + (rnd.nextFloat() - 0.5f) * 0.35f;
                boltSegment(vc, p, x, y, z, nx, ny, nz, 0.022f, 0xFFFFFF, 220);
                boltSegment(vc, p, x, y, z, nx, ny, nz, 0.06f, rgb, 90);
                x = nx; y = ny; z = nz;
            }
        }
    }

    private static void boltSegment(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0,
                                    float x1, float y1, float z1, float w, int rgb, int alpha) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        // Two crossed ribbons so the bolt reads from any angle
        vc.addVertex(p, x0 - w, y0, z0).setColor(r, g, b, alpha);
        vc.addVertex(p, x1 - w, y1, z1).setColor(r, g, b, alpha);
        vc.addVertex(p, x1 + w, y1, z1).setColor(r, g, b, alpha);
        vc.addVertex(p, x0 + w, y0, z0).setColor(r, g, b, alpha);
        vc.addVertex(p, x0, y0, z0 - w).setColor(r, g, b, alpha);
        vc.addVertex(p, x1, y1, z1 - w).setColor(r, g, b, alpha);
        vc.addVertex(p, x1, y1, z1 + w).setColor(r, g, b, alpha);
        vc.addVertex(p, x0, y0, z0 + w).setColor(r, g, b, alpha);
    }

    /** Glowing ball: white core inside a colored halo. */
    public static void sphere(PoseStack pose, MultiBufferSource buffers, float radius, int rgb, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(KI_TEX));
        float pulse = 1f + 0.07f * Mth.sin(time * 0.9f);
        uvSphere(pose.last(), vc, radius * 0.55f * pulse, scaleColor(mix(rgb, 0xFFFFFF, 0.75f), 1f));
        uvSphere(pose.last(), vc, radius * pulse, scaleColor(rgb, 0.9f));
        uvSphere(pose.last(), vc, radius * 1.45f * pulse, scaleColor(rgb, 0.35f));
    }

    private static void uvSphere(PoseStack.Pose p, VertexConsumer vc, float radius, int color) {
        final int lat = 8, lon = 12;
        for (int i = 0; i < lat; i++) {
            float th0 = Mth.PI * i / lat, th1 = Mth.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float ph0 = Mth.TWO_PI * j / lon, ph1 = Mth.TWO_PI * (j + 1) / lon;
                quad(vc, p, sph(th0, ph0, radius), sph(th0, ph1, radius), sph(th1, ph1, radius), sph(th1, ph0, radius),
                        (float) j / lon, (float) (j + 1) / lon, (float) i / lat, (float) (i + 1) / lat, color, color);
            }
        }
    }

    private static Vec3 sph(float theta, float phi, float r) {
        return new Vec3(Mth.sin(theta) * Mth.cos(phi) * r, Mth.cos(theta) * r, Mth.sin(theta) * Mth.sin(phi) * r);
    }

    /** Cylinder from the origin of the pose to {@code to}, used for ki waves. */
    public static void beam(PoseStack pose, MultiBufferSource buffers, Vec3 to, float radius, int rgb, float time) {
        double len = to.length();
        if (len < 0.05) return;
        Vec3 dir = to.scale(1 / len);
        Vec3 side = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(side).normalize();
        Vec3 v = dir.cross(u).normalize();

        VertexConsumer vc = buffers.getBuffer(RenderType.energySwirl(AURA_TEX, 0f, time * 0.08f % 1f));
        PoseStack.Pose p = pose.last();
        cylinder(vc, p, u, v, to, radius, scaleColor(rgb, 0.9f), (float) len);
        cylinder(vc, p, u, v, to, radius * 0.45f, scaleColor(mix(rgb, 0xFFFFFF, 0.8f), 1f), (float) len);
    }

    private static void cylinder(VertexConsumer vc, PoseStack.Pose p, Vec3 u, Vec3 v, Vec3 to, float radius, int color, float len) {
        final int sides = 10;
        for (int s = 0; s < sides; s++) {
            float a0 = Mth.TWO_PI * s / sides, a1 = Mth.TWO_PI * (s + 1) / sides;
            Vec3 o0 = u.scale(Mth.cos(a0) * radius).add(v.scale(Mth.sin(a0) * radius));
            Vec3 o1 = u.scale(Mth.cos(a1) * radius).add(v.scale(Mth.sin(a1) * radius));
            quad(vc, p, o0, o1, to.add(o1), to.add(o0), 0f, 1f, 0f, len * 0.5f, color, color);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float u0, float u1, float v0, float v1, int colorLow, int colorHigh) {
        // Both windings, the shells are seen from inside and outside
        vert(vc, p, a, u0, v0, colorLow); vert(vc, p, b, u1, v0, colorLow);
        vert(vc, p, c, u1, v1, colorHigh); vert(vc, p, d, u0, v1, colorHigh);
        vert(vc, p, d, u0, v1, colorHigh); vert(vc, p, c, u1, v1, colorHigh);
        vert(vc, p, b, u1, v0, colorLow); vert(vc, p, a, u0, v0, colorLow);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, Vec3 pos, float u, float v, int argb) {
        vc.addVertex(p, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(p, 0f, 1f, 0f);
    }

    /** Additive blending ignores alpha, so intensity darkens the color instead. */
    public static int scaleColor(int rgb, float k) {
        k = Mth.clamp(k, 0f, 1f);
        int r = (int) ((rgb >> 16 & 255) * k), g = (int) ((rgb >> 8 & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
        int g = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return r << 16 | g << 8 | bl;
    }
}
