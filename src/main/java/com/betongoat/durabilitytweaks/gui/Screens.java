package com.betongoat.durabilitytweaks.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Screen and overlay handling moved from Minecraft to Gui in 26.2. */
public final class Screens {
    public static Screen current(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }

    public static void open(Minecraft minecraft, Screen screen) {
        //? if >=26.2 {
        minecraft.gui.setScreen(screen);
        //?} else {
        /*minecraft.setScreen(screen);
        *///?}
    }

    /** Whether a full screen overlay (the resource loading screen) is showing. */
    public static boolean overlayActive(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.overlay() != null;
        //?} else {
        /*return minecraft.getOverlay() != null;
        *///?}
    }

    private Screens() {
    }
}
