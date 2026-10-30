package com.dragonblock.common.network.packet;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class C2SUpgradeStatPacket {
    private final String stat;

    public C2SUpgradeStatPacket(String stat) {
        this.stat = stat;
    }

    public C2SUpgradeStatPacket(FriendlyByteBuf buf) {
        this.stat = buf.readUtf(16);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(stat, 16);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            boolean upgraded = data.upgradeStat(stat);
            if (upgraded) {
                data.saveToPlayer(player);
                ModMessages.syncToPlayer(player);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);

                player.displayClientMessage(
                        Component.literal("§a¡Has mejorado tu atributo " + stat.toUpperCase() + "!"),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("§cNo tienes suficientes Puntos de Entrenamiento (TP)."),
                        true
                );
            }
        });
        context.setPacketHandled(true);
    }
}
