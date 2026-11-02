package com.dragonblock.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/** You boarded a ship: show the planet picker. */
public class S2COpenTravelPacket {
    private final int shipId;

    public S2COpenTravelPacket(int shipId) {
        this.shipId = shipId;
    }

    public S2COpenTravelPacket(FriendlyByteBuf buf) {
        this.shipId = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(shipId);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.dragonblock.client.gui.TravelScreen(shipId))));
        context.setPacketHandled(true);
    }
}
