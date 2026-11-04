package com.dragonblock.common.data;

import com.dragonblock.common.data.Transformation.HairStyle;

import java.util.List;

/**
 * Character creator options per race: the body comes from the race, these palettes personalize it.
 * "Accent" is the race's second color: the clothes for humans and Saiyans, the arm patches of a
 * Namekian, the gems of an Arcosian, the trousers of a Majin.
 */
public final class Appearance {
    private Appearance() {}

    public static int[] skinPalette(Race race) {
        return switch (race) {
            case NAMEKIAN -> new int[]{0x6BBF4A, 0x5AA83E, 0x7FCB5E, 0x4E9A48, 0x8AC86A};
            case ARCOSIAN -> new int[]{0xF2EEF6, 0xD8D2EA, 0xF0D8E8, 0xB8C0D8, 0x9C98B8};
            case MAJIN -> new int[]{0xF59AC8, 0xE88AB8, 0xC8A0E8, 0x9AC0E8, 0xF0B090};
            default -> new int[]{0xF2CFA6, 0xE7B98A, 0xC98E5E, 0x9C6A44, 0x6E4A30};
        };
    }

    public static int[] accentPalette(Race race) {
        return switch (race) {
            case SAIYAN -> new int[]{0x16161C, 0x1E2A4A, 0x5A1A1A, 0x2E3A2A, 0x3A2A4A};
            case NAMEKIAN -> new int[]{0xE59AA4, 0xD98A6A, 0xC9A0D8, 0xE8C090, 0xA8D0E8};
            case ARCOSIAN -> new int[]{0x8A3CC8, 0x3C6CD8, 0x2EA060, 0xC83C4A, 0xD8A030};
            case MAJIN -> new int[]{0xF4F0F0, 0x2A2A30, 0x4A3C80, 0xE8D8A0, 0x8A2A40};
            default -> new int[]{0x2A4A9A, 0xD86A1E, 0x9A2A2A, 0x2E6A3A, 0x5A3A8A};
        };
    }

    public static final int[] EYES = {0x1A1A1A, 0x3A2A1E, 0x2A4A8A, 0x2E6A3A, 0xB02030, 0x6A3A8A, 0x9A7A2A};
    public static final int[] HAIR = {0x15120F, 0x3A2416, 0x6A4022, 0x8A2A20, 0x2A3A5A, 0xD8D8D8, 0x4A4A4A};

    public static int defaultSkin(Race race) { return skinPalette(race)[0]; }
    public static int defaultAccent(Race race) { return accentPalette(race)[0]; }
    public static int defaultEyes(Race race) { return race == Race.ARCOSIAN ? 0xB02030 : 0x1A1A1A; }
    public static int defaultHair(Race race) { return 0x15120F; }

    /** Styles the creator offers. Races without hair only get NONE. */
    public static List<HairStyle> hairStyles(Race race) {
        return switch (race) {
            case SAIYAN, HALF_SAIYAN -> List.of(HairStyle.WARRIOR, HairStyle.FLAME, HairStyle.SCRUFFY, HairStyle.SHORT, HairStyle.LONG, HairStyle.NONE);
            case HUMAN -> List.of(HairStyle.SHORT, HairStyle.SCRUFFY, HairStyle.LONG, HairStyle.WARRIOR, HairStyle.FLAME, HairStyle.NONE);
            default -> List.of(HairStyle.NONE);
        };
    }

    public static HairStyle defaultHairStyle(Race race) {
        return hairStyles(race).get(0);
    }

    public static boolean hasHair(Race race) {
        return defaultHairStyle(race) != HairStyle.NONE;
    }

    public static String hairStyleName(HairStyle style) {
        return switch (style) {
            case WARRIOR -> "Guerrero";
            case FLAME -> "Llama";
            case SCRUFFY -> "Rebelde";
            case SHORT -> "Corto";
            case LONG -> "Melena";
            case NONE -> "Sin pelo";
            default -> style.name();
        };
    }

    public static boolean inPalette(int[] palette, int color) {
        for (int c : palette) if (c == color) return true;
        return false;
    }
}
