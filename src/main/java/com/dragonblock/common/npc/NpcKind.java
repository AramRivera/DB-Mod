package com.dragonblock.common.npc;

/**
 * Characters of the world. Original designs on the classic archetypes of the genre: the old master on
 * his island, the genius inventor, the judge of souls, the master of the small heavenly planet.
 */
public enum NpcKind {
    MASTER("Maestro Kaido", 1.0f),
    SCIENTIST("Doctora Vela", 0.95f),
    JUDGE("El Gran Juez", 3.0f),
    CELESTIAL("Maestro Celeste", 1.05f);

    private final String displayName;
    private final float scale;

    NpcKind(String displayName, float scale) {
        this.displayName = displayName;
        this.scale = scale;
    }

    public String getDisplayName() { return displayName; }
    public float getScale() { return scale; }
    public String id() { return name().toLowerCase(); }

    public static NpcKind byId(String id) {
        for (NpcKind k : values()) if (k.id().equals(id)) return k;
        return MASTER;
    }
}
