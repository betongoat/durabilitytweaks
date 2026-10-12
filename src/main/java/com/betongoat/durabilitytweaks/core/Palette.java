package com.betongoat.durabilitytweaks.core;

/**
 * Twelve colour slots, one per netherite durability band (see {@link DurabilityTier}).
 * Slot 0 is used just below 100%, slot 11 for the last 5%.
 */
public final class Palette {
    public static final int SLOTS = 12;

    public static final Palette CLASSIC = new Palette(new int[] {
        0x1FD62A, // 90-100  green
        0x6BE01F, // 80-90
        0xA8E61C, // 70-80   lime
        0xE3E81C, // 60-70
        0xFFD21F, // 50-60   yellow
        0xFFA41F, // 40-50
        0xFF7A1A, // 30-40   orange
        0xFF4719, // 20-30
        0xE01E1E, // 15-20   red
        0xD01E8C, // 10-15   magenta
        0x9B2BE8, //  5-10   purple
        0x2E5BFF, //  0-5    blue
    });

    public static final Palette GRADIENT = new Palette(gradient());

    public static final Palette COLORBLIND = new Palette(new int[] {
        0xFDE725, 0xC2DF23, 0x86D549, 0x52C569, 0x2AB07F, 0x1E9B8A,
        0x25858E, 0x2D708E, 0x38588C, 0x433E85, 0x482173, 0x440154,
    });

    private final int[] rgb;

    private Palette(int[] rgb) {
        if (rgb.length != SLOTS) {
            throw new IllegalArgumentException("A palette needs " + SLOTS + " colours");
        }
        this.rgb = rgb;
    }

    /** Opaque ARGB colour of the given slot. */
    public int color(int slot) {
        return 0xFF000000 | rgb[Math.max(0, Math.min(SLOTS - 1, slot))];
    }

    /** Green to red over the first nine slots, then darker reds for critical durability. */
    private static int[] gradient() {
        int[] colors = new int[SLOTS];
        for (int i = 0; i <= 8; i++) {
            colors[i] = hsv(120f * (1f - i / 8f), 0.85f, 1f);
        }
        colors[9] = 0xC2185B;
        colors[10] = 0x8E1238;
        colors[11] = 0x5C0A1E;
        return colors;
    }

    private static int hsv(float hueDegrees, float saturation, float value) {
        float h = (hueDegrees % 360f) / 60f;
        float c = value * saturation;
        float x = c * (1 - Math.abs(h % 2 - 1));
        float m = value - c;
        float r, g, b;
        switch ((int) h) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
