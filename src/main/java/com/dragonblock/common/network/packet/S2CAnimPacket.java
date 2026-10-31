package com.dragonblock.common.network.packet;

import com.dragonblock.client.anim.AnimState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/** One-shot animation (blast, wave release, dash) broadcast to everyone tracking the player. */
public class S2CAnimPacket {
    public enum Anim { BLAST, WAVE_RELEASE, DASH, TRANSFORM }

    private final int entityId;
    private final Anim anim;

    public S2CAnimPacket(int entityId, Anim anim) {
        this.entityId = entityId;
        this.anim = anim;
    }

    public S2CAnimPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.anim = buf.readEnum(Anim.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeEnum(anim);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AnimState.play(entityId, anim)));
        context.setPacketHandled(true);
    }
}
