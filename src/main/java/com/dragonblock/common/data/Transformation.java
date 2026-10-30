package com.dragonblock.common.data;

public enum Transformation {
    BASE("Forma Base", 1.0f, 0.0f, 0xFF00FF, 0, 0),
    KAIOKEN("Kaio-ken", 1.5f, 0.5f, 0xFF2222, 10, 10),
    SUPER_SAIYAN("Super Saiyajin", 2.5f, 1.0f, 0xFFDD00, 20, 20);

    private final String displayName;
    private final float statMultiplier;
    private final float kiDrainPerSecond;
    private final int auraColor;
    private final int reqStr;
    private final int reqWil;

    Transformation(String displayName, float statMultiplier, float kiDrainPerSecond, int auraColor, int reqStr, int reqWil) {
        this.displayName = displayName;
        this.statMultiplier = statMultiplier;
        this.kiDrainPerSecond = kiDrainPerSecond;
        this.auraColor = auraColor;
        this.reqStr = reqStr;
        this.reqWil = reqWil;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getStatMultiplier() {
        return statMultiplier;
    }

    public float getKiDrainPerSecond() {
        return kiDrainPerSecond;
    }

    public int getAuraColor() {
        return auraColor;
    }

    public int getReqStr() {
        return reqStr;
    }

    public int getReqWil() {
        return reqWil;
    }

    public boolean canTransform(Race race, int str, int wil) {
        if (this == BASE) return true;
        if (this == KAIOKEN) return str >= reqStr && wil >= reqWil;
        if (this == SUPER_SAIYAN) {
            return (race == Race.SAIYAN || race == Race.HALF_SAIYAN) && str >= reqStr && wil >= reqWil;
        }
        return false;
    }
}
