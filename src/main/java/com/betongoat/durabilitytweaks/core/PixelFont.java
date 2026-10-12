package com.betongoat.durabilitytweaks.core;

/** 5x7 digits in the style of Minecraft's own font, for painting numbers into armor textures. */
public final class PixelFont {
    public static final int GLYPH_WIDTH = 5;
    public static final int GLYPH_HEIGHT = 7;
    public static final int SPACING = 1;

    private static final String[][] DIGITS = {
        {".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."}, // 0
        {"..#..", ".##..", "..#..", "..#..", "..#..", "..#..", "#####"}, // 1
        {".###.", "#...#", "....#", "..##.", ".#...", "#...#", "#####"}, // 2
        {".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."}, // 3
        {"...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."}, // 4
        {"#####", "#....", "####.", "....#", "....#", "#...#", ".###."}, // 5
        {"..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."}, // 6
        {"#####", "#...#", "....#", "...#.", "..#..", "..#..", "..#.."}, // 7
        {".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."}, // 8
        {".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."}, // 9
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
        return DIGITS[digit - '0'][y].charAt(x) == '#';
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
