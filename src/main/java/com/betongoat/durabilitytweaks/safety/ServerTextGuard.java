package com.betongoat.durabilitytweaks.safety;

import com.betongoat.durabilitytweaks.DurabilityTweaks;

/**
 * Keeps servers from detecting the mod.
 *
 * <p>The mod is purely client side: it sends no packets and registers nothing, so a server cannot
 * see it. The one known trick left is "translation probing": a server puts a translation key or a
 * key binding of a mod on a sign and opens the sign editor; the client fills the editor with the
 * translated text and sends it back, revealing which mods are installed. While the client handles
 * such a server request, this guard makes the mod's own keys resolve exactly like on a vanilla
 * client (to the raw key), so the reply looks vanilla.
 */
public final class ServerTextGuard {
    private static int depth;

    public static void enter() {
        depth++;
    }

    public static void exit() {
        depth = Math.max(0, depth - 1);
    }

    /** Whether text is currently being prepared for a server. */
    public static boolean active() {
        return depth > 0;
    }

    /** Whether a translation or key binding key belongs to this mod. */
    public static boolean isOwnKey(String key) {
        return key != null && key.contains(DurabilityTweaks.MOD_ID);
    }

    private ServerTextGuard() {
    }
}
