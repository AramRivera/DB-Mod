package com.dragonblock.client.anim;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Player model that layers the mod's poses on top of vanilla animation. Swapped into every
 * PlayerRenderer at startup, so it drives every player in view. The first-person arm renderer poses
 * this same model with {@link #firstPerson} set, so both cameras share one set of poses.
 */
public class DBPlayerModel extends PlayerModel<AbstractClientPlayer> {
    /** Set while ClientRenderEvents draws the local player's arms from the eyes. */
    public boolean firstPerson = false;

    public DBPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        PlayerDBCData data = PlayerDBCData.get(player);
        applyBulk(data.getTransformation().getBulk());
        if (limbSwing == 0 && limbSwingAmount == 0 && ageInTicks == 0) return; // vanilla first-person hand render

        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float shake = Mth.sin(ageInTicks * 2.7f) * 0.03f;

        if (data.getTransformation().getHairStyle() != Transformation.HairStyle.NONE) hat.visible = false;

        switch (AnimState.pose(player, partial)) {
            case GUARD -> {
                // Forearms crossed in front of the face
                if (firstPerson) {
                    rightArm.xRot = -1.9f; rightArm.yRot = -0.95f; rightArm.zRot = 0f;
                    leftArm.xRot = -1.72f; leftArm.yRot = 0.95f; leftArm.zRot = 0f;
                } else {
                    rightArm.xRot = -1.45f + pitch * 0.5f; rightArm.yRot = -0.75f; rightArm.zRot = 0f;
                    leftArm.xRot = -1.30f + pitch * 0.5f; leftArm.yRot = 0.85f; leftArm.zRot = 0f;
                }
            }
            case BLAST -> {
                float p = AnimState.progress(player, S2CAnimPacket.Anim.BLAST, AnimState.BLAST_TICKS, partial);
                float thrust = p < 0.3f ? p / 0.3f : 1f - (p - 0.3f) / 0.7f * 0.4f;
                rightArm.xRot = Mth.lerp(thrust, rightArm.xRot, (firstPerson ? -1.62f : -Mth.HALF_PI) + pitch);
                rightArm.yRot = Mth.lerp(thrust, rightArm.yRot, firstPerson ? -0.3f : yaw - 0.1f);
                rightArm.zRot = 0f;
                body.yRot = firstPerson ? 0f : 0.25f * thrust;
            }
            case WAVE_RELEASE -> {
                // Both palms pushed toward the target
                float reach = firstPerson ? -1.62f : -Mth.HALF_PI;
                float spread = firstPerson ? 0.35f : 0.2f;
                rightArm.xRot = reach + pitch; rightArm.yRot = yaw - spread; rightArm.zRot = 0f;
                leftArm.xRot = reach + pitch; leftArm.yRot = yaw + spread; leftArm.zRot = 0f;
                rightLeg.xRot = 0.35f; leftLeg.xRot = -0.35f;
            }
            case WAVE_CHARGE -> {
                if (firstPerson) {
                    // Cupped hands low in front, so the gathering sphere is in view
                    rightArm.xRot = -1.45f + shake; rightArm.yRot = -0.6f; rightArm.zRot = 0f;
                    leftArm.xRot = -1.45f - shake; leftArm.yRot = 0.6f; leftArm.zRot = 0f;
                } else {
                    // Cupped hands drawn back to the right hip, torso twisted
                    body.yRot = -0.45f;
                    rightArm.xRot = 0.35f + shake; rightArm.yRot = -0.35f; rightArm.zRot = 0.25f;
                    leftArm.xRot = -0.35f + shake; leftArm.yRot = 1.0f; leftArm.zRot = 0.1f;
                    rightLeg.xRot = 0.3f; leftLeg.xRot = -0.3f;
                    rightLeg.zRot = 0.08f; leftLeg.zRot = -0.08f;
                }
            }
            case POWER_UP -> {
                if (firstPerson) {
                    // Clenched fists trembling low in front of the chest
                    rightArm.xRot = -1.28f + shake * 2; rightArm.yRot = -0.2f; rightArm.zRot = 0.25f;
                    leftArm.xRot = -1.28f - shake * 2; leftArm.yRot = 0.2f; leftArm.zRot = -0.25f;
                } else {
                    // Arms flexed down and out, legs planted wide, head thrown back
                    rightArm.xRot = 0.15f + shake; rightArm.yRot = 0f; rightArm.zRot = 0.75f + shake;
                    leftArm.xRot = 0.15f - shake; leftArm.yRot = 0f; leftArm.zRot = -0.75f - shake;
                    rightLeg.zRot = 0.18f; leftLeg.zRot = -0.18f;
                    rightLeg.xRot = leftLeg.xRot = 0f;
                    head.xRot = -0.45f + shake;
                }
            }
            case CHARGE -> {
                if (firstPerson) {
                    rightArm.xRot = -1.38f + shake; rightArm.yRot = -0.35f; rightArm.zRot = 0f;
                    leftArm.xRot = -1.38f - shake; leftArm.yRot = 0.35f; leftArm.zRot = 0f;
                } else {
                    // Fists clenched at the sides, slight crouch
                    rightArm.xRot = -0.25f + shake; rightArm.yRot = 0.1f; rightArm.zRot = 0.45f;
                    leftArm.xRot = -0.25f - shake; leftArm.yRot = -0.1f; leftArm.zRot = -0.45f;
                    rightLeg.zRot = 0.12f; leftLeg.zRot = -0.12f;
                    rightLeg.xRot = leftLeg.xRot = 0f;
                }
            }
            case FLY -> {
                // Body is tilted by the renderer; streamline the limbs and keep looking forward
                float tilt = AnimState.flightTilt(player) * Mth.DEG_TO_RAD;
                head.xRot = pitch - tilt;
                rightArm.xRot = 0.25f; rightArm.yRot = 0f; rightArm.zRot = 0.12f;
                leftArm.xRot = 0.25f; leftArm.yRot = 0f; leftArm.zRot = -0.12f;
                rightLeg.xRot = 0.08f + Mth.sin(ageInTicks * 0.2f) * 0.04f;
                leftLeg.xRot = 0.02f - Mth.sin(ageInTicks * 0.2f) * 0.04f;
                rightLeg.zRot = leftLeg.zRot = 0f;
            }
            case HOVER -> {
                rightLeg.xRot = 0.15f + Mth.sin(ageInTicks * 0.08f) * 0.12f;
                leftLeg.xRot = -0.05f - Mth.sin(ageInTicks * 0.08f) * 0.12f;
                rightArm.zRot += 0.15f;
                leftArm.zRot -= 0.15f;
            }
            case NONE -> {}
        }

        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    /** Thicker arms and torso for muscled forms (copyFrom keeps the overlay parts in sync). */
    private void applyBulk(float bulk) {
        for (ModelPart part : new ModelPart[]{body, rightArm, leftArm}) {
            part.xScale = bulk;
            part.zScale = bulk;
        }
        rightArm.yScale = leftArm.yScale = body.yScale = 1f;
    }

    /** Where the fists are (model space, blocks): midpoint between both hands after posing. */
    public Vec3 handsMidpoint() {
        Vec3 r = handPoint(rightArm), l = handPoint(leftArm);
        return r.add(l).scale(0.5);
    }

    private static Vec3 handPoint(ModelPart arm) {
        PoseStack ps = new PoseStack();
        arm.translateAndRotate(ps);
        Vector4f v = new Vector4f(0f, 10f / 16f, 0f, 1f).mul(ps.last().pose());
        return new Vec3(v.x, v.y, v.z);
    }
}
