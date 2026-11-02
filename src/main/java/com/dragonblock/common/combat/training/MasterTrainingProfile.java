package com.dragonblock.common.combat.training;

import com.dragonblock.common.npc.NpcKind;

import java.util.Collections;
import java.util.List;

/**
 * Data profile for a master NPC's training catalogue.
 * Data-driven: adding new masters (Karin, Piccolo, Vegeta, Goku, Whis) is done by
 * registering new profiles without altering the combat engine.
 */
public class MasterTrainingProfile {
    private final String id;
    private final NpcKind npcKind;
    private final String masterName;
    private final String introDialogue;
    private final List<TrainingChallenge> challenges;

    public MasterTrainingProfile(String id, NpcKind npcKind, String masterName, String introDialogue, List<TrainingChallenge> challenges) {
        this.id = id;
        this.npcKind = npcKind;
        this.masterName = masterName;
        this.introDialogue = introDialogue;
        this.challenges = challenges;
    }

    public String getId() { return id; }
    public NpcKind getNpcKind() { return npcKind; }
    public String getMasterName() { return masterName; }
    public String getIntroDialogue() { return introDialogue; }
    public List<TrainingChallenge> getChallenges() { return Collections.unmodifiableList(challenges); }
}
