package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.anim.DBPlayerModel;
import com.dragonblock.client.fx.TechniqueFx;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.event.DragonBlockEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientRenderEvents {
    /** Players whose pose we pushed in Pre and must pop in Post. */
    private static final java.util.Set<Integer> TILTED = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        AnimState.clear();
        TILTED.clear();
        PlayerDBCData.clearClient();
    }

    // ------------------------------------------------------------------ flight tilt (third person)

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.isCanceled()) return;
        Player player = event.getEntity();
        if (AnimState.pose(player, event.getPartialTick()) != AnimState.Pose.FLY) return;

        float tilt = AnimState.flightTilt(player);
        float yaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        float pivot = player.getBbHeight() * 0.5f;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Lean forward around the body's center, along the direction the body faces
        pose.translate(0, pivot, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-tilt));
        pose.mulPose(Axis.YP.rotationDegrees(-(180f - yaw)));
        pose.translate(0, -pivot, 0);
        TILTED.add(player.getId());
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (TILTED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    // ------------------------------------------------------------------ FOV

    /** Our DEX speed bonus would widen the FOV like a speed potion; divide it back out. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        PlayerDBCData data = PlayerDBCData.get(player);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double walk = player.getAbilities().getWalkingSpeed();
        if (walk <= 0) return;

        double withOurs = speed.getValue();
        double factor = 1;
        for (String id : new String[]{"dex_speed_bonus", "guard_slow"}) {
            var mod = speed.getModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, id));
            if (mod != null) factor *= 1 + mod.amount();
        }
        if (factor <= 0.01) return;
        double without = withOurs / factor;
        float correction = (float) (((without / walk) + 1) / ((withOurs / walk) + 1));
        event.setNewFovModifier(event.getNewFovModifier() * correction);
    }

    // ------------------------------------------------------------------ first-person arms

    /**
     * First-person shoulder pivots (model px, eyes at y=-3.6). A Minecraft arm has no elbow, so from the true
     * shoulder it never enters the view; like the vanilla hand, the shoulders sit in front of the camera.
     * Values tuned with tools/preview_poses.py.
     */
    private static final float FP_SHOULDER_X = 6.0f, FP_SHOULDER_Y = 0.8f, FP_SHOULDER_Z = -4.0f;
    /** Eye height in model space: the eyes sit 3.6 px above the neck pivot. */
    private static final float EYE_Y = -3.6f / 16f;
    /** PlayerRenderer draws players at 15/16 scale. */
    private static final float PLAYER_SCALE = 0.9375f;

    /**
     * Forge has no hook to change first-person arm animation, so while a pose is active we cancel the
     * vanilla hands and draw the arms of our own posed model, placed relative to the eyes and tilted with
     * the camera pitch. The arms in first person are therefore exactly the ones other players see.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || mc.getCameraEntity() != player) return; // spectating someone: not our arms
        float partial = event.getPartialTick();
        AnimState.Pose pose = AnimState.pose(player, partial);
        if (pose == AnimState.Pose.NONE || pose == AnimState.Pose.FLY || pose == AnimState.Pose.HOVER) return;

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return; // both arms are drawn once, from the main-hand event
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)
                || !(renderer.getModel() instanceof DBPlayerModel model)) return;

        PlayerDBCData data = PlayerDBCData.get(player);
        float pitch = event.getInterpolatedPitch();
        float age = player.tickCount + partial;

        model.attackTime = 0f;
        model.crouching = false;
        model.swimAmount = 0f;
        model.firstPerson = true;
        // Arms follow the view in first person, so no head pitch here
        model.setupAnim(player, player.walkAnimation.position(partial), player.walkAnimation.speed(partial), age, 0f, 0f);
        model.firstPerson = false;

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        // Camera space -> model space: flip like the entity renderer does, scale like PlayerRenderer,
        // and put the eyes at the camera origin.
        ps.scale(-PLAYER_SCALE, -PLAYER_SCALE, PLAYER_SCALE);
        ps.translate(0f, -EYE_Y, 0f);

        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        var skin = buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float rx = model.rightArm.x, ry = model.rightArm.y, rz = model.rightArm.z;
        float lx = model.leftArm.x, ly = model.leftArm.y, lz = model.leftArm.z;
        model.rightArm.setPos(-FP_SHOULDER_X, FP_SHOULDER_Y, FP_SHOULDER_Z);
        model.leftArm.setPos(FP_SHOULDER_X, FP_SHOULDER_Y, FP_SHOULDER_Z);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightArm.render(ps, skin, light, overlay);
        model.rightSleeve.render(ps, skin, light, overlay);
        Technique tech = pose == AnimState.Pose.TECH_FIRE ? AnimState.firedTechnique(player)
                : pose == AnimState.Pose.TECH_CHARGE ? data.getSelectedTechnique() : null;
        if (tech == null || !DBPlayerModel.oneArmed(tech.getStance(), pose == AnimState.Pose.TECH_FIRE)) {
            model.leftArm.render(ps, skin, light, overlay); // single-arm moves keep the idle arm out of view
            model.leftSleeve.render(ps, skin, light, overlay);
        }

        if (pose == AnimState.Pose.TECH_CHARGE) TechniqueFx.renderCharge(ps, buffers, model, data, age, true);
        ps.popPose();
        model.rightArm.setPos(rx, ry, rz);
        model.leftArm.setPos(lx, ly, lz);
    }
}
