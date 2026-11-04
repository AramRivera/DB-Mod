package com.dragonblock.common.combat;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2CAnimPacket;
import com.dragonblock.common.npc.Dialogues;
import com.dragonblock.common.npc.Dialogues.Node;
import com.dragonblock.common.npc.Dialogues.Option;
import com.dragonblock.common.npc.NpcKind;
import com.dragonblock.common.world.Landmarks;
import com.dragonblock.common.world.Planet;
import com.dragonblock.common.world.Worlds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Instant transmission between worlds: Teleport while sneaking. Two fingers to the forehead, you feel the
 * ki of everyone online and of the characters you have spoken with, wherever they are, and appear at their
 * side. Souls only reach souls (the Other World), the living only reach the living. Every pick is checked
 * again on the server when it is made: the target may have moved, died or left.
 */
public final class InstantTransmission {
    /** Player flag prefix: you know this character's ki because you talked to them. */
    public static final String KNOWN = "ki:";
    private static final int COOLDOWN = 600;
    private static final float KI_SHARE = 0.25f;
    private static final int PAGE = 7;
    private static final Map<UUID, Integer> READY_AT = new ConcurrentHashMap<>();

    private InstantTransmission() {}

    public static void open(ServerPlayer player, int page) {
        PlayerDBCData data = PlayerDBCData.get(player);
        List<Option> targets = new ArrayList<>();
        for (NpcKind kind : NpcKind.values()) {
            Landmarks.Spot spot = Landmarks.find(player.server, kind);
            if (spot != null && data.hasFlag(KNOWN + kind.id()) && reachable(data, spot.dim())) {
                targets.add(new Option(kind.getDisplayName() + "  ·  " + worldName(spot.dim()), p -> toCharacter(p, kind)));
            }
        }
        List<ServerPlayer> others = new ArrayList<>(player.server.getPlayerList().getPlayers());
        others.sort(Comparator.comparingLong(o -> -PlayerDBCData.get(o).getBattlePower()));
        for (ServerPlayer other : others) {
            if (!canSense(player, data, other)) continue;
            UUID id = other.getUUID();
            targets.add(new Option(other.getGameProfile().getName() + "  ·  " + worldName(other.level().dimension())
                    + "  ·  " + String.format("%,d", PlayerDBCData.get(other).getBattlePower()), p -> toPlayer(p, id)));
        }

        int from = Math.min(page * PAGE, targets.size());
        List<Option> shown = new ArrayList<>(targets.subList(from, Math.min(from + PAGE, targets.size())));
        if (from + PAGE < targets.size()) shown.add(new Option("Seguir buscando...", p -> open(p, page + 1)));
        shown.add(new Option("Dejarlo", Dialogues::close));
        String text = targets.isEmpty()
                ? "Te llevas dos dedos a la frente... pero no reconoces ningún ki. Habla con la gente para aprender a sentir el suyo."
                : "Te llevas dos dedos a la frente y buscas un ki conocido. ¿A quién quieres ir?";
        Dialogues.showSelf(player, "Teletransportación instantánea", new Node(text, shown));
    }

    private static boolean reachable(PlayerDBCData data, ResourceKey<Level> dim) {
        return data.isDead() == (dim == Worlds.OTHERWORLD);
    }

    private static boolean canSense(ServerPlayer player, PlayerDBCData data, ServerPlayer other) {
        return other != player && other.isAlive() && !other.isSpectator() && PlayerDBCData.get(other).isRaceChosen()
                && reachable(data, other.level().dimension());
    }

    private static String worldName(ResourceKey<Level> dim) {
        if (dim == Worlds.OTHERWORLD) return "Otro Mundo";
        Planet planet = Planet.of(dim);
        return planet == null ? "lejos" : planet.getDisplayName();
    }

    /** Ki and cooldown, checked when the pick is made; consumed only if the jump happens. */
    private static boolean ready(ServerPlayer player, PlayerDBCData data) {
        if (!data.hasLearned(Technique.TELEPORT)) return false;
        int wait = READY_AT.getOrDefault(player.getUUID(), 0) - player.server.getTickCount();
        if (wait > 0) {
            player.displayClientMessage(Component.literal("Aún no te concentras lo suficiente (" + (wait / 20 + 1) + " s)."), true);
            return false;
        }
        if (data.getCurrentKi() < data.getMaxKi() * KI_SHARE) {
            player.displayClientMessage(Component.literal("Ki insuficiente: necesitas una cuarta parte de tu ki."), true);
            return false;
        }
        return true;
    }

    private static void toCharacter(ServerPlayer player, NpcKind kind) {
        PlayerDBCData data = PlayerDBCData.get(player);
        Landmarks.Spot spot = Landmarks.find(player.server, kind);
        if (spot == null || !reachable(data, spot.dim()) || !ready(player, data)) return;
        ServerLevel level = player.server.getLevel(spot.dim());
        if (level == null) return;
        Worlds.ensureBuilt(level);
        depart(player, data);
        Worlds.sendTo(player, spot.dim(), spot.pos().south(2));
        arrive(player);
    }

    private static void toPlayer(ServerPlayer player, UUID id) {
        PlayerDBCData data = PlayerDBCData.get(player);
        ServerPlayer target = player.server.getPlayerList().getPlayer(id);
        if (target == null || !canSense(player, data, target)) {
            player.displayClientMessage(Component.literal("Ese ki se ha desvanecido."), true);
            return;
        }
        if (!ready(player, data)) return;
        depart(player, data);
        // Right behind them, facing them; if that spot is blocked, on their own spot lifted to open air
        Vec3 behind = target.position().add(target.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.6));
        ServerLevel level = target.serverLevel();
        if (level.noCollision(player, player.getBoundingBox().move(behind.subtract(player.position())))) {
            Vec3 look = target.position().subtract(behind);
            player.teleportTo(level, behind.x, behind.y, behind.z, (float) (Math.atan2(-look.x, look.z) * 180 / Math.PI), 0f);
        } else {
            Worlds.sendTo(player, level.dimension(), target.blockPosition());
        }
        arrive(player);
    }

    private static void depart(ServerPlayer player, PlayerDBCData data) {
        data.consumeKi(data.getMaxKi() * KI_SHARE);
        READY_AT.put(player.getUUID(), player.server.getTickCount() + COOLDOWN);
        player.stopRiding();
        player.resetFallDistance();
        ModMessages.broadcastAnim(player, S2CAnimPacket.Anim.TECHNIQUE, Technique.TELEPORT.ordinal());
        burst(player);
    }

    private static void arrive(ServerPlayer player) {
        player.resetFallDistance();
        burst(player);
        ModMessages.syncToTracking(player);
    }

    private static void burst(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 24, 0.3, 0.7, 0.3, 0.06);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.9f);
    }
}
