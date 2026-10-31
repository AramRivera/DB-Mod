package com.dragonblock.client.renderer;

import com.dragonblock.common.entity.ShenlongEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ShenlongRenderer extends EntityRenderer<ShenlongEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;

    public ShenlongRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(ShenlongEntity entity) {
        return null;
    }

    @Override
    public void render(ShenlongEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float time = entity.tickCount + partialTick;

        // Render Dragon Aura around Shenlong
        EnergyRender.aura(poseStack, buffer, time, 0x00FF55, 0.8f, 2.5f, 1.2f, 3.5f, true);

        // Dragon serpentine coiled body
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        PoseStack.Pose p = poseStack.last();

        int segments = 24;
        float bodyRadius = 0.45f;
        int emeraldGreen = 0xFF0A6828;
        int creamYellow = 0xFFD8D278;

        Vec3 prevCenter = null;
        for (int i = 0; i <= segments; i++) {
            float progress = (float) i / segments;
            // Coiling spiral equation going upwards
            float angle = progress * Mth.TWO_PI * 2.2f + (time * 0.03f);
            float spiralRadius = 1.4f * (1.0f - progress * 0.4f);
            float x = Mth.cos(angle) * spiralRadius;
            float y = progress * 3.8f + Mth.sin(time * 0.08f + progress * 4.0f) * 0.25f;
            float z = Mth.sin(angle) * spiralRadius;
            Vec3 center = new Vec3(x, y, z);

            if (prevCenter != null) {
                renderBodySegment(p, vc, prevCenter, center, bodyRadius * (1.1f - progress * 0.4f), emeraldGreen, creamYellow);
            }
            prevCenter = center;

            // Dragon Head at top
            if (i == segments) {
                renderDragonHead(poseStack, p, buffer, center, time);
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderBodySegment(PoseStack.Pose p, VertexConsumer vc, Vec3 from, Vec3 to, float radius, int green, int belly) {
        Vec3 dir = to.subtract(from).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize().scale(radius);
        Vec3 normalUp = right.cross(dir).normalize().scale(radius);

        final int sides = 8;
        for (int s = 0; s < sides; s++) {
            float a0 = Mth.TWO_PI * s / sides;
            float a1 = Mth.TWO_PI * (s + 1) / sides;

            Vec3 offset0 = right.scale(Mth.cos(a0)).add(normalUp.scale(Mth.sin(a0)));
            Vec3 offset1 = right.scale(Mth.cos(a1)).add(normalUp.scale(Mth.sin(a1)));

            int col = (s >= 5 && s <= 7) ? belly : green;

            drawQuad(vc, p,
                    from.add(offset0), from.add(offset1),
                    to.add(offset1), to.add(offset0),
                    col);
        }
    }

    private void renderDragonHead(PoseStack poseStack, PoseStack.Pose p, MultiBufferSource buffer, Vec3 headPos, float time) {
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        float hx = (float) headPos.x;
        float hy = (float) headPos.y;
        float hz = (float) headPos.z;

        // Snout & Head box (Emerald)
        drawBox(vc, p, hx - 0.4f, hy - 0.2f, hz - 0.6f, hx + 0.4f, hy + 0.4f, hz + 0.4f, 0xFF0B7B30);

        // Glowing Ruby Red Eyes
        VertexConsumer vcGlow = buffer.getBuffer(RenderType.eyes(ResourceLocation.withDefaultNamespace("textures/entity/spider_eyes.png")));
        int redEye = 0xFFFF1010;
        drawBox(vcGlow, p, hx + 0.35f, hy + 0.15f, hz + 0.1f, hx + 0.42f, hy + 0.3f, hz + 0.25f, redEye);
        drawBox(vcGlow, p, hx - 0.42f, hy + 0.15f, hz + 0.1f, hx - 0.35f, hy + 0.3f, hz + 0.25f, redEye);

        // Golden Antlers / Horns
        int gold = 0xFFD89B15;
        drawBox(vc, p, hx + 0.25f, hy + 0.35f, hz - 0.2f, hx + 0.35f, hy + 1.1f, hz - 0.1f, gold);
        drawBox(vc, p, hx - 0.35f, hy + 0.35f, hz - 0.2f, hx - 0.25f, hy + 1.1f, hz - 0.1f, gold);

        // Dragon Whiskers
        float wOsc = Mth.sin(time * 0.15f) * 0.15f;
        drawBox(vc, p, hx + 0.38f, hy - 0.05f, hz + 0.3f, hx + 0.9f + wOsc, hy - 0.3f, hz + 0.35f, 0xFFFFFFFF);
        drawBox(vc, p, hx - 0.9f - wOsc, hy - 0.05f, hz + 0.3f, hx - 0.38f, hy - 0.3f, hz + 0.35f, 0xFFFFFFFF);
    }

    private void drawBox(VertexConsumer vc, PoseStack.Pose p, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
        Vec3 v0 = new Vec3(minX, minY, minZ);
        Vec3 v1 = new Vec3(maxX, minY, minZ);
        Vec3 v2 = new Vec3(maxX, maxY, minZ);
        Vec3 v3 = new Vec3(minX, maxY, minZ);

        Vec3 v4 = new Vec3(minX, minY, maxZ);
        Vec3 v5 = new Vec3(maxX, minY, maxZ);
        Vec3 v6 = new Vec3(maxX, maxY, maxZ);
        Vec3 v7 = new Vec3(minX, maxY, maxZ);

        drawQuad(vc, p, v0, v1, v2, v3, color); // North
        drawQuad(vc, p, v5, v4, v7, v6, color); // South
        drawQuad(vc, p, v4, v0, v3, v7, color); // West
        drawQuad(vc, p, v1, v5, v6, v2, color); // East
        drawQuad(vc, p, v3, v2, v6, v7, color); // Up
        drawQuad(vc, p, v4, v5, v1, v0, color); // Down
    }

    private void drawQuad(VertexConsumer vc, PoseStack.Pose p, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        vert(vc, p, a, color);
        vert(vc, p, b, color);
        vert(vc, p, c, color);
        vert(vc, p, d, color);
    }

    private void vert(VertexConsumer vc, PoseStack.Pose p, Vec3 pos, int argb) {
        vc.addVertex(p, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(argb)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(p, 0f, 1f, 0f);
    }
}
