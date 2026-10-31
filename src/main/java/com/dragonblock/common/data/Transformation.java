package com.dragonblock.common.data;

import java.util.EnumSet;
import java.util.Set;

/**
 * Every form in the mod. A form unlocks when the player reaches {@code minLevel}
 * and has mastered its {@code parent} to at least {@link #UNLOCK_MASTERY}.
 */
public enum Transformation {
    BASE("Forma Base", null, all(), 0, 1.0f, 0f, 0x9AB8FF, HairStyle.NONE, 0, AuraStyle.CALM, 0),

    // Universal
    KAIOKEN("Kaio-ken", BASE, all(), 30, 1.5f, 1.2f, 0xFF2A2A, HairStyle.NONE, 0, AuraStyle.FLAME, 0),
    KAIOKEN_X10("Kaio-ken x10", KAIOKEN, all(), 80, 2.6f, 3.0f, 0xFF1010, HairStyle.NONE, 0, AuraStyle.FLAME, 0),
    KAIOKEN_X20("Kaio-ken x20", KAIOKEN_X10, all(), 150, 3.6f, 6.0f, 0xE00000, HairStyle.NONE, 0, AuraStyle.STORM, 0),
    POTENTIAL_UNLEASHED("Potencial Liberado", BASE, of(Race.HUMAN, Race.NAMEKIAN, Race.HALF_SAIYAN), 220, 4.5f, 0.6f, 0xEDEDFF, HairStyle.NONE, 0, AuraStyle.CALM, 0),
    ULTRA_INSTINCT_SIGN("Instinto Superior (Presagio)", BASE, all(), 450, 7.0f, 6.0f, 0xB9D4FF, HairStyle.NONE, 0, AuraStyle.SILVER, 0),
    ULTRA_INSTINCT("Instinto Superior Dominado", ULTRA_INSTINCT_SIGN, all(), 600, 9.0f, 7.0f, 0xF2F6FF, HairStyle.CREST, 0xDCE4F0, AuraStyle.SILVER, 0),

    // Saiyan line
    SUPER_SAIYAN("Super Saiyajin", BASE, saiyans(), 40, 2.0f, 1.5f, 0xFFD23A, HairStyle.FLARE, 0xFFE15A, AuraStyle.FLAME, 0),
    SSJ_GRADE_2("Super Saiyajin Grado 2", SUPER_SAIYAN, saiyans(), 70, 2.25f, 2.5f, 0xFFC820, HairStyle.HEAVY, 0xFFD94A, AuraStyle.FLAME, 0),
    SSJ_GRADE_3("Super Saiyajin Grado 3", SSJ_GRADE_2, saiyans(), 95, 2.5f, 4.0f, 0xFFBE10, HairStyle.HEAVY, 0xFFD030, AuraStyle.FLAME, 0),
    SSJ_FULL_POWER("Super Saiyajin Pleno", SUPER_SAIYAN, saiyans(), 110, 2.2f, 0.0f, 0xFFE070, HairStyle.FLARE, 0xFFE878, AuraStyle.CALM, 0),
    SUPER_SAIYAN_2("Super Saiyajin 2", SUPER_SAIYAN, saiyans(), 130, 3.0f, 2.8f, 0xFFD23A, HairStyle.RIDGE, 0xFFE15A, AuraStyle.STORM, 0),
    SUPER_SAIYAN_3("Super Saiyajin 3", SUPER_SAIYAN_2, saiyans(), 200, 4.0f, 7.0f, 0xFFC020, HairStyle.MANE, 0xFFD838, AuraStyle.STORM, 0),
    OOZARU("Ozaru", BASE, saiyans(), 50, 3.0f, 1.0f, 0xC0602A, HairStyle.NONE, 0, AuraStyle.CALM, 0x5A3A22),
    GOLDEN_OOZARU("Ozaru Dorado", OOZARU, of(Race.SAIYAN), 260, 5.0f, 4.0f, 0xFFC830, HairStyle.NONE, 0, AuraStyle.FLAME, 0xE8B830),
    SUPER_SAIYAN_4("Super Saiyajin 4", GOLDEN_OOZARU, of(Race.SAIYAN), 320, 6.0f, 3.5f, 0xFF4030, HairStyle.WILD, 0x1A1418, AuraStyle.FLAME, 0xB02A30),
    SUPER_SAIYAN_GOD("Super Saiyajin Dios", SUPER_SAIYAN_3, saiyans(), 360, 6.0f, 2.0f, 0xFF5A3C, HairStyle.SLEEK, 0xD8304A, AuraStyle.FLAME, 0),
    SUPER_SAIYAN_BLUE("Super Saiyajin Blue", SUPER_SAIYAN_GOD, saiyans(), 450, 7.5f, 6.0f, 0x30C8FF, HairStyle.FLARE, 0x48D8FF, AuraStyle.FLAME, 0),
    SSJ_BLUE_EVOLVED("Super Saiyajin Blue Evolución", SUPER_SAIYAN_BLUE, saiyans(), 560, 8.5f, 6.5f, 0x2060FF, HairStyle.RIDGE, 0x2A6CF0, AuraStyle.STORM, 0),
    ULTRA_EGO("Ultra Ego", SUPER_SAIYAN_BLUE, of(Race.SAIYAN), 640, 9.5f, 5.0f, 0x9A3CFF, HairStyle.FLARE, 0x7A3AD0, AuraStyle.FLAME, 0),
    BEAST("Bestia", POTENTIAL_UNLEASHED, of(Race.HALF_SAIYAN), 640, 9.5f, 5.0f, 0xE0E8FF, HairStyle.SPIRE, 0xE6E9F2, AuraStyle.STORM, 0),

