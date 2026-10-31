package com.dragonblock.client.fx;

import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.anim.DBPlayerModel;
import com.dragonblock.client.renderer.EnergyRender;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Energy gathering for a technique being charged, drawn in model space where the stance puts the hands,
 * plus the release effects that are not projectiles (the Kiai shockwave, the Taiyoken flash). Both the
 * third-person layer and the first-person arm renderer call this, so the two views always match.
 */
public final class TechniqueFx {
    private TechniqueFx() {}

    public static void renderCharge(PoseStack pose, MultiBufferSource buffers, DBPlayerModel model, PlayerDBCData data,
                                    float time, boolean firstPerson) {
        Technique tech = data.getSelectedTechnique();
        if (tech == null || tech.isInstant() || !data.isTechHeld()) return;
        float c = data.getTechCharge();
        int color = tech.colorFor(data.getTransformation());
        // First person draws smaller and a bit further out so the energy doesn't fill the screen
        float k = (firstPerson ? 0.7f : 1f) * Technique.powerScale(data);

        switch (tech.getStance()) {
            case ONE_PALM -> orb(pose, buffers, model.rightHand(), (0.06f + 0.22f * c) * k, color, time, firstPerson);
            case CUPPED_HIP, OVERHEAD -> {
                orb(pose, buffers, model.handsMidpoint(), (0.07f + 0.24f * c) * k, color, time, firstPerson);
                if (c > 0.6f) crackle(pose, buffers, model.handsMidpoint(), color, time, 1);
            }
            case GALICK -> {
                orb(pose, buffers, model.handsMidpoint(), (0.06f + 0.2f * c) * k, color, time, firstPerson);
                if (!firstPerson && ((int) time) % 5 < 3) EnergyRender.lightning(pose, buffers, (int) time / 2, color, 2, 0.8f);
            }
            case WIDE_ARMS -> {
                orb(pose, buffers, model.rightHand(), (0.05f + 0.15f * c) * k, color, time, firstPerson);
                orb(pose, buffers, model.leftHand(), (0.05f + 0.15f * c) * k, color, time, firstPerson);
                if (c > 0.4f) {
                    crackle(pose, buffers, model.rightHand(), color, time, 1);
                    crackle(pose, buffers, model.leftHand(), color, time + 3, 1);
                }
            }
            case FOREHEAD, FACE, FINGER -> {
                orb(pose, buffers, model.rightHand(), (0.03f + 0.07f * c) * k, color, time, firstPerson);
                if (c > 0.3f) crackle(pose, buffers, model.rightHand(), color, time, 1);
                if (tech.getStance() == Technique.Stance.FACE) {
                    orb(pose, buffers, model.leftHand(), (0.03f + 0.07f * c) * k, color, time, firstPerson);
                }
            }
            case RAISED_ONE -> {
                pose.pushPose();
                Vec3 hand = model.rightHand();
                pose.translate(hand.x, hand.y - 0.18f - 0.2f * c, hand.z);
                EnergyRender.disc(pose, buffers, (0.12f + 0.42f * c) * k, color, time);
                pose.popPose();
            }
            case RAISED_BOTH -> {
                // The sphere grows high above the head, fed from both raised hands
                Vec3 hands = model.handsMidpoint();
                float r = (0.2f + 1.9f * c) * Technique.powerScale(data);
                pose.pushPose();
                pose.translate(hands.x, hands.y - 0.5f - r, hands.z);
                EnergyRender.sphere(pose, buffers, r, color, time);
                pose.popPose();
            }
            case TENSE -> {}
        }
    }

    /** Release effects that live on the body: Kiai shockwave and Taiyoken flash (third person). */
    public static void renderRelease(PoseStack pose, MultiBufferSource buffers, Player player, PlayerDBCData data, float partial) {
        Technique tech = AnimState.firedTechnique(player);
        float t = AnimState.progress(player, S2CAnimPacket.Anim.TECHNIQUE, tech.getSustain() + 6, partial);
        if (t < 0) return;
        int color = tech.colorFor(data.getTransformation());
        switch (tech.getKind()) {
            case SHOCKWAVE -> EnergyRender.burst(pose, buffers, t, color, 1.3f);
            case FLASH -> {
                if (t > 0.6f) return;
                pose.pushPose();
                pose.translate(0, -0.25f, 0);
                EnergyRender.sphere(pose, buffers, 0.4f + 3.5f * t, EnergyRender.scaleColor(0xFFFFF0, 1f - t / 0.6f) & 0xFFFFFF, 0);
                pose.popPose();
            }
            default -> {}
        }
    }

    private static void orb(PoseStack pose, MultiBufferSource buffers, Vec3 at, float radius, int color, float time, boolean firstPerson) {
        pose.pushPose();
        pose.translate(at.x, at.y, at.z - (firstPerson ? 0.08f : 0f));
        EnergyRender.sphere(pose, buffers, radius, color, time);
        pose.popPose();
    }

    private static void crackle(PoseStack pose, MultiBufferSource buffers, Vec3 at, int color, float time, int bolts) {
        if (((int) time) % 4 >= 2) return;
        pose.pushPose();
        pose.translate(at.x, at.y - 1.2f * 0.3f, at.z);
        pose.scale(0.3f, 0.3f, 0.3f);
        EnergyRender.lightning(pose, buffers, (int) time * 7, color, bolts, 0.6f);
        pose.popPose();
    }
}
