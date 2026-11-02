package com.dragonblock.common.npc;

/**
 * Characters of the world. Original designs on the classic archetypes of the genre: the old master on
 * his island, the genius inventor, the judge of souls, the master of the small heavenly planet, the
 * tournament host, the guardian of the sky palace, the ancient elder of the green planet and his people.
 */
public enum NpcKind {
    MASTER("Maestro Kaido", 1.0f, 40),
    SCIENTIST("Doctora Vela", 0.95f, 40),
    JUDGE("El Gran Juez", 3.0f, 0),
    CELESTIAL("Maestro Celeste", 1.05f, 48),
    /** Runs the tournament; no calm ring around him, the arena is for fighting. */
    ANNOUNCER("El Presentador", 1.0f, 0),
    GUARDIAN("El Guardián", 1.1f, 48),
    ELDER("Gran Anciano Tsuru", 2.0f, 48),
    VILLAGER("Aldeano namekiano", 1.0f, 32);

    private final String displayName;
    private final float scale;
    /** Blocks around this character where players can't hurt each other or break the place (0: none). */
    private final int safeRadius;

    NpcKind(String displayName, float scale, int safeRadius) {
        this.displayName = displayName;
        this.scale = scale;
        this.safeRadius = safeRadius;
    }

    public String getDisplayName() { return displayName; }
    public float getScale() { return scale; }
    public int getSafeRadius() { return safeRadius; }
    /** Blocks around this character only operators can change: the safe ring, or the whole arena for the host. */
    public int getGuardRadius() { return this == ANNOUNCER ? 40 : safeRadius; }
    public String id() { return name().toLowerCase(); }

    public static NpcKind byId(String id) {
        for (NpcKind k : values()) if (k.id().equals(id)) return k;
        return MASTER;
    }
}
