package com.betongoat.durabilitytweaks.core;

import com.betongoat.durabilitytweaks.config.ColorStyle;

/**
 * Builds the durability variant of an armor texture. Pure pixel maths on ARGB arrays,
 * so it works the same on every Minecraft version (and with any resource pack).
 */
public final class TextureProcessor {
    /** Textures are upscaled to at least this width when borders or numbers need sub-texel detail. */
    private static final int DETAIL_WIDTH = 256;
    private static final int TEXT_SHADOW = 0xFF141414;
    private static final int WHITE = 0xFFFFFFFF;

    /**
     * @param style        how the armor itself is coloured
     * @param color        durability colour (opaque ARGB)
     * @param outlineTexels border width for {@link ColorStyle#OUTLINE}, in armor texels
     * @param text         number to paint, or null
     * @param textOnHead   paint the number on the helmet's forehead
     * @param textOnChest  paint the number on the chestplate
     */
    public record Request(ColorStyle style, int color, double outlineTexels, String text, boolean textOnHead, boolean textOnChest) {
        public boolean hasText() {
            return text != null && (textOnHead || textOnChest);
        }

        /** Number colour: white on fully coloured armor, the durability colour otherwise. */
        public int textColor() {
            return style == ColorStyle.FULL ? WHITE : color;
        }

        /** Whether this request changes the texture at all. */
        public boolean isNoop() {
            return style == ColorStyle.NONE && !hasText();
        }
    }

    public record Image(int[] argb, int width, int height) {
    }

    public static Image process(int[] source, int width, int height, Request request) {
        boolean detail = request.style() == ColorStyle.OUTLINE || request.hasText();
        int factor = detail ? Math.max(1, (DETAIL_WIDTH + width - 1) / width) : 1;
        int w = width * factor;
        int h = height * factor;
        int[] out = upscale(source, width, height, factor);
        double texel = w / (double) ArmorLayout.TEXELS_WIDE;

        switch (request.style()) {
            case FULL -> recolor(out, source, request.color());
            case OUTLINE -> outline(out, w, h, texel, Math.max(1, (int) Math.round(request.outlineTexels() * texel)), request.color());
            case NONE -> { }
        }

        if (request.hasText()) {
            if (request.textOnHead()) {
                drawNumber(out, w, h, scaled(ArmorLayout.HEAD.front(), texel), request.text(), request.textColor(), true);
            }
            if (request.textOnChest()) {
                drawNumber(out, w, h, scaled(ArmorLayout.BODY.front(), texel), request.text(), request.textColor(), false);
            }
        }
        return new Image(out, w, h);
    }

    private static int[] upscale(int[] source, int width, int height, int factor) {
        if (factor == 1) {
            return source.clone();
        }
        int w = width * factor;
        int[] out = new int[w * height * factor];
        for (int y = 0; y < height * factor; y++) {
            int row = (y / factor) * width;
            for (int x = 0; x < w; x++) {
                out[y * w + x] = source[row + x / factor];
            }
        }
        return out;
    }

