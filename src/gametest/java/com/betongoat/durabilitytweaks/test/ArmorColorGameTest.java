package com.betongoat.durabilitytweaks.test;

import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.NumberMode;
import com.betongoat.durabilitytweaks.gui.DurabilityConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Starts the real game, puts damaged armor on armor stands and takes screenshots of every style
 * plus the settings screen. Run with {@code ./gradlew :<version>:runClientGameTest}.
 */
public class ArmorColorGameTest implements FabricClientGameTest {
    private static final int[] PERCENTS = {100, 95, 75, 55, 35, 17, 7, 2};
    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};
    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
    private static final int[] NETHERITE_MAX = {407, 592, 555, 481};
    private static final int[] DIAMOND_MAX = {363, 528, 495, 429};

    @Override
    public void runTest(ClientGameTestContext context) {
        context.setScreen(() -> new DurabilityConfigScreen(null));
        context.waitTicks(5);
        context.takeScreenshot("durabilitytweaks_settings");
        context.setScreen(() -> null);

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            waitForChunks(context);

            int y = context.computeOnClient(client -> (int) Math.floor(client.player.getY()));
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("tp @a 0.5 " + y + " 0.5 0 12");

            spawnRow(world, "netherite", NETHERITE_MAX, y);
            waitForChunks(context);
            screenshots(context, "netherite");

            world.getServer().runCommand("kill @e[type=minecraft:armor_stand]");
            spawnRow(world, "diamond", DIAMOND_MAX, y);
            context.waitTicks(20);
            screenshots(context, "diamond");

            DurabilityConfig.reset();
        }
    }

    /**
     * The chunk waiting API moved between versions (getClientWorld / getClientLevel / getConnection),
     * so this waits for the player and then gives the chunks a few seconds instead.
     */
    private static void waitForChunks(ClientGameTestContext context) {
        context.waitFor(client -> client.player != null && client.level != null);
        context.waitTicks(100);
    }

    /** Eight armor stands facing the player, from full to almost broken armor. */
    private static void spawnRow(TestSingleplayerContext world, String material, int[] maxDamage, int y) {
        for (int i = 0; i < PERCENTS.length; i++) {
            String tag = "dt" + i;
            double x = -3.5 + i;
            world.getServer().runCommand("summon minecraft:armor_stand " + x + " " + y + " 4.5 {Rotation:[180f,0f],NoGravity:1b,ShowArms:1b,Tags:[\"" + tag + "\"]}");
            for (int slot = 0; slot < SLOTS.length; slot++) {
                int damage = (int) Math.round(maxDamage[slot] * (100 - PERCENTS[i]) / 100.0);
                world.getServer().runCommand("item replace entity @e[tag=" + tag + ",limit=1] armor." + SLOTS[slot]
                    + " with minecraft:" + material + "_" + PIECES[slot] + "[damage=" + damage + "]");
            }
        }
    }

    private static void screenshots(ClientGameTestContext context, String material) {
        style(context, ColorStyle.FULL, NumberMode.OFF);
        context.takeScreenshot("durabilitytweaks_" + material + "_full");
        style(context, ColorStyle.FULL, NumberMode.HELMET_AND_CHESTPLATE);
        context.takeScreenshot("durabilitytweaks_" + material + "_full_number");
        style(context, ColorStyle.OUTLINE, NumberMode.HELMET);
        context.takeScreenshot("durabilitytweaks_" + material + "_outline_number");
        style(context, ColorStyle.NONE, NumberMode.HELMET_AND_CHESTPLATE);
        context.takeScreenshot("durabilitytweaks_" + material + "_number_only");
    }

    private static void style(ClientGameTestContext context, ColorStyle style, NumberMode numberMode) {
        context.runOnClient(client -> {
            DurabilityConfig.get().style = style;
            DurabilityConfig.get().numberMode = numberMode;
        });
        context.waitTicks(3);
    }
}
