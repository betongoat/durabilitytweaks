package com.betongoat.durabilitytweaks.core;

/**
 * Durability bands. A band is the range of remaining durability that shares one colour.
 *
 * <ul>
 *   <li>Netherite: a new colour every 10% from 100% down to 20%, then every 5%.</li>
 *   <li>Diamond (and every other armor): every 10% from 100% down to 10%,
 *       plus two extra colours for intense PvP (10-5% and 5-0%).</li>
 * </ul>
 */
public enum DurabilityTier {
    NETHERITE(
        new int[] {90, 80, 70, 60, 50, 40, 30, 20, 15, 10, 5, 0},
        new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}),
    DIAMOND(
        new int[] {90, 80, 70, 60, 50, 40, 30, 20, 10, 5, 0},
        new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11});

    /** Lower bound (inclusive, in percent) of every band, highest band first. */
    private final int[] lowerBounds;
    /** Palette slot used by every band. */
    private final int[] paletteSlots;

    DurabilityTier(int[] lowerBounds, int[] paletteSlots) {
        this.lowerBounds = lowerBounds;
        this.paletteSlots = paletteSlots;
    }

    public int bandCount() {
        return lowerBounds.length;
    }

    /** Band for the remaining durability in percent, or -1 when the armor is undamaged. */
    public int band(double percentRemaining) {
        if (percentRemaining >= 100.0) {
            return -1;
        }
        for (int i = 0; i < lowerBounds.length; i++) {
            if (percentRemaining >= lowerBounds[i]) {
                return i;
            }
        }
        return lowerBounds.length - 1;
    }

    public int lowerBound(int band) {
        return lowerBounds[band];
    }

    public int upperBound(int band) {
        return band == 0 ? 100 : lowerBounds[band - 1];
    }

    public int paletteSlot(int band) {
        return paletteSlots[band];
    }
}
