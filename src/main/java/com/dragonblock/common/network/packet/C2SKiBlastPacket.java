package com.dragonblock.common.network.packet;

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
            if (data.getCurrentKi() >= KiAttackItem.KI_COST) {
                data.setCurrentKi(data.getCurrentKi() - KiAttackItem.KI_COST);
                data.saveToPlayer(player);
                ModMessages.syncToPlayer(player);

                float damage = 8.0f + (data.getEffectiveWil() * 0.75f);
                float explosionPower = 1.2f + Math.min(2.5f, data.getEffectiveWil() * 0.05f);

                KiBlastEntity blast = new KiBlastEntity(player.level(), player, damage, explosionPower);
                blast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.8F, 0.5F);
                player.level().addFreshEntity(blast);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.KI_BLAST.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            } else {
                player.displayClientMessage(
                        Component.literal("§c¡No tienes suficiente Ki para disparar una ráfaga! (Necesitas 15 Ki)")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
        });
        context.setPacketHandled(true);
    }
}
