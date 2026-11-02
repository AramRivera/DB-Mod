package com.dragonblock.common.combat.melee;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SCombatActionPacket;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.dragonblock.common.world.SafeZones;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative Melee Combat and Combo Manager inspired by Dragon Ball Xenoverse 2.
 * Validates range, angles, cooldowns, combo timing, damage scaling, guard breaking,
 * knockbacks and transformation mastery rewards.
 */
public final class MeleeCombatManager {
    public static final int COMBO_WINDOW_TICKS = 18;
    public static final int LIGHT_COOLDOWN_TICKS = 6;
    public static final int HEAVY_COOLDOWN_TICKS = 10;
    public static final double BASE_REACH = 4.6;

    public static final class ComboState {
        public int step = 0;
        public long lastAttackTick = 0;
        public long nextAllowedAttackTick = 0;
        public long stunUntilTick = 0;
        public boolean isGuarding = false;
    }

    private static final Map<UUID, ComboState> STATES = new HashMap<>();

    private MeleeCombatManager() {}

    public static ComboState getState(UUID id) {
        return STATES.computeIfAbsent(id, k -> new ComboState());
    }

    public static void clear(UUID id) {
        STATES.remove(id);
    }

    public static void handleCombatAction(ServerPlayer player, C2SCombatActionPacket.Action action, int targetId) {
        if (!player.isAlive()) return;
        ComboState state = getState(player.getUUID());
        long now = player.level().getGameTime();

        if (now < state.stunUntilTick) {
            // Player is stunned / guard broken
            return;
        }

        switch (action) {
            case LIGHT_ATTACK -> handleLightAttack(player, state, targetId, now);
            case HEAVY_ATTACK -> handleHeavyAttack(player, state, targetId, now);
            case GUARD_START -> {
                state.isGuarding = true;
                PlayerDBCData.get(player).setGuarding(true);
            }
            case GUARD_END -> {
                state.isGuarding = false;
                PlayerDBCData.get(player).setGuarding(false);
            }
            case DODGE -> handleDodge(player, state, now);
        }
    }

    private static void handleLightAttack(ServerPlayer player, ComboState state, int targetId, long now) {
        if (now < state.nextAllowedAttackTick) return;

        // Reset combo if player waited too long
        if (now - state.lastAttackTick > COMBO_WINDOW_TICKS) {
            state.step = 0;
        }

        int currentStep = state.step;
        state.step = (currentStep + 1) % 4;
        state.lastAttackTick = now;
        state.nextAllowedAttackTick = now + (currentStep == 3 ? 12 : LIGHT_COOLDOWN_TICKS);

        S2CAnimPacket.Anim anim = switch (currentStep) {
            case 0 -> S2CAnimPacket.Anim.PUNCH_LIGHT_1;
            case 1 -> S2CAnimPacket.Anim.PUNCH_LIGHT_2;
            case 2 -> S2CAnimPacket.Anim.KICK_HEAVY;
            default -> S2CAnimPacket.Anim.UPPERCUT;
        };

        // Broadcast visual animation to all tracking players
        ModMessages.broadcastAnim(player, anim, 0);

        // Find and resolve target hit
        LivingEntity target = findTarget(player, targetId);
        if (target != null) {
            applyMeleeHit(player, target, currentStep, false, now);
        } else {
            // Miss swoosh sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.4f + currentStep * 0.1f);
        }
    }

    private static void handleHeavyAttack(ServerPlayer player, ComboState state, int targetId, long now) {
        if (now < state.nextAllowedAttackTick) return;

        state.step = 0;
        state.lastAttackTick = now;
        state.nextAllowedAttackTick = now + HEAVY_COOLDOWN_TICKS;

        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.KICK_HEAVY, 0);

