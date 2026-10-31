package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.ClientConfig;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.anim.DBPlayerModel;
import net.minecraft.world.phys.Vec3;
import com.dragonblock.client.fx.TransformFx;
import com.dragonblock.client.model.DBModels;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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

/** Draws race features, form hair/eyes/fur, the ki aura and transformation effects on every player. */
public class DBPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation HAIR_TEX = tex("hair");
    private static final ResourceLocation FUR_TEX = tex("fur");
    private static final ResourceLocation VEST_TEX = tex("fur_vest");
    private static final ResourceLocation FEATURE_TEX = tex("feature");
    private static final int FULL_BRIGHT = 0xF000F0;

    private final Map<Transformation.HairStyle, ModelPart> hair = new EnumMap<>(Transformation.HairStyle.class);
    private final ModelPart tail;
    private final ModelPart thickTail;
    private final ModelPart tailWrapped;
    private final ModelPart antennae;
    private final ModelPart horns;
    private final ModelPart tendril;
    private final ModelPart apeHead;
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
        apeHead = models.bakeLayer(DBModels.APE_HEAD);
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

        renderStrainGlow(pose, buffers, player, form, ageInTicks);
        renderFur(pose, buffers, light, body, form);
        renderRaceFeatures(pose, buffers, light, player, data, body, ageInTicks);
        renderHair(pose, buffers, light, player, data, body, partialTick);
        renderEyes(pose, buffers, body, form);
        if (form.isOozaru()) renderApeHead(pose, buffers, light, body, form);
        renderEnergy(pose, buffers, player, data, ageInTicks, partialTick);
    }

    // ------------------------------------------------------------------ body

    /** Kaio-ken: the whole body glows red, pulsing with the heartbeat. */
    private void renderStrainGlow(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player,
                                  Transformation form, float age) {
        if (!form.isKaioken()) return;
        getParentModel().copyPropertiesTo(fur);
        fur.setAllVisible(true);
        int alpha = (int) (70 + 50 * (0.5f + 0.5f * Mth.sin(age * 0.35f)));
        fur.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(player.getSkin().texture())),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, alpha << 24 | 0xFF3020);
    }

    private void renderFur(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> body,
                           Transformation form) {
        if (form.getFurColor() == 0) return;
        body.copyPropertiesTo(fur);
        fur.setAllVisible(true);
        fur.hat.visible = false;
        ResourceLocation texture = FUR_TEX;
        if (form == Transformation.SSJ4) {
            // Fur vest: arms, back and flanks only; the face and legs stay the player's own
            fur.head.visible = fur.leftLeg.visible = fur.rightLeg.visible = false;
            texture = VEST_TEX;
        }
        fur.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                OverlayTexture.NO_OVERLAY, 0xFF000000 | form.getFurColor());
    }

    private void renderRaceFeatures(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                    PlayerDBCData data, PlayerModel<AbstractClientPlayer> body, float age) {
        Race race = data.getRace();
        Transformation form = data.getTransformation();
        int color = 0xFF000000 | race.getFeatureColor();
        boolean flying = AnimState.isFlying(player, data);
        switch (race.getBodyFeature()) {
            case TAIL -> {
                if (data.isTailOut() || form.needsTail()) {
                    int tailColor = form.getFurColor() != 0 ? form.getFurColor() : race.getFeatureColor();
                    animateTail(tail, age, flying, player.isCrouching());
                    onPart(pose, buffers, light, body.body, tail, 0xFF000000 | tailColor);
                } else {
                    onPart(pose, buffers, light, body.body, tailWrapped, color);
                }
            }
            case HORNS_AND_TAIL -> {
                animateTail(thickTail, age, flying, player.isCrouching());
                onPart(pose, buffers, light, body.body, thickTail, color);
                if (form != Transformation.BASE && form != Transformation.ARCOSIAN_FINAL) {
                    onPart(pose, buffers, light, body.head, horns, 0xFF2A2430);
                }
            }
            case ANTENNAE -> onPart(pose, buffers, light, body.head, antennae, color);
            case HEAD_TENDRIL -> {
                animateTendril(age);
                onPart(pose, buffers, light, body.head, tendril, color);
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ hair and face

    /**
     * Hair of the current form. While powering up, the target form's hair flickers in (most forms) or
     * grows out from the scalp (cataclysm forms); right after transforming it overshoots slightly.
     */
    private void renderHair(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                            PlayerDBCData data, PlayerModel<AbstractClientPlayer> body, float partial) {
        if (!body.head.visible) return;
        Transformation shown = data.getTransformation();
        float growth = 1f;

        if (data.isTransformHeld() && data.getSelectedForm().getHairStyle() != Transformation.HairStyle.NONE) {
            Transformation target = data.getSelectedForm();
            float c = data.getTransformCharge();
            if (target.getSequence() == Transformation.Sequence.CATACLYSM) {
                shown = target;
                growth = 0.25f + 0.75f * c;
            } else if (c > 0.3f && (c > 0.75f || (player.tickCount / 2) % 2 == 0)) {
                shown = target; // the hair flashes between forms before it settles
            }
        } else {
            float burst = AnimState.progress(player, S2CAnimPacket.Anim.TRANSFORM, TransformFx.BURST_TICKS, partial);
            if (burst >= 0) growth = 1f + 0.1f * Mth.sin(burst * Mth.PI);
        }

        ModelPart part = hair.get(shown.getHairStyle());
        if (part == null || shown.isOozaru()) return;
        pose.pushPose();
        body.head.translateAndRotate(pose);
        if (growth != 1f) {
            // Grow from the crown of the skull
            pose.translate(0, -0.5f, 0);
            pose.scale(growth, growth, growth);
            pose.translate(0, 0.5f, 0);
        }
        part.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HAIR_TEX)),
                shown.hasGlowingHair() ? FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY, 0xFF000000 | shown.getHairColor());
        pose.popPose();
    }

    /** Colored irises over the skin's eyes (standard eye row), plus the red rim of the tier-4 form. */
    private void renderEyes(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> body,
                            Transformation form) {
        int eye = form.getEyeColor();
        if (eye == 0 || !body.head.visible || form.isOozaru()) return; // the ape head has its own eyes
        pose.pushPose();
        body.head.translateAndRotate(pose);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(FEATURE_TEX));
        float z = -4.02f, y = -4f;
        faceRect(vc, pose, -2.6f, y, -1.4f, y + 1f, z, 0xFF000000 | eye);
        faceRect(vc, pose, 1.4f, y, 2.6f, y + 1f, z, 0xFF000000 | eye);
        if (form == Transformation.SSJ4) {
            faceRect(vc, pose, -3.2f, y - 0.5f, -0.8f, y - 0.15f, z - 0.01f, 0xFFC02028);
            faceRect(vc, pose, 0.8f, y - 0.5f, 3.2f, y - 0.15f, z - 0.01f, 0xFFC02028);
        }
        pose.popPose();
    }

    private static void faceRect(VertexConsumer vc, PoseStack pose, float x0, float y0, float x1, float y1, float z, int argb) {
        PoseStack.Pose p = pose.last();
        float s = 1f / 16f;
        vertex(vc, p, x0 * s, y1 * s, z * s, argb);
        vertex(vc, p, x1 * s, y1 * s, z * s, argb);
        vertex(vc, p, x1 * s, y0 * s, z * s, argb);
        vertex(vc, p, x0 * s, y0 * s, z * s, argb);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, int argb) {
        vc.addVertex(p, x, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT).setNormal(p, 0, 0, -1);
    }

    private void renderApeHead(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> body,
                               Transformation form) {
        if (!body.head.visible) return;
        pose.pushPose();
        body.head.translateAndRotate(pose);
        apeHead.getChild("fur").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FUR_TEX)), light,
                OverlayTexture.NO_OVERLAY, 0xFF000000 | form.getFurColor());
        VertexConsumer skin = buffers.getBuffer(RenderType.entityCutoutNoCull(FEATURE_TEX));
        apeHead.getChild("skin").render(pose, skin, light, OverlayTexture.NO_OVERLAY,
                form == Transformation.GOLDEN_OOZARU ? 0xFFE8C890 : 0xFFC89A70);
        apeHead.getChild("dark").render(pose, skin, light, OverlayTexture.NO_OVERLAY, 0xFF2A1A12);
        apeHead.getChild("fangs").render(pose, skin, light, OverlayTexture.NO_OVERLAY, 0xFFF4F0E6);
        apeHead.getChild("eyes").render(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(FEATURE_TEX)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000 | form.getEyeColor());
        pose.popPose();
    }

    // ------------------------------------------------------------------ energy

    private void renderEnergy(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, PlayerDBCData data,
                              float time, float partial) {
        Transformation form = data.getTransformation();
        Transformation shown = data.isTransformHeld() ? data.getSelectedForm() : form;
        boolean local = player == Minecraft.getInstance().player;
        boolean visible = ClientConfig.showAura(local);
        float power = TransformFx.auraPower(player, partial);

        if (power > 0.02f && visible) {
            boolean silver = shown.getAuraStyle() == Transformation.AuraStyle.SILVER;
            boolean charging = data.isChargingKi() || data.isTransformHeld();
            float pulse = charging ? 1f + 0.05f * Mth.sin(time * 0.6f) : 1f;
            float size = EnergyRender.auraSize(data.getBattlePower(), shown.getStatMultiplier())
                    * (0.7f + 0.3f * Math.min(power, 1.35f)) * pulse;
            float intensity = Math.min(1f, power) * (silver ? 0.6f : 1f);
            EnergyRender.aura(pose, buffers, time, shown.getAuraColor(), intensity, size,
                    shown.getAuraWidth(), shown.getAuraHeight(), charging && player.onGround());
        }

        // Boosting through the air: a tight, streamlined aura hugs the (tilted) body
        if (com.dragonblock.client.fx.SpeedTrail.isBoosting(player) && visible) {
            float speed = (float) Math.min(1.0, (AnimState.horizontalSpeed(player) - 0.4) * 2.0);
            EnergyRender.aura(pose, buffers, time * 2f, form.getAuraColor(), 0.6f + 0.4f * speed, 0.75f, 0.55f, 1.0f, false);
        }

        // Lightning: storm forms always crackle; any power-up crackles past the halfway mark
        int bolts = form.getLightningColor() != 0 ? (form == Transformation.SSJ3 ? 3 : 2) : 0;
        int boltColor = form.getLightningColor();
        if (data.isTransformHeld() && data.getTransformCharge() > 0.5f) {
            bolts = Math.max(bolts, 2 + (int) (data.getTransformCharge() * 2));
            boltColor = shown.getLightningColor() != 0 ? shown.getLightningColor()
                    : EnergyRender.mix(shown.getAuraColor(), 0xFFFFFF, 0.5f);
        }
        if (bolts > 0 && ((int) time + player.getId()) % 6 < 4) {
            EnergyRender.lightning(pose, buffers, player.getId() * 31 + (int) time / 2, boltColor, bolts, 1f);
        }

        // Completion burst, shaped by the form's sequence
        float burst = AnimState.progress(player, S2CAnimPacket.Anim.TRANSFORM, TransformFx.BURST_TICKS, partial);
        if (burst >= 0 && visible) {
            float size = EnergyRender.auraSize(data.getBattlePower(), form.getStatMultiplier());
            switch (form.getSequence()) {
                case DIVINE -> EnergyRender.pillar(pose, buffers, burst, form.getAuraColor(), time);
                case STRAIN -> EnergyRender.burst(pose, buffers, burst, form.getAuraColor(), size * 0.6f);
                case CATACLYSM, GIANT -> EnergyRender.burst(pose, buffers, burst, form.getAuraColor(), size * 1.5f);
                default -> EnergyRender.burst(pose, buffers, burst, form.getAuraColor(), size);
            }
        }

        // Ki wave gathering between the cupped hands
        if (data.isWaveHeld() && data.getWaveCharge() > 0f) {
            pose.pushPose();
            Vec3 hands = getParentModel() instanceof DBPlayerModel m ? m.handsMidpoint() : new Vec3(-0.45, 0.62, -0.15);
            pose.translate(hands.x, hands.y, hands.z);
            EnergyRender.sphere(pose, buffers, 0.08f + 0.22f * data.getWaveCharge(), form.getAuraColor(), time);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ animation helpers

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

    private static void onPart(PoseStack pose, MultiBufferSource buffers, int light, ModelPart parent, ModelPart model, int argb) {
        if (!parent.visible) return;
        pose.pushPose();
        parent.translateAndRotate(pose);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FEATURE_TEX)), light, OverlayTexture.NO_OVERLAY, argb);
        pose.popPose();
    }
}
