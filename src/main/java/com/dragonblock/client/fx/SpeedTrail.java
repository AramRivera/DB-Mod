package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.ClientConfig;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.renderer.DBRenderTypes;
import com.dragonblock.client.renderer.EnergyRender;
import com.dragonblock.common.data.PlayerDBCData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Energy ribbon left behind by players flying at full speed. Visual only, built on every client. */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SpeedTrail {
    /** Blocks per tick above which a flier counts as boosting. */
    public static final double BOOST_SPEED = 0.55;
    private static final int LENGTH = 14;
    private static final Map<Integer, Deque<Vec3>> TRAILS = new HashMap<>();

    private SpeedTrail() {}

    public static boolean isBoosting(Player player) {
        // Only the fast flight (sprinting in the air), cruising stays clean
        return player.isSprinting() && AnimState.isFlying(player, PlayerDBCData.get(player)) && AnimState.horizontalSpeed(player) > BOOST_SPEED;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            TRAILS.clear();
            return;
        }
        for (Player player : mc.level.players()) {
            Deque<Vec3> trail = TRAILS.computeIfAbsent(player.getId(), id -> new ArrayDeque<>());
            if (isBoosting(player)) {
                trail.addFirst(player.position().add(0, player.getBbHeight() * 0.5, 0));
                while (trail.size() > LENGTH) trail.removeLast();
            } else if (!trail.isEmpty()) {
                trail.removeLast(); // the tail catches up and fades
            }
        }
        TRAILS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TRAILS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        float time = mc.level.getGameTime() + event.getPartialTick();
        VertexConsumer vc = buffers.getBuffer(DBRenderTypes.energy(EnergyRender.AURA_TEX, 0f, -time * 0.1f % 1f));
        PoseStack.Pose p = ps.last();

        for (Map.Entry<Integer, Deque<Vec3>> e : TRAILS.entrySet()) {
            if (e.getValue().size() < 2 || !(mc.level.getEntity(e.getKey()) instanceof Player player)) continue;
            if (!ClientConfig.showAura(player == mc.player)) continue;
            int rgb = PlayerDBCData.get(player).getTransformation().getAuraColor();
            Vec3[] pts = e.getValue().toArray(new Vec3[0]);
            float scale = player.getScale();
            for (int i = 0; i < pts.length - 1; i++) {
                float f0 = 1f - (float) i / pts.length, f1 = 1f - (float) (i + 1) / pts.length;
                ribbon(vc, p, pts[i].subtract(cam), pts[i + 1].subtract(cam), 0.55f * scale * f0, 0.55f * scale * f1,
                        EnergyRender.scaleColor(rgb, 0.8f * f0), EnergyRender.scaleColor(rgb, 0.8f * f1), cam, pts[i]);
            }
        }
        buffers.endBatch();
    }

    /** Camera-facing quad between two trail points. */
    private static void ribbon(VertexConsumer vc, PoseStack.Pose p, Vec3 a, Vec3 b, float wa, float wb,
                               int ca, int cb, Vec3 cam, Vec3 world) {
        Vec3 dir = b.subtract(a);
        Vec3 toCam = a.scale(-1).normalize();
        Vec3 side = dir.cross(toCam);
        if (side.lengthSqr() < 1e-6) return;
        side = side.normalize();
        vert(vc, p, a.add(side.scale(wa)), 0, 0, ca);
        vert(vc, p, a.subtract(side.scale(wa)), 1, 0, ca);
        vert(vc, p, b.subtract(side.scale(wb)), 1, 1, cb);
        vert(vc, p, b.add(side.scale(wb)), 0, 1, cb);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, Vec3 v, float u, float w, int argb) {
        vc.addVertex(p, (float) v.x, (float) v.y, (float) v.z).setColor(argb).setUv(u, w)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
