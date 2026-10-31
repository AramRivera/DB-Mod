package com.dragonblock.common.network.packet;

import com.dragonblock.client.gui.ShenlongWishScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class S2COpenWishScreenPacket {
    private final int entityId;

    public S2COpenWishScreenPacket(int entityId) {
        this.entityId = entityId;
    }

    public S2COpenWishScreenPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new ShenlongWishScreen(entityId));
            });
        });
        context.setPacketHandled(true);
    }
}
