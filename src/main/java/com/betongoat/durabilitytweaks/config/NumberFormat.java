package com.betongoat.durabilitytweaks.config;

public enum NumberFormat implements OptionEnum {
    /** Remaining durability points, e.g. {@code 432}. */
    POINTS,
    /** Remaining durability in percent, e.g. {@code 73}. */
    PERCENT;

    @Override
    public String key() {
        return "format." + name().toLowerCase();
    }
}