    // Arcosian line (base is their suppressed form)
    ARCOSIAN_SECOND("Segunda Forma", BASE, of(Race.ARCOSIAN), 40, 1.6f, 0.8f, 0xC070FF, HairStyle.NONE, 0, AuraStyle.CALM, 0),
    ARCOSIAN_THIRD("Tercera Forma", ARCOSIAN_SECOND, of(Race.ARCOSIAN), 80, 2.2f, 1.2f, 0xB060F0, HairStyle.NONE, 0, AuraStyle.FLAME, 0),
    ARCOSIAN_FINAL("Forma Final", ARCOSIAN_THIRD, of(Race.ARCOSIAN), 120, 3.0f, 0.0f, 0xD090FF, HairStyle.NONE, 0, AuraStyle.CALM, 0),
    ARCOSIAN_FULL_POWER("Forma Final 100%", ARCOSIAN_FINAL, of(Race.ARCOSIAN), 200, 4.2f, 4.0f, 0xE070FF, HairStyle.NONE, 0, AuraStyle.STORM, 0),
    ARCOSIAN_GOLDEN("Forma Dorada", ARCOSIAN_FINAL, of(Race.ARCOSIAN), 400, 7.0f, 6.0f, 0xFFD040, HairStyle.NONE, 0, AuraStyle.FLAME, 0),
    ARCOSIAN_BLACK("Forma Negra", ARCOSIAN_GOLDEN, of(Race.ARCOSIAN), 620, 9.5f, 5.5f, 0x8030C0, HairStyle.NONE, 0, AuraStyle.STORM, 0),

    // Namekian line
    NAMEKIAN_GIANT("Gigante Namekiano", BASE, of(Race.NAMEKIAN), 60, 2.0f, 1.5f, 0x80FF90, HairStyle.NONE, 0, AuraStyle.CALM, 0),
    NAMEKIAN_SYNC("Fusión Namekiana", BASE, of(Race.NAMEKIAN), 180, 3.8f, 1.0f, 0xB0FFB0, HairStyle.NONE, 0, AuraStyle.FLAME, 0),
    NAMEKIAN_ORANGE("Namekiano Naranja", NAMEKIAN_SYNC, of(Race.NAMEKIAN), 500, 8.0f, 5.0f, 0xFF8C30, HairStyle.NONE, 0, AuraStyle.STORM, 0),

    // Majin line
    MAJIN_SUPER("Majin Superior", BASE, of(Race.MAJIN), 60, 2.4f, 1.2f, 0xFF7AC0, HairStyle.NONE, 0, AuraStyle.FLAME, 0),
    MAJIN_PURE("Majin Puro", MAJIN_SUPER, of(Race.MAJIN), 260, 5.0f, 3.5f, 0xFF4AA0, HairStyle.NONE, 0, AuraStyle.STORM, 0);

