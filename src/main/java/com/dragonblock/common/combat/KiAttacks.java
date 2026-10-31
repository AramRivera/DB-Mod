package com.dragonblock.common.combat;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.entity.KiBlastEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-side ki techniques. Every cost and cooldown is checked here, never on the client. */
public final class KiAttacks {
    public static final float BLAST_COST = 15f;
    public static final float WAVE_MIN_COST = 30f;
    public static final float DASH_COST = 8f;
    private static final int BLAST_COOLDOWN = 8;
    private static final int DASH_COOLDOWN = 16;

    private KiAttacks() {}

    public static void tick(ServerPlayer player, PlayerDBCData data) {
        if (data.blastCooldown > 0) data.blastCooldown--;
        if (data.dashCooldown > 0) data.dashCooldown--;
        if (data.dashIframes > 0) data.dashIframes--;

        // Charging a wave: 2.5s to full, 0.5 Ki per tick
        if (data.isWaveHeld() && data.getWaveCharge() < 1f && data.consumeKi(0.5f * FormEffects.kiCostMultiplier(data))) {
            data.setWaveCharge(data.getWaveCharge() + 1f / 50f);
            if (player.tickCount % 8 == 0) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.KI_CHARGE.get(), SoundSource.PLAYERS, 0.5f, 0.8f + data.getWaveCharge());
            }
        }
    }

    public static void fireBlast(ServerPlayer player, PlayerDBCData data) {
        if (data.blastCooldown > 0) return;
        float cost = BLAST_COST * FormEffects.kiCostMultiplier(data);
        if (!data.consumeKi(cost)) {
            player.displayClientMessage(Component.literal("Ki insuficiente (" + (int) BLAST_COST + ")."), true);
            return;
        }
        data.blastCooldown = BLAST_COOLDOWN;

        float damage = (6.0f + data.getEffectiveWil() * 0.6f) * FormEffects.kiMultiplier(data);
        float explosion = 0.8f + Math.min(2.0f, data.getEffectiveWil() * 0.02f);
        spawn(player, data, damage, explosion, 0.35f, false, 2.0f);
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.BLAST);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.KI_BLAST.get(), SoundSource.PLAYERS, 1.0f, 1.1f);
    }

    /** Fires the charged wave when the key is released. Below 20% charge it fizzles. */
    public static void releaseWave(ServerPlayer player, PlayerDBCData data) {
        float charge = data.getWaveCharge();
        data.setWaveCharge(0f);
        if (charge < 0.2f) return;

        float damage = (10.0f + data.getEffectiveWil() * 1.1f) * (0.5f + 1.5f * charge) * FormEffects.kiMultiplier(data);
        float explosion = 1.5f + Math.min(4.0f, data.getEffectiveWil() * 0.03f) * charge;
        spawn(player, data, damage, explosion, 0.5f + 1.1f * charge, true, 2.6f);
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.WAVE_RELEASE);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.KI_BLAST.get(), SoundSource.PLAYERS, 1.8f, 0.6f);
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
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.DASH);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    private static void spawn(ServerPlayer player, PlayerDBCData data, float damage, float explosion,
                              float size, boolean beam, float speed) {
        int color = data.getTransformation().getAuraColor();
        KiBlastEntity blast = new KiBlastEntity(player.level(), player, damage, explosion, color, size, beam);
        blast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, beam ? 0.0F : 0.6F);
        player.level().addFreshEntity(blast);
    }
}
