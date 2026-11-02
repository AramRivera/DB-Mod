package com.dragonblock.common.entity;

/**
 * Hostile fighters of the world. Original designs on the genre's archetypes: a desert bandit, the
 * armored soldiers of a galactic empire, a combat android, and the emperor himself, who transforms.
 */
public enum EnemyKind {
    //          name                         hp   dmg  speed armor  TP   ki color  ki dmg  scale
    BANDIT("Bandido del Desierto", 30, 4, 0.30, 2, 20, 0, 0, 1.0f),
    SOLDIER("Soldado del Imperio", 40, 5, 0.28, 6, 45, 0xC070FF, 6, 1.0f),
    ANDROID("Androide de Combate", 90, 9, 0.30, 10, 140, 0xFF3030, 10, 1.05f),
    EMPEROR("Kryos, Emperador Galáctico", 500, 12, 0.32, 12, 1500, 0xB040FF, 16, 1.15f);

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
}
