package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.gui.DBCCharacterScreen;
import com.dragonblock.client.keybind.ModKeyBindings;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SActionPacket;
import com.dragonblock.common.network.packet.C2SFlyPacket;
import com.dragonblock.common.network.packet.C2SInputStatePacket;
import com.dragonblock.common.network.packet.C2SKiBlastPacket;
import net.minecraft.client.Minecraft;
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
        if (ModKeyBindings.KI_BLAST_KEY.consumeClick()) ModMessages.sendToServer(new C2SKiBlastPacket());
        if (ModKeyBindings.REVERT_KEY.consumeClick()) ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.REVERT, 0));
        if (ModKeyBindings.TOGGLE_AURA_KEY.consumeClick()) {
            String label = switch (ClientConfig.cycleAura()) {
                case ALL -> "Auras: todas visibles";
                case HIDE_MINE -> "Auras: la tuya oculta";
                case HIDE_ALL -> "Auras: todas ocultas";
            };
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(label), true);
        }
        if (ModKeyBindings.DASH_KEY.consumeClick()) ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.DASH, 0));
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

        // First join: open the sheet so the player picks a race (wait for the server sync)
        if (!promptedRace && mc.screen == null && mc.player.tickCount > 40) {
            promptedRace = true;
            if (!PlayerDBCData.get(mc.player).isRaceChosen()) mc.setScreen(new DBCCharacterScreen());
        }

        int flags = 0;
        if (mc.screen == null) {
            if (ModKeyBindings.CHARGE_KI_KEY.isDown()) flags |= C2SInputStatePacket.CHARGE;
            if (ModKeyBindings.TRANSFORM_KEY.isDown()) flags |= C2SInputStatePacket.TRANSFORM;
            if (ModKeyBindings.GUARD_KEY.isDown()) flags |= C2SInputStatePacket.GUARD;
            if (ModKeyBindings.KI_WAVE_KEY.isDown()) flags |= C2SInputStatePacket.WAVE;
        }
        if (flags != lastInputFlags) {
            lastInputFlags = flags;
            ModMessages.sendToServer(new C2SInputStatePacket(flags));
        }
    }
}
