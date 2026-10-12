package com.betongoat.durabilitytweaks.compat;

import com.betongoat.durabilitytweaks.gui.DurabilityConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.util.Map;

/** Adds the settings button to Mod Menu. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DurabilityConfigScreen::new;
    }

    /** The universal jar shows up as the parent entry in Mod Menu; give it the settings button too. */
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return Map.of("durabilitytweaks-universal", (ConfigScreenFactory<?>) DurabilityConfigScreen::new);
    }
}
