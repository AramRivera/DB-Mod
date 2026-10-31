package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.model.DBModels;
import com.dragonblock.common.block.DragonBallBlock;
import com.dragonblock.common.entity.ShenlongEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ritual on screen. Rising: the balls pulse, a column of light erupts and the dragon flows out of it
 * head first, coiling up into the sky. Waiting: it hangs there, undulating, eyes on its summoner.
 * Leaving: it dissolves into light from the tail up while the balls rise and shoot off in all directions.
 * Keep path(), SEGMENTS and SCALE in sync with tools/preview_dragon_blender.py.
 */
public class ShenlongRenderer extends EntityRenderer<ShenlongEntity> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/dragon.png");
    private static final float SCALE = 3.4f / 16f;
    private static final int SEGMENTS = 44;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Map<String, Integer> COLORS = Map.of(
            "scales", 0xFF3FA34D, "belly", 0xFFE8D9A0, "horn", 0xFFC9A46A, "whisker", 0xFF2A3A2A,
            "teeth", 0xFFF4F0E6, "eye", 0xFFFF2020, "fin", 0xFFE0602A);

    private final ModelPart head, segment, tail, leg;
    /** Ball positions (relative to the dragon) seen when it appeared, for the glow and the scattering. */
    private final Map<Integer, List<Vec3>> balls = new HashMap<>();

    public ShenlongRenderer(EntityRendererProvider.Context context) {
        super(context);
        head = context.bakeLayer(DBModels.DRAGON.get("head"));
        segment = context.bakeLayer(DBModels.DRAGON.get("segment"));
        tail = context.bakeLayer(DBModels.DRAGON.get("tail"));
        leg = context.bakeLayer(DBModels.DRAGON.get("leg"));
    }

    /** The coil (entity-relative, y up): s = 0 down at the balls, 1 high up where the head is. */
    static Vec3 path(float s, float t) {
        float r = 7f + 3f * Mth.sin(Mth.PI * s);
        float th = Mth.TWO_PI * 2.2f * s + 0.15f * Mth.sin(t * 0.05f + s * 12f);
        return new Vec3(r * Mth.cos(th), 2f + 34f * s + 0.6f * Mth.sin(t * 0.07f + s * 9f), r * Mth.sin(th));
    }

    @Override
    public boolean shouldRender(ShenlongEntity entity, Frustum frustum, double x, double y, double z) {
        return true; // forty blocks tall: never cull it by its small origin box
    }

    @Override
    public void render(ShenlongEntity dragon, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = dragon.tickCount + partial;
        float age = dragon.stateAge() + partial;
        List<Vec3> spheres = balls.computeIfAbsent(dragon.getId(), id -> findBalls(dragon));
        if (balls.size() > 16) balls.keySet().removeIf(id -> dragon.level().getEntity(id) == null);

        float emerge = 1f, pillar = 0f, dissolve = 0f;
        switch (dragon.getState()) {
            case RISING -> {
                pillar = Mth.clamp((age - 50f) / 15f, 0f, 1f);
                float e = Mth.clamp((age - 70f) / (ShenlongEntity.RISE_TICKS - 70f), 0f, 1f);
                emerge = 1f - (1f - e) * (1f - e) * (1f - e); // bursts out, then settles
            }
            case WAITING -> pillar = Math.max(0f, 1f - age / 40f);
            case LEAVING -> dissolve = Mth.clamp((age - 30f) / 70f, 0f, 1.05f);
        }

        renderBalls(pose, buffers, dragon, spheres, age, t);
        if (pillar > 0) {
            pose.pushPose();
            EnergyRender.beam(pose, buffers, new Vec3(0, 70, 0), 0.4f + 2.2f * pillar, EnergyRender.scaleColor(0xFFF2B0, pillar) & 0xFFFFFF, t);
            pose.popPose();
        }
        if (dragon.getState() == ShenlongEntity.State.RISING && age < 70) return; // only the light so far

        float ds = 1f / SEGMENTS;
        int gone = (int) (dissolve * SEGMENTS);
        for (int i = 0; i < SEGMENTS; i++) {
            float s = emerge - (SEGMENTS - i) * ds;
            if (s < 0 || i < gone) continue;
            Vec3 at = path(s, t);
            if (i == gone && dissolve > 0) {
                // The light eating its way up the body
                pose.pushPose();
                pose.translate(at.x, at.y, at.z);
                EnergyRender.sphere(pose, buffers, 2.2f, 0xFFF8D0, t);
                pose.popPose();
            }
            float size = 0.55f + 0.45f * Math.min(1f, i / 10f);
            pose.pushPose();
            place(pose, at, path(s + ds, t), size);
            draw(i == 0 ? tail : segment, pose, buffers);
            if (i == SEGMENTS - 7 || i == SEGMENTS - 20) {
                for (int side = -1; side <= 1; side += 2) {
                    pose.pushPose();
                    pose.translate(side * 3.2f, 2.5f, -1f); // in model pixels: hang from the belly
                    pose.mulPose(Axis.ZP.rotation(-side * 0.6f + Mth.sin(t * 0.1f + i) * 0.15f));
                    draw(leg, pose, buffers);
                    pose.popPose();
                }
            }
            pose.popPose();
        }
        if (dissolve >= 1f) return;

        // The head looks at its summoner (or straight along the coil)
        Vec3 headAt = path(emerge, t);
        Entity summoner = dragon.level().getEntity(dragon.getSummonerId());
        Vec3 look = summoner != null ? summoner.getEyePosition(partial).subtract(dragon.getPosition(partial)) : path(Math.min(1f, emerge + ds), t).add(0, -2, 0);
        pose.pushPose();
        place(pose, headAt, look, 1f);
        draw(head, pose, buffers);
        pose.popPose();
    }

    /** Moves the pose to {@code at}, turns its forward (-z) toward {@code toward}, and sets model pixel scale. */
    private static void place(PoseStack pose, Vec3 at, Vec3 toward, float size) {
        Vec3 d = toward.subtract(at).normalize();
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-d.x, -d.z)));
        pose.mulPose(Axis.XP.rotation((float) Math.asin(Mth.clamp(d.y, -1, 1))));
        float s = SCALE * size;
        pose.scale(-s, -s, s); // model pixels are y-down, like every Minecraft model
    }

    private static void draw(ModelPart part, PoseStack pose, MultiBufferSource buffers) {
        for (String group : DBModels.DRAGON_GROUPS) {
            ModelPart child = part.getChild(group);
            boolean eye = group.equals("eye");
            child.render(pose, buffers.getBuffer(eye ? RenderType.entityTranslucentEmissive(TEX) : RenderType.entityCutoutNoCull(TEX)),
                    FULL_BRIGHT, OverlayTexture.NO_OVERLAY, COLORS.get(group));
        }
    }

    /** Glowing orbs over the balls; when the dragon leaves they rise and fly off in seven directions. */
    private static void renderBalls(PoseStack pose, MultiBufferSource buffers, ShenlongEntity dragon, List<Vec3> spheres, float age, float t) {
        boolean leaving = dragon.getState() == ShenlongEntity.State.LEAVING;
        for (int i = 0; i < spheres.size(); i++) {
            Vec3 at = spheres.get(i).add(0, 0.3, 0);
            float glow = 0.6f + 0.4f * Mth.sin(t * 0.3f + i);
            if (leaving) {
                float rise = Math.min(age, 40f) / 40f, fly = Math.max(0f, age - 40f) / 80f;
                if (fly >= 1f) continue;
                double dir = i * Mth.TWO_PI / Math.max(1, spheres.size()) + 0.4;
                at = at.add(Math.cos(dir) * fly * 90, rise * 22 + fly * 30, Math.sin(dir) * fly * 90);
                glow = 1f;
            } else if (dragon.getState() == ShenlongEntity.State.RISING) {
                glow *= 0.5f + Math.min(1f, age / 50f); // swelling as the ritual builds
            }
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            EnergyRender.sphere(pose, buffers, 0.35f * glow + (leaving ? 0.3f : 0f), 0xFFA020, t);
            pose.popPose();
        }
    }

    private static List<Vec3> findBalls(ShenlongEntity dragon) {
        List<Vec3> out = new ArrayList<>();
        BlockPos c = dragon.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
            if (dragon.level().getBlockState(p).getBlock() instanceof DragonBallBlock) {
                out.add(Vec3.atBottomCenterOf(p).subtract(dragon.position()));
            }
        }
        return out;
    }

    @Override
    public ResourceLocation getTextureLocation(ShenlongEntity entity) {
        return TEX;
    }
}
