package com.dragonblock.client;

import net.minecraft.client.Minecraft;

/**
 * Client-side ends of server packets. Packet classes are loaded on the dedicated server too, and the JVM
 * verifier loads every class a method body hands to another (here: our screens as Minecraft's Screen),
 * so code that opens screens must live here and be reached only through a static call.
 */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void openWish(int dragonId) {
        Minecraft.getInstance().setScreen(new com.dragonblock.client.gui.WishScreen(dragonId));
    }

    public static void openTravel(int shipId) {
        Minecraft.getInstance().setScreen(new com.dragonblock.client.gui.TravelScreen(shipId));
    }
}
