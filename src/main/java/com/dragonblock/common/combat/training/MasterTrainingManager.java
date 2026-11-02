package com.dragonblock.common.combat.training;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.npc.Dialogues;
import com.dragonblock.common.npc.NpcKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

/**
 * Server-authoritative manager for master training sessions.
 * Coordinates challenges, evaluation, rewards, anti-farming and master reactions.
 */
public final class MasterTrainingManager {
    public static final class Session {
        public final MasterTrainingProfile profile;
        public int currentChallengeIndex = 0;
        public int progress = 0;
        public long startedTick;

        public Session(MasterTrainingProfile profile, long startedTick) {
            this.profile = profile;
            this.startedTick = startedTick;
        }
    }

    private static final Map<String, MasterTrainingProfile> PROFILES = new LinkedHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    static {
        // Register default Master Kaido / Roshi training profile
        List<TrainingChallenge> roshiChallenges = List.of(
                new TrainingChallenge("roshi_combo", "Fundamentos del Puño",
                        "Encadena un combo completo de 4 golpes (Golpe 1 -> Golpe 2 -> Patada -> Gancho).",
                        TrainingChallenge.Type.COMBO_CHAIN, 75),
                new TrainingChallenge("roshi_guard", "Defensa del Caparazón",
                        "Mantén la guardia [B] para proteger tu cuerpo y absorber impactos.",
                        TrainingChallenge.Type.GUARD_DEFENSE, 75),
                new TrainingChallenge("roshi_dodge", "Reflejos del Ermitaño",
                        "Ejecuta una Esquiva Perfecta [R] justo antes de recibir un impacto.",
                        TrainingChallenge.Type.PERFECT_DODGE, 120),
                new TrainingChallenge("roshi_chase", "El Dragón Ascendente",
                        "Lanza al oponente con un Gancho, persíguelo en el aire con [R] y remata con Meteor Smash.",
                        TrainingChallenge.Type.AERIAL_CHASE, 200)
        );

        registerProfile(new MasterTrainingProfile("roshi", NpcKind.MASTER, "Maestro Kaido",
                "¡Bienvenido al dojo de la Escuela Tortuga! El verdadero poder nace del dominio de tu propio cuerpo.",
                roshiChallenges));
    }

    private MasterTrainingManager() {}

    public static void registerProfile(MasterTrainingProfile profile) {
        PROFILES.put(profile.getId(), profile);
    }

    public static MasterTrainingProfile getProfile(String id) {
        return PROFILES.get(id);
    }

    public static Collection<MasterTrainingProfile> getAllProfiles() {
        return Collections.unmodifiableCollection(PROFILES.values());
    }

    public static boolean isInTraining(UUID playerId) {
        return SESSIONS.containsKey(playerId);
    }

    public static Session getSession(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static void startTraining(ServerPlayer player, String profileId) {
        MasterTrainingProfile profile = PROFILES.get(profileId);
        if (profile == null) return;

        Dialogues.close(player);
        long now = player.level().getGameTime();
        Session session = new Session(profile, now);
        SESSIONS.put(player.getUUID(), session);

        player.sendSystemMessage(Component.literal("§6=== " + profile.getMasterName().toUpperCase() + " — ENTRENAMIENTO ==="));
        player.sendSystemMessage(Component.literal("§e" + profile.getIntroDialogue()));
        showCurrentChallenge(player, session);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.4f);
    }

    public static void stopTraining(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void showCurrentChallenge(ServerPlayer player, Session session) {
        if (session.currentChallengeIndex >= session.profile.getChallenges().size()) {
            finishTraining(player, session);
            return;
        }

        TrainingChallenge challenge = session.profile.getChallenges().get(session.currentChallengeIndex);
        player.sendSystemMessage(Component.literal("§aDesafío " + (session.currentChallengeIndex + 1) + ": §f" + challenge.getTitle()));
        player.sendSystemMessage(Component.literal("§7Objetivo: " + challenge.getInstructions()));
        player.sendSystemMessage(Component.literal("§bRecompensa: +" + challenge.getRewardTp() + " TP"));
    }

    public static void onCombatAction(ServerPlayer player, TrainingChallenge.Type actionType) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        if (session.currentChallengeIndex >= session.profile.getChallenges().size()) {
            finishTraining(player, session);
            return;
        }

        TrainingChallenge challenge = session.profile.getChallenges().get(session.currentChallengeIndex);
        if (challenge.getType() == actionType) {
            completeCurrentChallenge(player, session, challenge);
        }
    }

    private static void completeCurrentChallenge(ServerPlayer player, Session session, TrainingChallenge challenge) {
        PlayerDBCData data = PlayerDBCData.get(player);
        data.addTrainingPoints(challenge.getRewardTp());
        data.saveToPlayer(player);

        player.sendSystemMessage(Component.literal("§a✔ ¡Desafío superado: " + challenge.getTitle() + "! §6(+" + challenge.getRewardTp() + " TP)"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);

        session.currentChallengeIndex++;
        if (session.currentChallengeIndex < session.profile.getChallenges().size()) {
            showCurrentChallenge(player, session);
        } else {
            finishTraining(player, session);
        }
    }

    private static void finishTraining(ServerPlayer player, Session session) {
        SESSIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("§6=== ¡ENTRENAMIENTO COMPLETADO CON ÉXITO! ==="));
        player.sendSystemMessage(Component.literal("§e" + session.profile.getMasterName() + ": §f¡Excelente trabajo! Has demostrado una técnica formidable en combate cuerpo a cuerpo. ¡El mundo necesita guerreros como tú!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1.0f);
    }
}
