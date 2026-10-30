package com.dragonblock.common.network.packet;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class C2SChargeKiPacket {
    private final boolean charging;

    public C2SChargeKiPacket(boolean charging) {
        this.charging = charging;
    }

    public C2SChargeKiPacket(FriendlyByteBuf buf) {
        this.charging = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(charging);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PlayerDBCData data = PlayerDBCData.get(player);
                data.setChargingKi(charging);
                data.saveToPlayer(player);
                ModMessages.syncToPlayer(player);
            }
        });
        context.setPacketHandled(true);
    }
}