    /** Parent mastery (%) needed before a child form unlocks. */
    public static final float UNLOCK_MASTERY = 25.0f;

    public enum HairStyle { NONE, FLARE, HEAVY, RIDGE, MANE, WILD, SLEEK, SPIRE, CREST }

    /** CALM = soft flame, FLAME = full aura, STORM = aura + lightning, SILVER = thin sparkling aura. */
    public enum AuraStyle { CALM, FLAME, STORM, SILVER }

    private final String displayName;
    private final Transformation parent;
    private final Set<Race> races;
    private final int minLevel;
    private final float statMultiplier;
    private final float kiDrainPerSecond;
    private final int auraColor;
    private final HairStyle hairStyle;
    private final int hairColor;
    private final AuraStyle auraStyle;
    private final int furColor;

    Transformation(String displayName, Transformation parent, Set<Race> races, int minLevel, float statMultiplier,
                   float kiDrainPerSecond, int auraColor, HairStyle hairStyle, int hairColor, AuraStyle auraStyle, int furColor) {
        this.displayName = displayName;
        this.parent = parent;
        this.races = races;
        this.minLevel = minLevel;
        this.statMultiplier = statMultiplier;
        this.kiDrainPerSecond = kiDrainPerSecond;
        this.auraColor = auraColor;
        this.hairStyle = hairStyle;
        this.hairColor = hairColor;
        this.auraStyle = auraStyle;
        this.furColor = furColor;
    }

    private static Set<Race> all() { return EnumSet.allOf(Race.class); }
    private static Set<Race> saiyans() { return EnumSet.of(Race.SAIYAN, Race.HALF_SAIYAN); }
    private static Set<Race> of(Race first, Race... rest) { return EnumSet.of(first, rest); }

    public String getDisplayName() { return displayName; }
    public Transformation getParent() { return parent; }
    public int getMinLevel() { return minLevel; }
    public float getStatMultiplier() { return statMultiplier; }
    public float getKiDrainPerSecond() { return kiDrainPerSecond; }
    public int getAuraColor() { return auraColor; }
    public HairStyle getHairStyle() { return hairStyle; }
    public int getHairColor() { return hairColor; }
    public AuraStyle getAuraStyle() { return auraStyle; }
    /** 0 = no fur. Ozaru forms cover the whole body, SSJ4 only the torso and arms. */
    public int getFurColor() { return furColor; }

    public boolean isAvailableTo(Race race) { return races.contains(race); }
    public boolean isKaioken() { return this == KAIOKEN || this == KAIOKEN_X10 || this == KAIOKEN_X20; }
    public boolean isOozaru() { return this == OOZARU || this == GOLDEN_OOZARU; }
    public boolean needsTail() { return isOozaru() || this == SUPER_SAIYAN_4; }
    /** Glowing hair is rendered full-bright. */
    public boolean hasGlowingHair() { return hairStyle != HairStyle.NONE && this != SUPER_SAIYAN_4 && this != SUPER_SAIYAN_GOD; }
    public boolean canFly() { return !isOozaru() && this != NAMEKIAN_GIANT; }

    /** How the power-up looks while the key is held and when it completes. */
    public enum Sequence {
        /** Quick golden flash. */ FLASH,
        /** Ground cracks, lightning, rising rocks. */ SURGE,
        /** Long, the world shakes, hair grows in. */ CATACLYSM,
        /** Calm: aura folds into the body, then a pillar of light. */ DIVINE,
        /** Body swells to giant size. */ GIANT,
        /** Red flare around the body. */ STRAIN
    }

    /** Particles the aura sheds while active. */
    public enum Particles { NONE, SPARKS, EMBERS, SPARKLES, DUST }

