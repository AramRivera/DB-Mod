package com.dragonblock.common.combat;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Server-side transformation flow: hold the key to power up into the selected form, mastery, exhaustion. */
public final class FormController {
    private FormController() {}

    /** Called every server tick. */
    public static void tick(ServerPlayer player, PlayerDBCData data) {
        checkFullMoon(player, data);
        Transformation target = data.getSelectedForm();

        if (data.isTransformHeld() && target != data.getTransformation()) {
            String lock = data.getLockReason(target);
            if (lock != null) {
                data.setTransformHeld(false);
                data.setTransformCharge(0f);
                player.displayClientMessage(Component.literal(target.getDisplayName() + ": " + lock), true);
            } else if (target.isOozaru() && !isNightSky(player)) {
                data.setTransformHeld(false);
                data.setTransformCharge(0f);
                player.displayClientMessage(Component.literal("El Ozaru solo despierta de noche y a cielo abierto."), true);
            } else {
                // Powering up drains a little Ki and shakes the ground
                data.setCurrentKi(data.getCurrentKi() - 0.15f);
                data.setTransformCharge(data.getTransformCharge() + 1f / (20f * data.getTransformTimeSeconds(target)));
                if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                            6, 0.6, 0.05, 0.6, 0.08);
                }
                if (data.getTransformCharge() >= 1f) transform(player, data, target);
            }
        }

        Transformation form = data.getTransformation();
        if (form == Transformation.BASE) return;

        // Forced reverts
        if (form.isOozaru() && !data.forcedForm && (!isNightSky(player) || !data.isTailOut())) {
            revert(player, data);
            player.displayClientMessage(Component.literal("La luz del día te devuelve a tu forma normal."), true);
            return;
        }

        float drain = data.getCurrentDrainPerSecond() / 20f;
        if (data.getCurrentKi() <= drain) {
            data.setCurrentKi(0);
            revert(player, data);
            player.displayClientMessage(Component.literal("Agotamiento: tu cuerpo vuelve a la forma base."), true);
            return;
        }
        data.setCurrentKi(data.getCurrentKi() - drain);

        // Kaio-ken strains the body
        if (form.isKaioken() && player.tickCount % 20 == 0) {
            float strain = (form.ordinal() - Transformation.KAIOKEN.ordinal() + 1) * (1f - data.getMastery(form) / 140f);
            if (player.getHealth() > strain + 1f) player.setHealth(player.getHealth() - strain);
        }

        // Passive mastery: ~0.05%/s, faster with MND
        if (player.tickCount % 20 == 0) {
            data.addMastery(form, 0.05f * (1f + data.getEffectiveMnd() / 100f));
        }
    }

    public static void transform(ServerPlayer player, PlayerDBCData data, Transformation target) {
        data.forcedForm = false;
        data.setTransformation(target);
        data.setTransformCharge(0f);
        data.setTransformHeld(false);
        data.saveToPlayer(player);
        ModMessages.syncToTracking(player);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.6f, target.isOozaru() ? 0.5f : 1.0f);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                    50, 0.8, 1.2, 0.8, 0.3);
        }
        player.displayClientMessage(Component.literal(target.getDisplayName()), true);
    }

    public static void revert(ServerPlayer player, PlayerDBCData data) {
        if (data.getTransformation() == Transformation.BASE) return;
        data.setTransformation(Transformation.BASE);
        data.setTransformCharge(0f);
        data.forcedForm = false;
        data.saveToPlayer(player);
        ModMessages.syncToTracking(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TRANSFORM.get(), SoundSource.PLAYERS, 0.6f, 0.6f);
    }

    /**
     * Classic trigger: a Saiyan with the tail out who looks up at the full moon turns Ozaru on the spot,
     * regardless of the selected form or level.
     */
    private static void checkFullMoon(ServerPlayer player, PlayerDBCData data) {
        if (player.tickCount % 10 != 0 || !data.getRace().hasSaiyanTail() || !data.isTailOut()) return;
        if (data.getTransformation().isOozaru() || player.getXRot() > -25f) return;
        if (isNightSky(player) && player.serverLevel().getMoonPhase() == 0) {
            Transformation form = data.isUnlocked(Transformation.GOLDEN_OOZARU) ? Transformation.GOLDEN_OOZARU : Transformation.OOZARU;
            player.displayClientMessage(Component.literal("La luna llena despierta al Ozaru."), true);
            transform(player, data, form);
        }
    }

    private static boolean isNightSky(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return level.dimensionType().hasSkyLight()
                && level.isNight()
                && level.canSeeSky(player.blockPosition().above());
    }
}