    /**
     * Replaces the hue of every pixel with the durability colour, keeping the original shading.
     * Brightness is normalised around the texture's average, so dark netherite and light diamond
     * end up equally vivid.
     */
    private static void recolor(int[] pixels, int[] source, int color) {
        double sum = 0, sumSq = 0;
        int count = 0;
        for (int p : source) {
            if (alpha(p) > 0) {
                double l = luminance(p);
                sum += l;
                sumSq += l * l;
                count++;
            }
        }
        double mean = count == 0 ? 0.5 : sum / count;
        double std = count == 0 ? 0 : Math.sqrt(Math.max(0, sumSq / count - mean * mean));
        int cr = (color >> 16) & 0xFF, cg = (color >> 8) & 0xFF, cb = color & 0xFF;
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            if (alpha(p) == 0) {
                continue;
            }
            double z = std < 0.01 ? 0 : (luminance(p) - mean) / std;
            double shade = Math.max(0.35, Math.min(1.0, 0.82 + z * 0.15));
            double highlight = z > 1.2 ? Math.min(0.35, (z - 1.2) * 0.3) : 0;
            int r = channel(cr * shade, highlight);
            int g = channel(cg * shade, highlight);
            int b = channel(cb * shade, highlight);
            pixels[i] = (p & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
    }

    private static int channel(double value, double towardsWhite) {
        return (int) Math.round(Math.max(0, Math.min(255, value + (255 - value) * towardsWhite)));
    }

    /** Paints a border along the edge of every model face and around holes (e.g. the helmet's visor). */
    private static void outline(int[] pixels, int w, int h, double texel, int thickness, int color) {
        boolean[] opaque = new boolean[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            opaque[i] = alpha(pixels[i]) > 0;
        }
        for (ArmorLayout.Cube cube : ArmorLayout.CUBES) {
            for (int[] face : cube.faces()) {
                int[] r = scaled(face, texel);
                int x0 = Math.max(0, r[0]), y0 = Math.max(0, r[1]);
                int x1 = Math.min(w, r[0] + r[2]), y1 = Math.min(h, r[1] + r[3]);
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        if (opaque[y * w + x] && isBorder(opaque, w, x, y, x0, y0, x1, y1, thickness)) {
                            pixels[y * w + x] = color;
                        }
                    }
                }
            }
        }
    }

    private static boolean isBorder(boolean[] opaque, int w, int x, int y, int x0, int y0, int x1, int y1, int t) {
        if (x - x0 < t || x1 - 1 - x < t || y - y0 < t || y1 - 1 - y < t) {
            return true;
        }
        for (int dy = -t; dy <= t; dy++) {
            for (int dx = -t; dx <= t; dx++) {
                if (!opaque[(y + dy) * w + x + dx]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Paints a number centred on a face, with a dark shadow ring so it stays readable on any colour.
     *
     * @param top place it near the top of the face (helmet forehead) instead of the upper middle (chest)
     */
    private static void drawNumber(int[] pixels, int w, int h, int[] face, String text, int color, boolean top) {
        // Size the digits as if there were at least two, so "7" is not drawn bigger than "75"
        int fontWidth = PixelFont.width(text);
        int sizingWidth = Math.max(fontWidth, PixelFont.width("00"));
        int maxHeight = top ? face[3] / 2 : face[3] * 3 / 5;
        int scale = 1;
        while (sizingWidth * (scale + 1) + 2 <= face[2] - 2 && PixelFont.GLYPH_HEIGHT * (scale + 1) + 2 <= maxHeight) {
            scale++;
        }
        int textWidth = fontWidth * scale;
        int textHeight = PixelFont.GLYPH_HEIGHT * scale;
        int x0 = face[0] + (face[2] - textWidth) / 2;
        int y0 = top
            ? face[1] + Math.max(1, face[3] / 16)
            : face[1] + (face[3] * 2 / 5) - textHeight / 2;

        // Shadow ring first, then the glyphs on top of it.
        for (int y = -1; y <= textHeight; y++) {
            for (int x = -1; x <= textWidth; x++) {
                if (!lit(text, x, y, scale) && nearLit(text, x, y, scale)) {
                    set(pixels, w, h, x0 + x, y0 + y, TEXT_SHADOW);
                }
            }
        }
        for (int y = 0; y < textHeight; y++) {
            for (int x = 0; x < textWidth; x++) {
                if (lit(text, x, y, scale)) {
                    set(pixels, w, h, x0 + x, y0 + y, color);
                }
            }
        }
    }

    private static boolean lit(String text, int x, int y, int scale) {
        return x >= 0 && y >= 0 && PixelFont.pixel(text, x / scale, y / scale);
    }

    private static boolean nearLit(String text, int x, int y, int scale) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (lit(text, x + dx, y + dy, scale)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void set(int[] pixels, int w, int h, int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < w && y < h) {
            pixels[y * w + x] = color;
        }
    }

    private static int[] scaled(int[] rect, double texel) {
        int x = (int) Math.round(rect[0] * texel);
        int y = (int) Math.round(rect[1] * texel);
        int x1 = (int) Math.round((rect[0] + rect[2]) * texel);
        int y1 = (int) Math.round((rect[1] + rect[3]) * texel);
        return new int[] {x, y, x1 - x, y1 - y};
    }

    private static int alpha(int argb) {
        return argb >>> 24;
    }

    private static double luminance(int argb) {
        return (0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF)) / 255.0;
    }

    private TextureProcessor() {
    }
}
