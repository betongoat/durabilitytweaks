package com.betongoat.durabilitytweaks.config;

/** Which armor pieces show the durability number (from the four settings toggles). */
public record NumberMode(boolean helmet, boolean chestplate, boolean leggings, boolean boots) {
    public static final NumberMode OFF = new NumberMode(false, false, false, false);
    public static final NumberMode HELMET = new NumberMode(true, false, false, false);
    public static final NumberMode HELMET_AND_CHESTPLATE = new NumberMode(true, true, false, false);
    public static final NumberMode ALL = new NumberMode(true, true, true, true);

    /** Only this one piece. */
    public static NumberMode only(ArmorPiece piece) {
        return new NumberMode(piece == ArmorPiece.HELMET, piece == ArmorPiece.CHESTPLATE, piece == ArmorPiece.LEGGINGS, piece == ArmorPiece.BOOTS);
    }

    public boolean on(ArmorPiece piece) {
        return switch (piece) {
            case HELMET -> helmet;
            case CHESTPLATE -> chestplate;
            case LEGGINGS -> leggings;
            case BOOTS -> boots;
        };
    }

    public boolean any() {
        return helmet || chestplate || leggings || boots;
    }

    /** Numbers on leggings or boots need the armor's left leg un-mirrored, or they would read backwards there. */
    public boolean onLegs() {
        return leggings || boots;
    }
}
