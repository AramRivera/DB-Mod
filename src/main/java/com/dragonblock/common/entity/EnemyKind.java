package com.dragonblock.common.entity;

/**
 * Hostile fighters of the world. Original designs on the genre's archetypes: a desert bandit, the
 * armored soldiers of a galactic empire, a combat android, the emperor himself, who transforms, the
 * empire's sprouted foot soldiers, dinosaurs, an invader with a tail and a regenerating pink demon.
 */
public enum EnemyKind {
    //          name                         hp   dmg  speed armor  TP   ki color  ki dmg  scale
    BANDIT("Bandido del Desierto", 30, 4, 0.30, 2, 20, 0, 0, 1.0f),
    SOLDIER("Soldado del Imperio", 40, 5, 0.28, 6, 45, 0xC070FF, 6, 1.0f),
    ANDROID("Androide de Combate", 90, 9, 0.30, 10, 140, 0xFF3030, 10, 1.05f),
    EMPEROR("Kryos, Emperador Galáctico", 500, 12, 0.32, 12, 1500, 0xB040FF, 16, 1.15f),
    /** Small, fast, leaps at you and, when beaten down, clings on and blows itself up. */
    SPROUT("Germinado", 26, 4, 0.34, 2, 30, 0, 0, 0.8f),
    /** Earth's giant lizards: no ki, but a bite that hurts. */
    DINO("Dinosaurio", 70, 8, 0.27, 4, 60, 0, 0, 1.0f),
    /** A warrior of a conquering race, with a tail: under the full moon he becomes a giant ape. */
    RAIDER("Invasor Saiyajin", 80, 8, 0.32, 8, 150, 0xFFD040, 10, 1.0f),
    /** A round pink demon that keeps regenerating and sheds smaller copies of itself. */
    MAJIN("Majin Glotón", 900, 14, 0.29, 6, 2500, 0xFF70C0, 14, 1.3f),
    MAJIN_SPAWN("Pequeño Majin", 40, 5, 0.33, 2, 40, 0xFF70C0, 5, 0.6f);

    private final String displayName;
    private final double health, damage, speed, armor;
    private final int tp, kiColor;
    private final float kiDamage, scale;

    EnemyKind(String displayName, double health, double damage, double speed, double armor, int tp, int kiColor, float kiDamage, float scale) {
        this.displayName = displayName;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.tp = tp;
        this.kiColor = kiColor;
        this.kiDamage = kiDamage;
        this.scale = scale;
    }

    public String getDisplayName() { return displayName; }
    public double getHealth() { return health; }
    public double getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public double getArmor() { return armor; }
    /** Training points for whoever lands the final blow. */
    public int getTp() { return tp; }
    /** Ki blast color, 0 for fighters who only punch. */
    public int getKiColor() { return kiColor; }
    public float getKiDamage() { return kiDamage; }
    public float getScale() { return scale; }
    public boolean shoots() { return kiColor != 0; }
    public String id() { return name().toLowerCase(); }
    /** Fights with a boss bar (the invader only once he has become the ape). */
    public boolean isBoss() { return this == EMPEROR || this == MAJIN || this == RAIDER; }
}
