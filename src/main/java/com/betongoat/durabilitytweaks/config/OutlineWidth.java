package com.betongoat.durabilitytweaks.config;

public enum OutlineWidth implements OptionEnum {
    /** Half an armor pixel. */
    THIN(0.5),
    /** One full armor pixel. */
    THICK(1.0);

    /** Width of the border measured in armor texels. */
    public final double texels;

    OutlineWidth(double texels) {
        this.texels = texels;
    }

    @Override
    public String key() {
        return "outline." + name().toLowerCase();
    }
}
