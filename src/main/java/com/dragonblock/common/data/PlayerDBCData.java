package com.dragonblock.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDBCData {
    private static final String NBT_KEY = "DragonBlockData";
    private static final Map<UUID, PlayerDBCData> CLIENT_CACHE = new HashMap<>();

    private Race race = Race.HUMAN;
    private Transformation transformation = Transformation.BASE;

    private int str = 5;
    private int dex = 5;
    private int con = 5;
    private int wil = 5;
    private int mnd = 5;

    private float currentKi = 50.0f;
    private int trainingPoints = 100;

    private boolean isChargingKi = false;
    private boolean isFlying = false;

    public PlayerDBCData() {
        this.currentKi = getMaxKi();
    }

    public static PlayerDBCData get(Player player) {
        if (player.level().isClientSide) {
            return CLIENT_CACHE.computeIfAbsent(player.getUUID(), uuid -> new PlayerDBCData());
        }
        CompoundTag persistent = player.getPersistentData();
        PlayerDBCData data = new PlayerDBCData();
        if (persistent.contains(NBT_KEY)) {
            data.loadFromNBT(persistent.getCompound(NBT_KEY));
        } else {
            data.saveToPlayer(player);
        }
        return data;
    }

    public static void setClientData(UUID uuid, PlayerDBCData data) {
        CLIENT_CACHE.put(uuid, data);
    }

    public void saveToPlayer(Player player) {
        if (!player.level().isClientSide) {
            CompoundTag persistent = player.getPersistentData();
            CompoundTag tag = new CompoundTag();
            saveToNBT(tag);
            persistent.put(NBT_KEY, tag);
        }
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putString("Race", race.name());
        tag.putString("Transformation", transformation.name());
        tag.putInt("STR", str);
        tag.putInt("DEX", dex);
        tag.putInt("CON", con);
        tag.putInt("WIL", wil);
        tag.putInt("MND", mnd);
        tag.putFloat("CurrentKi", currentKi);
        tag.putInt("TP", trainingPoints);
        tag.putBoolean("IsCharging", isChargingKi);
        tag.putBoolean("IsFlying", isFlying);
    }

    public void loadFromNBT(CompoundTag tag) {
        try {
            if (tag.contains("Race")) this.race = Race.valueOf(tag.getString("Race"));
        } catch (IllegalArgumentException ignored) {
            this.race = Race.HUMAN;
        }

        try {
            if (tag.contains("Transformation")) this.transformation = Transformation.valueOf(tag.getString("Transformation"));
        } catch (IllegalArgumentException ignored) {
            this.transformation = Transformation.BASE;
        }

        if (tag.contains("STR")) this.str = Math.max(1, tag.getInt("STR"));
        if (tag.contains("DEX")) this.dex = Math.max(1, tag.getInt("DEX"));
        if (tag.contains("CON")) this.con = Math.max(1, tag.getInt("CON"));
        if (tag.contains("WIL")) this.wil = Math.max(1, tag.getInt("WIL"));
        if (tag.contains("MND")) this.mnd = Math.max(1, tag.getInt("MND"));
        if (tag.contains("TP")) this.trainingPoints = Math.max(0, tag.getInt("TP"));

        float max = getMaxKi();
        if (tag.contains("CurrentKi")) {
            this.currentKi = Math.min(max, Math.max(0.0f, tag.getFloat("CurrentKi")));
        } else {
            this.currentKi = max;
        }

        if (tag.contains("IsCharging")) this.isChargingKi = tag.getBoolean("IsCharging");
        if (tag.contains("IsFlying")) this.isFlying = tag.getBoolean("IsFlying");
    }

    public float getMaxKi() {
        return 50.0f + (wil * 10.0f * race.getWilModifier());
    }

    public int getEffectiveStr() {
        return (int) Math.max(1, str * race.getStrModifier() * transformation.getStatMultiplier());
    }

    public int getEffectiveDex() {
        return (int) Math.max(1, dex * race.getDexModifier() * transformation.getStatMultiplier());
    }

    public int getEffectiveCon() {
        return (int) Math.max(1, con * race.getConModifier() * transformation.getStatMultiplier());
    }

    public int getEffectiveWil() {
        return (int) Math.max(1, wil * race.getWilModifier() * transformation.getStatMultiplier());
    }

    public long getBattlePower() {
        float kiRatio = getMaxKi() > 0 ? (currentKi / getMaxKi()) : 1.0f;
        float baseBP = (getEffectiveStr() * 10.0f) + (getEffectiveDex() * 8.0f) + (getEffectiveCon() * 10.0f) + (getEffectiveWil() * 12.0f) + (mnd * 5.0f);
        return Math.max(5, (long) (baseBP * (0.35f + 0.65f * kiRatio)));
    }

    public int getUpgradeCost(String statName) {
        int statValue = switch (statName.toUpperCase()) {
            case "STR" -> str;
            case "DEX" -> dex;
            case "CON" -> con;
            case "WIL" -> wil;
            case "MND" -> mnd;
            default -> 100;
        };
        return statValue * 10;
    }

    public boolean upgradeStat(String statName) {
        int cost = getUpgradeCost(statName);
        if (trainingPoints < cost) return false;

        trainingPoints -= cost;
        switch (statName.toUpperCase()) {
            case "STR" -> str++;
            case "DEX" -> dex++;
            case "CON" -> con++;
            case "WIL" -> wil++;
            case "MND" -> mnd++;
            default -> {
                trainingPoints += cost;
                return false;
            }
        }
        return true;
    }

    // Getters and setters
    public Race getRace() { return race; }
    public void setRace(Race race) { this.race = race; }

    public Transformation getTransformation() { return transformation; }
    public void setTransformation(Transformation transformation) { this.transformation = transformation; }

    public int getStr() { return str; }
    public void setStr(int str) { this.str = str; }

    public int getDex() { return dex; }
    public void setDex(int dex) { this.dex = dex; }

    public int getCon() { return con; }
    public void setCon(int con) { this.con = con; }

    public int getWil() { return wil; }
    public void setWil(int wil) { this.wil = wil; }

    public int getMnd() { return mnd; }
    public void setMnd(int mnd) { this.mnd = mnd; }

    public float getCurrentKi() { return currentKi; }
    public void setCurrentKi(float currentKi) { this.currentKi = Math.min(getMaxKi(), Math.max(0.0f, currentKi)); }

    public int getTrainingPoints() { return trainingPoints; }
    public void setTrainingPoints(int tp) { this.trainingPoints = Math.max(0, tp); }
    public void addTrainingPoints(int tp) { this.trainingPoints = Math.max(0, this.trainingPoints + tp); }

    public boolean isChargingKi() { return isChargingKi; }
    public void setChargingKi(boolean chargingKi) { isChargingKi = chargingKi; }

    public boolean isFlying() { return isFlying; }
    public void setFlying(boolean flying) { isFlying = flying; }
}
