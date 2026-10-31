package com.dragonblock.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/** The dragon is waiting: open the wish screen for its summoner. */
public class S2COpenWishPacket {
    private final int entityId;

    public S2COpenWishPacket(int entityId) {
        this.entityId = entityId;
    }

    public S2COpenWishPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.dragonblock.client.gui.WishScreen(entityId))));
        context.setPacketHandled(true);
    }
}
