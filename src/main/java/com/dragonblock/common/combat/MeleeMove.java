package com.dragonblock.common.combat;

/**
 * The bare-handed moveset. Light presses run the chain JAB → CROSS → KICK; a heavy press is a HAMMER on
 * its own or a LAUNCHER at the end of the chain. Timings are in ticks and shared with the client, which
 * animates each move over exactly these windows (see DBPlayerModel).
 */
public enum MeleeMove {
    //        startup active recovery  damage range  knock  lift  stun  ki   guardBreak
    JAB(3, 2, 6, 1.0f, 2.6f, 0.25f, 0.05f, 8, 0f, false),
    CROSS(3, 2, 6, 1.1f, 2.6f, 0.30f, 0.05f, 8, 0f, false),
    KICK(4, 3, 9, 1.35f, 2.9f, 0.60f, 0.10f, 12, 0f, false),
    HAMMER(9, 3, 14, 2.2f, 3.0f, 1.20f, 0.20f, 18, 6f, true),
    LAUNCHER(6, 3, 12, 1.8f, 3.0f, 0.30f, 0.95f, 24, 6f, false);

    public final int startup, active, recovery;
    /** Multiplier on the fighter's normal punch. */
    public final float damage;
    /** Blocks from the eyes to the closest point of the target. */
    public final float range;
    public final float knockback, lift;
    /** Ticks the target can't attack or guard after being hit (players only). */
    public final int stun;
    public final float kiCost;
    public final boolean guardBreak;

    MeleeMove(int startup, int active, int recovery, float damage, float range, float knockback, float lift, int stun,
              float kiCost, boolean guardBreak) {
        this.startup = startup;
        this.active = active;
        this.recovery = recovery;
        this.damage = damage;
        this.range = range;
        this.knockback = knockback;
        this.lift = lift;
        this.stun = stun;
        this.kiCost = kiCost;
        this.guardBreak = guardBreak;
    }

    public int total() { return startup + active + recovery; }

    /** From this tick the next press may cut the recovery short and start the follow-up. */
    public int linkFrom() { return startup + active + recovery / 2; }

    /** What a press does after this move: the next link, or null when the chain is over. */
    public MeleeMove next(boolean heavy) {
        return switch (this) {
            case JAB -> heavy ? HAMMER : CROSS;
            case CROSS -> heavy ? HAMMER : KICK;
            case KICK -> heavy ? LAUNCHER : null;
            default -> null;
        };
    }

    public static MeleeMove opener(boolean heavy) { return heavy ? HAMMER : JAB; }
}
