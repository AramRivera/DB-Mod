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

public class C2SFlyPacket {
    public C2SFlyPacket() {}

    public C2SFlyPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || com.dragonblock.common.combat.melee.MeleeCombatManager.isBusy(player)) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            boolean newFlight = !data.isFlying();

            if (newFlight && !data.getTransformation().canFly()) {
                player.displayClientMessage(Component.literal("Tu cuerpo es demasiado grande para volar."), true);
                return;
            }
            if (newFlight && data.getCurrentKi() < 5.0f) {
                player.displayClientMessage(
                        Component.literal("§c¡No tienes suficiente Ki para volar (Bukujutsu)!"),
                        true
                );
                return;
            }

            data.setFlying(newFlight);
            data.saveToPlayer(player);

            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = newFlight;
                player.getAbilities().flying = newFlight;
                player.onUpdateAbilities();
            }

            ModMessages.syncToPlayer(player);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.5f, 1.2f);

            player.displayClientMessage(
                    Component.literal(newFlight ? "§b[Bukujutsu] Vuelo activado." : "§7[Bukujutsu] Vuelo desactivado.")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        });
        context.setPacketHandled(true);
    }
}
