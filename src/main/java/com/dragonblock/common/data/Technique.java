package com.dragonblock.common.data;

/**
 * Ki techniques. Each one has its own way of being charged (the stance the body takes and where the
 * energy gathers) and of being released (the kind of attack). Learned with TP in the character sheet,
 * equipped into the bar on the right of the HUD (Ctrl + mouse wheel) and used by holding the technique key.
 */
public enum Technique {
    //            name                     kind            stance             color     charge  min   cost   dmg   TP    lvl  cd   sustain
    KI_BLAST("Ráfaga de Ki", Kind.BLAST, Stance.ONE_PALM, 0, 0f, 0f, 15, 1.0f, 0, 0, 8, 8,
            "Una esfera de ki rápida. Mantén la tecla para disparar en ráfaga."),
    KAMEHAMEHA("Kamehameha", Kind.BEAM, Stance.CUPPED_HIP, 0x4FC3FF, 2.5f, 0.25f, 60, 2.6f, 300, 15, 60, 30,
            "Las manos ahuecadas en la cadera reúnen el ki y lo liberan en una onda azul."),
    GALICK_GUN("Galick Ho", Kind.BEAM, Stance.GALICK, 0xB868FF, 2.0f, 0.25f, 55, 2.4f, 300, 15, 60, 26,
            "Torso girado y palmas atrás: una onda violeta disparada de golpe."),
    MASENKO("Masenko", Kind.BEAM, Stance.OVERHEAD, 0xFFE45A, 1.5f, 0.25f, 45, 2.0f, 200, 10, 40, 20,
            "Las manos sobre la frente cargan un destello amarillo rápido."),
    FINAL_FLASH("Final Flash", Kind.BEAM, Stance.WIDE_ARMS, 0xFFF27A, 4.0f, 0.4f, 120, 4.0f, 900, 60, 140, 40,
            "Brazos abiertos de par en par; al juntarlos, un rayo enorme lo arrasa todo."),
    MAKANKOSAPPO("Makankosappo", Kind.PIERCE, Stance.FOREHEAD, 0xFFD24A, 3.0f, 0.5f, 70, 3.4f, 600, 40, 100, 16,
            "Dos dedos en la frente; el rayo en espiral atraviesa todo lo que encuentra."),
    DEATH_BEAM("Rayo Mortal", Kind.PIERCE, Stance.FINGER, 0xFF5AC8, 0f, 0f, 20, 1.4f, 250, 20, 14, 8,
            "Un rayo fino y veloz disparado con un solo dedo."),
    KIENZAN("Kienzan", Kind.DISC, Stance.RAISED_ONE, 0xFFE878, 1.5f, 0.5f, 50, 3.0f, 450, 30, 80, 10,
            "Un disco giratorio sobre la mano que corta a través de los enemigos."),
    GENKIDAMA("Genkidama", Kind.BOMB, Stance.RAISED_BOTH, 0x9ADFFF, 7.0f, 0.3f, 200, 7.0f, 1500, 90, 400, 20,
            "Ambos brazos al cielo reúnen la energía de todo ser vivo en una esfera colosal."),
    BIG_BANG("Big Bang Attack", Kind.BALL, Stance.ONE_PALM, 0x7AB8FF, 1.2f, 0.3f, 70, 3.0f, 700, 45, 90, 14,
            "Una sola palma extendida lanza una esfera devastadora."),
    KIAI("Kiai", Kind.SHOCKWAVE, Stance.TENSE, 0, 0.8f, 0.2f, 35, 1.2f, 150, 5, 50, 10,
            "Un grito de ki que empuja a todos los que te rodean."),
    TAIYOKEN("Taiyoken", Kind.FLASH, Stance.FACE, 0xFFFFF0, 0.5f, 1f, 30, 0f, 200, 10, 300, 10,
            "Un destello cegador que deja sin vista a quien te esté mirando."),
    TELEPORT("Teletransportación", Kind.TELEPORT, Stance.FOREHEAD, 0xE8F0FF, 0.75f, 1f, 40, 0f, 800, 50, 100, 6,
            "Dos dedos en la frente: apareces detrás de quien miras, o donde apuntes.");

    /** What happens on release. */
    public enum Kind { BLAST, BEAM, PIERCE, DISC, BOMB, BALL, SHOCKWAVE, FLASH, TELEPORT }

    /** How the body charges it, which also decides where the energy gathers. */
    public enum Stance { ONE_PALM, CUPPED_HIP, GALICK, OVERHEAD, WIDE_ARMS, FOREHEAD, FINGER, RAISED_ONE, RAISED_BOTH, TENSE, FACE }

    public static final int MAX_EQUIPPED = 8;

    private final String displayName;
    private final Kind kind;
    private final Stance stance;
    private final int color;
    private final int chargeTicks;
    private final float minCharge;
    private final float kiCost;
    private final float damage;
    private final int tpCost;
    private final int minLevel;
    private final int cooldown;
    private final int sustain;
    private final String description;

    Technique(String displayName, Kind kind, Stance stance, int color, float chargeSeconds, float minCharge, float kiCost,
              float damage, int tpCost, int minLevel, int cooldown, int sustain, String description) {
        this.displayName = displayName;
        this.kind = kind;
        this.stance = stance;
        this.color = color;
        this.chargeTicks = Math.round(chargeSeconds * 20);
        this.minCharge = minCharge;
        this.kiCost = kiCost;
        this.damage = damage;
        this.tpCost = tpCost;
        this.minLevel = minLevel;
        this.cooldown = cooldown;
        this.sustain = sustain;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Kind getKind() { return kind; }
    public Stance getStance() { return stance; }
    /** Fixed color of the technique, or 0 to use the aura color of the current form. */
    public int getColor() { return color; }
    public int colorFor(Transformation form) { return color != 0 ? color : form.getAuraColor(); }
    /** Ticks from zero to full charge; 0 means it fires the moment the key is pressed. */
    public int getChargeTicks() { return chargeTicks; }
    public boolean isInstant() { return chargeTicks == 0; }
    /** Charge needed to fire on release; below it the technique fizzles. */
    public float getMinCharge() { return minCharge; }
    /** Ki spent: on firing for instant techniques, spread over the charge for the rest. */
    public float getKiCost() { return kiCost; }
    public float getDamage() { return damage; }
    public int getTpCost() { return tpCost; }
    public int getMinLevel() { return minLevel; }
    public int getCooldown() { return cooldown; }
    /** Ticks the release pose is held (and a beam stays connected to the hands). */
    public int getSustain() { return sustain; }
    public String getDescription() { return description; }

    /**
     * How much bigger and heavier techniques get with the user's power: 1 in base form, around 1.15 as
     * Super Saiyajin, about 2.3 in the top forms, plus a little from battle power. Size, explosion and the
     * charge visuals use it (damage already grows through the form's stats).
     */
    public static float powerScale(PlayerDBCData d) {
        float bp = (float) Math.log10(Math.max(100, d.getBattlePower()) / 100.0);
        return Math.min(2.6f, Math.max(1f, 0.85f + 0.15f * d.getTransformation().getStatMultiplier() + 0.08f * bp));
    }
}
