package com.dragonblock.common.network.packet;

import com.dragonblock.client.anim.AnimState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/** One-shot animation (technique release, dash, transformation, melee attacks) broadcast to everyone tracking the player. */
public class S2CAnimPacket {
    /** TECHNIQUE carries the technique ordinal in {@code arg}. */
    public enum Anim { TECHNIQUE, DASH, TRANSFORM, SCOUTER_BREAK, PUNCH_LIGHT_1, PUNCH_LIGHT_2, KICK_HEAVY, UPPERCUT }

    private final int entityId;
    private final Anim anim;
    private final int arg;

    public S2CAnimPacket(int entityId, Anim anim, int arg) {
        this.entityId = entityId;
        this.anim = anim;
        this.arg = arg;
    }

    public S2CAnimPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.anim = buf.readEnum(Anim.class);
        this.arg = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeEnum(anim);
        buf.writeVarInt(arg);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AnimState.play(entityId, anim, arg)));
        context.setPacketHandled(true);
    }
}
