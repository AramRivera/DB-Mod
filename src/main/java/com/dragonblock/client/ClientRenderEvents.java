package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.anim.AnimState;
import com.dragonblock.client.renderer.EnergyRender;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.event.DragonBlockEvents;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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
    }

    // ------------------------------------------------------------------ flight tilt (third person)

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
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
        double factor = 1 + DragonBlockEvents.dexSpeedBonus(data);
        if (data.isGuarding()) factor *= 0.5;
        double without = withOurs / factor;
        float correction = (float) (((without / walk) + 1) / ((withOurs / walk) + 1));
        event.setNewFovModifier(event.getNewFovModifier() * correction);
    }

    // ------------------------------------------------------------------ first-person arms

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) return;
        AnimState.Pose pose = AnimState.pose(player, event.getPartialTick());
        if (pose == AnimState.Pose.NONE || pose == AnimState.Pose.FLY || pose == AnimState.Pose.HOVER) return;

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return; // both arms are drawn once, from the main-hand event

        PlayerDBCData data = PlayerDBCData.get(player);
        float partial = event.getPartialTick();
        float time = player.tickCount + partial;
        float shake = Mth.sin(time * 2.7f) * 0.01f;
        PoseStack ps = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);

        // Offsets are added to vanilla's empty-hand position: x toward the center, y up, z away from the camera
        switch (pose) {
            case BLAST -> {
                float p = AnimState.progress(player, S2CAnimPacket.Anim.BLAST, AnimState.BLAST_TICKS, partial);
                float thrust = p < 0.3f ? p / 0.3f : 1f - (p - 0.3f) / 0.7f;
                arm(ps, buffers, light, player, renderer, true, -0.28f * thrust, 0.22f * thrust, -0.25f * thrust, -25f * thrust);
            }
            case WAVE_RELEASE -> {
                arm(ps, buffers, light, player, renderer, true, -0.32f, 0.2f, -0.3f, -20f);
                arm(ps, buffers, light, player, renderer, false, -0.32f, 0.2f, -0.3f, -20f);
            }
            case WAVE_CHARGE -> {
                float c = data.getWaveCharge();
                arm(ps, buffers, light, player, renderer, true, -0.42f + shake, 0.02f, 0.05f, -35f);
                arm(ps, buffers, light, player, renderer, false, -0.42f - shake, 0.02f, 0.05f, -35f);
                ps.pushPose();
                ps.translate(0, -0.38f, -0.85f);
                EnergyRender.sphere(ps, buffers, 0.05f + 0.13f * c, data.getTransformation().getAuraColor(), time);
                ps.popPose();
            }
            case GUARD -> {
                arm(ps, buffers, light, player, renderer, true, -0.38f, 0.3f, 0.05f, -60f);
                arm(ps, buffers, light, player, renderer, false, -0.38f, 0.22f, 0.1f, -60f);
            }
            case POWER_UP, CHARGE -> {
                float s = pose == AnimState.Pose.POWER_UP ? shake * 3f : shake;
                arm(ps, buffers, light, player, renderer, true, 0.05f + s, -0.12f, 0.1f, 15f);
                arm(ps, buffers, light, player, renderer, false, 0.05f - s, -0.12f, 0.1f, 15f);
            }
            default -> {}
        }
    }

    /** Vanilla's first-person empty-arm transform with an extra offset/roll applied first. */
    private static void arm(PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buffers, int light,
                            AbstractClientPlayer player, PlayerRenderer renderer, boolean right,
                            float inward, float up, float back, float roll) {
        float side = right ? 1f : -1f;
        ps.pushPose();
        ps.translate(side * inward, up, back);
        ps.translate(side * 0.64f, -0.6f, -0.72f);
        ps.mulPose(Axis.YP.rotationDegrees(side * 45f));
        ps.mulPose(Axis.ZP.rotationDegrees(side * roll));
        ps.translate(side * -1.0f, 3.6f, 3.5f);
        ps.mulPose(Axis.ZP.rotationDegrees(side * 120f));
        ps.mulPose(Axis.XP.rotationDegrees(200f));
        ps.mulPose(Axis.YP.rotationDegrees(side * -135f));
        ps.translate(side * 5.6f, 0f, 0f);
        if (right) renderer.renderRightHand(ps, buffers, light, player);
        else renderer.renderLeftHand(ps, buffers, light, player);
        ps.popPose();
    }
}
