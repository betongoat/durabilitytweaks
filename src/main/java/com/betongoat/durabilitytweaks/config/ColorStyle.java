package com.betongoat.durabilitytweaks.config;

/** How the durability colour is applied to a worn armor piece. */
public enum ColorStyle implements OptionEnum {
    /** The whole armor piece is recoloured (classic dura pack look). */
    FULL,
    /** The armor keeps its texture, only the edges of every face get a coloured border. */
    OUTLINE,
    /** No colour at all; useful together with the durability number. */
    NONE;

    @Override
    public String key() {
        return "style." + name().toLowerCase();
    }
}
