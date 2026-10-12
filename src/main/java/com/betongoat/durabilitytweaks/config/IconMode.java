package com.betongoat.durabilitytweaks.config;

/** Where armor item icons get the durability colour. */
public enum IconMode implements OptionEnum {
    OFF,
    /** On the HUD only: hotbar and armor HUD mods, not in inventories. */
    HUD,
    /** Everywhere, inventories included. */
    EVERYWHERE;

    @Override
    public String key() {
        return "icons." + name().toLowerCase();
    }
}
