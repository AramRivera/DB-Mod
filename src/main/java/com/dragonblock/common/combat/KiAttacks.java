package com.dragonblock.common.combat;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.entity.KiBlastEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side ki techniques. Every cost, charge and cooldown is decided here, never on the client. */
public final class KiAttacks {
    public static final float DASH_COST = 8f;
    private static final int DASH_COOLDOWN = 16;

    private KiAttacks() {}

    public static void tick(ServerPlayer player, PlayerDBCData data) {
        data.cooldowns.replaceAll((t, ticks) -> Math.max(0, ticks - 1));
        if (data.dashCooldown > 0) data.dashCooldown--;
        if (data.dashIframes > 0) data.dashIframes--;

        Technique tech = data.getSelectedTechnique();
        if (!data.isTechHeld() || tech == null || onCooldown(data, tech)) return;

        if (tech.isInstant()) {
            fire(player, data, tech, 1f); // holding the key keeps firing as the cooldown allows
        } else if (data.getTechCharge() < 1f) {
            float perTick = tech.getKiCost() * FormEffects.kiCostMultiplier(data) / tech.getChargeTicks();
            if (data.consumeKi(perTick)) {
                data.setTechCharge(data.getTechCharge() + 1f / tech.getChargeTicks());
                if (player.tickCount % 8 == 0) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.KI_CHARGE.get(), SoundSource.PLAYERS, 0.5f, 0.7f + data.getTechCharge() * 0.8f);
                }
            } else if (player.tickCount % 20 == 0) {
                player.displayClientMessage(Component.literal("Ki insuficiente para seguir cargando."), true);
            }
        }
    }

    private static boolean onCooldown(PlayerDBCData data, Technique tech) {
        return data.cooldowns.getOrDefault(tech, 0) > 0;
    }

    /** Key released: fires the selected technique if it gathered enough charge, else it fizzles. */
    public static void release(ServerPlayer player, PlayerDBCData data) {
        Technique tech = data.getSelectedTechnique();
        float charge = data.getTechCharge();
        data.setTechCharge(0f);
        if (tech == null || tech.isInstant() || onCooldown(data, tech)) return;
        if (charge + 1e-3f < tech.getMinCharge()) {
            if (charge > 0.05f) player.displayClientMessage(Component.literal(tech.getDisplayName() + ": carga insuficiente."), true);
            return;
        }
        fire(player, data, tech, charge);
    }

    /** The ki blast item fires the basic technique directly. */
    public static void fireBlast(ServerPlayer player, PlayerDBCData data) {
        if (!onCooldown(data, Technique.KI_BLAST)) fire(player, data, Technique.KI_BLAST, 1f);
    }

    private static void fire(ServerPlayer player, PlayerDBCData data, Technique tech, float charge) {
        if (tech.isInstant()) {
            float cost = tech.getKiCost() * FormEffects.kiCostMultiplier(data);
            if (!data.consumeKi(cost)) {
                if (player.tickCount % 10 == 0) player.displayClientMessage(Component.literal("Ki insuficiente (" + (int) cost + ")."), true);
                return;
            }
        }
        data.cooldowns.put(tech, tech.getCooldown());

        int color = tech.colorFor(data.getTransformation());
        float base = (6.0f + data.getEffectiveWil() * 0.6f) * FormEffects.kiMultiplier(data) * tech.getDamage();
        float power = tech.isInstant() ? 1f : 0.4f + 0.6f * charge;
        float damage = base * power;
        float wil = Math.min(4f, data.getEffectiveWil() * 0.03f);

        switch (tech.getKind()) {
            case BLAST -> spawn(player, tech, damage, 0.8f + wil * 0.5f, color, 0.35f, 2.0f, 0.6f);
            case BEAM -> {
                float width = tech == Technique.FINAL_FLASH ? 1.3f : 0.75f;
                spawn(player, tech, damage, (1.5f + wil) * power, color, (0.4f + width * charge), 2.6f, 0f);
            }
            case PIERCE -> spawn(player, tech, damage, tech == Technique.MAKANKOSAPPO ? 1.2f : 0f, color,
                    tech == Technique.MAKANKOSAPPO ? 0.22f : 0.12f, tech == Technique.DEATH_BEAM ? 4.0f : 3.2f, 0f);
            case DISC -> spawn(player, tech, damage, 0f, color, 0.35f + 0.35f * charge, 1.6f, 0f);
            case BOMB -> spawn(player, tech, damage, (3f + wil * 1.5f) * (0.5f + charge), color, 0.6f + 2.2f * charge, 0.55f, 0f);
            case BALL -> spawn(player, tech, damage, (2.2f + wil) * power, color, 0.5f + 0.5f * charge, 1.8f, 0f);
            case SHOCKWAVE -> shockwave(player, damage, 3f + 4f * charge);
            case FLASH -> solarFlare(player);
            case TELEPORT -> teleport(player);
        }

        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.TECHNIQUE, tech.ordinal());
        float pitch = switch (tech.getKind()) {
            case BLAST, PIERCE -> 1.2f;
            case BOMB -> 0.5f;
            case BEAM -> 0.7f;
            default -> 0.9f;
        };
        if (tech.getKind() != Technique.Kind.TELEPORT) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.KI_BLAST.get(), SoundSource.PLAYERS, tech.isInstant() ? 1.0f : 1.8f, pitch);
        }
    }

    private static void spawn(ServerPlayer player, Technique tech, float damage, float explosion, int color,
                              float size, float speed, float inaccuracy) {
        // Past ~8 explosions mostly cause lag and crater whole builds
        KiBlastEntity blast = new KiBlastEntity(player.level(), player, damage, Math.min(8f, explosion), color, size, tech);
        blast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
        player.level().addFreshEntity(blast);
    }

    /** Kiai: everything around is shoved away and roughed up. */
    private static void shockwave(ServerPlayer player, float damage, float radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && mayAffect(player, e) && e.hasLineOfSight(player))) {
            Vec3 away = e.position().subtract(player.position());
            double dist = away.length();
            if (dist > radius) continue;
            double strength = 1.6 * (1 - dist / radius) + 0.4;
            Vec3 dir = dist < 1e-3 ? player.getLookAngle() : away.scale(1 / dist);
            // Only shove what the hit actually landed on (respects invulnerability and protections)
            if (!e.hurt(player.damageSources().playerAttack(player), damage * (float) (1 - dist / radius * 0.6))) continue;
            e.push(dir.x * strength, 0.35 + strength * 0.2, dir.z * strength);
            e.hurtMarked = true;
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 6, radius * 0.4, 0.3, radius * 0.4, 0);
        }
    }

    /** Taiyoken: anyone looking at you within range is blinded; mobs lose track of you. */
    private static void solarFlare(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(14),
                e -> e != player && e.isAlive() && mayAffect(player, e))) {
            Vec3 toCaster = eye.subtract(e.getEyePosition()).normalize();
            if (e.getLookAngle().dot(toCaster) < 0.35 || !e.hasLineOfSight(player)) continue;
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            if (e instanceof Mob mob) mob.setTarget(null);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 2.0f);
    }

    /** Other players are only affected when this server allows PvP between the two (teams included). */
    private static boolean mayAffect(ServerPlayer caster, LivingEntity target) {
        return !(target instanceof net.minecraft.world.entity.player.Player other) || caster.canHarmPlayer(other);
    }

    /** True when nothing solid stands between the target and {@code spot}, at knee and at eye height. */
    private static boolean openFrom(Entity target, Vec3 spot) {
        for (double h : new double[]{0.5, target.getEyeHeight()}) {
            BlockHitResult wall = target.level().clip(new ClipContext(target.position().add(0, h, 0), spot.add(0, h, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
            if (wall.getType() != HitResult.Type.MISS) return false;
        }
        return true;
    }

    /** Instant transmission: behind the creature you look at, or onto the block you point at. */
    private static void teleport(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(64));
        BlockHitResult block = player.level().clip(new ClipContext(eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? reach : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end,
                new AABB(eye, end).inflate(1.5), e -> e instanceof LivingEntity && e != player && !e.isSpectator());

        Vec3 dest;
        if (hit != null) {
            Entity target = hit.getEntity();
            Vec3 behind = target.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.6);
            // Behind the target, else in front; never on the far side of a wall from it
            Vec3 back = target.position().add(behind), front = target.position().subtract(behind);
            dest = openFrom(target, back) ? back : openFrom(target, front) ? front : null;
            if (dest == null) {
                player.displayClientMessage(Component.literal("No hay espacio para aparecer ahí."), true);
                return;
            }
        } else if (block.getType() == HitResult.Type.BLOCK) {
            dest = Vec3.atBottomCenterOf(block.getBlockPos().relative(block.getDirection()));
        } else {
            player.displayClientMessage(Component.literal("No sientes ningún ki hacia donde miras."), true);
            return;
        }
        if (!player.level().noCollision(player, player.getBoundingBox().move(dest.subtract(player.position())))) {
            player.displayClientMessage(Component.literal("No hay espacio para aparecer ahí."), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);
        float yaw = player.getYRot();
        if (hit != null) {
            // Arrive facing the target
            Vec3 look = hit.getEntity().position().subtract(dest);
            yaw = (float) (Math.atan2(-look.x, look.z) * 180 / Math.PI);
        }
        player.connection.teleport(dest.x, dest.y, dest.z, yaw, player.getXRot());
        level.sendParticles(ParticleTypes.END_ROD, dest.x, dest.y + 1, dest.z, 20, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 2.0f);
    }

    public static void dash(ServerPlayer player, PlayerDBCData data) {
        if (data.dashCooldown > 0 || !data.consumeKi(DASH_COST)) return;
        data.dashCooldown = DASH_COOLDOWN;
        data.dashIframes = 6;

        Vec3 look = player.getLookAngle();
        Vec3 dir = data.isFlying() ? look : new Vec3(look.x, 0, look.z).normalize();
        double strength = 1.1 + Math.min(1.2, data.getEffectiveDex() * 0.004);
        player.setDeltaMovement(dir.scale(strength).add(0, data.isFlying() ? 0 : 0.15, 0));
        player.hurtMarked = true; // pushes the velocity to the client
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.DASH, 0);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8f, 1.3f);
    }
}
