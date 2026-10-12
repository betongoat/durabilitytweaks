package com.betongoat.durabilitytweaks.config;

/** Whose armor gets coloured. */
public enum ArmorOwners implements OptionEnum {
    EVERYONE,
    /** Only the armor you wear (third person, inventory). */
    ONLY_ME,
    /** Only other players, mobs and armor stands. */
    ONLY_OTHERS;

    public boolean includes(boolean own) {
        return this == EVERYONE || (this == ONLY_ME) == own;
    }

    @Override
    public String key() {
        return "owners." + name().toLowerCase();
    }
}
