package com.dragonblock.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player visual preferences, saved in config/dragonblock-client.toml. */
public final class ClientConfig {
    public enum AuraVisibility { ALL, HIDE_MINE, HIDE_ALL }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<AuraVisibility> AURA;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        AURA = b.comment("Which ki auras are drawn. Cosmetic only, never affects gameplay.")
                .defineEnum("auraVisibility", AuraVisibility.ALL);
        SPEC = b.build();
    }

    private ClientConfig() {}

    public static boolean showAura(boolean isLocalPlayer) {
        return switch (AURA.get()) {
            case ALL -> true;
            case HIDE_MINE -> !isLocalPlayer;
            case HIDE_ALL -> false;
        };
    }

    public static AuraVisibility cycleAura() {
        AuraVisibility next = AuraVisibility.values()[(AURA.get().ordinal() + 1) % AuraVisibility.values().length];
        AURA.set(next);
        AURA.save();
        return next;
    }
}
