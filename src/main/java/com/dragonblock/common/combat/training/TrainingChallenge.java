package com.dragonblock.common.combat.training;

import net.minecraft.network.chat.Component;

/**
 * Data definition for an extensible martial arts training challenge.
 * Shares the exact same combat engine and actions as PvP.
 */
public class TrainingChallenge {
    public enum Type {
        COMBO_CHAIN,
        GUARD_DEFENSE,
        PERFECT_DODGE,
        AERIAL_CHASE
    }

    private final String id;
    private final String title;
    private final String instructions;
    private final Type type;
    private final int rewardTp;

    public TrainingChallenge(String id, String title, String instructions, Type type, int rewardTp) {
        this.id = id;
        this.title = title;
        this.instructions = instructions;
        this.type = type;
        this.rewardTp = rewardTp;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public Type getType() { return type; }
    public int getRewardTp() { return rewardTp; }
}
