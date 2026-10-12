package com.betongoat.durabilitytweaks;

import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.NumberMode;
import com.betongoat.durabilitytweaks.core.ArmorLayout;
import com.betongoat.durabilitytweaks.core.DurabilityInfo;
import com.betongoat.durabilitytweaks.core.DurabilityTier;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import com.betongoat.durabilitytweaks.safety.ServerTextGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Checks run inside the real game by CI (smoke test and client game tests). Throws on failure. */
public final class SelfCheck {
    private static final String KEY = "key.durabilitytweaks.open_settings";

    public static void run() {
        // A recoloured armor texture can be generated from the vanilla one
        DurabilityInfo info = DurabilityInfo.of(DurabilityTier.NETHERITE, 300, 592);
        ArmorTextureManager.Resolved texture = ArmorTextureManager.resolve(
            ArmorTextureManager.vanillaTexture("netherite", ArmorLayout.Kind.OUTER), ArmorLayout.Kind.OUTER, info,
            ColorStyle.FULL, NumberMode.HELMET_AND_CHESTPLATE);
        check(texture != null && texture.id().getNamespace().equals(DurabilityTweaks.MOD_ID), "armor texture was not generated");
        //? if <26.1 {
        /*checkIcons();
        *///?}

        // Our translations work normally...
        check(!Component.translatable(KEY).getString().equals(KEY), "own translation missing");
        // ...but look vanilla (untranslated) while text is prepared for a server
        ServerTextGuard.enter();
        try {
            check(Component.translatable(KEY).getString().equals(KEY), "translation leaked to server text");
            check(Component.keybind(KEY).getString().equals(KEY), "key binding leaked to server text");
        } finally {
            ServerTextGuard.exit();
        }
        check(!Component.translatable(KEY).getString().equals(KEY), "translation not restored after guard");
        DurabilityTweaks.LOGGER.info("Self check passed");
    }

    /**
     * Damaged armor gets a coloured icon on the HUD, full armor keeps the vanilla one.
     * Since 26.1 item stacks only work once a world is loaded.
     */
    public static void checkIcons() {
        ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
        check(ArmorTextureManager.icon(chestplate, true) == null, "full armor icon was recoloured");
        chestplate.setDamageValue(chestplate.getMaxDamage() / 2);
        check(ArmorTextureManager.icon(chestplate, true) != null, "armor icon was not generated");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Durability Tweaks self check failed: " + message);
        }
    }

    private SelfCheck() {
    }
}
