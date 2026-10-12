package com.betongoat.durabilitytweaks.config;

public enum ArmorFilter implements OptionEnum {
    /** Every armor piece that can lose durability. */
    ALL,
    /** Only diamond and netherite armor, like the original dura packs. */
    DIAMOND_NETHERITE;

    @Override
    public String key() {
        return "filter." + name().toLowerCase();
    }
}
