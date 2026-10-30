package com.dragonblock.common.network;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.packet.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class ModMessages {
    private static final int PROTOCOL_VERSION = 1;
    public static final SimpleChannel INSTANCE = ChannelBuilder.named(
            ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "messages"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();

    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(C2SChargeKiPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SChargeKiPacket::toBytes)
                .decoder(C2SChargeKiPacket::new)
                .consumerMainThread(C2SChargeKiPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2STransformPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2STransformPacket::toBytes)
                .decoder(C2STransformPacket::new)
                .consumerMainThread(C2STransformPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SFlyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SFlyPacket::toBytes)
                .decoder(C2SFlyPacket::new)
                .consumerMainThread(C2SFlyPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SKiBlastPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SKiBlastPacket::toBytes)
                .decoder(C2SKiBlastPacket::new)
                .consumerMainThread(C2SKiBlastPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SUpgradeStatPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SUpgradeStatPacket::toBytes)
                .decoder(C2SUpgradeStatPacket::new)
                .consumerMainThread(C2SUpgradeStatPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SSelectRacePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSelectRacePacket::toBytes)
                .decoder(C2SSelectRacePacket::new)
                .consumerMainThread(C2SSelectRacePacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CSyncDBCDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncDBCDataPacket::toBytes)
                .decoder(S2CSyncDBCDataPacket::new)
                .consumerMainThread(S2CSyncDBCDataPacket::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        INSTANCE.send(message, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.send(message, PacketDistributor.PLAYER.with(player));
    }

    public static void syncToPlayer(ServerPlayer player) {
        PlayerDBCData data = PlayerDBCData.get(player);
        CompoundTag tag = new CompoundTag();
        data.saveToNBT(tag);
        sendToPlayer(new S2CSyncDBCDataPacket(player.getUUID(), tag), player);
    }
}
