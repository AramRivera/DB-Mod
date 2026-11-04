package com.dragonblock.common.network.packet;

import com.dragonblock.common.npc.Dialogues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** The option a player picked; Dialogues checks it against what that player was offered. */
public class C2SDialogueChoicePacket {
    private final int npcId, index;

    public C2SDialogueChoicePacket(int npcId, int index) {
        this.npcId = npcId;
        this.index = index;
    }

    public C2SDialogueChoicePacket(FriendlyByteBuf buf) {
        this.npcId = buf.readInt();
        this.index = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(npcId);
        buf.writeVarInt(index);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) Dialogues.choose(player, npcId, index);
        });
        context.setPacketHandled(true);
    }
}
