package com.terraformersmc.modmenu.api;

/**
 * Compile-only stub of Mod Menu's API (excluded from the built jar).
 * Mod Menu provides the real interface at runtime; this avoids depending on a Mod Menu build per Minecraft version.
 */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return null;
    }
}
