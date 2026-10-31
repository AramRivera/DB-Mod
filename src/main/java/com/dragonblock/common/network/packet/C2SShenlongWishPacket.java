package com.dragonblock.common.network.packet;

import com.dragonblock.common.entity.ShenlongEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class C2SShenlongWishPacket {
    private final int wishId;
    private final int entityId;

    public C2SShenlongWishPacket(int wishId, int entityId) {
        this.wishId = wishId;
        this.entityId = entityId;
    }

    public C2SShenlongWishPacket(FriendlyByteBuf buf) {
        this.wishId = buf.readInt();
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(wishId);
        buf.writeInt(entityId);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof ShenlongEntity shenlong && !shenlong.isWishGranted()) {
                if (shenlong.distanceToSqr(player) < 32.0 * 32.0) {
                    shenlong.grantWish(wishId, player);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
