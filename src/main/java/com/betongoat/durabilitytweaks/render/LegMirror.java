package com.betongoat.durabilitytweaks.render;

import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import net.minecraft.client.Minecraft;

/**
 * The armor's left leg normally shows the leg texture mirrored, which would make a number on the
 * leggings or boots read backwards there. While leg numbers are on, the armor models are built with
 * an un-mirrored left leg instead. Models are built when resources load, so switching needs a reload.
 */
public final class LegMirror {
    private static boolean builtUnmirrored;

    /** Asked while the armor models are built: whether to leave the left leg un-mirrored. */
    public static boolean unmirrorWhileBuilding() {
        builtUnmirrored = wanted();
        return builtUnmirrored;
    }

    private static boolean wanted() {
        return DurabilityConfig.get().numberMode().onLegs();
    }

    /** Rebuilds the armor models (resource reload) if the leg number setting changed since they were built. */
    public static void reloadIfNeeded(Minecraft minecraft) {
        if (wanted() != builtUnmirrored) {
            builtUnmirrored = wanted();
            minecraft.reloadResourcePacks();
        }
    }

    private LegMirror() {
    }
}
