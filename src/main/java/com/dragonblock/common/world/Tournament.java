package com.dragonblock.common.world;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.npc.NpcKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * The martial arts tournament on Earth: sign up with the host, and two at a time fight on the ring. You
 * lose by touching the ground outside it, by being knocked out (the blow that would kill you ends the
 * fight instead) or by leaving. Nobody else can hit the fighters, and they can't hit anyone else. After
 * three minutes the judges decide by remaining health. All of it runs on the server thread.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Tournament {
    /** The ring is (2*HALF+1) blocks wide, one block above the ground, centered HALF+4 north of the host. */
    public static final int HALF = 12;
    private static final int COUNTDOWN = 60, LIMIT = 20 * 180;
    public static final int REWARD = 300;

    private record Match(UUID a, UUID b, int startedAt) {
        boolean has(Entity e) { return e.getUUID().equals(a) || e.getUUID().equals(b); }
    }

    private static final Deque<UUID> QUEUE = new ArrayDeque<>();
    private static Match match;

    private Tournament() {}

    /** The ring's center at ground level (fighters stand one block higher), or null if the arena isn't built. */
    public static BlockPos ring(MinecraftServer server) {
        Landmarks.Spot spot = Landmarks.find(server, NpcKind.ANNOUNCER);
        return spot == null || spot.dim() != Level.OVERWORLD ? null : spot.pos().north(HALF + 4);
    }

    public static boolean isQueued(ServerPlayer p) { return QUEUE.contains(p.getUUID()); }
    public static boolean isFighting(ServerPlayer p) { return match != null && match.has(p); }
    public static int queueSize() { return QUEUE.size(); }

    public static String current(MinecraftServer server) {
        if (match == null) return null;
        return name(server, match.a()) + " contra " + name(server, match.b());
    }

    public static void join(ServerPlayer p) {
        if (!isQueued(p) && !isFighting(p)) QUEUE.addLast(p.getUUID());
        tryStart(p.server);
    }

    public static void leave(ServerPlayer p) {
        QUEUE.remove(p.getUUID());
    }

    private static String name(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p == null ? "???" : p.getGameProfile().getName();
    }

    private static boolean eligible(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null && p.isAlive() && !p.isSpectator() && p.level().dimension() == Level.OVERWORLD && !PlayerDBCData.get(p).isDead();
    }

    private static void tryStart(MinecraftServer server) {
        BlockPos ring = ring(server);
        if (match != null || ring == null) return;
        QUEUE.removeIf(id -> !eligible(server, id));
        if (QUEUE.size() < 2) return;
        UUID a = QUEUE.pollFirst(), b = QUEUE.pollFirst();
        match = new Match(a, b, server.getTickCount());
        ServerPlayer pa = server.getPlayerList().getPlayer(a), pb = server.getPlayerList().getPlayer(b);
        var level = server.overworld();
        pa.stopRiding();
        pb.stopRiding();
        pa.teleportTo(level, ring.getX() + 0.5, ring.getY() + 1, ring.getZ() - HALF + 3.5, 0f, 0f);
        pb.teleportTo(level, ring.getX() + 0.5, ring.getY() + 1, ring.getZ() + HALF - 2.5, 180f, 0f);
        for (ServerPlayer p : new ServerPlayer[]{pa, pb}) {
            heal(p);
            p.resetFallDistance();
        }
        announce(server, "¡Siguiente combate: " + name(server, a) + " contra " + name(server, b) + "!");
    }

    private static void heal(ServerPlayer p) {
        PlayerDBCData d = PlayerDBCData.get(p);
        d.setBody(d.getMaxBody());
        p.setHealth(p.getMaxHealth());
    }

    private static void announce(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage(Component.literal("[Torneo] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.YELLOW)), false);
    }

    private static void title(ServerPlayer p, String text, ChatFormatting color) {
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(text).withStyle(color, ChatFormatting.BOLD)));
    }

    private static void end(MinecraftServer server, UUID winner, UUID loser, String how) {
        match = null;
        announce(server, name(server, winner) + " vence a " + name(server, loser) + " " + how + ".");
        ServerPlayer w = server.getPlayerList().getPlayer(winner), l = server.getPlayerList().getPlayer(loser);
        if (w != null) {
            PlayerDBCData d = PlayerDBCData.get(w);
            d.addTrainingPoints(REWARD);
            d.saveToPlayer(w);
            ModMessages.syncToPlayer(w);
            title(w, "¡VICTORIA!", ChatFormatting.GOLD);
            w.level().playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            w.displayClientMessage(Component.literal("+" + REWARD + " TP").withStyle(ChatFormatting.GOLD), true);
        }
        if (l != null) title(l, "DERROTA", ChatFormatting.RED);
        BlockPos ring = ring(server);
        for (ServerPlayer p : new ServerPlayer[]{w, l}) {
            if (p == null) continue;
            heal(p);
            if (ring != null && p.level().dimension() == Level.OVERWORLD) {
                p.teleportTo(server.overworld(), ring.getX() + 0.5, ring.getY(), ring.getZ() + HALF + 6.5, 180f, 0f);
            }
        }
        tryStart(server);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (match == null) {
            if (QUEUE.size() >= 2 && server.getTickCount() % 20 == 0) tryStart(server);
            return;
        }
        Match m = match;
        int age = server.getTickCount() - m.startedAt();
        ServerPlayer a = server.getPlayerList().getPlayer(m.a()), b = server.getPlayerList().getPlayer(m.b());
        if (!eligible(server, m.a())) { end(server, m.b(), m.a(), "por abandono"); return; }
        if (!eligible(server, m.b())) { end(server, m.a(), m.b(), "por abandono"); return; }
        if (age < COUNTDOWN) {
            if (age % 20 == 0) for (ServerPlayer p : new ServerPlayer[]{a, b}) title(p, String.valueOf(3 - age / 20), ChatFormatting.WHITE);
            return;
        }
        if (age == COUNTDOWN) for (ServerPlayer p : new ServerPlayer[]{a, b}) title(p, "¡A PELEAR!", ChatFormatting.GOLD);
        BlockPos ring = ring(server);
        if (ring == null) { match = null; return; }
        if (out(a, ring)) { end(server, m.b(), m.a(), "por salir del ring"); return; }
        if (out(b, ring)) { end(server, m.a(), m.b(), "por salir del ring"); return; }
        if (age > COUNTDOWN + LIMIT) {
            boolean aWins = a.getHealth() / a.getMaxHealth() >= b.getHealth() / b.getMaxHealth();
            end(server, aWins ? m.a() : m.b(), aWins ? m.b() : m.a(), "por decisión de los jueces");
        }
    }

    /** Touching the ground beside the ring, or running far away from it. */
    private static boolean out(ServerPlayer p, BlockPos ring) {
        double dx = Math.abs(p.getX() - ring.getX() - 0.5), dz = Math.abs(p.getZ() - ring.getZ() - 0.5);
        boolean grounded = p.onGround() && p.getY() < ring.getY() + 0.6 && (dx > HALF + 0.5 || dz > HALF + 0.5);
        return grounded || dx > HALF + 24 || dz > HALF + 24;
    }

    /** No outside help, no hitting the crowd, and no blows before the bell. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (match == null || event.getEntity().level().isClientSide) return;
        Entity attacker = event.getSource().getEntity();
        boolean target = match.has(event.getEntity()), source = attacker != null && match.has(attacker);
        if (target != source && attacker != null) event.setCanceled(true);
        else if (target && event.getEntity().getServer().getTickCount() - match.startedAt() < COUNTDOWN) event.setCanceled(true);
    }

    /** The blow that would knock a fighter out ends the match instead of killing them. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        Match m = match;
        if (m == null || !(event.getEntity() instanceof ServerPlayer p) || !m.has(p)) return;
        if (event.getAmount() < p.getHealth()) return;
        event.setCanceled(true);
        boolean aLost = p.getUUID().equals(m.a());
        end(p.server, aLost ? m.b() : m.a(), p.getUUID(), "por K.O.");
    }
}
