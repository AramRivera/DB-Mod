package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks of the ground that tear loose and float up around a player powering up, then get blasted
 * away when the transformation completes. Purely visual and client-side, so every client spawns its
 * own debris from the same synced charge state.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Debris {
    private static final int MAX = 90;
    private static final List<Chunk> CHUNKS = new ArrayList<>();

    private static final class Chunk {
        final int owner;
        final BlockState state;
        final float size, spinX, spinY;
        Vec3 pos, prevPos, vel;
        float hover; // height above the ground it drifts up to
        int age, life;
        boolean blasted;

        Chunk(int owner, BlockState state, Vec3 pos, float size, float hover, RandomSource rnd) {
            this.owner = owner;
            this.state = state;
            this.pos = this.prevPos = pos;
            this.vel = new Vec3(0, 0.02 + rnd.nextDouble() * 0.04, 0);
            this.size = size;
            this.hover = hover;
            this.spinX = (rnd.nextFloat() - 0.5f) * 12f;
            this.spinY = (rnd.nextFloat() - 0.5f) * 12f;
            this.life = 200 + rnd.nextInt(60);
        }
    }

    private Debris() {}

    /** Tears up to {@code count} chunks out of the ground around the player this tick. */
    public static void spawn(Player player, int count, float radius, float maxHover) {
        RandomSource rnd = player.getRandom();
        for (int i = 0; i < count && CHUNKS.size() < MAX; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 1.0 + rnd.nextDouble() * radius;
            double x = player.getX() + Math.cos(a) * r, z = player.getZ() + Math.sin(a) * r;
            BlockPos ground = BlockPos.containing(x, player.getY() - 0.5, z);
            BlockState state = player.level().getBlockState(ground);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
            float size = 0.12f + rnd.nextFloat() * 0.22f;
            CHUNKS.add(new Chunk(player.getId(), state, new Vec3(x, ground.getY() + 1.0, z), size,
                    0.6f + rnd.nextFloat() * maxHover, rnd));
        }
    }

    /** Transformation complete: every chunk around this player is flung outward. */
    public static void blast(Player player, float strength) {
        for (Chunk c : CHUNKS) {
            if (c.owner != player.getId()) continue;
            Vec3 out = c.pos.subtract(player.position()).multiply(1, 0, 1).normalize();
            c.vel = out.scale(0.5 * strength + Math.random() * 0.3).add(0, 0.25 + Math.random() * 0.25, 0);
            c.blasted = true;
            c.life = Math.min(c.life, c.age + 40);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            CHUNKS.clear();
            return;
        }
        CHUNKS.removeIf(c -> {
            c.prevPos = c.pos;
            c.age++;
            if (c.blasted) {
                c.vel = c.vel.add(0, -0.05, 0).scale(0.97);
            } else {
                // Drift up to the hover height, bob there; drop if the owner stopped powering up
                var owner = mc.level.getEntity(c.owner);
                boolean powering = owner instanceof Player p && com.dragonblock.common.data.PlayerDBCData.get(p).isTransformHeld();
                if (!powering) {
                    c.vel = c.vel.add(0, -0.04, 0);
                } else {
                    double groundY = owner.getY();
                    double target = groundY + c.hover;
                    double vy = c.pos.y < target ? 0.03 : Math.sin(c.age * 0.15) * 0.01;
                    c.vel = new Vec3(c.vel.x * 0.9, vy, c.vel.z * 0.9);
                }
            }
            c.pos = c.pos.add(c.vel);
            boolean landed = !c.blasted && c.vel.y < 0 && !mc.level.getBlockState(BlockPos.containing(c.pos)).isAir();
            return c.age > c.life || landed || c.pos.y < mc.level.getMinBuildHeight();
        });
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || CHUNKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack ps = new PoseStack();
        ps.mulPose(event.getPoseStack());

        for (Chunk c : CHUNKS) {
            Vec3 p = c.prevPos.lerp(c.pos, partial);
            float fade = c.blasted ? Mth.clamp((c.life - c.age) / 40f, 0f, 1f) : 1f;
            float t = c.age + partial;
            ps.pushPose();
            ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            ps.mulPose(Axis.XP.rotationDegrees(t * c.spinX));
            ps.mulPose(Axis.YP.rotationDegrees(t * c.spinY));
            float s = c.size * (0.4f + 0.6f * fade);
            ps.scale(s, s, s);
            ps.translate(-0.5, -0.5, -0.5);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p));
            mc.getBlockRenderer().renderSingleBlock(c.state, ps, buffers, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        buffers.endBatch();
    }
}
