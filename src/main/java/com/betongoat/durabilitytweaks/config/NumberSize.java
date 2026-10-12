package com.betongoat.durabilitytweaks.config;

public enum NumberSize implements OptionEnum {
    SMALL(0.45),
    MEDIUM(0.62),
    LARGE(0.82);

    /** Width of a three digit number as a share of the armor face it is painted on. */
    public final double faceShare;

    NumberSize(double faceShare) {
        this.faceShare = faceShare;
    }

    @Override
    public String key() {
        return "size." + name().toLowerCase();
    }
}
