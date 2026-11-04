package com.dragonblock.common.network.packet;

import com.dragonblock.common.data.PlayerDBCData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

public class S2CSyncDBCDataPacket {
    private final UUID playerUUID;
    private final CompoundTag tag;

    public S2CSyncDBCDataPacket(UUID playerUUID, CompoundTag tag) {
        this.playerUUID = playerUUID;
        this.tag = tag;
    }

    public S2CSyncDBCDataPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.tag = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeNbt(tag);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                PlayerDBCData data = new PlayerDBCData();
                if (tag != null) {
                    data.loadFromNBT(tag);
                }
                PlayerDBCData.setClientData(playerUUID, data);
            });
        });
        context.setPacketHandled(true);
    }
}
