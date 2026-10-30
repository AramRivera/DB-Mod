package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.gui.DBCCharacterScreen;
import com.dragonblock.client.keybind.ModKeyBindings;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SChargeKiPacket;
import com.dragonblock.common.network.packet.C2SFlyPacket;
import com.dragonblock.common.network.packet.C2SKiBlastPacket;
import com.dragonblock.common.network.packet.C2STransformPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean wasCharging = false;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (ModKeyBindings.CHARACTER_SHEET_KEY.consumeClick()) {
            mc.setScreen(new DBCCharacterScreen());
        }

        if (ModKeyBindings.TRANSFORM_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2STransformPacket());
        }

        if (ModKeyBindings.FLY_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SFlyPacket());
        }

        if (ModKeyBindings.KI_BLAST_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SKiBlastPacket());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            wasCharging = false;
            return;
        }

        boolean isKeyDown = mc.screen == null && ModKeyBindings.CHARGE_KI_KEY.isDown();
        if (isKeyDown != wasCharging) {
            wasCharging = isKeyDown;
            ModMessages.sendToServer(new C2SChargeKiPacket(wasCharging));
        }
    }
}
