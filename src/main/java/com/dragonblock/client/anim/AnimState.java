package com.dragonblock.client.anim;

import com.dragonblock.client.fx.Debris;
import com.dragonblock.client.fx.LightningStrikes;
import com.dragonblock.client.overlay.DBCKiOverlay;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.network.packet.S2CAnimPacket.Anim;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side animation state for every player in view. Continuous poses (charging, guarding, flying)
 * come from the synced {@link PlayerDBCData}; one-shot moves come from {@code S2CAnimPacket}.
 * First and third person read the same state, so both cameras always agree.
 */
public final class AnimState {
    public static final int DASH_TICKS = 6;

    private static final Map<Integer, Shots> ONE_SHOTS = new HashMap<>();
    /** When the local player last fired each technique, for the cooldown sweep in the HUD. */
    private static final Map<Technique, Long> LOCAL_FIRED = new EnumMap<>(Technique.class);

    private static final class Shots {
        final long[] times = new long[Anim.values().length];
        Technique technique = Technique.KI_BLAST;
    }

    public enum Pose { NONE, GUARD, TECH_FIRE, TECH_CHARGE, POWER_UP, CHARGE, FLY, HOVER }

    private AnimState() {}

    public static void play(int entityId, Anim anim, int arg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        Shots shots = ONE_SHOTS.computeIfAbsent(entityId, id -> new Shots());
        shots.times[anim.ordinal()] = now;
        if (!(mc.level.getEntity(entityId) instanceof Player p)) return;

        if (anim == Anim.TRANSFORM) {
            var form = PlayerDBCData.get(p).getTransformation();
            Debris.blast(p, Mth.clamp(form.getStatMultiplier() / 3f, 0.5f, 2.5f));
            LightningStrikes.burst(p, form);
            if (p != mc.player && mc.player != null) senseKi(mc.player, p, form);
            if (com.dragonblock.client.fx.CinematicCamera.worthy(form)) {
                // A wall of dust rolls out from the blast
                for (int i = 0; i < 48; i++) {
                    double a = Math.PI * 2 * i / 48, v = 0.5 + p.getRandom().nextDouble() * 0.3;
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(),
                            Math.cos(a) * v, 0.02, Math.sin(a) * v);
                }
            }
        } else if (anim == Anim.TECHNIQUE && arg >= 0 && arg < Technique.values().length) {
            Technique tech = Technique.values()[arg];
            shots.technique = tech;
            if (p == mc.player) LOCAL_FIRED.put(tech, now);
            if (tech == Technique.TAIYOKEN && mc.player != null && p != mc.player) solarFlareHits(mc.player, p);
            // Big releases jolt everyone nearby, more the stronger the user
            float weight = switch (tech.getKind()) {
                case BEAM, BALL -> 0.35f;
                case BOMB, SHOCKWAVE -> 0.6f;
                default -> 0f;
            };
            if (weight > 0 && mc.player != null && p.distanceTo(mc.player) < 24) {
                com.dragonblock.client.fx.TransformFx.kick(weight * Technique.powerScale(PlayerDBCData.get(p)) * (1f - p.distanceTo(mc.player) / 24f));
            }
        }
    }

    private static final String[] COMPASS = {"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};

    /** Fighters feel a big power-up nearby: how strong, which way and how far. */
    private static void senseKi(Player self, Player other, com.dragonblock.common.data.Transformation form) {
        float mult = form.getStatMultiplier();
        if (mult < 2f) return;
        double dx = other.getX() - self.getX(), dz = other.getZ() - self.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        String size = mult >= 8 ? "abrumador" : mult >= 5 ? "colosal" : mult >= 3 ? "enorme" : "poderoso";
        double bearing = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = north, clockwise
        String where = dist < 6 ? "justo a tu lado" : "al " + COMPASS[Math.floorMod((int) Math.round(bearing / 45), 8)] + " (" + dist + " m)";
        self.displayClientMessage(net.minecraft.network.chat.Component.literal("Sientes un ki " + size + " " + where)
                .withColor(form.getAuraColor()), true);
    }

    /** Taiyoken blinds whoever is looking at the caster: a white-out on their screen. */
    private static void solarFlareHits(Player viewer, Player caster) {
        Vec3 toCaster = caster.getEyePosition().subtract(viewer.getEyePosition());
        if (toCaster.length() > 14 || viewer.getLookAngle().dot(toCaster.normalize()) < 0.35) return;
        DBCKiOverlay.whiteOut();
    }

    public static void clear() {
        ONE_SHOTS.clear();
        LOCAL_FIRED.clear();
    }

    /** 0..1 progress of a one-shot, or -1 when it isn't playing. */
    public static float progress(Player player, Anim anim, int duration, float partialTick) {
        Shots shots = ONE_SHOTS.get(player.getId());
        if (shots == null || shots.times[anim.ordinal()] == 0) return -1f;
        float age = player.level().getGameTime() - shots.times[anim.ordinal()] + partialTick;
        return age < 0 || age > duration ? -1f : age / duration;
    }

    /** The technique most recently fired by this player. */
    public static Technique firedTechnique(Player player) {
        Shots shots = ONE_SHOTS.get(player.getId());
        return shots == null ? Technique.KI_BLAST : shots.technique;
    }

    /** 0..1 of the release pose of the last fired technique, or -1. */
    public static float fireProgress(Player player, float partialTick) {
        return progress(player, Anim.TECHNIQUE, firedTechnique(player).getSustain(), partialTick);
    }

    /** Remaining cooldown 0..1 of a technique for the local player (1 = just fired). */
    public static float cooldown(Technique tech) {
        Long at = LOCAL_FIRED.get(tech);
        Minecraft mc = Minecraft.getInstance();
        if (at == null || mc.level == null || tech.getCooldown() == 0) return 0f;
        float left = 1f - (mc.level.getGameTime() - at) / (float) tech.getCooldown();
        return Mth.clamp(left, 0f, 1f);
    }

    public static Pose pose(Player player, float partialTick) {
        PlayerDBCData data = PlayerDBCData.get(player);
        if (data.isGuarding()) return Pose.GUARD;
        if (fireProgress(player, partialTick) >= 0) return Pose.TECH_FIRE;
        if (data.isTechHeld() && data.getSelectedTechnique() != null && !data.getSelectedTechnique().isInstant()) return Pose.TECH_CHARGE;
        if (data.isTransformHeld()) return Pose.POWER_UP;
        if (data.isChargingKi()) return Pose.CHARGE;
        if (isFlying(player, data)) return horizontalSpeed(player) > 0.08 ? Pose.FLY : Pose.HOVER;
        return Pose.NONE;
    }

    /** Remote players' abilities aren't synced, so flight is our flag plus being airborne. */
    public static boolean isFlying(Player player, PlayerDBCData data) {
        return data.isFlying() && !player.onGround() && !player.isPassenger();
    }

    public static double horizontalSpeed(Player player) {
        return Math.sqrt(Mth.square(player.getX() - player.xo) + Mth.square(player.getZ() - player.zo));
    }

    /** Body tilt in flight, 0..80 degrees depending on speed. */
    public static float flightTilt(Player player) {
        return (float) Mth.clamp(horizontalSpeed(player) * 140.0, 0.0, 80.0);
    }
}
