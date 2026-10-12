package com.betongoat.durabilitytweaks.config;

/** A config enum that can be cycled through in the settings screen. */
public interface OptionEnum {
    /** Translation key suffix, appended to {@code durabilitytweaks.option.}. */
    String key();

    @SuppressWarnings("unchecked")
    static <E extends Enum<E>> E next(E value) {
        E[] values = (E[]) value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }
}
