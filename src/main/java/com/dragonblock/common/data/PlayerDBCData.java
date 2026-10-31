package com.dragonblock.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDBCData {
    private static final String NBT_KEY = "DragonBlockData";
    private static final Map<UUID, PlayerDBCData> CLIENT_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerDBCData> SERVER_CACHE = new ConcurrentHashMap<>();

    private Race race = Race.HUMAN;
    private boolean raceChosen = false;
    private Transformation transformation = Transformation.BASE;
    private Transformation selectedForm = Transformation.BASE;
    private final EnumMap<Transformation, Float> mastery = new EnumMap<>(Transformation.class);

    private int str = 5;
    private int dex = 5;
    private int con = 5;
    private int wil = 5;
    private int mnd = 5;

    private float currentKi = 50.0f;
    /** Real health pool. Vanilla health only mirrors body / maxBody so hearts never overflow. */
    private float body = -1f;
    private int trainingPoints = 100;
    private long lastZenkaiTime = -100000L;

    private boolean isChargingKi = false;
    private boolean isFlying = false;
    private boolean tailOut = true;

    // Live input state (synced so other players see auras / guard, never persisted)
    private boolean guarding = false;
    private boolean transformHeld = false;
    private boolean waveHeld = false;
    private float transformCharge = 0f;
    private float waveCharge = 0f;

    // Server-only state
    /** Set by /dev form so sky checks (Ozaru) don't revert a forced form. */
    public boolean forcedForm = false;
    /** Vanilla health we last wrote; any other difference came from vanilla healing or damage. */
    public float lastMirroredHealth = -1f;
    public float lastMaxBody = -1f;
    /** Ultra Ego rage 0..1, see FormEffects. */
    public float rage = 0f;
    /** Scale currently applied; eases toward the form's scale so giants visibly grow. */
    public float appliedScale = 1f;
    public boolean guardApplied = false;
    public boolean rootApplied = false;

    public int blastCooldown = 0;
    public int dashCooldown = 0;
    public int dashIframes = 0;

    public PlayerDBCData() {
        this.currentKi = getMaxKi();
    }

    public static PlayerDBCData get(Player player) {
        if (player.level().isClientSide) {
            return CLIENT_CACHE.computeIfAbsent(player.getUUID(), uuid -> new PlayerDBCData());
        }
        return SERVER_CACHE.computeIfAbsent(player.getUUID(), uuid -> {
            CompoundTag persistent = player.getPersistentData();
            PlayerDBCData data = new PlayerDBCData();
            if (persistent.contains(NBT_KEY)) {
                data.loadFromNBT(persistent.getCompound(NBT_KEY));
            } else {
                data.saveToPlayer(player);
            }
            return data;
        });
    }

    public static void setClientData(UUID uuid, PlayerDBCData data) {
        CLIENT_CACHE.put(uuid, data);
    }

    /** Client logout: forget every other player's data. */
    public static void clearClient() {
        CLIENT_CACHE.clear();
    }

    public static void removePlayer(UUID uuid) {
        SERVER_CACHE.remove(uuid);
        CLIENT_CACHE.remove(uuid);
    }

    public void saveToPlayer(Player player) {
        if (!player.level().isClientSide) {
            CompoundTag tag = new CompoundTag();
            saveToNBT(tag);
            player.getPersistentData().put(NBT_KEY, tag);
        }
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putString("Race", race.name());
        tag.putBoolean("RaceChosen", raceChosen);
        tag.putString("Transformation", transformation.name());
        tag.putString("SelectedForm", selectedForm.name());
        tag.putInt("STR", str);
        tag.putInt("DEX", dex);
        tag.putInt("CON", con);
        tag.putInt("WIL", wil);
        tag.putInt("MND", mnd);
        tag.putFloat("CurrentKi", currentKi);
        tag.putFloat("Body", getBody());
        tag.putFloat("MaxBody", getMaxBody());
        tag.putInt("TP", trainingPoints);
        tag.putLong("LastZenkai", lastZenkaiTime);
        tag.putBoolean("IsCharging", isChargingKi);
        tag.putBoolean("IsFlying", isFlying);
        tag.putBoolean("TailOut", tailOut);
        tag.putBoolean("Guarding", guarding);
        tag.putFloat("TransformCharge", transformCharge);
        tag.putFloat("WaveCharge", waveCharge);

        CompoundTag masteryTag = new CompoundTag();
        mastery.forEach((form, value) -> masteryTag.putFloat(form.name(), value));
        tag.put("Mastery", masteryTag);
    }

    public void loadFromNBT(CompoundTag tag) {
        this.race = parseEnum(Race.class, tag.getString("Race"), Race.HUMAN);
        this.transformation = parseEnum(Transformation.class, tag.getString("Transformation"), Transformation.BASE);
        this.selectedForm = parseEnum(Transformation.class, tag.getString("SelectedForm"), Transformation.BASE);
        // Saves from before race selection existed already picked a race
        this.raceChosen = tag.contains("RaceChosen") ? tag.getBoolean("RaceChosen") : tag.contains("Race");

        if (tag.contains("STR")) this.str = Math.max(1, tag.getInt("STR"));
        if (tag.contains("DEX")) this.dex = Math.max(1, tag.getInt("DEX"));
        if (tag.contains("CON")) this.con = Math.max(1, tag.getInt("CON"));
        if (tag.contains("WIL")) this.wil = Math.max(1, tag.getInt("WIL"));
        if (tag.contains("MND")) this.mnd = Math.max(1, tag.getInt("MND"));
        if (tag.contains("TP")) this.trainingPoints = Math.max(0, tag.getInt("TP"));
        if (tag.contains("LastZenkai")) this.lastZenkaiTime = tag.getLong("LastZenkai");

        this.currentKi = tag.contains("CurrentKi")
                ? Math.min(getMaxKi(), Math.max(0.0f, tag.getFloat("CurrentKi")))
                : getMaxKi();

        this.body = tag.contains("Body") ? tag.getFloat("Body") : -1f;
        this.isChargingKi = tag.getBoolean("IsCharging");
        this.isFlying = tag.getBoolean("IsFlying");
        this.tailOut = !tag.contains("TailOut") || tag.getBoolean("TailOut");
        this.guarding = tag.getBoolean("Guarding");
        this.transformCharge = tag.getFloat("TransformCharge");
        this.waveCharge = tag.getFloat("WaveCharge");

        mastery.clear();
        CompoundTag masteryTag = tag.getCompound("Mastery");
        for (String key : masteryTag.getAllKeys()) {
            Transformation form = parseEnum(Transformation.class, key, null);
            if (form != null) mastery.put(form, Math.min(100f, Math.max(0f, masteryTag.getFloat(key))));
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return name == null || name.isEmpty() ? fallback : Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // ---------------------------------------------------------------- stats

    public int getLevel() {
        return str + dex + con + wil + mnd;
    }

    /** Health pool: 20 at base CON, grows with effective CON (so transformations make you tankier). */
    public float getMaxBody() {
        return 20f + Math.max(0, getEffectiveCon() - 5) * 4f;
    }

    public float getBody() {
        return body < 0 ? getMaxBody() : Math.min(body, getMaxBody());
    }

    public void setBody(float body) {
        this.body = Math.max(0f, Math.min(getMaxBody(), body));
    }

    public float getMaxKi() {
        return 50.0f + (wil * 10.0f * race.getWilModifier());
    }

    private float formMultiplier() {
        return transformation.getStatMultiplier();
    }

    public int getEffectiveStr() { return (int) Math.max(1, str * race.getStrModifier() * formMultiplier()); }
    public int getEffectiveDex() { return (int) Math.max(1, dex * race.getDexModifier() * formMultiplier()); }
    public int getEffectiveCon() { return (int) Math.max(1, con * race.getConModifier() * formMultiplier()); }
    public int getEffectiveWil() { return (int) Math.max(1, wil * race.getWilModifier() * formMultiplier()); }
    public int getEffectiveMnd() { return (int) Math.max(1, mnd * race.getMndModifier()); }

    public long getBattlePower() {
        float kiRatio = getMaxKi() > 0 ? (currentKi / getMaxKi()) : 1.0f;
        float baseBP = (getEffectiveStr() * 10.0f) + (getEffectiveDex() * 8.0f) + (getEffectiveCon() * 10.0f)
                + (getEffectiveWil() * 12.0f) + (getEffectiveMnd() * 5.0f);
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

        switch (statName.toUpperCase()) {
            case "STR" -> str++;
            case "DEX" -> dex++;
            case "CON" -> con++;
            case "WIL" -> wil++;
            case "MND" -> mnd++;
            default -> { return false; }
        }
        trainingPoints -= cost;
        return true;
    }

    // ---------------------------------------------------------------- forms

    public float getMastery(Transformation form) {
        return mastery.getOrDefault(form, 0f);
    }

    public void addMastery(Transformation form, float amount) {
        if (form == Transformation.BASE) return;
        mastery.put(form, Math.min(100f, getMastery(form) + amount));
    }

    public void setMastery(Transformation form, float value) {
        mastery.put(form, Math.min(100f, Math.max(0f, value)));
    }

    /** Why a form is locked, or null when it can be used right now. */
    public String getLockReason(Transformation form) {
        if (form == Transformation.BASE) return null;
        if (!form.isAvailableTo(race)) return "No disponible para tu raza";
        if (getLevel() < form.getMinLevel()) return "Requiere nivel " + form.getMinLevel();
        Transformation parent = form.getParent();
        if (parent != Transformation.BASE && getMastery(parent) < Transformation.UNLOCK_MASTERY) {
            return "Domina " + parent.getDisplayName() + " al " + (int) Transformation.UNLOCK_MASTERY + "%";
        }
        if (form.needsTail() && !tailOut) return "Requiere la cola";
        return null;
    }

    public boolean isUnlocked(Transformation form) {
        return getLockReason(form) == null;
    }

    /** Ki per second the current form costs after mastery (up to 60% cheaper). */
    public float getCurrentDrainPerSecond() {
        return transformation.getKiDrainPerSecond() * (1f - 0.6f * getMastery(transformation) / 100f);
    }

    /** Seconds of holding the transform key, by form size and mastery. */
    public float getTransformTimeSeconds(Transformation target) {
        return target.getBaseTransformSeconds() * (1f - 0.75f * getMastery(target) / 100f);
    }

    // ---------------------------------------------------------------- getters / setters

    public Race getRace() { return race; }
    public void setRace(Race race) { this.race = race; }
    public boolean isRaceChosen() { return raceChosen; }
    public void setRaceChosen(boolean raceChosen) { this.raceChosen = raceChosen; }

    public Transformation getTransformation() { return transformation; }
    public void setTransformation(Transformation transformation) { this.transformation = transformation; }
    public Transformation getSelectedForm() { return selectedForm; }
    public void setSelectedForm(Transformation selectedForm) { this.selectedForm = selectedForm; }

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
    public boolean consumeKi(float amount) {
        if (currentKi < amount) return false;
        currentKi -= amount;
        return true;
    }

    public int getTrainingPoints() { return trainingPoints; }
    public void setTrainingPoints(int tp) { this.trainingPoints = Math.max(0, tp); }
    public void addTrainingPoints(int tp) { this.trainingPoints = Math.max(0, this.trainingPoints + tp); }

    public long getLastZenkaiTime() { return lastZenkaiTime; }
    public void setLastZenkaiTime(long time) { this.lastZenkaiTime = time; }

    public boolean isChargingKi() { return isChargingKi; }
    public void setChargingKi(boolean chargingKi) { isChargingKi = chargingKi; }
    public boolean isFlying() { return isFlying; }
    public void setFlying(boolean flying) { isFlying = flying; }
    public boolean isTailOut() { return tailOut; }
    public void setTailOut(boolean tailOut) { this.tailOut = tailOut; }

    public boolean isGuarding() { return guarding; }
    public void setGuarding(boolean guarding) { this.guarding = guarding; }
    public boolean isTransformHeld() { return transformHeld; }
    public void setTransformHeld(boolean held) { this.transformHeld = held; }
    public boolean isWaveHeld() { return waveHeld; }
    public void setWaveHeld(boolean held) { this.waveHeld = held; }
    public float getTransformCharge() { return transformCharge; }
    public void setTransformCharge(float charge) { this.transformCharge = Math.min(1f, Math.max(0f, charge)); }
    public float getWaveCharge() { return waveCharge; }
    public void setWaveCharge(float charge) { this.waveCharge = Math.min(1f, Math.max(0f, charge)); }
}
