package com.dragonblock.client.anim;

import com.dragonblock.common.combat.MeleeMove;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.data.Technique.Stance;
import com.dragonblock.common.data.Transformation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Player model that layers the mod's poses on top of vanilla animation. Swapped into every
 * PlayerRenderer at startup, so it drives every player in view. The first-person arm renderer poses
 * this same model with {@link #firstPerson} set, so both cameras share one set of poses.
 *
 * Poses never snap: each player keeps a smoothed copy of the posed angles that eases toward the target,
 * and a weight that fades the whole pose in and out over vanilla's walking animation.
 */
public class DBPlayerModel extends PlayerModel<AbstractClientPlayer> {
    /** Set while ClientRenderEvents draws the local player's arms from the eyes. */
    public boolean firstPerson = false;

    private static final int PARTS = 6;
    /** key -> {smoothed angles (18), weight, last age}. Key separates first and third person. */
    private final Map<Integer, float[]> blend = new HashMap<>();

    public DBPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    private ModelPart[] posed() {
        return new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg};
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        PlayerDBCData data = PlayerDBCData.get(player);
        applyBulk(data.getTransformation().getBulk());
        if (data.getTransformation().getHairStyle() != Transformation.HairStyle.NONE || data.getHairStyle() != Transformation.HairStyle.NONE) {
            hat.visible = false; // the skin's outer head layer would poke through the hair
        }
        if (limbSwing == 0 && limbSwingAmount == 0 && ageInTicks == 0) return; // vanilla first-person hand render

        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        AnimState.Pose pose = AnimState.pose(player, partial);

        ModelPart[] parts = posed();
        float[] vanilla = snapshot(parts);
        if (pose != AnimState.Pose.NONE) applyPose(player, data, pose, ageInTicks, netHeadYaw, headPitch, partial);
        float[] target = snapshot(parts);

        // Ease the posed angles and the pose weight; one-shot releases move faster than stances
        if (blend.size() > 256) blend.clear(); // entities come and go; a reset only costs one frame of easing
        float[] state = blend.computeIfAbsent(player.getId() * 2 + (firstPerson ? 1 : 0), k -> {
            float[] s = new float[PARTS * 3 + 2];
            System.arraycopy(vanilla, 0, s, 0, PARTS * 3);
            s[PARTS * 3 + 1] = ageInTicks;
            return s;
        });
        float dt = Mth.clamp(ageInTicks - state[PARTS * 3 + 1], 0f, 5f);
        state[PARTS * 3 + 1] = ageInTicks;
        // Punches are a few ticks long: follow them almost directly or the strike gets smoothed away
        float speed = pose == AnimState.Pose.MELEE ? 2.2f : pose == AnimState.Pose.TECH_FIRE ? 0.9f : 0.4f;
        float alpha = 1f - (float) Math.exp(-dt * speed);
        float weight = state[PARTS * 3];
        if (pose != AnimState.Pose.NONE) {
            if (weight < 0.01f) System.arraycopy(vanilla, 0, state, 0, PARTS * 3); // start from where the limbs are
            for (int i = 0; i < PARTS * 3; i++) state[i] = Mth.lerp(alpha, state[i], target[i]);
            weight = 1f;
        } else {
            weight = Mth.lerp(1f - (float) Math.exp(-dt * 0.35f), weight, 0f);
        }
        state[PARTS * 3] = weight;
        for (int i = 0; i < PARTS * 3; i++) target[i] = Mth.lerp(weight, vanilla[i], state[i]);
        restore(parts, target);

        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    private static float[] snapshot(ModelPart[] parts) {
        float[] a = new float[PARTS * 3];
        for (int i = 0; i < PARTS; i++) {
            a[i * 3] = parts[i].xRot;
            a[i * 3 + 1] = parts[i].yRot;
            a[i * 3 + 2] = parts[i].zRot;
        }
        return a;
    }

    private static void restore(ModelPart[] parts, float[] a) {
        for (int i = 0; i < PARTS; i++) {
            parts[i].xRot = a[i * 3];
            parts[i].yRot = a[i * 3 + 1];
            parts[i].zRot = a[i * 3 + 2];
        }
    }

    private void arms(float rx, float ry, float rz, float lx, float ly, float lz) {
        rightArm.xRot = rx; rightArm.yRot = ry; rightArm.zRot = rz;
        leftArm.xRot = lx; leftArm.yRot = ly; leftArm.zRot = lz;
    }

    private void wideStance(float spread) {
        rightLeg.xRot = leftLeg.xRot = 0f;
        rightLeg.zRot = spread;
        leftLeg.zRot = -spread;
    }

    private void applyPose(AbstractClientPlayer player, PlayerDBCData data, AnimState.Pose pose, float age,
                           float netHeadYaw, float headPitch, float partial) {
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float shake = Mth.sin(age * 2.7f) * 0.03f;
        float breath = Mth.sin(age * 0.18f);

        switch (pose) {
            case GUARD -> {
                // Forearms crossed in front of the face
                if (firstPerson) arms(-1.9f, -0.95f, 0f, -1.72f, 0.95f, 0f);
                else {
                    arms(-1.45f + pitch * 0.5f, -0.75f, 0f, -1.30f + pitch * 0.5f, 0.85f, 0f);
                    wideStance(0.1f);
                }
            }
            case MELEE -> melee(AnimState.meleeMove(player), AnimState.meleeProgress(player, partial), pitch, yaw);
            case TECH_CHARGE -> {
                Technique tech = data.getSelectedTechnique();
                float c = data.getTechCharge();
                if (tech != null) charge(tech.getStance(), c, shake * (1f + 2f * c), pitch, yaw, breath);
            }
            case TECH_FIRE -> {
                Technique tech = AnimState.firedTechnique(player);
                release(tech.getStance(), AnimState.fireProgress(player, partial), pitch, yaw);
            }
            case POWER_UP -> {
                float c = data.getTransformCharge();
                if (firstPerson) {
                    // Clenched fists trembling low in front of the chest
                    arms(-1.28f + shake * 2, -0.2f, 0.25f, -1.28f - shake * 2, 0.2f, -0.25f);
                } else {
                    // Arms flexed down and out, legs planted wide, head thrown back as the scream builds
                    float flex = 0.6f + 0.25f * c;
                    arms(0.15f + shake, 0f, flex + shake, 0.15f - shake, 0f, -flex - shake);
                    wideStance(0.18f + 0.06f * c);
                    head.xRot = -0.2f - 0.35f * c + shake;
                    body.xRot = -0.05f * c;
                }
            }
            case CHARGE -> {
                if (firstPerson) {
                    arms(-1.38f + shake, -0.35f, 0f, -1.38f - shake, 0.35f, 0f);
                } else {
                    // Fists clenched at the sides, slight crouch, breathing hard
                    arms(-0.25f + shake, 0.1f, 0.45f + breath * 0.04f, -0.25f - shake, -0.1f, -0.45f - breath * 0.04f);
                    wideStance(0.12f);
                    body.xRot = 0.08f + breath * 0.02f;
                    head.xRot = Mth.clamp(pitch, -0.4f, 0.4f) - 0.08f;
                }
            }
            case FLY -> {
                // Body is tilted by the renderer; streamline the limbs and keep looking forward
                float tilt = AnimState.flightTilt(player) * Mth.DEG_TO_RAD;
                float trail = Mth.clamp(tilt / 1.4f, 0f, 1f);
                head.xRot = pitch - tilt;
                arms(0.25f + 0.25f * trail, 0f, 0.12f, 0.25f + 0.25f * trail, 0f, -0.12f);
                rightLeg.xRot = 0.08f + Mth.sin(age * 0.2f) * 0.04f;
                leftLeg.xRot = 0.02f - Mth.sin(age * 0.2f) * 0.04f;
                rightLeg.zRot = leftLeg.zRot = 0f;
            }
            case HOVER -> {
                // Floating: legs drift, one knee-less leg a little forward, arms loose
                rightLeg.xRot = 0.15f + Mth.sin(age * 0.08f) * 0.12f;
                leftLeg.xRot = -0.05f - Mth.sin(age * 0.08f) * 0.12f;
                rightLeg.zRot = 0.04f;
                leftLeg.zRot = -0.04f;
                rightArm.zRot += 0.15f + breath * 0.03f;
                leftArm.zRot -= 0.15f + breath * 0.03f;
            }
            case NONE -> {}
        }
    }

    /** Charging stance of each technique; the energy is drawn where these put the hands. */
    private void charge(Stance stance, float c, float shake, float pitch, float yaw, float breath) {
        if (firstPerson) {
            switch (stance) {
                case ONE_PALM -> arms(-1.62f + shake, -0.3f, 0f, -1.5f - shake, 0.5f, 0f);
                case CUPPED_HIP -> arms(-1.45f + shake, -0.6f, 0f, -1.45f - shake, 0.6f, 0f);
                case GALICK -> arms(-1.5f + shake, 0.5f, 0f, -1.5f - shake, 0.95f, 0f);
                case OVERHEAD -> arms(-2.4f + shake, -0.5f, 0f, -2.4f - shake, 0.5f, 0f);
                case WIDE_ARMS -> arms(-1.45f + shake, 0.95f - 0.2f * c, 0f, -1.45f - shake, -0.95f + 0.2f * c, 0f);
                case FOREHEAD, FACE -> arms(-2.2f + shake, -0.5f, 0f, -2.2f - shake, 0.5f, 0f);
                case FINGER -> arms(-1.62f, -0.25f, 0f, 0f, 0f, 0f);
                case RAISED_ONE -> arms(-2.6f + shake, -0.2f, 0f, 0f, 0f, 0f);
                case RAISED_BOTH -> arms(-2.7f + shake, -0.3f, 0f, -2.7f - shake, 0.3f, 0f);
                case TENSE -> arms(-1.28f + shake * 2, -0.2f, 0.25f, -1.28f - shake * 2, 0.2f, -0.25f);
            }
            return;
        }
        switch (stance) {
            case ONE_PALM -> {
                // Palm out at arm's length, the other hand bracing the wrist
                arms(-Mth.HALF_PI + pitch + shake, yaw - 0.1f, 0f, -1.3f + pitch - shake, yaw + 0.55f, 0f);
                body.yRot = 0.2f;
                wideStance(0.14f);
            }
            case CUPPED_HIP -> {
                // Cupped hands drawn back to the right hip, torso twisted, weight on the back leg
                body.yRot = -0.45f - 0.1f * c;
                arms(0.35f + shake, -0.35f, 0.25f, -0.35f + shake, 1.0f, 0.1f);
                rightLeg.xRot = 0.3f; leftLeg.xRot = -0.3f;
                rightLeg.zRot = 0.08f; leftLeg.zRot = -0.08f;
            }
            case GALICK -> {
                // Shoulder toward the target, both palms held out to the side
                body.yRot = 0.55f;
                arms(-Mth.HALF_PI + shake, 1.3f, 0f, -Mth.HALF_PI - shake, 1.0f, 0f);
                wideStance(0.16f);
            }
            case OVERHEAD -> {
                // Both hands over the forehead, palms together
                arms(-2.9f + shake, -0.35f, 0f, -2.9f - shake, 0.35f, 0f);
                head.xRot = -0.15f;
                wideStance(0.1f);
            }
            case WIDE_ARMS -> {
                // Arms flung wide to the sides, chest open, gathering slowly closes the arms
                float open = 1.5f - 0.15f * c;
                arms(-0.2f + shake, 0f, open, -0.2f - shake, 0f, -open);
                body.xRot = -0.08f;
                head.xRot = -0.2f;
                wideStance(0.2f);
            }
            case FOREHEAD -> {
                // Two fingers on the forehead, the other arm loose
                arms(-2.47f + shake, -0.91f, 0f, -0.2f, 0.1f, -0.1f);
                head.xRot = 0.15f;
            }
            case FINGER -> arms(-Mth.HALF_PI + pitch, yaw, 0f, 0f, 0f, -0.08f);
            case RAISED_ONE -> {
                // One arm straight up, a disc spinning over the palm
                arms(-3.0f + shake, 0f, 0.15f, 0.1f, 0f, -0.15f);
                head.xRot = -0.25f;
            }
            case RAISED_BOTH -> {
                // Both arms reaching for the sky, eyes on the growing sphere
                arms(-3.0f + shake, 0f, 0.28f, -3.0f - shake, 0f, -0.28f);
                head.xRot = -0.55f;
                body.xRot = -0.06f + breath * 0.02f;
                wideStance(0.14f);
            }
            case TENSE -> {
                // Fists clenched back, chest out, building up to the shout
                arms(0.4f + shake, 0f, 0.35f + shake, 0.4f - shake, 0f, -0.35f - shake);
                head.xRot = 0.25f;
                body.xRot = 0.1f;
                wideStance(0.2f);
            }
            case FACE -> {
                // Fingers spread at the temples
                arms(-2.3f + shake, -0.65f, 0f, -2.3f - shake, 0.65f, 0f);
                wideStance(0.1f);
            }
        }
    }

    /** Release pose, t = 0..1 over the technique's sustain. */
    private void release(Stance stance, float t, float pitch, float yaw) {
        float snap = Mth.clamp(t * 4f, 0f, 1f); // the strike lands in the first quarter
        float recoil = t > 0.6f ? (t - 0.6f) / 0.4f * 0.3f : 0f;
        switch (stance) {
            case FINGER, ONE_PALM -> {
                float reach = (firstPerson ? -1.62f : -Mth.HALF_PI) + pitch;
                rightArm.xRot = Mth.lerp(snap, rightArm.xRot, reach + recoil);
                rightArm.yRot = Mth.lerp(snap, rightArm.yRot, firstPerson ? -0.3f : yaw - 0.1f);
                rightArm.zRot = 0f;
                if (!firstPerson) body.yRot = 0.25f * snap;
            }
            case FOREHEAD -> {
                // Two fingers thrust at the target, the free hand gripping the wrist
                float reach = (firstPerson ? -1.62f : -Mth.HALF_PI) + pitch;
                arms(reach, firstPerson ? -0.25f : yaw - 0.05f, 0f, reach + 0.25f, firstPerson ? 0.45f : yaw + 0.55f, 0f);
                if (!firstPerson) body.yRot = 0.2f;
            }
            case RAISED_ONE -> {
                // Overarm throw
                arms(Mth.lerp(snap, -3.0f, -0.9f), 0f, 0.1f, 0.2f, 0f, -0.15f);
                if (!firstPerson) body.xRot = 0.15f * snap;
            }
            case RAISED_BOTH -> {
                arms(Mth.lerp(snap, -3.0f, -1.7f), -0.2f, 0.1f, Mth.lerp(snap, -3.0f, -1.7f), 0.2f, -0.1f);
                if (!firstPerson) {
                    body.xRot = 0.2f * snap;
                    head.xRot = pitch;
                }
            }
            case TENSE -> {
                // Arms thrown open with the shout
                float open = 1.3f * snap;
                if (firstPerson) arms(-1.4f, 0.4f + open * 0.6f, 0f, -1.4f, -0.4f - open * 0.6f, 0f);
                else {
                    arms(-0.3f, 0f, open, -0.3f, 0f, -open);
                    head.xRot = -0.35f;
                    body.xRot = -0.12f;
                    wideStance(0.25f);
                }
            }
            case FACE -> arms(-2.3f, -0.85f, 0.2f, -2.3f, 0.85f, -0.2f);
            default -> {
                // Two-handed beams: both palms pushed at the target
                float reach = (firstPerson ? -1.62f : -Mth.HALF_PI) + pitch + (stance == Stance.OVERHEAD ? -0.15f : 0f);
                float spread = firstPerson ? 0.35f : 0.2f;
                arms(reach + recoil, (firstPerson ? 0f : yaw) - spread, 0f, reach + recoil, (firstPerson ? 0f : yaw) + spread, 0f);
                if (!firstPerson) {
                    body.yRot = 0f;
                    rightLeg.xRot = 0.35f; leftLeg.xRot = -0.35f;
                    rightLeg.zRot = leftLeg.zRot = 0f;
                }
            }
        }
    }

    // Keyframe state of the move being posed: phase 0 wind-up, 1 strike, 2 recovery, and progress in it
    private int keyPhase;
    private float keyT;

    /** Value between the three keyframes of a hand-to-hand move: at rest, wound up, at the moment of impact. */
    private float key(float rest, float wound, float hit) {
        float s = keyT * keyT * (3f - 2f * keyT);
        return switch (keyPhase) {
            case 0 -> Mth.lerp(s, rest, wound);
            case 1 -> Mth.lerp(s, wound, hit);
            default -> Mth.lerp(s, hit, rest);
        };
    }

    /** Hand-to-hand move, t = 0..1 over its ticks: the strike lands exactly in the server's active window. */
    private void melee(MeleeMove m, float t, float pitch, float yaw) {
        float total = m.total(), a = m.startup / total, b = (m.startup + m.active) / total;
        keyPhase = t < a ? 0 : t < b ? 1 : 2;
        keyT = Mth.clamp(keyPhase == 0 ? t / a : keyPhase == 1 ? (t - a) / (b - a) : (t - b) / (1f - b), 0f, 1f);
        float guard = firstPerson ? -1.3f : -1.1f;
        float reach = firstPerson ? -1.62f : -Mth.HALF_PI + pitch;
        float aimR = firstPerson ? -0.1f : yaw - 0.05f, aimL = firstPerson ? 0.1f : yaw + 0.05f;
        // Fists up by default; each move overrides the limbs it uses
        arms(guard, -0.35f, 0f, guard, 0.35f, 0f);
        if (!firstPerson) wideStance(0.1f);
        switch (m) {
            case JAB -> {
                rightArm.xRot = key(guard, guard + 0.35f, reach);
                rightArm.yRot = key(-0.35f, -0.45f, aimR);
                if (!firstPerson) body.yRot = key(0f, -0.15f, 0.3f);
            }
            case CROSS -> {
                leftArm.xRot = key(guard, guard + 0.35f, reach);
                leftArm.yRot = key(0.35f, 0.45f, aimL);
                if (!firstPerson) body.yRot = key(0f, 0.15f, -0.35f);
            }
            case HAMMER -> {
                // Drawn far back, the whole body turns into the blow
                rightArm.xRot = key(guard, guard + 1.0f, reach - 0.1f);
                rightArm.yRot = key(-0.35f, 0.4f, aimR);
                if (!firstPerson) {
                    body.yRot = key(0f, -0.6f, 0.55f);
                    rightLeg.xRot = key(0f, 0.3f, 0.45f);
                    leftLeg.xRot = key(0f, -0.2f, -0.5f);
                }
            }
            case KICK -> {
                if (!firstPerson) {
                    rightLeg.zRot = 0f;
                    rightLeg.xRot = key(0f, 0.45f, -1.55f);
                    leftLeg.xRot = key(0f, 0f, 0.15f);
                    body.xRot = key(0f, 0.05f, -0.2f);
                }
            }
            case LAUNCHER -> {
                // Crouch with the fist low, then drive it straight up
                rightArm.xRot = key(guard, 0.3f, -2.8f);
                rightArm.yRot = key(-0.35f, -0.2f, -0.15f);
                if (!firstPerson) {
                    body.xRot = key(0f, 0.25f, -0.25f);
                    rightLeg.xRot = key(0f, 0.35f, -0.1f);
                    leftLeg.xRot = key(0f, -0.35f, 0.1f);
                }
            }
        }
    }

    /** First person shows only the right arm for these (the other one would float in from nowhere). */
    public static boolean oneArmed(Stance stance, boolean firing) {
        return switch (stance) {
            case FINGER, RAISED_ONE -> true;
            case ONE_PALM -> firing;
            default -> false;
        };
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
        return handPoint(rightArm).add(handPoint(leftArm)).scale(0.5);
    }

    public Vec3 rightHand() {
        return handPoint(rightArm);
    }

    public Vec3 leftHand() {
        return handPoint(leftArm);
    }

    private static Vec3 handPoint(ModelPart arm) {
        PoseStack ps = new PoseStack();
        arm.translateAndRotate(ps);
        Vector4f v = new Vector4f(0f, 10f / 16f, 0f, 1f).mul(ps.last().pose());
        return new Vec3(v.x, v.y, v.z);
    }
}
