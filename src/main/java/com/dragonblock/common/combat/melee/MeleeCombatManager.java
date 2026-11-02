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
 * aerial Boost Dash chases, Meteor Smash downward slams, and Z-Vanish evasions.
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
        public long launchedUntilTick = 0;
        public int launchedByPlayerId = 0;
        public int chasingTargetId = 0;
        public long chaseWindowUntilTick = 0;
        public long lastIncomingAttackTick = 0;
        public UUID lastAttackerUUID = null;
        public long groundSlamUntilTick = 0;
    }

    private static final Map<UUID, ComboState> STATES = new HashMap<>();

    private MeleeCombatManager() {}

    public static ComboState getState(UUID id) {
        return STATES.computeIfAbsent(id, k -> new ComboState());
    }

    public static void clear(UUID id) {
        STATES.remove(id);
    }

    public static boolean isTargetLaunched(UUID id, long now) {
        ComboState state = STATES.get(id);
        return state != null && now < state.launchedUntilTick;
    }

    public static void handleCombatAction(ServerPlayer player, C2SCombatActionPacket.Action action, int targetId) {
        if (!player.isAlive()) return;
        ComboState state = getState(player.getUUID());
        long now = player.level().getGameTime();

        // While in hitstun / guard break, only EVASIVE_VANISH is permitted
        if (now < state.stunUntilTick && action != C2SCombatActionPacket.Action.EVASIVE_VANISH) {
            return;
        }

        switch (action) {
            case LIGHT_ATTACK -> handleLightAttack(player, state, targetId, now);
            case HEAVY_ATTACK -> handleHeavyAttack(player, state, targetId, now);
            case GUARD_START -> {
                state.isGuarding = true;
                PlayerDBCData.get(player).setGuarding(true);
                com.dragonblock.common.combat.training.MasterTrainingManager.onCombatAction(player, com.dragonblock.common.combat.training.TrainingChallenge.Type.GUARD_DEFENSE);
            }
            case GUARD_END -> {
                state.isGuarding = false;
                PlayerDBCData.get(player).setGuarding(false);
            }
            case DODGE -> handleDodge(player, state, now);
            case BOOST_CHASE -> handleBoostChase(player, state, targetId, now);
            case METEOR_SMASH -> handleMeteorSmash(player, state, targetId, now);
            case EVASIVE_VANISH -> handleEvasiveVanish(player, state, now);
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

        // Check for Perfect Dodge (dodging within 5 ticks of an incoming attack)
        boolean perfect = (now - state.lastIncomingAttackTick) <= 5 && state.lastAttackerUUID != null;
        float kiCost = perfect ? 4f : 12f;

        if (data.consumeKi(kiCost)) {
            state.nextAllowedAttackTick = now + (perfect ? 6 : 10);
            Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
            player.push(dir.x * (perfect ? 1.45 : 1.1), 0.15, dir.z * (perfect ? 1.45 : 1.1));
            player.hurtMarked = true;

            if (perfect) {
                // Perfect Dodge reward and feedback
                player.displayClientMessage(Component.literal("¡Esquiva Perfecta!"), true);
                data.addTrainingPoints(10);
                com.dragonblock.common.combat.training.MasterTrainingManager.onCombatAction(player, com.dragonblock.common.combat.training.TrainingChallenge.Type.PERFECT_DODGE);
                if (data.getTransformation() != Transformation.BASE) {
                    data.addMastery(data.getTransformation(), 0.10f);
                }
                ServerLevel level = (ServerLevel) player.level();
                level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.8f);

                // Punish attacker with whiff freeze
                ComboState attackerState = STATES.get(state.lastAttackerUUID);
                if (attackerState != null) {
                    attackerState.nextAllowedAttackTick = now + 14;
                }
            } else {
                ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.DASH, 0);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.8f);
            }
        }
    }

    /** Boost Dash Chase: Launches the player at high velocity towards an airborne/launched opponent. */
    private static void handleBoostChase(ServerPlayer player, ComboState state, int targetId, long now) {
        LivingEntity target = null;
        Entity entity = player.level().getEntity(targetId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            target = living;
        } else if (state.chasingTargetId != 0) {
            Entity chasing = player.level().getEntity(state.chasingTargetId);
            if (chasing instanceof LivingEntity living && living.isAlive()) {
                target = living;
            }
        }
        if (target == null || target == player) return;

        PlayerDBCData data = PlayerDBCData.get(player);
        if (!data.consumeKi(12f)) return;

        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(player.getEyePosition());
        double dist = toTarget.length();
        if (dist > 32.0 || dist < 1.0) return;

        Vec3 homing = toTarget.normalize().scale(Math.min(2.4, Math.max(1.5, dist * 0.35)));
        player.setDeltaMovement(homing);
        player.hurtMarked = true;

        state.chasingTargetId = target.getId();
        state.chaseWindowUntilTick = now + 30;

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 1.6f);
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.DASH, 0);
    }

    /** Meteor Smash: Downward hammer smash on an airborne opponent, rocketing them to the ground. */
    private static void handleMeteorSmash(ServerPlayer player, ComboState state, int targetId, long now) {
        LivingEntity target = findTarget(player, targetId);
        if (target == null) return;

        PlayerDBCData data = PlayerDBCData.get(player);
        Transformation form = data.getTransformation();
        float str = (float) data.getEffectiveStr();
        float baseDmg = (4.0f + str * 0.45f) * 1.85f;
        float totalDamage = baseDmg * (form.getStatMultiplier() > 1f ? (1f + (form.getStatMultiplier() - 1f) * 0.6f) : 1f);

        // Slam target downward into the floor
        target.hurt(player.damageSources().playerAttack(player), totalDamage);
        target.setDeltaMovement(0, -1.85, 0);
        target.hurtMarked = true;

        if (target instanceof ServerPlayer tp) {
            getState(tp.getUUID()).groundSlamUntilTick = now + 35;
        }

        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.METEOR_SMASH, 0);

        ServerLevel level = (ServerLevel) player.level();
        Vec3 impact = target.position().add(0, target.getBbHeight() * 0.8, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.7f);

        data.addTrainingPoints(5);
        com.dragonblock.common.combat.training.MasterTrainingManager.onCombatAction(player, com.dragonblock.common.combat.training.TrainingChallenge.Type.AERIAL_CHASE);
        if (form != Transformation.BASE) {
            data.addMastery(form, 0.10f);
        }
        data.saveToPlayer(player);
    }

    /** Z-Vanish Evasive: Spends Ki to break out of an opponent's combo string and appear behind them. */
    private static void handleEvasiveVanish(ServerPlayer player, ComboState state, long now) {
        PlayerDBCData data = PlayerDBCData.get(player);
        if (!data.consumeKi(30f)) {
            player.displayClientMessage(Component.literal("Ki insuficiente para Z-Vanish evasivo (Requiere 30 Ki)."), true);
            return;
        }

        state.stunUntilTick = 0; // Break stun cleanly
        state.nextAllowedAttackTick = now + 8;

        // Teleport behind whoever last attacked this player or behind own look vector
        Vec3 dest;
        if (state.lastAttackerUUID != null && player.level().getPlayerByUUID(state.lastAttackerUUID) instanceof ServerPlayer attacker) {
            dest = attacker.position().subtract(attacker.getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
        } else {
            dest = player.position().subtract(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
        }

        player.teleportTo(dest.x, dest.y + 0.1, dest.z);
        player.hurtMarked = true;

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.2, 0.4, 0.2, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.3f, 1.7f);
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.VANISH, 0);
        player.displayClientMessage(Component.literal("¡Z-Vanish Evasivo!"), true);
    }

    private static LivingEntity findTarget(ServerPlayer player, int targetId) {
        double maxDist = BASE_REACH * player.getScale();
        if (targetId != 0) {
            Entity entity = player.level().getEntity(targetId);
            if (entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && !(living instanceof NpcEntity)) {
                if (player.distanceTo(living) <= maxDist + living.getBbWidth() + 1.0) {
                    Vec3 toTarget = living.getEyePosition().subtract(player.getEyePosition()).normalize();
                    if (player.getLookAngle().dot(toTarget) > 0.15) {
                        return living;
                    }
                }
            }
        }

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
            if (look.dot(toE.normalize()) > 0.35 && d < bestDist) {
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

        // Combo scaling
        float comboScale = Math.max(0.64f, 1.0f - (comboStep * 0.12f));
        float formMultiplier = form.getStatMultiplier();
        float totalDamage = baseDmg * comboScale * (formMultiplier > 1f ? (1f + (formMultiplier - 1f) * 0.5f) : 1f);

        // Record incoming attack on defender for Perfect Dodge detection
        if (target instanceof ServerPlayer targetPlayer) {
            ComboState targetState = getState(targetPlayer.getUUID());
            targetState.lastIncomingAttackTick = now;
            targetState.lastAttackerUUID = attacker.getUUID();

            if (targetState.isGuarding) {
                PlayerDBCData targetData = PlayerDBCData.get(targetPlayer);
                boolean kiSufficient = targetData.consumeKi(totalDamage * 1.4f);
                if (!kiSufficient || isHeavy) {
                    // GUARD BREAK!
                    targetState.stunUntilTick = now + 35;
                    targetData.setGuarding(false);
                    targetState.isGuarding = false;
                    targetPlayer.displayClientMessage(Component.literal("¡Ruptura de Guardia! Aturdido"), true);
                    attacker.displayClientMessage(Component.literal("¡Rompiste su guardia!"), true);
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 1.2f);
                } else {
                    totalDamage *= 0.32f; // Blocked 68%
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.9f);
                }
            } else {
                // Apply hitstun to target (stuns for 12 ticks, allowing combo follow-up or evasive vanish)
                targetState.stunUntilTick = now + 12;
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

            // Mark target as launched for the chase window!
            if (target instanceof ServerPlayer tp) {
                ComboState ts = getState(tp.getUUID());
                ts.launchedUntilTick = now + 25;
                ts.launchedByPlayerId = attacker.getId();
            }
            ComboState as = getState(attacker.getUUID());
            as.chasingTargetId = target.getId();
            as.chaseWindowUntilTick = now + 25;

            level.sendParticles(ParticleTypes.SONIC_BOOM, impactPos.x, impactPos.y, impactPos.z, 1, 0, 0, 0, 0);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.4f, 0.7f);
            attacker.displayClientMessage(Component.literal("¡Enemigo por los aires! Presiona [R] para perseguir"), true);
            com.dragonblock.common.combat.training.MasterTrainingManager.onCombatAction(attacker, com.dragonblock.common.combat.training.TrainingChallenge.Type.COMBO_CHAIN);
        } else if (isHeavy || comboStep == 2) {
            target.push(dir.x * 0.65, 0.18, dir.z * 0.65);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 8, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.9f);
        } else {
            target.push(dir.x * 0.12, 0.04, dir.z * 0.12);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 4, 0.1, 0.1, 0.1, 0.05);
            level.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.1f + comboStep * 0.1f);
        }

        // Active Transformation Mastery Reward
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
