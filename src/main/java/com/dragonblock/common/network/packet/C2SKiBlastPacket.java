package com.dragonblock.common.network.packet;

import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.entity.KiBlastEntity;
import com.dragonblock.common.item.KiAttackItem;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class C2SKiBlastPacket {
    public C2SKiBlastPacket() {}

    public C2SKiBlastPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            KiAttacks.fireBlast(player, data);
            data.saveToPlayer(player);
            ModMessages.syncToPlayer(player);
        });
        context.setPacketHandled(true);
    }
}
