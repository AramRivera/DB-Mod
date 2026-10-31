package com.dragonblock.client.anim;

import com.dragonblock.common.data.PlayerDBCData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Player model that layers the mod's poses on top of vanilla animation. Swapped into every
 * PlayerRenderer at startup, so it drives every player in view.
 */
public class DBPlayerModel extends PlayerModel<AbstractClientPlayer> {
    public DBPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (limbSwing == 0 && limbSwingAmount == 0 && ageInTicks == 0) return; // first-person hand render

        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        PlayerDBCData data = PlayerDBCData.get(player);
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float shake = Mth.sin(ageInTicks * 2.7f) * 0.03f;

        // Hair models replace the skin's hat layer while a haired form is active
        if (data.getTransformation().getHairStyle() != com.dragonblock.common.data.Transformation.HairStyle.NONE) {
            hat.visible = false;
        }

        switch (AnimState.pose(player, partial)) {
            case GUARD -> {
                // Forearms crossed in front of the face
                rightArm.xRot = -1.45f; rightArm.yRot = -0.75f; rightArm.zRot = 0f;
                leftArm.xRot = -1.30f; leftArm.yRot = 0.85f; leftArm.zRot = 0f;
            }
            case BLAST -> {
                float p = AnimState.progress(player, com.dragonblock.common.network.packet.S2CAnimPacket.Anim.BLAST,
                        AnimState.BLAST_TICKS, partial);
                float thrust = p < 0.3f ? p / 0.3f : 1f - (p - 0.3f) / 0.7f * 0.4f;
                rightArm.xRot = Mth.lerp(thrust, rightArm.xRot, -Mth.HALF_PI + pitch);
                rightArm.yRot = Mth.lerp(thrust, rightArm.yRot, yaw - 0.1f);
                rightArm.zRot = 0f;
                body.yRot = 0.25f * thrust;
            }
            case WAVE_RELEASE -> {
                // Both palms pushed forward
                rightArm.xRot = -Mth.HALF_PI + pitch; rightArm.yRot = yaw - 0.18f; rightArm.zRot = 0f;
                leftArm.xRot = -Mth.HALF_PI + pitch; leftArm.yRot = yaw + 0.18f; leftArm.zRot = 0f;
                rightLeg.xRot = 0.35f; leftLeg.xRot = -0.35f;
            }
            case WAVE_CHARGE -> {
                // Cupped hands drawn back to the right hip, torso twisted
                body.yRot = -0.45f;
                rightArm.xRot = 0.35f + shake; rightArm.yRot = -0.35f; rightArm.zRot = 0.25f;
                leftArm.xRot = -0.35f + shake; leftArm.yRot = 1.0f; leftArm.zRot = 0.1f;
                rightLeg.xRot = 0.3f; leftLeg.xRot = -0.3f;
                rightLeg.zRot = 0.08f; leftLeg.zRot = -0.08f;
            }
            case POWER_UP -> {
                // Arms flexed down and out, legs planted wide, head thrown back
                rightArm.xRot = 0.15f + shake; rightArm.yRot = 0f; rightArm.zRot = 0.75f + shake;
                leftArm.xRot = 0.15f - shake; leftArm.yRot = 0f; leftArm.zRot = -0.75f - shake;
                rightLeg.zRot = 0.18f; leftLeg.zRot = -0.18f;
                rightLeg.xRot = leftLeg.xRot = 0f;
                head.xRot = -0.45f + shake;
            }
            case CHARGE -> {
                // Fists clenched at the sides, slight crouch
                rightArm.xRot = -0.25f + shake; rightArm.yRot = 0.1f; rightArm.zRot = 0.45f;
                leftArm.xRot = -0.25f - shake; leftArm.yRot = -0.1f; leftArm.zRot = -0.45f;
                rightLeg.zRot = 0.12f; leftLeg.zRot = -0.12f;
                rightLeg.xRot = leftLeg.xRot = 0f;
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
                // Legs drift slowly, arms relaxed
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
}
