package com.dragonblock.common.combat;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** What makes each form play differently, beyond its stat multiplier. All server-side. */
public final class FormEffects {
    private FormEffects() {}

    /** Ultra Ego builds rage from damage taken; decays over time. 0..1. */
    private static final String RAGE_KEY = "rage";

    public static void tick(ServerPlayer player, PlayerDBCData data) {
        Transformation form = data.getTransformation();
        if (player.tickCount % 20 != 0) return;
        switch (form) {
            // Divine forms: calm ki control, the body slowly mends
            case SSJ_GOD -> data.setBody(data.getBody() + data.getMaxBody() * 0.01f);
            case SSJ4, MAJIN_PURE -> data.setBody(data.getBody() + data.getMaxBody() * 0.015f);
            case ARCOSIAN_FINAL -> data.setCurrentKi(data.getCurrentKi() + data.getMaxKi() * 0.01f);
            default -> {}
        }
        data.rage = Math.max(0f, data.rage - 0.03f);
    }

    /** Multiplier for melee damage dealt. */
    public static float meleeMultiplier(ServerPlayer player, PlayerDBCData data) {
        return switch (data.getTransformation()) {
            case SSJ3 -> 1.15f;
            case OOZARU, GOLDEN_OOZARU, NAMEKIAN_GIANT -> 1.5f;
            case ULTRA_EGO -> 1f + 0.6f * data.rage;
            case BEAST -> {
                if (player.getRandom().nextFloat() < 0.2f) {
                    critEffect(player);
                    yield 1.6f;
                }
                yield 1f;
            }
            default -> 1f;
        };
    }

    /** Multiplier for ki technique damage. */
    public static float kiMultiplier(PlayerDBCData data) {
        return switch (data.getTransformation()) {
            case SSJ2, SSJ_BLUE_EVOLVED -> 1.15f;
            case OOZARU, GOLDEN_OOZARU -> 1.6f;
            case ULTRA_EGO -> 1f + 0.4f * data.rage;
            default -> 1f;
        };
    }

    /** Multiplier for ki technique cost. Blue is perfect ki control. */
    public static float kiCostMultiplier(PlayerDBCData data) {
        return switch (data.getTransformation()) {
            case SSJ_BLUE, SSJ_BLUE_EVOLVED -> 0.7f;
            case SSJ_GOD -> 0.85f;
            default -> 1f;
        };
    }

    /** Multiplier for damage taken (after DEX). */
    public static float incomingMultiplier(PlayerDBCData data) {
        return switch (data.getTransformation()) {
            case NAMEKIAN_ORANGE -> 0.75f;
            case OOZARU, GOLDEN_OOZARU, NAMEKIAN_GIANT -> 0.8f;
            default -> 1f;
        };
    }

    /** Movement speed multiplier on top of DEX (Grade 3 muscle is heavy, giants are slow). */
    public static float speedMultiplier(PlayerDBCData data) {
        return switch (data.getTransformation()) {
            case OOZARU, GOLDEN_OOZARU, NAMEKIAN_GIANT -> 0.85f;
            default -> 1f;
        };
    }

    /**
     * Ultra Instinct moves on its own: a chance to slip any hit completely, sidestepping the attacker.
     * Returns true when the hit was dodged.
     */
    public static boolean tryDodge(ServerPlayer player, PlayerDBCData data, Vec3 from) {
        float chance = switch (data.getTransformation()) {
            case UI_SIGN -> 0.3f;
            case UI_MASTERED -> 0.55f;
            default -> 0f;
        };
        chance *= 0.6f + 0.4f * data.getMastery(data.getTransformation()) / 100f;
        if (chance <= 0 || player.getRandom().nextFloat() >= chance) return false;

        // Sidestep perpendicular to the incoming direction
        Vec3 dir = from == null ? player.getLookAngle() : player.position().subtract(from).normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(player.getRandom().nextBoolean() ? 0.9 : -0.9);
        player.setDeltaMovement(side.add(0, 0.1, 0));
        player.hurtMarked = true;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7f, 1.6f);
        return true;
    }

    /** Ultra Ego grows stronger the more it is hit. */
    public static void onDamaged(PlayerDBCData data, float bodyFraction) {
        if (data.getTransformation() == Transformation.ULTRA_EGO) {
            data.rage = Math.min(1f, data.rage + bodyFraction * 3f);
        }
    }

    private static void critEffect(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 20, 0.4, 0.5, 0.4, 0.3);
        }
    }
}
