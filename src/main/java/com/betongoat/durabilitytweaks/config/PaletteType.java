package com.betongoat.durabilitytweaks.config;

import com.betongoat.durabilitytweaks.core.Palette;

public enum PaletteType implements OptionEnum {
    /** Green, lime, yellow, orange, red, purple, blue - the look of the classic dura packs. */
    CLASSIC(Palette.CLASSIC),
    /** Smooth green to red gradient, ending in dark crimson. */
    GRADIENT(Palette.GRADIENT),
    /** Viridis-like palette that stays readable for colour blind players. */
    COLORBLIND(Palette.COLORBLIND),
    /** The player's own colours ({@link DurabilityConfig#customColors}). */
    CUSTOM(null);

    /** Fixed colours, null for {@link #CUSTOM}. */
    public final Palette palette;

    PaletteType(Palette palette) {
        this.palette = palette;
    }

    @Override
    public String key() {
        return "palette." + name().toLowerCase();
    }
}
