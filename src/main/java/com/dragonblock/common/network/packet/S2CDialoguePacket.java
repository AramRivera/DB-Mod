package com.dragonblock.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

/** One line of a conversation and the labels of its options; npcId -1 closes the dialogue. */
public class S2CDialoguePacket {
    private final int npcId;
    private final String speaker, text;
    private final List<String> options;

    public S2CDialoguePacket(int npcId, String speaker, String text, List<String> options) {
        this.npcId = npcId;
        this.speaker = speaker;
        this.text = text;
        this.options = options;
    }

    public S2CDialoguePacket(FriendlyByteBuf buf) {
        this.npcId = buf.readInt();
        this.speaker = buf.readUtf(64);
        this.text = buf.readUtf(1024);
        int n = Math.min(8, buf.readVarInt());
        this.options = new ArrayList<>();
        for (int i = 0; i < n; i++) options.add(buf.readUtf(128));
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(npcId);
        buf.writeUtf(speaker, 64);
        buf.writeUtf(text, 1024);
        buf.writeVarInt(options.size());
        for (String o : options) buf.writeUtf(o, 128);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dragonblock.client.gui.DialogueScreen.show(npcId, speaker, text, options)));
        context.setPacketHandled(true);
    }
}
