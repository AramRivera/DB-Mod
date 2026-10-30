package com.dragonblock.common.network.packet;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class C2STransformPacket {
    public C2STransformPacket() {}

    public C2STransformPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            Transformation current = data.getTransformation();

            if (current != Transformation.BASE) {
                // Revert to base
                data.setTransformation(Transformation.BASE);
                data.saveToPlayer(player);
                ModMessages.syncToPlayer(player);

                player.displayClientMessage(
                        Component.literal("Has vuelto a tu forma base.").withStyle(ChatFormatting.YELLOW),
                        true
                );
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TRANSFORM.get(), SoundSource.PLAYERS, 0.8f, 0.7f);
            } else {
                // Try Super Saiyan first if eligible
                if (Transformation.SUPER_SAIYAN.canTransform(data.getRace(), data.getStr(), data.getWil())) {
                    data.setTransformation(Transformation.SUPER_SAIYAN);
                    data.saveToPlayer(player);
                    ModMessages.syncToPlayer(player);

                    triggerTransformEffects(player, "¡¡SUPER SAIYAJIN ACTIVADO!!", ChatFormatting.GOLD);
                } else if (Transformation.KAIOKEN.canTransform(data.getRace(), data.getStr(), data.getWil())) {
                    data.setTransformation(Transformation.KAIOKEN);
                    data.saveToPlayer(player);
                    ModMessages.syncToPlayer(player);

                    triggerTransformEffects(player, "¡¡KAIO-KEN ACTIVADO!!", ChatFormatting.RED);
                } else {
                    player.displayClientMessage(
                        Component.literal("§cNo cumples los requisitos de atributos (STR/WIL) o raza para transformarte."),
                        true
                    );
                }
            }
        });
        context.setPacketHandled(true);
    }

    private void triggerTransformEffects(ServerPlayer player, String message, ChatFormatting color) {
        player.displayClientMessage(
                Component.literal(message).withStyle(color, ChatFormatting.BOLD),
                true
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.8f, 1.0f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.8, 1.2, 0.8, 0.2);
        }
    }
}