    public Sequence getSequence() {
        return switch (this) {
            case BASE -> Sequence.FLASH;
            case KAIOKEN, KAIOKEN_X10, KAIOKEN_X20 -> Sequence.STRAIN;
            case SUPER_SAIYAN_3, SUPER_SAIYAN_4, BEAST, NAMEKIAN_ORANGE, MAJIN_PURE, ARCOSIAN_BLACK, SSJ_BLUE_EVOLVED -> Sequence.CATACLYSM;
            case SUPER_SAIYAN_2, SSJ_GRADE_3, ARCOSIAN_FULL_POWER, ULTRA_EGO, MAJIN_SUPER, NAMEKIAN_SYNC -> Sequence.SURGE;
            case SUPER_SAIYAN_GOD, SUPER_SAIYAN_BLUE, ULTRA_INSTINCT_SIGN, ULTRA_INSTINCT, POTENTIAL_UNLEASHED,
                 ARCOSIAN_GOLDEN, SSJ_FULL_POWER -> Sequence.DIVINE;
            case OOZARU, GOLDEN_OOZARU, NAMEKIAN_GIANT -> Sequence.GIANT;
            default -> Sequence.FLASH;
        };
    }

    public Particles getParticles() {
        return switch (this) {
            case SUPER_SAIYAN_2, SUPER_SAIYAN_3, SSJ_BLUE_EVOLVED, BEAST, ARCOSIAN_FULL_POWER, ARCOSIAN_BLACK,
                 KAIOKEN_X20, MAJIN_PURE, NAMEKIAN_ORANGE -> Particles.SPARKS;
            case SUPER_SAIYAN_GOD, ULTRA_EGO, SUPER_SAIYAN_4, KAIOKEN, KAIOKEN_X10 -> Particles.EMBERS;
            case SUPER_SAIYAN_BLUE, ULTRA_INSTINCT_SIGN, ULTRA_INSTINCT, POTENTIAL_UNLEASHED, ARCOSIAN_GOLDEN -> Particles.SPARKLES;
            case SSJ_GRADE_2, SSJ_GRADE_3, OOZARU, GOLDEN_OOZARU, NAMEKIAN_GIANT -> Particles.DUST;
            default -> Particles.NONE;
        };
    }

    /** Lightning crackling over the body (0 = none). */
    public int getLightningColor() {
        return switch (this) {
            case SUPER_SAIYAN_2, SUPER_SAIYAN_3 -> 0xBFE8FF;
            case SSJ_BLUE_EVOLVED -> 0x9FC8FF;
            case BEAST -> 0xFF5070;
            case ARCOSIAN_FULL_POWER, ARCOSIAN_BLACK -> 0xE0A0FF;
            case MAJIN_PURE, NAMEKIAN_ORANGE, KAIOKEN_X20 -> 0xFFD0B0;
            default -> 0;
        };
    }

    /** Iris color drawn over the skin's eyes (0 = keep the skin). */
    public int getEyeColor() {
        return switch (this) {
            case SUPER_SAIYAN, SSJ_GRADE_2, SSJ_GRADE_3, SSJ_FULL_POWER, SUPER_SAIYAN_2, SUPER_SAIYAN_3 -> 0x2FD8C8;
            case SUPER_SAIYAN_GOD -> 0xE0304A;
            case SUPER_SAIYAN_BLUE, SSJ_BLUE_EVOLVED -> 0x30C8FF;
            case ULTRA_INSTINCT_SIGN, ULTRA_INSTINCT -> 0xD8E2F0;
            case ULTRA_EGO -> 0xB060FF;
            case BEAST, OOZARU, GOLDEN_OOZARU, KAIOKEN_X20 -> 0xFF2020;
            case SUPER_SAIYAN_4 -> 0xFFC020;
            case MAJIN_PURE, ARCOSIAN_BLACK -> 0xFF3070;
            default -> 0;
        };
    }

    /** Extra muscle mass on arms and torso. */
    public float getBulk() {
        return switch (this) {
            case SSJ_GRADE_2 -> 1.08f;
            case SSJ_GRADE_3 -> 1.2f;
            case ARCOSIAN_FULL_POWER -> 1.12f;
            case SUPER_SAIYAN_4, ULTRA_EGO -> 1.06f;
            default -> 1.0f;
        };
    }

    /** Aura proportions: (width, height) multipliers on top of the power-based size. */
    public float getAuraWidth() {
        return switch (auraStyle) {
            case SILVER -> 0.8f;
            case STORM -> 1.15f;
            default -> this == SUPER_SAIYAN_GOD ? 0.85f : 1.0f;
        };
    }

