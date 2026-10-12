package com.betongoat.durabilitytweaks.core;

/** Snapshot of how worn an armor piece is. */
public record DurabilityInfo(DurabilityTier tier, int remaining, int max) {

    public static DurabilityInfo of(DurabilityTier tier, int damage, int max) {
        return new DurabilityInfo(tier, Math.max(0, Math.min(max, max - damage)), max);
    }

    public double percentExact() {
        return max <= 0 ? 100.0 : remaining * 100.0 / max;
    }

    /** Percentage shown to the player: never rounds a working piece down to 0 or a damaged one up to 100. */
    public int percentDisplay() {
        if (remaining >= max) {
            return 100;
        }
        int percent = (int) Math.floor(percentExact());
        return remaining > 0 ? Math.max(1, Math.min(99, percent)) : 0;
    }

    public boolean isFull() {
        return remaining >= max;
    }

    /** Durability band, -1 for undamaged armor. */
    public int band() {
        return tier.band(percentExact());
    }
}
