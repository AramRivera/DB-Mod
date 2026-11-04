package com.dragonblock.common.network.packet;

import com.dragonblock.common.entity.ShenlongEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** The summoner's chosen wish. The dragon itself checks who is asking and that it is still waiting. */
public class C2SWishPacket {
    private final int entityId;
    private final int wish;

    public C2SWishPacket(int entityId, int wish) {
        this.entityId = entityId;
        this.wish = wish;
    }

    public C2SWishPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.wish = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(wish);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            ShenlongEntity.Wish[] wishes = ShenlongEntity.Wish.values();
            if (player == null || wish < 0 || wish >= wishes.length) return;
            if (player.level().getEntity(entityId) instanceof ShenlongEntity dragon) dragon.grant(player, wishes[wish]);
        });
        context.setPacketHandled(true);
    }
}
