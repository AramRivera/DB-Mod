package com.dragonblock.common.network.packet;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class C2SSelectRacePacket {
    private final String raceName;

    public C2SSelectRacePacket(String raceName) {
        this.raceName = raceName;
    }

    public C2SSelectRacePacket(FriendlyByteBuf buf) {
        this.raceName = buf.readUtf(32);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(raceName, 32);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            if (data.isRaceChosen()) return; // race is permanent, /dev race to change it

            try {
                Race newRace = Race.valueOf(raceName);
                data.setRace(newRace);
                data.setRaceChosen(true);
                data.setTransformation(Transformation.BASE);
                data.setSelectedForm(Transformation.BASE);
                data.saveToPlayer(player);
                ModMessages.syncToTracking(player);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("Raza elegida: " + newRace.getDisplayName()), false);
            } catch (IllegalArgumentException ignored) {}
        });
        context.setPacketHandled(true);
    }
}
