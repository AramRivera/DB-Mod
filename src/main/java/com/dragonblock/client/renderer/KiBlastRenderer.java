package com.dragonblock.client.renderer;

import com.dragonblock.common.data.Technique;
import com.dragonblock.common.entity.KiBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Draws each traveling technique its own way: spheres, beams tied to the hands, spirals, discs. */
public class KiBlastRenderer extends EntityRenderer<KiBlastEntity> {
    public KiBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(KiBlastEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true; // a beam's tail can be on screen while its head is not
    }

    @Override
    public void render(KiBlastEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = entity.tickCount + partialTick;
        Technique tech = entity.getTechnique();
        float size = entity.getSize();
        int color = entity.getColor();

        switch (tech.getKind()) {
            case DISC -> EnergyRender.disc(pose, buffers, size * 1.4f, color, time);
            case BOMB -> {
                EnergyRender.sphere(pose, buffers, size, color, time);
                EnergyRender.sphere(pose, buffers, size * 1.25f, EnergyRender.scaleColor(color, 0.25f) & 0xFFFFFF, time * 0.5f);
            }
            case BEAM, PIERCE -> {
                EnergyRender.sphere(pose, buffers, size * (tech.getKind() == Technique.Kind.BEAM ? 1.25f : 1f), color, time);
                if (entity.origin == null) break;
                Vec3 here = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                        Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
                Vec3 tail = entity.prevOrigin == null ? entity.origin : entity.prevOrigin.lerp(entity.origin, partialTick);
                Vec3 back = tail.subtract(here);
                EnergyRender.beam(pose, buffers, back, size * (tech.getKind() == Technique.Kind.BEAM ? 0.75f : 0.5f), color, time);
                if (tech == Technique.MAKANKOSAPPO) EnergyRender.spiral(pose, buffers, back, size * 1.6f, 0xB84DFF, time);
            }
            default -> EnergyRender.sphere(pose, buffers, size, color, time);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(KiBlastEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
