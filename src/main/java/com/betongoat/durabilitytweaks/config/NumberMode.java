package com.betongoat.durabilitytweaks.config;

/** Which armor pieces show the durability number (derived from the two settings toggles). */
public enum NumberMode implements OptionEnum {
    OFF(false, false),
    HELMET(true, false),
    CHESTPLATE(false, true),
    HELMET_AND_CHESTPLATE(true, true);

    private final boolean helmet;
    private final boolean chestplate;

    NumberMode(boolean helmet, boolean chestplate) {
        this.helmet = helmet;
        this.chestplate = chestplate;
    }

    public static NumberMode of(boolean helmet, boolean chestplate) {
        for (NumberMode mode : values()) {
            if (mode.helmet == helmet && mode.chestplate == chestplate) {
                return mode;
            }
        }
        return OFF;
    }

    public boolean onHelmet() {
        return helmet;
    }

    public boolean onChestplate() {
        return chestplate;
    }

    public boolean any() {
        return helmet || chestplate;
    }

    @Override
    public String key() {
        return "number." + name().toLowerCase();
    }
}
