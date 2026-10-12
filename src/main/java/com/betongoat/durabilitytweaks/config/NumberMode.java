package com.betongoat.durabilitytweaks.config;

/** Where the durability number is painted onto the armor. */
public enum NumberMode implements OptionEnum {
    OFF,
    HELMET,
    HELMET_AND_CHESTPLATE;

    public boolean onHelmet() {
        return this != OFF;
    }

    public boolean onChestplate() {
        return this == HELMET_AND_CHESTPLATE;
    }

    @Override
    public String key() {
        return "number." + name().toLowerCase();
    }
}
