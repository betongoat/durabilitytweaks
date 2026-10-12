package com.betongoat.durabilitytweaks;

import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.gui.DurabilityConfigScreen;
import com.betongoat.durabilitytweaks.gui.Screens;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
//? if >=1.21.9 {
import net.minecraft.resources.Identifier;
//?}

public final class DurabilityTweaks implements ClientModInitializer {
    public static final String MOD_ID = "durabilitytweaks";
    public static final String VERSION = /*$ mod_version*/ "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openSettings;
    private static boolean smokeTestDone;

    public static KeyMapping openSettingsKey() {
        return openSettings;
    }

    @Override
    public void onInitializeClient() {
        DurabilityConfig.load(FabricLoader.getInstance().getConfigDir());

        //? if >=1.21.9 {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        //?} else {
        /*String category = "key.categories.durabilitytweaks";
        *///?}
        KeyMapping key = new KeyMapping("key.durabilitytweaks.open_settings", InputConstants.KEY_K, category);
        //? if >=26.1 {
        openSettings = KeyMappingHelper.registerKeyMapping(key);
        //?} else {
        /*openSettings = KeyBindingHelper.registerKeyBinding(key);
        *///?}

        boolean smokeTest = Boolean.getBoolean("durabilitytweaks.smokeTest");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (smokeTest && !smokeTestDone && !Screens.overlayActive(client) && Screens.current(client) != null) {
                // CI check: the game finished loading with all mixins applied
                smokeTestDone = true;
                SelfCheck.run();
                LOGGER.info("Smoke test passed: the game finished loading");
                client.stop();
            }
            ArmorTextureManager.tick();
            while (openSettings.consumeClick()) {
                if (Screens.current(client) == null) {
                    Screens.open(client, new DurabilityConfigScreen(null));
                }
            }
        });
        LOGGER.info("Durability Tweaks {} by betongoat loaded", VERSION);
    }
}
