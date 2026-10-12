package com.betongoat.durabilitytweaks.core;

/**
 * UV layout of the vanilla humanoid armor textures (64x32 texels, scaled for HD packs).
 * Both the outer layer (helmet, chestplate, boots) and the leggings layer use the same layout.
 */
public final class ArmorLayout {
    public static final int TEXELS_WIDE = 64;
    public static final int TEXELS_HIGH = 32;

    /** One model cube, using Minecraft's box UV unwrapping. */
    public record Cube(int u, int v, int width, int height, int depth) {
        /** The six faces as {x, y, w, h} rectangles in texels. */
        public int[][] faces() {
            return new int[][] {
                {u + depth, v, width, depth},                          // top
                {u + depth + width, v, width, depth},                  // bottom
                {u, v + depth, depth, height},                         // right side
                {u + depth, v + depth, width, height},                 // front
                {u + depth + width, v + depth, depth, height},         // left side
                {u + depth + width + depth, v + depth, width, height}, // back
            };
        }

        public int[] front() {
            return faces()[3];
        }
    }

    public static final Cube HEAD = new Cube(0, 0, 8, 8, 8);
    public static final Cube HAT = new Cube(32, 0, 8, 8, 8);
    public static final Cube BODY = new Cube(16, 16, 8, 12, 4);
    public static final Cube ARM = new Cube(40, 16, 4, 12, 4);
    public static final Cube LEG = new Cube(0, 16, 4, 12, 4);

    public static final Cube[] CUBES = {HEAD, HAT, BODY, ARM, LEG};

    /** Which armor texture a resource path belongs to. */
    public enum Kind {
        /** Helmet, chestplate and boots. */
        OUTER,
        /** Leggings. */
        LEGGINGS
    }

    /**
     * Classifies an armor texture by its path, or returns null for anything that is not humanoid armor
     * (elytra, horse armor, baby armor, ...).
     */
    public static Kind classify(String path) {
        if (path.startsWith("textures/entity/equipment/humanoid/")) {
            return Kind.OUTER;
        }
        if (path.startsWith("textures/entity/equipment/humanoid_leggings/")) {
            return Kind.LEGGINGS;
        }
        // 1.21 - 1.21.1: textures/models/armor/<material>_layer_<1|2>[_overlay].png
        if (path.startsWith("textures/models/armor/")) {
            if (path.contains("_layer_1")) {
                return Kind.OUTER;
            }
            if (path.contains("_layer_2")) {
                return Kind.LEGGINGS;
            }
        }
        return null;
    }

    private ArmorLayout() {
    }
}
