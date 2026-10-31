package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.model.DBModels;
import com.dragonblock.client.model.ShenlongModel;
import com.dragonblock.common.entity.ShenlongEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ShenlongRenderer extends EntityRenderer<ShenlongEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            DragonBlockMod.MODID, "textures/entity/shenlong.png");

    private final ShenlongModel model;

    public ShenlongRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.5f;
        this.model = new ShenlongModel(context.bakeLayer(DBModels.SHENLONG));
    }

    @Override
    public ResourceLocation getTextureLocation(ShenlongEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ShenlongEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float time = entity.tickCount + partialTick;

        // Render Divine Dragon Aura around Shenlong
        EnergyRender.aura(poseStack, buffer, time, 0x00FF66, 0.85f, 3.2f, 1.4f, 4.5f, true);

        // Position and orient Shenlong model (looming majestically in the air)
        poseStack.translate(0.0, 1.5, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        // Standard Minecraft entity model inversion (+Y down -> +Y up) and scaling
        float scale = 1.8f;
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0F, -1.8F, 0.0F);

        // Animate model
        this.model.setupAnim(entity, 0, 0, time, 0, 0);

        // Main colored cut-out pass
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        // Glowing red dragon eyes pass (Full Bright)
        VertexConsumer eyesVc = buffer.getBuffer(RenderType.eyes(TEXTURE));
        this.model.renderToBuffer(poseStack, eyesVc, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
