package com.dragonblock.common.network.packet;

import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Held-key state from the client. Sent only when a key changes, the server does the rest every tick. */
public class C2SInputStatePacket {
    public static final int CHARGE = 1;
    public static final int TRANSFORM = 2;
    public static final int GUARD = 4;
    public static final int WAVE = 8;

    private final int flags;

    public C2SInputStatePacket(int flags) {
        this.flags = flags;
    }

    public C2SInputStatePacket(FriendlyByteBuf buf) {
        this.flags = buf.readByte();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(flags);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            data.setChargingKi((flags & CHARGE) != 0);
            data.setTransformHeld((flags & TRANSFORM) != 0);
            data.setGuarding((flags & GUARD) != 0);
            boolean wave = (flags & WAVE) != 0;
            if (data.isWaveHeld() && !wave) KiAttacks.releaseWave(player, data);
            data.setWaveHeld(wave);
            if (!data.isTransformHeld()) data.setTransformCharge(0f);
            data.saveToPlayer(player);
            ModMessages.syncToTracking(player);
        });
        context.setPacketHandled(true);
    }
}
