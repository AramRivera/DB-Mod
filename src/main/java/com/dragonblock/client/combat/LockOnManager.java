package com.dragonblock.client.combat;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.gui.UiTheme;
import com.dragonblock.client.renderer.DBRenderTypes;
import com.dragonblock.client.renderer.EnergyRender;
import com.dragonblock.common.entity.NpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Free third-person lock-on targeting inspired by Dragon Ball Xenoverse 2.
 * Keeps manual player camera control fully intact while gently nudging the camera
 * toward the active combat target and rendering a 3D animated targeting reticle in world space.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LockOnManager {
    public static final double MAX_LOCK_DIST = 36.0;
    public static final double BREAK_LOCK_DIST = 44.0;

    private static LivingEntity target = null;
    private static float lockBlend = 0.0f;

    private LockOnManager() {}

    public static LivingEntity getTarget() {
        return target;
    }

    public static boolean isLocked() {
        return target != null && target.isAlive() && !target.isRemoved();
    }

    public static void clear() {
        target = null;
        lockBlend = 0.0f;
    }

    /** Toggles lock-on targeting on or off. */
    public static void toggle(LocalPlayer player) {
        if (isLocked()) {
            clear();
        } else {
            findBestTarget(player);
        }
    }

    /** Finds the candidate most aligned with the player's crosshair inside the lock range. */
    public static void findBestTarget(LocalPlayer player) {
        if (player.level() == null) return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = player.getBoundingBox().inflate(MAX_LOCK_DIST);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> isValidTarget(player, e));
        if (candidates.isEmpty()) {
            clear();
            return;
        }

        LivingEntity best = null;
        double bestScore = -Double.MAX_VALUE;

        for (LivingEntity e : candidates) {
            Vec3 toEntity = e.getEyePosition().subtract(eye);
            double dist = toEntity.length();
            if (dist > MAX_LOCK_DIST || dist < 0.1) continue;

            double dot = look.dot(toEntity.normalize());
            if (dot < 0.25) continue; // Must be roughly in front (~75 degree cone)

            // Prioritize centering in crosshair, penalize distance slightly
            double score = dot * 3.0 - (dist / MAX_LOCK_DIST);
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }

        target = best;
        if (target != null) {
            lockBlend = 1.0f;
        }
    }

    /** Cycles through available targets in the combat area. */
    public static void cycleTarget(LocalPlayer player, boolean forward) {
        if (player.level() == null) return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = player.getBoundingBox().inflate(MAX_LOCK_DIST);

        List<LivingEntity> candidates = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> isValidTarget(player, e)));
        if (candidates.isEmpty()) {
            clear();
            return;
        }

        candidates.sort(Comparator.comparingDouble(e -> {
            Vec3 toE = e.getEyePosition().subtract(eye).normalize();
            return -look.dot(toE);
        }));

        int currentIndex = candidates.indexOf(target);
        if (currentIndex < 0) {
            target = candidates.get(0);
        } else {
            int nextIndex = (currentIndex + (forward ? 1 : -1) + candidates.size()) % candidates.size();
            target = candidates.get(nextIndex);
        }
    }

    private static boolean isValidTarget(LocalPlayer player, LivingEntity e) {
        if (e == player || !e.isAlive() || e.isRemoved() || e.isSpectator()) return false;
        // Do not lock onto peaceful dialogue NPCs
        if (e instanceof NpcEntity) return false;
        // Check line of sight or very close proximity
        return player.hasLineOfSight(e) || player.distanceTo(e) < 6.0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        if (target != null) {
            if (!target.isAlive() || target.isRemoved() || mc.player.distanceTo(target) > BREAK_LOCK_DIST
                    || target.level() != mc.player.level()) {
                clear();
            }
        }
    }

    /** Smooth camera angular assist toward locked target. Keeps full mouse control for the player. */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !isLocked() || mc.options.getCameraType().isFirstPerson()) return;

        float partial = (float) event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 targetCenter = target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0);
        Vec3 toTarget = targetCenter.subtract(camPos);
        double distSq = toTarget.lengthSqr();
        if (distSq < 0.25) return;

        float wantedYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z)));

        float currentYaw = event.getYaw();
        float currentPitch = event.getPitch();

        float yawDiff = Mth.wrapDegrees(wantedYaw - currentYaw);
        float pitchDiff = Mth.clamp(wantedPitch - currentPitch, -35f, 35f);

        // Soft guidance (0.09f per frame) so player can still freely orbit or aim
        event.setYaw(currentYaw + yawDiff * 0.09f);
        event.setPitch(currentPitch + pitchDiff * 0.09f);
    }

    /** Renders the 3D rotating combat reticle in world space centered on the target. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !isLocked()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || target == null) return;

        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        Vec3 targetPos = target.getPosition(partial).add(0, target.getBbHeight() * 0.5, 0);
        Vec3 rel = targetPos.subtract(cam);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        ps.translate(rel.x, rel.y, rel.z);

        float time = mc.level.getGameTime() + partial;
        float radius = Math.max(0.7f, target.getBbWidth() * 0.85f);

        // Rotate to face camera
        ps.mulPose(Axis.YP.rotationDegrees(-event.getCamera().getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(event.getCamera().getXRot()));
        // Slow rotation around reticle axis
        ps.mulPose(Axis.ZP.rotationDegrees(time * 3.5f));

        VertexConsumer vc = buffers.getBuffer(DBRenderTypes.energy(EnergyRender.AURA_TEX, 0, 0));
        PoseStack.Pose p = ps.last();

        int gold = 0xFFFFC93C;
        drawDiamondReticle(vc, p, radius, gold);
        buffers.endBatch();
    }

    private static void drawDiamondReticle(VertexConsumer vc, PoseStack.Pose p, float r, int color) {
        float thick = 0.08f;
        // 4 corner brackets
        bracket(vc, p, r, r, thick, color);
        bracket(vc, p, -r, r, thick, color);
        bracket(vc, p, -r, -r, thick, color);
        bracket(vc, p, r, -r, thick, color);
    }

    private static void bracket(VertexConsumer vc, PoseStack.Pose p, float cx, float cy, float t, int color) {
        float sx = Math.signum(cx), sy = Math.signum(cy);
        float len = Math.abs(cx) * 0.45f;

        // Horizontal segment
        vert(vc, p, cx, cy, 0, color);
        vert(vc, p, cx - sx * len, cy, 0, color);
        vert(vc, p, cx - sx * len, cy - sy * t, 0, color);
        vert(vc, p, cx, cy - sy * t, 0, color);

        // Vertical segment
        vert(vc, p, cx, cy, 0, color);
        vert(vc, p, cx, cy - sy * len, 0, color);
        vert(vc, p, cx - sx * t, cy - sy * len, 0, color);
        vert(vc, p, cx - sx * t, cy, 0, color);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, int argb) {
        vc.addVertex(p, x, y, z)
                .setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, (argb >> 24 & 0xFF) == 0 ? 255 : (argb >> 24 & 0xFF))
                .setUv(0, 0)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(p, 0, 1, 0);
    }
}
