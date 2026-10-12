package com.betongoat.durabilitytweaks.config;

public enum ArmorPiece implements OptionEnum {
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS;

    @Override
    public String key() {
        return "piece." + name().toLowerCase();
    }
}
