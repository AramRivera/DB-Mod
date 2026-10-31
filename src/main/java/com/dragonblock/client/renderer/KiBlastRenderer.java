package com.dragonblock.client.renderer;

import com.dragonblock.common.entity.KiBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Energy ball, plus a beam trailing back to where it was fired for ki waves. */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity> {
    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KiBlastEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = entity.tickCount + partialTick;
        EnergyRender.sphere(pose, buffers, entity.getSize(), entity.getColor(), time);

        if (entity.isBeam() && entity.origin != null) {
            Vec3 here = new Vec3(
                    net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX()),
                    net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()),
                    net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ()));
            EnergyRender.beam(pose, buffers, entity.origin.subtract(here), entity.getSize() * 0.75f, entity.getColor(), time);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(KiBlastEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
