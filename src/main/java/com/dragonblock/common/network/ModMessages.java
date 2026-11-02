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
    private static final int PROTOCOL_VERSION = 9;
    public static final SimpleChannel INSTANCE = ChannelBuilder.named(
            ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "messages"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();

    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(C2SInputStatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SInputStatePacket::toBytes)
                .decoder(C2SInputStatePacket::new)
                .consumerMainThread(C2SInputStatePacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SActionPacket::toBytes)
                .decoder(C2SActionPacket::new)
                .consumerMainThread(C2SActionPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SFlyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SFlyPacket::toBytes)
                .decoder(C2SFlyPacket::new)
                .consumerMainThread(C2SFlyPacket::handle)
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

        INSTANCE.messageBuilder(S2CAnimPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CAnimPacket::toBytes)
                .decoder(S2CAnimPacket::new)
                .consumerMainThread(S2CAnimPacket::handle)
                .add();

        INSTANCE.messageBuilder(S2COpenWishPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenWishPacket::toBytes)
                .decoder(S2COpenWishPacket::new)
                .consumerMainThread(S2COpenWishPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SWishPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SWishPacket::toBytes)
                .decoder(C2SWishPacket::new)
                .consumerMainThread(C2SWishPacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CDialoguePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CDialoguePacket::toBytes)
                .decoder(S2CDialoguePacket::new)
                .consumerMainThread(S2CDialoguePacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SDialogueChoicePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SDialogueChoicePacket::toBytes)
                .decoder(C2SDialogueChoicePacket::new)
                .consumerMainThread(C2SDialogueChoicePacket::handle)
                .add();

        INSTANCE.messageBuilder(S2COpenTravelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenTravelPacket::toBytes)
                .decoder(S2COpenTravelPacket::new)
                .consumerMainThread(S2COpenTravelPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2STravelPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2STravelPacket::toBytes)
                .decoder(C2STravelPacket::new)
                .consumerMainThread(C2STravelPacket::handle)
                .add();

        INSTANCE.messageBuilder(com.dragonblock.common.network.packet.C2SCombatActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.dragonblock.common.network.packet.C2SCombatActionPacket::toBytes)
                .decoder(com.dragonblock.common.network.packet.C2SCombatActionPacket::new)
                .consumerMainThread(com.dragonblock.common.network.packet.C2SCombatActionPacket::handle)
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
        sendToPlayer(createSync(player), player);
    }

    /** What each player's watchers last received; identical data is not sent again. */
    private static final java.util.Map<java.util.UUID, CompoundTag> LAST_BROADCAST = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Sync to the player and everyone tracking them, so auras, hair and tails render for others. Skipped
     * when nothing changed since the last broadcast: with a full server most players are idle most of the
     * time, and this runs every second for each of them. New watchers get a copy in onStartTracking.
     */
    public static void syncToTracking(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        PlayerDBCData.get(player).saveToNBT(tag);
        if (tag.equals(LAST_BROADCAST.put(player.getUUID(), tag))) return;
        INSTANCE.send(new S2CSyncDBCDataPacket(player.getUUID(), tag), PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    public static void forget(java.util.UUID player) {
        LAST_BROADCAST.remove(player);
    }

    public static void broadcastAnim(ServerPlayer player, S2CAnimPacket.Anim anim, int arg) {
        INSTANCE.send(new S2CAnimPacket(player.getId(), anim, arg), PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    public static S2CSyncDBCDataPacket createSync(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        PlayerDBCData.get(player).saveToNBT(tag);
        return new S2CSyncDBCDataPacket(player.getUUID(), tag);
    }
}