        LivingEntity target = findTarget(player, targetId);
        if (target != null) {
            applyMeleeHit(player, target, 2, true, now);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.9f);
        }
    }

    private static void handleDodge(ServerPlayer player, ComboState state, long now) {
        if (now < state.nextAllowedAttackTick) return;
        PlayerDBCData data = PlayerDBCData.get(player);
        if (data.consumeKi(12f)) {
            state.nextAllowedAttackTick = now + 10;
            Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
            player.push(dir.x * 1.1, 0.15, dir.z * 1.1);
            player.hurtMarked = true;
            ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.DASH, 0);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, int targetId) {
        double maxDist = BASE_REACH * player.getScale();
        if (targetId != 0) {
            Entity entity = player.level().getEntity(targetId);
            if (entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && !(living instanceof NpcEntity)) {
                if (player.distanceTo(living) <= maxDist + living.getBbWidth()) {
                    Vec3 toTarget = living.getEyePosition().subtract(player.getEyePosition()).normalize();
                    if (player.getLookAngle().dot(toTarget) > 0.2) {
                        return living;
                    }
                }
            }
        }

        // Raycast / cone search ahead
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = player.getBoundingBox().inflate(maxDist);

        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof NpcEntity));

        LivingEntity best = null;
        double bestDist = maxDist;

        for (LivingEntity e : list) {
            Vec3 toE = e.getEyePosition().subtract(eye);
            double d = toE.length();
            if (d > maxDist) continue;
            if (look.dot(toE.normalize()) > 0.4 && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static void applyMeleeHit(ServerPlayer attacker, LivingEntity target, int comboStep, boolean isHeavy, long now) {
        if (SafeZones.protects(attacker, target) || SafeZones.isSafe(target.level(), target.position())) {
            attacker.displayClientMessage(Component.literal("Objetivo protegido en zona segura."), true);
            return;
        }

        PlayerDBCData data = PlayerDBCData.get(attacker);
        Transformation form = data.getTransformation();

        float str = (float) data.getEffectiveStr();
        float baseDmg = 4.0f + str * 0.45f;
        if (isHeavy) baseDmg *= 1.45f;
        if (comboStep == 3) baseDmg *= 1.65f;

        // Combo scaling: each hit within a combo scales down by 12% to prevent infinite/unfair bursts
        float comboScale = Math.max(0.64f, 1.0f - (comboStep * 0.12f));
        float formMultiplier = form.getStatMultiplier();
        float totalDamage = baseDmg * comboScale * (formMultiplier > 1f ? (1f + (formMultiplier - 1f) * 0.5f) : 1f);

        boolean targetGuarding = false;
        if (target instanceof ServerPlayer targetPlayer) {
            PlayerDBCData targetData = PlayerDBCData.get(targetPlayer);
            ComboState targetState = getState(targetPlayer.getUUID());
            if (targetData.isGuarding() || targetState.isGuarding) {
                targetGuarding = true;
                boolean kiSufficient = targetData.consumeKi(totalDamage * 1.4f);
                if (!kiSufficient || isHeavy) {
                    // GUARD BREAK!
                    targetState.stunUntilTick = now + 35;
                    targetData.setGuarding(false);
                    targetPlayer.displayClientMessage(Component.literal("¡Ruptura de Guardia! Aturdido"), true);
                    attacker.displayClientMessage(Component.literal("¡Rompiste su guardia!"), true);
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 1.2f);
                } else {
                    totalDamage *= 0.32f; // Blocked 68%
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.9f);
                }
            }
        }

        // Apply damage
        target.hurt(attacker.damageSources().playerAttack(attacker), totalDamage);

        // Knockback vector
        Vec3 dir = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
        if (dir.lengthSqr() < 1e-4) dir = attacker.getLookAngle().multiply(1, 0, 1).normalize();

        ServerLevel level = (ServerLevel) attacker.level();
        Vec3 impactPos = target.position().add(0, target.getBbHeight() * 0.6, 0);

        if (comboStep == 3) {
            // Launch finisher (Uppercut) sends opponent into the sky for aerial follow-up
            target.push(dir.x * 0.35, 0.82, dir.z * 0.35);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.SONIC_BOOM, impactPos.x, impactPos.y, impactPos.z, 1, 0, 0, 0, 0);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.4f, 0.7f);
        } else if (isHeavy || comboStep == 2) {
            // Heavy kick knockback
            target.push(dir.x * 0.65, 0.18, dir.z * 0.65);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 8, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.9f);
        } else {
            // Light stagger
            target.push(dir.x * 0.12, 0.04, dir.z * 0.12);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 4, 0.1, 0.1, 0.1, 0.05);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.1f + comboStep * 0.1f);
        }

        // Active Transformation Mastery Reward: landed strikes grant +0.05% mastery
        if (form != Transformation.BASE) {
            float currentMastery = data.getMastery(form);
            if (currentMastery < 100.0f) {
                data.addMastery(form, 0.05f);
            }
        }

        // TP Reward for physical combat
        data.addTrainingPoints(1 + (comboStep == 3 ? 2 : 0));
        data.saveToPlayer(attacker);
    }
}