    public float getAuraHeight() {
        return switch (this) {
            case SUPER_SAIYAN_3, BEAST, ULTRA_EGO, ARCOSIAN_BLACK -> 1.25f;
            case SUPER_SAIYAN_GOD, ULTRA_INSTINCT_SIGN, ULTRA_INSTINCT -> 1.1f;
            default -> 1.0f;
        };
    }

    /** Seconds the power-up takes untrained; mastery cuts it to a quarter. Bigger forms take longer. */
    public float getBaseTransformSeconds() {
        float base = 1.4f + minLevel / 130f;
        return getSequence() == Sequence.CATACLYSM ? base * 1.4f : getSequence() == Sequence.STRAIN ? base * 0.5f : base;
    }

    /** One-line description of what makes the form play differently (shown in the forms tab). */
    public String getEssence() {
        return switch (this) {
            case BASE -> "Tu estado natural. Sin consumo de Ki.";
            case KAIOKEN, KAIOKEN_X10, KAIOKEN_X20 -> "Multiplica tu poder a costa de tu cuerpo: drena vida.";
            case POTENTIAL_UNLEASHED -> "Todo tu potencial sin transformación aparente. Consumo muy bajo.";
            case ULTRA_INSTINCT_SIGN -> "El cuerpo se mueve solo: 30% de esquivar cualquier golpe.";
            case ULTRA_INSTINCT -> "Esquiva el 55% de los golpes sin pensar.";
            case SUPER_SAIYAN -> "El primer despertar dorado. Equilibrado.";
            case SSJ_GRADE_2 -> "Más músculo: recibes un 10% menos de daño.";
            case SSJ_GRADE_3 -> "Músculo extremo: +25% golpe, -25% daño recibido, pero eres lento.";
            case SSJ_FULL_POWER -> "Dominio total: sin consumo, regenera Ki, técnicas más baratas.";
            case SUPER_SAIYAN_2 -> "Rayos constantes y +15% de daño de Ki.";
            case SUPER_SAIYAN_3 -> "El pelo crece hasta la cintura. +15% golpe, consumo muy alto.";
            case OOZARU -> "Gigante x4 bajo la luna llena. Golpe x1,5, no puede volar.";
            case GOLDEN_OOZARU -> "Gigante dorado. Ráfagas x1,6 y más resistencia.";
            case SUPER_SAIYAN_4 -> "Pelaje rojo y ojos dorados. Regenera el cuerpo.";
            case SUPER_SAIYAN_GOD -> "Energía divina serena: cura poco a poco, técnicas más baratas.";
            case SUPER_SAIYAN_BLUE -> "Control perfecto del Ki: técnicas un 30% más baratas.";
            case SSJ_BLUE_EVOLVED -> "Blue llevado al límite: técnicas baratas y +15% daño de Ki.";
            case ULTRA_EGO -> "Se fortalece cuanto más daño recibe.";
            case BEAST -> "Furia latente: 20% de golpes críticos x1,6.";
            case ARCOSIAN_SECOND, ARCOSIAN_THIRD -> "Libera parte del poder contenido.";
            case ARCOSIAN_FINAL -> "Tu forma verdadera: sin consumo y regenera Ki.";
            case ARCOSIAN_FULL_POWER -> "Todo el poder de la forma final, con músculo extra.";
            case ARCOSIAN_GOLDEN -> "Brillo dorado, poder enorme y consumo alto sin maestría.";
            case ARCOSIAN_BLACK -> "La cima arcosiana. Rayos violeta.";
            case NAMEKIAN_GIANT -> "Creces x3: golpe x1,5 y más resistencia, sin vuelo.";
            case NAMEKIAN_SYNC -> "Fusión de poder namekiano.";
            case NAMEKIAN_ORANGE -> "Despertar naranja: recibes un 25% menos de daño.";
            case MAJIN_SUPER -> "Majin endurecido.";
            case MAJIN_PURE -> "Caos puro: regenera el cuerpo rápidamente.";
        };
    }

    /** Body scale for size-changing forms. */
    public float getScale() {
        return switch (this) {
            case OOZARU, GOLDEN_OOZARU -> 4.0f;
            case NAMEKIAN_GIANT -> 3.0f;
            case ARCOSIAN_THIRD -> 1.15f;
            default -> 1.0f;
        };
    }
}
