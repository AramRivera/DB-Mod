package com.dragonblock.common.network.packet;

import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Held-key state from the client. Sent only when a key changes, the server does the rest every tick. */
public class C2SInputStatePacket {
    public static final int CHARGE = 1;
    public static final int TRANSFORM = 2;
    public static final int GUARD = 4;
    public static final int TECH = 8;
    /** A screen opened: drop held techniques instead of firing them. */
    public static final int SUPPRESS = 16;

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
            // Already transformed: the transform key does nothing until you revert
            data.setTransformHeld((flags & TRANSFORM) != 0 && data.getTransformation() == Transformation.BASE);
            data.setGuarding((flags & GUARD) != 0);
            boolean tech = (flags & TECH) != 0;
            if (data.isTechHeld() && !tech) {
                if ((flags & SUPPRESS) != 0) data.setTechCharge(0f);
                else KiAttacks.release(player, data);
            }
            data.setTechHeld(tech);
            if (!data.isTransformHeld()) data.setTransformCharge(0f);
            data.saveToPlayer(player);
            ModMessages.syncToTracking(player);
        });
        context.setPacketHandled(true);
    }
}
