package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.gui.DBCCharacterScreen;
import com.dragonblock.client.keybind.ModKeyBindings;
import com.dragonblock.client.overlay.TechniqueBar;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SActionPacket;
import com.dragonblock.common.network.packet.C2SFlyPacket;
import com.dragonblock.common.network.packet.C2SInputStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    private static int lastInputFlags = 0;
    private static boolean promptedRace = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (ModKeyBindings.CHARACTER_SHEET_KEY.consumeClick()) mc.setScreen(new DBCCharacterScreen());
        if (ModKeyBindings.FLY_KEY.consumeClick()) ModMessages.sendToServer(new C2SFlyPacket());
        if (ModKeyBindings.REVERT_KEY.consumeClick()) ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.REVERT, 0));
        if (ModKeyBindings.TOGGLE_AURA_KEY.consumeClick()) {
            String label = switch (ClientConfig.cycleAura()) {
                case ALL -> "Auras: todas visibles";
                case HIDE_MINE -> "Auras: la tuya oculta";
                case HIDE_ALL -> "Auras: todas ocultas";
            };
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(label), true);
        }
        if (ModKeyBindings.ACCESSORIES_KEY.consumeClick()) ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.OPEN_ACCESSORIES, 0));
        if (ModKeyBindings.DASH_KEY.consumeClick()) {
            int targetId = com.dragonblock.client.combat.LockOnManager.isLocked() && com.dragonblock.client.combat.LockOnManager.getTarget() != null
                    ? com.dragonblock.client.combat.LockOnManager.getTarget().getId() : 0;
            var target = com.dragonblock.client.combat.LockOnManager.getTarget();
            boolean targetAirborne = target != null && !target.onGround() && mc.player.distanceTo(target) > 3.0;

            if (ModKeyBindings.GUARD_KEY.isDown()) {
                // Evasive Vanish (Z-Vanish)
                ModMessages.sendToServer(new com.dragonblock.common.network.packet.C2SCombatActionPacket(
                        com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.EVASIVE_VANISH, targetId));
            } else if (targetAirborne) {
                // Boost Dash Chase toward airborne/launched opponent
                ModMessages.sendToServer(new com.dragonblock.common.network.packet.C2SCombatActionPacket(
                        com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.BOOST_CHASE, targetId));
            } else {
                // Directional combat dodge
                ModMessages.sendToServer(new com.dragonblock.common.network.packet.C2SCombatActionPacket(
                        com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.DODGE, targetId));
            }
        }
        if (ModKeyBindings.HEAVY_KEY.consumeClick()) combat(com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.HEAVY_ATTACK);
        if (ModKeyBindings.SMASH_KEY.consumeClick()) combat(com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.METEOR_SMASH);
        if (ModKeyBindings.CANCEL_TRAINING_KEY.consumeClick()) combat(com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.CANCEL_TRAINING);
        if (ModKeyBindings.CYCLE_KEY.consumeClick()) com.dragonblock.client.combat.LockOnManager.cycleTarget(mc.player, !Screen.hasShiftDown());
        if (ModKeyBindings.LOCK_ON_KEY.consumeClick()) com.dragonblock.client.combat.LockOnManager.toggle(mc.player);
    }

    private static void combat(com.dragonblock.common.network.packet.C2SCombatActionPacket.Action action) {
        var target = com.dragonblock.client.combat.LockOnManager.getTarget();
        ModMessages.sendToServer(new com.dragonblock.common.network.packet.C2SCombatActionPacket(action,
            target == null ? 0 : target.getId()));
    }

    @SubscribeEvent
    public static void onCombatClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !event.isAttack()
                || !com.dragonblock.client.combat.LockOnManager.isLocked()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        combat(com.dragonblock.common.network.packet.C2SCombatActionPacket.Action.LIGHT_ATTACK);
    }

    /** Ctrl + mouse wheel scrolls the technique bar instead of the hotbar. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !Screen.hasControlDown() || event.getDeltaY() == 0) return;
        PlayerDBCData data = PlayerDBCData.get(mc.player);
        if (data.getEquipped().isEmpty()) return;
        event.setCanceled(true);
        int slot = data.getSelectedSlot() - (int) Math.signum(event.getDeltaY());
        data.setSelectedSlot(slot); // instant feedback, the server confirms on sync
        TechniqueBar.onScrolled();
        ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.SELECT_TECHNIQUE, data.getSelectedSlot()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastInputFlags = 0;
            promptedRace = false;
            return;
        }
        PlayerDBCData local = PlayerDBCData.get(mc.player);

        // First join: open the sheet so the player creates their character (wait for the server sync)
        if (!promptedRace && mc.screen == null && mc.player.tickCount > 40) {
            promptedRace = true;
            if (!local.isRaceChosen()) mc.setScreen(new DBCCharacterScreen());
        }

        int flags = 0;
        if (mc.screen == null) {
            if (ModKeyBindings.CHARGE_KI_KEY.isDown()) flags |= C2SInputStatePacket.CHARGE;
            // Already transformed: the transform key does nothing until you revert
            if (ModKeyBindings.TRANSFORM_KEY.isDown() && local.getTransformation() == Transformation.BASE) {
                flags |= C2SInputStatePacket.TRANSFORM;
            }
            if (ModKeyBindings.GUARD_KEY.isDown()) flags |= C2SInputStatePacket.GUARD;
            if (ModKeyBindings.TECHNIQUE_KEY.isDown()) flags |= C2SInputStatePacket.TECH;
        }
        if (mc.screen != null && (lastInputFlags & C2SInputStatePacket.TECH) != 0) flags |= C2SInputStatePacket.SUPPRESS;
        if ((flags & ~C2SInputStatePacket.SUPPRESS) != (lastInputFlags & ~C2SInputStatePacket.SUPPRESS)) {
            // Predict locally so the stance, aura and HUD react the same frame; the server sync confirms
            local.setChargingKi((flags & C2SInputStatePacket.CHARGE) != 0);
            local.setTransformHeld((flags & C2SInputStatePacket.TRANSFORM) != 0);
            local.setGuarding((flags & C2SInputStatePacket.GUARD) != 0);
            local.setTechHeld((flags & C2SInputStatePacket.TECH) != 0);
            if (!local.isTransformHeld()) local.setTransformCharge(0f);
            if (!local.isTechHeld()) local.setTechCharge(0f);
            lastInputFlags = flags;
            ModMessages.sendToServer(new C2SInputStatePacket(flags));
        }
    }
}
