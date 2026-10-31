package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side transformation and aura effects for every player in view: eased aura power (so the aura
 * swells when you start charging and fades when you stop), per-form particles, and camera shake.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformFx {
    public static final int BURST_TICKS = 24;

    /** entityId -> {current aura power, previous tick's power}. */
    private static final Map<Integer, float[]> AURA = new HashMap<>();
    private static float shake = 0f;

    private TransformFx() {}

    /** Eased aura power 0..1.3 for rendering (above 1 while powering up). */
    public static float auraPower(Player player, float partialTick) {
        float[] a = AURA.get(player.getId());
        return a == null ? 0f : Mth.lerp(partialTick, a[1], a[0]);
    }

    /** Target aura power from the synced state. */
    private static float targetPower(PlayerDBCData data) {
        Transformation form = data.getTransformation();
        if (data.isTransformHeld()) return 1.0f + 0.35f * data.getTransformCharge();
        if (data.isChargingKi() || data.isWaveHeld()) return 1.0f;
        if (form != Transformation.BASE && form.getAuraStyle() != Transformation.AuraStyle.CALM) return 0.6f;
        return 0f;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            if (mc.level == null) AURA.clear();
            return;
        }
        shake *= 0.85f;

        for (Player player : mc.level.players()) {
            PlayerDBCData data = PlayerDBCData.get(player);
            float[] a = AURA.computeIfAbsent(player.getId(), id -> new float[2]);
            a[1] = a[0];
            float target = targetPower(data);
            // Swell in fast, fade out slower
            a[0] += (target - a[0]) * (target > a[0] ? 0.25f : 0.12f);

            double dist = player.distanceTo(mc.player);
            spawnParticles(mc.level, player, data, dist);

            // Big power-ups shake the camera of everyone nearby
            if (data.isTransformHeld() && dist < 32) {
                Transformation.Sequence seq = data.getSelectedForm().getSequence();
                float base = switch (seq) {
                    case CATACLYSM -> 0.9f;
                    case GIANT, SURGE -> 0.45f;
                    default -> 0.12f;
                };
                shake = Math.max(shake, base * data.getTransformCharge() * (float) (1 - dist / 32));
            }
            float burst = AnimState.progress(player, S2CAnimPacket.Anim.TRANSFORM, BURST_TICKS, 0);
            if (burst >= 0 && burst < 0.4f && dist < 32) {
                Transformation.Sequence seq = data.getTransformation().getSequence();
                if (seq != Transformation.Sequence.DIVINE) shake = Math.max(shake, (seq == Transformation.Sequence.CATACLYSM ? 1.4f : 0.7f) * (float) (1 - dist / 32));
            }
        }
        AURA.keySet().removeIf(id -> mc.level.getEntity(id) == null);
    }

    private static void spawnParticles(Level level, Player player, PlayerDBCData data, double dist) {
        if (dist > 48) return;
        RandomSource rnd = player.getRandom();
        Transformation form = data.isTransformHeld() ? data.getSelectedForm() : data.getTransformation();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        float scale = player.getScale();

        if (data.isTransformHeld()) {
            float c = data.getTransformCharge();
            switch (form.getSequence()) {
                case SURGE, CATACLYSM, GIANT -> {
                    // Ground breaks up and debris floats upward
                    BlockState ground = level.getBlockState(BlockPos.containing(x, y - 0.5, z));
                    if (!ground.isAir()) {
                        int n = form.getSequence() == Transformation.Sequence.CATACLYSM ? 4 : 2;
                        for (int i = 0; i < n; i++) {
                            double ang = rnd.nextDouble() * Math.PI * 2, r = (1.2 + rnd.nextDouble() * 2.5) * scale;
                            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                                    x + Math.cos(ang) * r, y + 0.1, z + Math.sin(ang) * r, 0, 0.25 + c * 0.35, 0);
                        }
                    }
                    if (rnd.nextFloat() < c * 0.3f) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + rnd.nextGaussian() * 0.6, y + 1 + rnd.nextGaussian() * 0.6,
                                z + rnd.nextGaussian() * 0.6, 0, 0, 0);
                    }
                }
                case DIVINE -> {
                    // Motes drawn inward from all around
                    double ang = rnd.nextDouble() * Math.PI * 2, r = 2.5;
                    level.addParticle(ParticleTypes.END_ROD, x + Math.cos(ang) * r, y + rnd.nextDouble() * 2.5, z + Math.sin(ang) * r,
                            -Math.cos(ang) * 0.12, 0.02, -Math.sin(ang) * 0.12);
                }
                case STRAIN -> level.addParticle(dust(form.getAuraColor(), 1.4f), x + rnd.nextGaussian() * 0.4,
                        y + rnd.nextDouble() * 1.9, z + rnd.nextGaussian() * 0.4, 0, 0.05, 0);
                case FLASH -> {}
            }
            return;
        }

        if (form == Transformation.BASE || player.tickCount % 2 != 0) return;
        double px = x + rnd.nextGaussian() * 0.5 * scale, py = y + rnd.nextDouble() * 2 * scale, pz = z + rnd.nextGaussian() * 0.5 * scale;
        switch (form.getParticles()) {
            case SPARKS -> { if (rnd.nextFloat() < 0.5f) level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0, 0); }
            case EMBERS -> level.addParticle(dust(form.getAuraColor(), 1.0f), px, py, pz, 0, 0.06, 0);
            case SPARKLES -> { if (rnd.nextFloat() < 0.4f) level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0.02, 0); }
            case DUST -> { if (player.onGround() && rnd.nextFloat() < 0.3f) level.addParticle(ParticleTypes.POOF, px, y + 0.05, pz, 0, 0.01, 0); }
            case NONE -> {}
        }
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shake < 0.01f) return;
        float t = (float) event.getPartialTick() + (Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount);
        event.setPitch(event.getPitch() + Mth.sin(t * 2.1f) * shake);
        event.setRoll(event.getRoll() + Mth.cos(t * 2.9f) * shake * 0.8f);
    }
}
