package com.betongoat.durabilitytweaks.config;

public enum NumberColor implements OptionEnum {
    /** Light grey on fully coloured armor, the durability colour on outline / plain armor. */
    AUTO(0),
    WHITE(0xFFFFFFFF),
    DURABILITY(0),
    BLACK(0xFF1E1E1E),
    YELLOW(0xFFFFE14D);

    public final int argb;

    NumberColor(int argb) {
        this.argb = argb;
    }

    @Override
    public String key() {
        return "color." + name().toLowerCase();
    }
}
