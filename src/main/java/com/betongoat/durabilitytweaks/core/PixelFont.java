package com.betongoat.durabilitytweaks.core;

/** Tiny 3x5 bitmap font for painting numbers into armor textures. */
public final class PixelFont {
    public static final int GLYPH_WIDTH = 3;
    public static final int GLYPH_HEIGHT = 5;
    public static final int SPACING = 1;

    private static final String[] DIGITS = {
        "111101101101111", // 0
        "010110010010111", // 1
        "111001111100111", // 2
        "111001111001111", // 3
        "101101111001001", // 4
        "111100111001111", // 5
        "111100111101111", // 6
        "111001001001001", // 7
        "111101111101111", // 8
        "111101111001111", // 9
    };

    /** Width in font pixels of a digit string. */
    public static int width(String text) {
        return text.isEmpty() ? 0 : text.length() * (GLYPH_WIDTH + SPACING) - SPACING;
    }

    /** Whether the font pixel (x, y) of the given digit is set. */
    public static boolean pixel(char digit, int x, int y) {
        if (digit < '0' || digit > '9' || x < 0 || y < 0 || x >= GLYPH_WIDTH || y >= GLYPH_HEIGHT) {
            return false;
        }
        return DIGITS[digit - '0'].charAt(y * GLYPH_WIDTH + x) == '1';
    }

    /** Whether the font pixel (x, y) of a whole string is set. */
    public static boolean pixel(String text, int x, int y) {
        if (x < 0) {
            return false;
        }
        int index = x / (GLYPH_WIDTH + SPACING);
        int inGlyph = x % (GLYPH_WIDTH + SPACING);
        return index < text.length() && pixel(text.charAt(index), inGlyph, y);
    }

    private PixelFont() {
    }
}
