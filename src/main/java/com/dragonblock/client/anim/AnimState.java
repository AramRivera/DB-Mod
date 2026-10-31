package com.dragonblock.client.anim;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.packet.S2CAnimPacket.Anim;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side animation state for every player in view. Continuous poses (charging, guarding, flying)
 * come from the synced {@link PlayerDBCData}; one-shot moves come from {@code S2CAnimPacket}.
 * First and third person read the same state, so both cameras always agree.
 */
public final class AnimState {
    public static final int BLAST_TICKS = 8;
    public static final int WAVE_RELEASE_TICKS = 14;
    public static final int DASH_TICKS = 6;

    private static final Map<Integer, long[]> ONE_SHOTS = new HashMap<>();

    public enum Pose { NONE, GUARD, BLAST, WAVE_RELEASE, WAVE_CHARGE, POWER_UP, CHARGE, FLY, HOVER }

    private AnimState() {}

    public static void play(int entityId, Anim anim) {
        if (Minecraft.getInstance().level == null) return;
        ONE_SHOTS.computeIfAbsent(entityId, id -> new long[Anim.values().length])[anim.ordinal()] =
                Minecraft.getInstance().level.getGameTime();
    }

    public static void clear() {
        ONE_SHOTS.clear();
    }

    /** 0..1 progress of a one-shot, or -1 when it isn't playing. */
    public static float progress(Player player, Anim anim, int duration, float partialTick) {
        long[] times = ONE_SHOTS.get(player.getId());
        if (times == null || times[anim.ordinal()] == 0) return -1f;
        float age = player.level().getGameTime() - times[anim.ordinal()] + partialTick;
        return age < 0 || age > duration ? -1f : age / duration;
    }

    public static Pose pose(Player player, float partialTick) {
        PlayerDBCData data = PlayerDBCData.get(player);
        if (data.isGuarding()) return Pose.GUARD;
        if (progress(player, Anim.BLAST, BLAST_TICKS, partialTick) >= 0) return Pose.BLAST;
        if (progress(player, Anim.WAVE_RELEASE, WAVE_RELEASE_TICKS, partialTick) >= 0) return Pose.WAVE_RELEASE;
        if (data.isWaveHeld()) return Pose.WAVE_CHARGE;
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
