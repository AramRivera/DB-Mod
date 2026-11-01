package com.dragonblock.common.npc;

import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2CDialoguePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Conversations. The server builds each line with its options (what an option does is a server-side
 * action, never sent to the client), remembers what it offered, and only runs the option a player picks
 * from that list while standing next to the same character.
 */
public final class Dialogues {
    public record Option(String label, Consumer<ServerPlayer> action) {}

    public record Node(String text, List<Option> options) {
        public Node(String text, Option... options) {
            this(text, List.of(options));
        }
    }

    private record Session(int npcId, List<Option> options) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private Dialogues() {}

    public static void open(ServerPlayer player, NpcEntity npc) {
        show(player, npc, NpcDialogues.root(player, npc));
    }

    public static void show(ServerPlayer player, NpcEntity npc, Node node) {
        SESSIONS.put(player.getUUID(), new Session(npc.getId(), node.options()));
        ModMessages.sendToPlayer(new S2CDialoguePacket(npc.getId(), npc.getKind().getDisplayName(), node.text(),
                node.options().stream().map(Option::label).toList()), player);
    }

    /** Ends the conversation on the player's screen. */
    public static void close(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        ModMessages.sendToPlayer(new S2CDialoguePacket(-1, "", "", List.of()), player);
    }

    /** A pick from the client: only valid for what this player was just offered, near that character. */
    public static void choose(ServerPlayer player, int npcId, int index) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.npcId() != npcId || index < 0 || index >= session.options().size()) return;
        if (!(player.level().getEntity(npcId) instanceof NpcEntity npc) || npc.distanceTo(player) > 10 * npc.getKind().getScale()) {
            close(player);
            return;
        }
        SESSIONS.remove(player.getUUID());
        session.options().get(index).action().accept(player);
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }
}
