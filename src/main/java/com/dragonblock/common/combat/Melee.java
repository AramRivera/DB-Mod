package com.dragonblock.common.combat;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side hand-to-hand fighting. The client only says "light" or "heavy"; the server decides whether
 * the move may start, walks it through startup → active → recovery, looks for the target in front of the
 * fists during the active window (one hit per move) and lands it through the normal damage path, so
 * guard, dodges, dash i-frames, safe zones, TP rewards and mastery all apply as for any other punch.
 */
public final class Melee {
    /** Hits after this many in one combo still hurt but no longer stun: every combo has an exit. */
    private static final int STUN_LIMIT = 8;
    /** Ticks without landing a hit before the combo counter (and its damage scaling) resets. */
    private static final int COMBO_RESET = 30;
    private static final int GUARD_BREAK_STUN = 30;

    /** True while one of our hits is inside LivingEntity.hurt, so the generic punch bonus isn't added twice. */
    static boolean resolving = false;

    private Melee() {}

    /** Per-player fight state. Live only: never saved, a relog or respawn starts clean. */
    public static final class State {
        MeleeMove move;
        int age;
        MeleeMove queued;
        boolean landed;
        /** Ticks this player can't attack or guard (hit, guard broken). */
        public int stun;
        int comboHits;
        long lastHitTime;
    }

    public static boolean isResolving() { return resolving; }

    /** A press from the client: starts a move, or queues the next link of the chain. */
    public static void request(ServerPlayer player, PlayerDBCData data, boolean heavy) {
        State s = data.melee;
        if (!canAct(player, data)) return;
        if (s.move == null) {
            start(player, data, MeleeMove.opener(heavy));
        } else if (s.queued == null && s.age >= 2) {
            s.queued = s.move.next(heavy); // buffer the follow-up (null once the chain is over); 2 ticks so a double click isn't two presses
        }
    }

    private static boolean canAct(ServerPlayer player, PlayerDBCData data) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && player.getMainHandItem().isEmpty()
                && data.melee.stun == 0 && !data.isGuarding() && !data.isTransformHeld() && !data.isTechHeld();
    }

    private static void start(ServerPlayer player, PlayerDBCData data, MeleeMove move) {
        if (move.kiCost > 0 && !data.consumeKi(move.kiCost)) {
            player.displayClientMessage(Component.literal("Ki insuficiente (" + (int) move.kiCost + ")."), true);
            return;
        }
        State s = data.melee;
        s.move = move;
        s.age = 0;
        s.queued = null;
        s.landed = false;
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.MELEE, move.ordinal());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                move.guardBreak || move == MeleeMove.LAUNCHER ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    public static void tick(ServerPlayer player, PlayerDBCData data) {
        State s = data.melee;
        if (s.stun > 0) s.stun--;
        if (s.move == null) return;
        if (s.stun > 0 || !player.isAlive()) { // hit out of the move
            s.move = s.queued = null;
            return;
        }
        s.age++;
        MeleeMove m = s.move;
        if (s.age == m.startup) lunge(player, m.range * Math.max(1f, data.appliedScale));
        if (!s.landed && s.age > m.startup && s.age <= m.startup + m.active) resolve(player, data, m);
        if (s.queued != null && s.age >= m.linkFrom()) {
            MeleeMove next = s.queued;
            s.move = null;
            if (canAct(player, data)) start(player, data, next);
        } else if (s.age >= m.total()) {
            s.move = null;
        }
    }

    private static void resolve(ServerPlayer player, PlayerDBCData data, MeleeMove m) {
        LivingEntity target = findTarget(player, m.range * Math.max(1f, data.appliedScale));
        if (target == null) return;
        State s = data.melee;
        s.landed = true;
        long now = player.level().getGameTime();
        if (now - s.lastHitTime > COMBO_RESET) s.comboHits = 0;

        PlayerDBCData victim = target instanceof ServerPlayer tp ? PlayerDBCData.get(tp) : null;
        ServerLevel level = player.serverLevel();
        if (victim != null && m.guardBreak && victim.isGuarding() && victim.melee.stun == 0) {
            victim.melee.stun = GUARD_BREAK_STUN; // the guard drops: this hit and the next ones land in full
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
            ((ServerPlayer) target).displayClientMessage(Component.literal("¡Guardia rota!"), true);
        }

        float punch = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + data.getEffectiveStr() * 0.4f;
        float scaling = Math.max(0.5f, 1f - 0.08f * s.comboHits);
        float damage = punch * FormEffects.meleeMultiplier(player, data) * m.damage * scaling;

        Vec3 before = target.getDeltaMovement();
        target.invulnerableTime = 0; // the move's own timing paces the hits, not vanilla's half-second immunity
        resolving = true;
        boolean hurt;
        try {
            hurt = target.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            resolving = false;
        }
        if (!hurt) return;

        s.comboHits++;
        s.lastHitTime = now;
        Vec3 dir = target.position().subtract(player.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? player.getLookAngle().multiply(1, 0, 1).normalize() : dir.normalize();
        // Vanilla's generic shove would send the target out of reach after the first jab: each move sets its own
        target.setDeltaMovement(before);
        target.push(dir.x * m.knockback, m.lift, dir.z * m.knockback);
        target.hurtMarked = true;
        com.dragonblock.common.event.DragonBlockEvents.powerGap(player, target);
        if (victim != null && s.comboHits <= STUN_LIMIT) {
            victim.melee.stun = Math.max(victim.melee.stun, m.stun);
            // Heavy blows knock a charging technique out of the hands
            if ((m.guardBreak || m == MeleeMove.LAUNCHER) && victim.getTechCharge() > 0f) victim.setTechCharge(0f);
        }

        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        boolean heavy = m.guardBreak || m == MeleeMove.LAUNCHER;
        level.sendParticles(heavy ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, heavy ? 14 : 6, 0.2, 0.2, 0.2, 0.3);
        level.playSound(null, at.x, at.y, at.z, heavy ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK,
                SoundSource.PLAYERS, 1f, heavy ? 0.8f : 1.2f);
    }

    /** As the blow comes out the fighter steps into it, closing a small gap so the chain keeps connecting. */
    private static void lunge(ServerPlayer player, float range) {
        LivingEntity target = findTarget(player, range + 1.5f);
        if (target == null) return;
        Vec3 to = target.position().subtract(player.position());
        double gap = to.horizontalDistance() - target.getBbWidth() / 2 - player.getBbWidth() / 2;
        if (gap < 0.6) return;
        Vec3 step = to.multiply(1, 0, 1).normalize().scale(Math.min(0.45, gap * 0.3));
        player.setDeltaMovement(player.getDeltaMovement().add(step));
        player.hurtMarked = true; // pushes the velocity to the client
    }

    /** The living thing best lined up with the fists: within reach of the eyes, in front, not behind a wall. */
    static LivingEntity findTarget(ServerPlayer player, float range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestScore = -Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range + 1),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() && KiAttacks.mayAffect(player, e))) {
            AABB box = e.getBoundingBox();
            Vec3 closest = new Vec3(clamp(eye.x, box.minX, box.maxX), clamp(eye.y, box.minY, box.maxY), clamp(eye.z, box.minZ, box.maxZ));
            double dist = eye.distanceTo(closest);
            if (dist > range) continue;
            double dot = look.dot(box.getCenter().subtract(eye).normalize());
            if (dist > 0.6 && dot < 0.5) continue; // outside a 60° cone, unless pressed right against you
            if (player.level().clip(new ClipContext(eye, closest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) continue;
            double score = dot - dist * 0.1;
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
