package com.betongoat.durabilitytweaks.config;

public enum NumberFormat implements OptionEnum {
    /** Remaining durability in percent, e.g. {@code 73}. */
    PERCENT,
    /** Remaining durability points, e.g. {@code 432}. */
    POINTS;

    @Override
    public String key() {
        return "format." + name().toLowerCase();
    }
}
