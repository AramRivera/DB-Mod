package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.ClientConfig;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

/**
 * While you power up into a strong form the camera glides out of your eyes to a still shot in front of
 * you, holds through the explosion, then glides back.
 * Only your own camera; other players see the transformation in their normal view.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CinematicCamera {
    private static final int HOLD_AFTER = 30;

    private static CameraType restore = null;
    private static float blend = 0f, prevBlend = 0f;
    private static int activeTicks = 0;
    /** Body yaw when the cinematic began; the camera stays put in front of it. */
    private static float lockedYaw = 0f;
    private static Method setPosition;
    private static boolean broken = false;

    private CinematicCamera() {}

    /** Strong forms get the cinematic; small ones transform in your normal view. */
    public static boolean worthy(Transformation form) {
        Transformation.Sequence seq = form.getSequence();
        return form.getStatMultiplier() >= 3f || seq == Transformation.Sequence.CATACLYSM
                || seq == Transformation.Sequence.DIVINE || seq == Transformation.Sequence.GIANT;
    }

    /** 0..1 how much of the cinematic is on screen (for the letterbox bars). */
    public static float amount(float partial) {
        return Mth.lerp(partial, prevBlend, blend);
    }

    /** Client tick: decide whether the cinematic runs, ease it in and out, swap the camera mode. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        prevBlend = blend;
        if (player == null) return;
        if (broken) {
            if (restore != null) mc.options.setCameraType(restore);
            restore = null;
            blend = prevBlend = 0f;
            return;
        }
        PlayerDBCData data = PlayerDBCData.get(player);
        boolean charging = data.isTransformHeld() && worthy(data.getSelectedForm());
        boolean afterBurst = AnimState.progress(player, S2CAnimPacket.Anim.TRANSFORM, HOLD_AFTER, 0) >= 0 && worthy(data.getTransformation());
        boolean on = ClientConfig.CINEMATIC.get() && (charging || afterBurst) && mc.screen == null;

        if (on && activeTicks == 0) lockedYaw = player.yBodyRot;
        activeTicks = on ? activeTicks + 1 : 0;
        blend = Mth.clamp(blend + (on ? 0.12f : -0.1f), 0f, 1f);
        if (blend > 0 && restore == null) {
            restore = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); // detached, so your own body is drawn
        } else if (blend == 0 && restore != null) {
            mc.options.setCameraType(restore);
            restore = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float k = amount((float) event.getPartialTick());
        if (player == null || k <= 0 || restore == null) return;
        PlayerDBCData data = PlayerDBCData.get(player);
        float partial = (float) event.getPartialTick();

        // A still shot straight at the face, framed on the whole body
        float scale = player.getScale();
        float dist = 3.6f * scale;
        float height = 0.15f * scale;
        Vec3 target = player.getPosition(partial).add(0, player.getBbHeight() * 0.58, 0);
        double rad = Math.toRadians(lockedYaw);
        Vec3 wanted = target.add(-Math.sin(rad) * dist, height, Math.cos(rad) * dist);
        var hit = player.level().clip(new ClipContext(target, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) wanted = hit.getLocation().add(target.subtract(hit.getLocation()).normalize().scale(0.3));

        Camera camera = event.getCamera();
        Vec3 from = camera.getPosition();
        Vec3 pos = from.lerp(wanted, k);
        Vec3 look = target.subtract(pos);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        if (!move(camera, pos)) return;
        event.setYaw(event.getYaw() + Mth.wrapDegrees(yaw - event.getYaw()) * k);
        event.setPitch(Mth.lerp(k, event.getPitch(), pitch));
    }

    /** Camera.setPosition is protected; found by signature so it works with production names too. */
    private static boolean move(Camera camera, Vec3 pos) {
        try {
            if (setPosition == null) {
                for (Method m : Camera.class.getDeclaredMethods()) {
                    if (m.getReturnType() == void.class && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Vec3.class) {
                        m.setAccessible(true);
                        setPosition = m;
                    }
                }
                if (setPosition == null) throw new IllegalStateException("Camera has no setPosition(Vec3)");
            }
            setPosition.invoke(camera, pos);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            DragonBlockMod.LOGGER.error("Cinematic camera disabled", e);
            return false;
        }
    }
}
