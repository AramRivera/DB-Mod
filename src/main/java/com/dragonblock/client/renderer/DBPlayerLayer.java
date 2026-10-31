package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.model.DBModels;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/** Draws race features, form hair, fur and the ki aura on top of every player. */
public class DBPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation HAIR_TEX = tex("hair");
    private static final ResourceLocation FUR_TEX = tex("fur");
    private static final ResourceLocation FEATURE_TEX = tex("feature");
    private static final int FULL_BRIGHT = 0xF000F0;

    private final Map<Transformation.HairStyle, ModelPart> hair = new EnumMap<>(Transformation.HairStyle.class);
    private final ModelPart tail;
    private final ModelPart thickTail;
    private final ModelPart tailWrapped;
    private final ModelPart antennae;
    private final ModelPart horns;
    private final ModelPart tendril;
    private final HumanoidModel<AbstractClientPlayer> fur;

    public DBPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        DBModels.HAIR.forEach((style, loc) -> hair.put(style, models.bakeLayer(loc)));
        tail = models.bakeLayer(DBModels.TAIL);
        thickTail = models.bakeLayer(DBModels.THICK_TAIL);
        tailWrapped = models.bakeLayer(DBModels.TAIL_WRAPPED);
        antennae = models.bakeLayer(DBModels.ANTENNAE);
        horns = models.bakeLayer(DBModels.HORNS);
        tendril = models.bakeLayer(DBModels.TENDRIL);
        fur = new HumanoidModel<>(models.bakeLayer(DBModels.FUR));
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/" + name + ".png");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        PlayerDBCData data = PlayerDBCData.get(player);
        Transformation form = data.getTransformation();
        PlayerModel<AbstractClientPlayer> body = getParentModel();
        int overlay = OverlayTexture.NO_OVERLAY;

        // Fur (Ozaru all over, SSJ4 on torso and arms)
        if (form.getFurColor() != 0) {
            body.copyPropertiesTo(fur);
            fur.setAllVisible(true);
            if (form == Transformation.SUPER_SAIYAN_4) {
                fur.head.visible = fur.hat.visible = fur.leftLeg.visible = fur.rightLeg.visible = false;
            }
            fur.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FUR_TEX)), light, overlay,
                    0xFF000000 | form.getFurColor());
        }

        // Race features
        Race race = data.getRace();
        VertexConsumerFeature feature = new VertexConsumerFeature(pose, buffers, light, 0xFF000000 | race.getFeatureColor());
        switch (race.getBodyFeature()) {
            case TAIL -> {
                if (data.isTailOut() || form.needsTail()) {
                    int tailColor = form.getFurColor() != 0 ? form.getFurColor() : race.getFeatureColor();
                    animateTail(tail, ageInTicks, data.isFlying() && player.getAbilities().flying, player.isCrouching());
                    feature.onPart(body.body, tail, 0xFF000000 | tailColor);
                } else {
                    feature.onPart(body.body, tailWrapped, feature.color);
                }
            }
            case HORNS_AND_TAIL -> {
                animateTail(thickTail, ageInTicks, data.isFlying() && player.getAbilities().flying, player.isCrouching());
                feature.onPart(body.body, thickTail, feature.color);
                if (form != Transformation.BASE && form != Transformation.ARCOSIAN_FINAL) {
                    feature.onPart(body.head, horns, 0xFF2A2430);
                }
            }
            case ANTENNAE -> feature.onPart(body.head, antennae, feature.color);
            case HEAD_TENDRIL -> {
                animateTendril(ageInTicks);
                feature.onPart(body.head, tendril, feature.color);
            }
            default -> {}
        }

        // Hair
        ModelPart hairPart = hair.get(form.getHairStyle());
        if (hairPart != null && body.head.visible) {
            pose.pushPose();
            body.head.translateAndRotate(pose);
            hairPart.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HAIR_TEX)),
                    form.hasGlowingHair() ? FULL_BRIGHT : light, overlay, 0xFF000000 | form.getHairColor());
            pose.popPose();
        }

        renderAura(pose, buffers, player, data, ageInTicks);
    }

    private void renderAura(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, PlayerDBCData data, float time) {
        Transformation form = data.getTransformation();
        Transformation shown = data.isTransformHeld() ? data.getSelectedForm() : form;

        float intensity;
        boolean powering = data.isChargingKi() || data.isTransformHeld() || data.isWaveHeld();
        if (powering) {
            intensity = 0.85f + 0.15f * data.getTransformCharge();
        } else if (form != Transformation.BASE && form.getAuraStyle() != Transformation.AuraStyle.CALM) {
            intensity = 0.55f;
        } else {
            intensity = 0f;
        }

        if (intensity > 0f) {
            boolean silver = shown.getAuraStyle() == Transformation.AuraStyle.SILVER;
            float size = EnergyRender.auraSize(data.getBattlePower(), shown.getStatMultiplier())
                    * (silver ? 0.85f : 1f) * (1f + (data.isTransformHeld() ? 0.3f * data.getTransformCharge() : 0f));
            EnergyRender.aura(pose, buffers, time, shown.getAuraColor(), silver ? intensity * 0.6f : intensity, size,
                    powering && player.onGround());
        }

        // Lightning: storm-type forms always, anything else while forcing a transformation
        boolean storm = form.getAuraStyle() == Transformation.AuraStyle.STORM;
        if (storm || (data.isTransformHeld() && data.getTransformCharge() > 0.5f)) {
            if (((int) time + player.getId()) % 5 < 3) {
                EnergyRender.lightning(pose, buffers, player.getId() * 31 + (int) time / 2,
                        EnergyRender.mix(shown.getAuraColor(), 0x9FD8FF, 0.5f));
            }
        }

        // Ki wave gathering between the hands
        if (data.isWaveHeld() && data.getWaveCharge() > 0f) {
            pose.pushPose();
            pose.translate(-0.45f, 0.62f, -0.15f); // cupped hands at the right hip
            EnergyRender.sphere(pose, buffers, 0.08f + 0.22f * data.getWaveCharge(), form.getAuraColor(), time);
            pose.popPose();
        }
    }

    /** Sway side to side, droop when standing, stream straight back in flight. */
    private static void animateTail(ModelPart tailRoot, float age, boolean flying, boolean crouching) {
        ModelPart seg = tailRoot.getChild("root");
        for (int i = 0; i < DBModels.TAIL_SEGMENTS; i++) {
            seg = seg.getChild("seg" + i);
            float sway = Mth.sin(age * 0.09f - i * 0.55f) * (0.08f + 0.04f * i);
            if (flying) {
                seg.xRot = i == 0 ? 0.25f : 0.03f;
                seg.yRot = sway * 0.4f;
            } else {
                seg.xRot = i == 0 ? (crouching ? -0.2f : -0.95f) : 0.2f + 0.03f * i;
                seg.yRot = sway;
            }
        }
    }

    private void animateTendril(float age) {
        ModelPart seg = tendril.getChild("root");
        for (int i = 0; i < DBModels.TENDRIL_SEGMENTS; i++) {
            seg = seg.getChild("seg" + i);
            seg.zRot = Mth.sin(age * 0.07f - i * 0.6f) * 0.06f * (i + 1);
        }
    }

    /** Small helper so every feature renders on its parent part the same way. */
    private static final class VertexConsumerFeature {
        private final PoseStack pose;
        private final MultiBufferSource buffers;
        private final int light;
        final int color;

        VertexConsumerFeature(PoseStack pose, MultiBufferSource buffers, int light, int color) {
            this.pose = pose;
            this.buffers = buffers;
            this.light = light;
            this.color = color;
        }

        void onPart(ModelPart parent, ModelPart model, int argb) {
            if (!parent.visible) return;
            pose.pushPose();
            parent.translateAndRotate(pose);
            model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FEATURE_TEX)), light, OverlayTexture.NO_OVERLAY, argb);
            pose.popPose();
        }
    }
}
