package com.dragonblock.client.fx;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.PlayerDBCData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rocks that tear loose from the ground and float up around a player powering up, then get blasted
 * away when the transformation completes. Each rock is a lumpy low-poly ball wrapped in the texture of
 * the block it came from. Purely visual: every client spawns its own from the same synced charge state.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Debris {
    private static final int MAX = 90;
    private static final List<Chunk> CHUNKS = new ArrayList<>();
    /** A few pre-built rock shapes, each a list of triangles (3 unit-scale vertices). */
    private static final List<Vector3f[][]> ROCKS = new ArrayList<>();

    static {
        Random rnd = new Random(7);
        for (int i = 0; i < 6; i++) ROCKS.add(rock(rnd));
    }

    private static final class Chunk {
        final int owner, shape;
        final BlockState state;
        final float size, spinX, spinY;
        Vec3 pos, prevPos, vel;
        float hover; // height above the ground it drifts up to
        int age, life;
        boolean blasted;

        Chunk(int owner, BlockState state, Vec3 pos, float size, float hover, RandomSource rnd) {
            this.owner = owner;
            this.state = state;
            this.shape = rnd.nextInt(ROCKS.size());
            this.pos = this.prevPos = pos;
            this.vel = new Vec3(0, 0.02 + rnd.nextDouble() * 0.04, 0);
            this.size = size;
            this.hover = hover;
            this.spinX = (rnd.nextFloat() - 0.5f) * 8f;
            this.spinY = (rnd.nextFloat() - 0.5f) * 8f;
            this.life = 200 + rnd.nextInt(60);
        }
    }

    private Debris() {}

    /** Tears up to {@code count} rocks out of the ground around the player this tick. */
    public static void spawn(Player player, int count, float radius, float maxHover) {
        RandomSource rnd = player.getRandom();
        for (int i = 0; i < count && CHUNKS.size() < MAX; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 1.0 + rnd.nextDouble() * radius;
            double x = player.getX() + Math.cos(a) * r, z = player.getZ() + Math.sin(a) * r;
            BlockPos ground = BlockPos.containing(x, player.getY() - 0.5, z);
            BlockState state = player.level().getBlockState(ground);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
            // Mostly pebbles, now and then a real boulder
            float size = rnd.nextFloat() < 0.15f ? 0.32f + rnd.nextFloat() * 0.18f : 0.1f + rnd.nextFloat() * 0.16f;
            CHUNKS.add(new Chunk(player.getId(), state, new Vec3(x, ground.getY() + 1.0 + size * 0.5, z), size,
                    0.6f + rnd.nextFloat() * maxHover, rnd));
            for (int k = 0; k < 3; k++) {
                player.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        x + rnd.nextGaussian() * 0.2, ground.getY() + 1.05, z + rnd.nextGaussian() * 0.2, 0, 0.08, 0);
            }
        }
    }

    /** Transformation complete: every rock around this player is flung outward. */
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
                boolean powering = owner instanceof Player p && PlayerDBCData.get(p).isTransformHeld();
                if (!powering) {
                    c.vel = c.vel.add(0, -0.04, 0);
                } else {
                    double target = owner.getY() + c.hover;
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || CHUNKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        // The camera rotation is already on RenderSystem's model-view stack; at AFTER_ENTITIES Forge also
        // passes it in the event, and applying both is what made the rocks vanish when you turned around.
        PoseStack ps = new PoseStack();

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
            BlockPos at = BlockPos.containing(p);
            int light = LevelRenderer.getLightColor(mc.level, at);
            int tint = c.state.is(BlockTags.LEAVES) ? mc.getBlockColors().getColor(c.state, mc.level, at, 0) : -1;
            drawRock(vc, ps.last(), ROCKS.get(c.shape), mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(c.state),
                    0xFF000000 | (tint == -1 ? 0xFFFFFF : tint), light);
            ps.popPose();
        }
        buffers.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
    }

    private static void drawRock(VertexConsumer vc, PoseStack.Pose pose, Vector3f[][] tris, TextureAtlasSprite sprite, int argb, int light) {
        for (Vector3f[] tri : tris) {
            Vector3f n = new Vector3f(tri[1]).sub(tri[0]).cross(new Vector3f(tri[2]).sub(tri[0])).normalize();
            // Planar projection along the face's dominant axis keeps the texture from smearing
            int axis = Math.abs(n.x) > Math.abs(n.y) ? (Math.abs(n.x) > Math.abs(n.z) ? 0 : 2) : (Math.abs(n.y) > Math.abs(n.z) ? 1 : 2);
            for (int i = 0; i < 4; i++) {
                Vector3f v = tri[Math.min(i, 2)]; // quads mode: repeat the last corner
                float a = axis == 0 ? v.z : v.x, b = axis == 1 ? v.z : v.y;
                vc.addVertex(pose, v.x, v.y, v.z).setColor(argb)
                        .setUv(sprite.getU(Mth.clamp(a * 0.5f + 0.5f, 0f, 1f)), sprite.getV(Mth.clamp(b * 0.5f + 0.5f, 0f, 1f)))
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n.x, n.y, n.z);
            }
        }
    }

    /** Subdivided octahedron with jittered radii: a lumpy, rounded stone of about unit radius. */
    private static Vector3f[][] rock(Random rnd) {
        Vector3f[] o = {new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0),
                new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};
        int[][] faces = {{0, 2, 4}, {4, 2, 1}, {1, 2, 5}, {5, 2, 0}, {4, 3, 0}, {1, 3, 4}, {5, 3, 1}, {0, 3, 5}};
        List<Vector3f[]> tris = new ArrayList<>();
        for (int[] f : faces) tris.add(new Vector3f[]{o[f[0]], o[f[1]], o[f[2]]});
        java.util.Map<String, Vector3f> shared = new java.util.HashMap<>();
        for (int level = 0; level < 2; level++) {
            List<Vector3f[]> next = new ArrayList<>();
            for (Vector3f[] t : tris) {
                Vector3f a = mid(t[0], t[1], shared), b = mid(t[1], t[2], shared), c = mid(t[2], t[0], shared);
                next.add(new Vector3f[]{t[0], a, c});
                next.add(new Vector3f[]{a, t[1], b});
                next.add(new Vector3f[]{c, b, t[2]});
                next.add(new Vector3f[]{a, b, c});
            }
            tris = next;
        }
        // Push every distinct vertex in or out once, and flatten a little so it reads as a stone
        java.util.Map<Vector3f, Vector3f> moved = new java.util.IdentityHashMap<>();
        float squash = 0.7f + rnd.nextFloat() * 0.25f;
        for (Vector3f[] t : tris) {
            for (int i = 0; i < 3; i++) {
                Vector3f src = t[i];
                t[i] = moved.computeIfAbsent(src, v -> new Vector3f(v).normalize()
                        .mul(0.78f + rnd.nextFloat() * 0.32f).mul(1f, squash, 1f));
            }
        }
        return tris.toArray(new Vector3f[0][]);
    }

    private static Vector3f mid(Vector3f a, Vector3f b, java.util.Map<String, Vector3f> shared) {
        Vector3f m = new Vector3f(a).add(b).normalize();
        String key = Math.round(m.x * 1000) + "," + Math.round(m.y * 1000) + "," + Math.round(m.z * 1000);
        return shared.computeIfAbsent(key, k -> m);
    }
}
