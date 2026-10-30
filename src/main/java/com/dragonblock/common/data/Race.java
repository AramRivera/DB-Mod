package com.dragonblock.common.data;

public enum Race {
    HUMAN("Humano", 1.0f, 1.0f, 1.0f, 1.0f, 1.2f, "Raza adaptable de la Tierra con rápida regeneración de Ki."),
    SAIYAN("Saiyajin", 1.3f, 1.1f, 1.0f, 1.2f, 0.9f, "Guerreros del espacio con gran poder ofensivo y Zenkai."),
    HALF_SAIYAN("Híbrido Saiyajin", 1.2f, 1.1f, 1.1f, 1.1f, 1.1f, "Gran potencial latente y rápido aprendizaje de TP."),
    NAMEKIAN("Namekiano", 0.9f, 1.0f, 1.4f, 1.3f, 1.0f, "Sabios con regeneración natural continua de salud."),
    ARCOSIAN("Demonio del Frío", 1.1f, 1.3f, 1.1f, 1.1f, 1.0f, "Raza de Freezer, veloces y con bajo consumo de Ki en vuelo."),
    MAJIN("Majin", 1.2f, 0.9f, 1.5f, 1.1f, 0.8f, "Cuerpos resistentes con alta defensa física natural.");

    private final String displayName;
    private final float strModifier;
    private final float dexModifier;
    private final float conModifier;
    private final float wilModifier;
    private final float mndModifier;
    private final String description;

    Race(String displayName, float str, float dex, float con, float wil, float mnd, String description) {
        this.displayName = displayName;
        this.strModifier = str;
        this.dexModifier = dex;
        this.conModifier = con;
        this.wilModifier = wil;
        this.mndModifier = mnd;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getStrModifier() {
        return strModifier;
    }

    public float getDexModifier() {
        return dexModifier;
    }

    public float getConModifier() {
        return conModifier;
    }

    public float getWilModifier() {
        return wilModifier;
    }

    public float getMndModifier() {
        return mndModifier;
    }

    public String getDescription() {
        return description;
    }
}
