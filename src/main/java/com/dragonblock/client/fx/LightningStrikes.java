package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.ClientConfig;
import com.dragonblock.client.renderer.EnergyRender;
import com.dragonblock.common.data.Transformation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Bolts that fall from the sky around a player powering up into one of the strongest forms. The higher
 * the form, the more often they strike: a few for Super Saiyajin 3, a storm for Ultra Ego. Visual only.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightningStrikes {
    private static final int LIFE = 7;
    private static final int MAX = 24;
    private static final List<Strike> STRIKES = new ArrayList<>();

    private record Strike(int owner, Vec3 ground, double height, int color, int seed, int[] age) {}

    private LightningStrikes() {}

    /** 0 for ordinary forms, then 0..1 from Super Saiyajin 3 up to the very top tier. Serene forms never storm. */
    public static float tier(Transformation form) {
        if (form.getStatMultiplier() < 4f || form.getSequence() == Transformation.Sequence.DIVINE) return 0f;
        return Mth.clamp((form.getStatMultiplier() - 3.5f) / 6f, 0.08f, 1f);
    }

    /** Called every client tick while {@code player} powers up toward {@code form}. */
    public static void charging(Player player, Transformation form, float charge) {
        float tier = tier(form);
        if (tier <= 0 || charge < 0.25f) return;
        // Strikes per second: about one for the lowest tier, five for the highest
        float perTick = (0.6f + 4.4f * tier) * charge / 20f;
        if (player.getRandom().nextFloat() < perTick) strike(player, form, 3.0, 8.0);
    }

    /** Volley at the instant the transformation completes. */
    public static void burst(Player player, Transformation form) {
        float tier = tier(form);
        if (tier <= 0) return;
        int count = 2 + Math.round(8 * tier);
        for (int i = 0; i < count; i++) strike(player, form, 2.0, 10.0);
    }

    private static void strike(Player player, Transformation form, double minR, double maxR) {
        if (STRIKES.size() >= MAX) return;
        RandomSource rnd = player.getRandom();
        double a = rnd.nextDouble() * Math.PI * 2, r = (minR + rnd.nextDouble() * (maxR - minR)) * player.getScale();
        double x = player.getX() + Math.cos(a) * r, z = player.getZ() + Math.sin(a) * r;
        var level = player.level();
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        // Under a roof the heightmap is far above; strike the floor the player stands on instead
        double groundY = top > player.getY() + 6 ? player.getY() : top;
        int color = form.getLightningColor() != 0 ? form.getLightningColor() : EnergyRender.mix(form.getAuraColor(), 0xFFFFFF, 0.55f);
        STRIKES.add(new Strike(player.getId(), new Vec3(x, groundY, z), 16 + rnd.nextDouble() * 12, color, rnd.nextInt(), new int[1]));

        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, groundY + 0.1, z,
                    rnd.nextGaussian() * 0.25, 0.15 + rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.25);
        }
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, groundY + 0.2, z, 0, 0.05, 0);
        if (rnd.nextFloat() < 0.45f) {
            level.playLocalSound(x, groundY, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER,
                    0.5f + rnd.nextFloat() * 0.3f, 1.1f + rnd.nextFloat() * 0.4f, false);
        }
        if (rnd.nextFloat() < 0.15f) {
            level.playLocalSound(x, groundY, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                    0.35f, 1.3f + rnd.nextFloat() * 0.3f, false);
        }
    }

    public static void tick() {
        if (Minecraft.getInstance().level == null) {
            STRIKES.clear();
            return;
        }
        STRIKES.removeIf(s -> ++s.age()[0] > LIFE);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || STRIKES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack(); // camera rotation is already on the model-view stack
        for (Strike s : STRIKES) {
            int age = s.age()[0];
            if (age == 2 || age == 5) continue; // the bolt flickers like real lightning
            if (!ClientConfig.showAura(s.owner() == (mc.player == null ? -1 : mc.player.getId()))) continue;
            Vec3 ground = s.ground().subtract(cam);
            float fade = 1f - (float) age / (LIFE + 1);
            EnergyRender.strike(ps, buffers, s.seed() + age / 3, ground.add(0, s.height(), 0), ground,
                    EnergyRender.scaleColor(s.color(), fade) & 0xFFFFFF, 1f);
        }
        buffers.endBatch();
    }
}
