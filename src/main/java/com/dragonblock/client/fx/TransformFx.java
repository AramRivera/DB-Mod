package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.renderer.DBRenderTypes;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.registry.ModSounds;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.sounds.SoundSource;
import org.joml.Matrix4f;
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
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
    /** 0..1 how dark the sky is around a strong power-up nearby, and the tint it takes. */
    private static float darkness = 0f, prevDarkness = 0f;
    private static int darkTint = 0;

    /** A one-off camera jolt (big techniques, impacts), never weaker than what is already shaking. */
    public static void kick(float strength) {
        shake = Math.max(shake, strength);
    }

    private TransformFx() {}

    /** Eased aura power 0..1.3 for rendering (above 1 while powering up). */
    public static float auraPower(Player player, float partialTick) {
        float[] a = AURA.get(player.getId());
        return a == null ? 0f : Mth.lerp(partialTick, a[1], a[0]);
    }

    /** Target aura power from the synced state. In flight the aura drops unless you are powering up. */
    private static float targetPower(Player player, PlayerDBCData data) {
        boolean chargingTech = chargingTechnique(data);
        if (AnimState.isFlying(player, data) && !data.isTransformHeld() && !data.isChargingKi() && !chargingTech) return 0f;
        Transformation form = data.getTransformation();
        if (data.isTransformHeld()) {
            float c = data.getTransformCharge();
            // The aura folds in for an instant right before the transformation bursts out
            return c > 0.9f ? 0.45f : 1.0f + 0.35f * c;
        }
        if (data.isChargingKi() || chargingTech) return 1.0f;
        if (form != Transformation.BASE && form.getAuraStyle() != Transformation.AuraStyle.CALM) return 0.6f;
        return 0f;
    }

    private static boolean chargingTechnique(PlayerDBCData data) {
        return data.isTechHeld() && data.getSelectedTechnique() != null && !data.getSelectedTechnique().isInstant();
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
        Debris.tick();
        SpeedTrail.tick();
        LightningStrikes.tick();
        CinematicCamera.tick();
        com.dragonblock.client.renderer.RaceSkins.tick();

        float dim = 0f;
        for (Player player : mc.level.players()) {
            PlayerDBCData data = PlayerDBCData.get(player);
            // The server syncs the charge a few times a second; advance it locally in between so it is smooth
            if (data.isTransformHeld() && data.getTransformCharge() < 0.98f) {
                data.setTransformCharge(data.getTransformCharge()
                        + 1f / (20f * data.getTransformTimeSeconds(data.getSelectedForm())));
            }
            if (chargingTechnique(data) && data.getTechCharge() < 1f) {
                data.setTechCharge(data.getTechCharge() + 1f / data.getSelectedTechnique().getChargeTicks());
            }
            float[] a = AURA.computeIfAbsent(player.getId(), id -> new float[2]);
            a[1] = a[0];
            float target = targetPower(player, data);
            // Swell in fast, fade out slower
            a[0] += (target - a[0]) * (target > a[0] ? 0.25f : 0.12f);

            double dist = player.distanceTo(mc.player);
            spawnParticles(mc.level, player, data, dist);

            // Powering up: the ground tears loose and the camera of everyone nearby shakes, harder for stronger forms
            if (data.isTransformHeld() && dist < 48) {
                Transformation goal = data.getSelectedForm();
                float c = data.getTransformCharge();
                float power = Mth.clamp(goal.getStatMultiplier() / 3f, 0.35f, 2.5f);
                if (goal.getSequence() == Transformation.Sequence.CATACLYSM) power *= 1.4f;
                if (goal.getSequence() == Transformation.Sequence.DIVINE) power *= 0.4f; // divine forms are serene
                if (player.getRandom().nextFloat() < (0.25f + c) * power) {
                    Debris.spawn(player, 1 + (int) (power * c), 2.0f + power, 1.2f + 1.6f * c * power);
                }
                if (goal.isTopTier() && dist < 20) shake = Math.max(shake, (0.2f + 0.5f * c) * c * (float) (1 - dist / 20));
                LightningStrikes.charging(player, goal, c);
                // Strong power-ups darken the sky for everyone around and rumble louder as they build
                if (CinematicCamera.worthy(goal) && goal.getSequence() != Transformation.Sequence.DIVINE) {
                    float d = (0.3f + 0.7f * LightningStrikes.tier(goal)) * c * (float) Math.max(0, 1 - dist / 80);
                    if (d > dim) {
                        dim = d;
                        darkTint = goal.getAuraColor();
                    }
                }
                if (player.tickCount % 8 == 0) {
                    mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.KI_CHARGE.get(), SoundSource.PLAYERS,
                            Math.min(1f, 0.25f + 0.5f * c * power), 0.5f + 0.9f * c, false);
                }
            } else if (data.isChargingKi() && dist < 16) {
                // Plain ki charging rattles the view a little, more the stronger the fighter
                float power = Mth.clamp((float) Math.log10(Math.max(10, data.getBattlePower())) / 7f, 0.1f, 1f)
                        * Mth.clamp(data.getTransformation().getStatMultiplier() / 4f, 0.5f, 2f);
                shake = Math.max(shake, (0.03f + 0.14f * power) * (float) (1 - dist / 16));
            }
            float burst = AnimState.progress(player, S2CAnimPacket.Anim.TRANSFORM, BURST_TICKS, 0);
            if (burst >= 0 && burst < 0.4f && dist < 20 && data.getTransformation().isTopTier()) {
                shake = Math.max(shake, 1.3f * (float) (1 - dist / 20));
            }
        }
        // The eternal dragon turns the sky nearly black and calls lightning while it rises
        for (var dragon : mc.level.getEntitiesOfClass(com.dragonblock.common.entity.ShenlongEntity.class, mc.player.getBoundingBox().inflate(256))) {
            float d = 0.95f * (float) Math.max(0, 1 - dragon.distanceTo(mc.player) / 256);
            if (dragon.getState() == com.dragonblock.common.entity.ShenlongEntity.State.LEAVING) d *= Math.max(0f, 1f - dragon.stateAge() / 110f);
            if (d > dim) {
                dim = d;
                darkTint = 0x1A6030;
            }
            if (dragon.getState() == com.dragonblock.common.entity.ShenlongEntity.State.RISING && mc.player.getRandom().nextFloat() < 0.18f) {
                LightningStrikes.strikeAt(mc.level, dragon.getId(), dragon.position(), 6, 26, 0xFFF4C0);
            }
        }
        prevDarkness = darkness;
        darkness += (dim - darkness) * (dim > darkness ? 0.06f : 0.03f);
        // Clashing techniques nearby rattle the view
        for (var clash : mc.level.getEntitiesOfClass(com.dragonblock.common.entity.KiBlastEntity.class,
                mc.player.getBoundingBox().inflate(28), com.dragonblock.common.entity.KiBlastEntity::isClashing)) {
            shake = Math.max(shake, 0.5f * (1f - clash.distanceTo(mc.player) / 28f));
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

    /** The horizon fog darkens with the sky. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float d = Mth.lerp((float) event.getPartialTick(), prevDarkness, darkness) * 0.75f;
        if (d < 0.01f) return;
        float tr = (darkTint >> 16 & 255) / 255f * 0.12f, tg = (darkTint >> 8 & 255) / 255f * 0.12f, tb = (darkTint & 255) / 255f * 0.12f;
        event.setRed(Mth.lerp(d, event.getRed(), tr));
        event.setGreen(Mth.lerp(d, event.getGreen(), tg));
        event.setBlue(Mth.lerp(d, event.getBlue(), tb));
    }

    /**
     * A dark dome around the camera, drawn after the world with depth testing: terrain in front hides it,
     * so only the sky (and the farthest land) darkens.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        float d = Mth.lerp(event.getPartialTick(), prevDarkness, darkness);
        if (d < 0.01f) return;
        Minecraft mc = Minecraft.getInstance();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(DBRenderTypes.DIM);
        Matrix4f m = new Matrix4f(); // the camera rotation is already on the model-view stack
        int r = (int) ((darkTint >> 16 & 255) * 0.1f), g = (int) ((darkTint >> 8 & 255) * 0.1f), b = (int) ((darkTint & 255) * 0.1f);
        int a = (int) (200 * d);
        final float radius = 140f;
        final int lat = 8, lon = 16;
        for (int i = 0; i < lat; i++) {
            float t0 = Mth.PI * i / lat, t1 = Mth.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float p0 = Mth.TWO_PI * j / lon, p1 = Mth.TWO_PI * (j + 1) / lon;
                domeVertex(vc, m, t0, p0, radius, r, g, b, a);
                domeVertex(vc, m, t0, p1, radius, r, g, b, a);
                domeVertex(vc, m, t1, p1, radius, r, g, b, a);
                domeVertex(vc, m, t1, p0, radius, r, g, b, a);
            }
        }
        buffers.endBatch(DBRenderTypes.DIM);
    }

    private static void domeVertex(VertexConsumer vc, Matrix4f m, float theta, float phi, float radius, int r, int g, int b, int a) {
        vc.addVertex(m, Mth.sin(theta) * Mth.cos(phi) * radius, Mth.cos(theta) * radius, Mth.sin(theta) * Mth.sin(phi) * radius)
                .setColor(r, g, b, a);
    }

    // Lowest priority: the shake goes on top of wherever the cinematic camera points
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shake < 0.01f) return;
        float t = (float) event.getPartialTick() + (Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount);
        event.setPitch(event.getPitch() + Mth.sin(t * 2.1f) * shake);
        event.setRoll(event.getRoll() + Mth.cos(t * 2.9f) * shake * 0.8f);
    }
}
