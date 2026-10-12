package com.betongoat.durabilitytweaks.test;

import com.betongoat.durabilitytweaks.SelfCheck;
import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.NumberMode;
import com.betongoat.durabilitytweaks.gui.DurabilityConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * Starts the real game, checks the mod works, and shoots the gallery: rows of armor stands in
 * every style on a quartz platform, a player close-up and the settings screen, all at 1920x1080.
 * Run with {@code ./gradlew :<version>:runClientGameTest}; images land in the run's screenshots folder.
 */
public class ArmorColorGameTest implements FabricClientGameTest {
    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};
    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
    private static final int[] NETHERITE_MAX = {407, 592, 555, 481};
    private static final int[] DIAMOND_MAX = {363, 528, 495, 429};
    private static final int[] NETHERITE_ROW = {100, 90, 75, 60, 45, 30, 17, 7};
    private static final int[] DIAMOND_ROW = {100, 85, 65, 50, 35, 15, 8, 3};

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            SelfCheck.run();
            // Software rendering on CI: an uncapped loading screen starves the integrated server
            client.options.framerateLimit().set(30);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            waitForWorld(context);
            // Full HD only after the world loaded: software rendering on CI is too slow to load a world at 1080p
            context.getInput().resizeWindow(1920, 1080);
            context.runOnClient(ArmorColorGameTest::hideHud);

            int y = context.computeOnClient(client -> (int) Math.floor(client.player.getY()));
            command(world, "time set noon");
            command(world, "weather clear");
            command(world, "fill -7 " + (y - 1) + " -1 7 " + (y - 1) + " 9 minecraft:smooth_quartz");
            // Close enough that the row fills the picture
            command(world, "tp @a 0.5 " + y + " 1.25 0 11");

            // Netherite row, classic full colour and with numbers
            spawnRow(world, "netherite", NETHERITE_MAX, NETHERITE_ROW, y);
            waitForWorld(context);
            shoot(context, "gallery_1_netherite_full", ColorStyle.FULL, NumberMode.OFF);
            shoot(context, "gallery_2_netherite_numbers", ColorStyle.FULL, NumberMode.HELMET_AND_CHESTPLATE);

            // Diamond row, full colour and outline + number
            command(world, "kill @e[type=minecraft:armor_stand]");
            spawnRow(world, "diamond", DIAMOND_MAX, DIAMOND_ROW, y);
            context.waitTicks(20);
            shoot(context, "gallery_3_diamond_full", ColorStyle.FULL, NumberMode.OFF);
            shoot(context, "gallery_4_diamond_outline", ColorStyle.OUTLINE, NumberMode.HELMET);
            shoot(context, "gallery_5_diamond_number_only", ColorStyle.NONE, NumberMode.HELMET_AND_CHESTPLATE);

            // Two players in worn armor, every piece at a different durability
            command(world, "kill @e[type=minecraft:armor_stand]");
            command(world, "tp @a 0.5 " + y + " 0.4 0 16");
            spawnPerson(world, "left", 1.45, y, 172);
            spawnPerson(world, "right", -0.45, y, 188);
            equip(world, "@e[tag=left,limit=1]", "netherite", NETHERITE_MAX, new int[] {80, 45, 62, 12});
            equip(world, "@e[tag=right,limit=1]", "diamond", DIAMOND_MAX, new int[] {35, 15, 55, 8});
            context.waitTicks(20);
            shoot(context, "gallery_6_player", ColorStyle.FULL, NumberMode.OFF);
            shoot(context, "gallery_7_player_numbers", ColorStyle.FULL, NumberMode.HELMET_AND_CHESTPLATE);

            // Settings screen over the world
            style(context, ColorStyle.FULL, NumberMode.OFF);
            context.setScreen(() -> new DurabilityConfigScreen(null));
            context.waitTicks(10);
            context.takeScreenshot("gallery_8_settings");
            context.setScreen(() -> null);
            DurabilityConfig.reset();
        }
    }

    private static void hideHud(Minecraft client) {
        //? if >=26.2 {
        if (!client.gui.hud.isHidden()) {
            client.gui.hud.toggle();
        }
        //?} else {
        /*client.options.hideGui = true;
        *///?}
    }

    /**
     * The chunk waiting API moved between versions (getClientWorld / getClientLevel / getConnection),
     * so this waits for the player and then gives the chunks a few seconds instead.
     */
    private static void waitForWorld(ClientGameTestContext context) {
        context.waitFor(client -> client.player != null && client.level != null);
        context.waitTicks(100);
    }

    private static void command(TestSingleplayerContext world, String command) {
        world.getServer().runCommand(command);
    }

    private static int damage(int maxDamage, int percentLeft) {
        return (int) Math.round(maxDamage * (100 - percentLeft) / 100.0);
    }

    /** A player look-alike: a mannequin (1.21.9+), an armor stand before that. */
    private static void spawnPerson(TestSingleplayerContext world, String tag, double x, int y, float yaw) {
        //? if >=1.21.9 {
        String entity = "minecraft:mannequin";
        //?} else {
        /*String entity = "minecraft:armor_stand";
        *///?}
        command(world, "summon " + entity + " " + x + " " + y + " 3.0 {Rotation:[" + yaw + "f,0f],immovable:1b,hide_description:1b,NoGravity:1b,ShowArms:1b,Tags:[\"" + tag + "\"]}");
    }

    private static void equip(TestSingleplayerContext world, String target, String material, int[] maxDamage, int[] percents) {
        for (int slot = 0; slot < SLOTS.length; slot++) {
            command(world, "item replace entity " + target + " armor." + SLOTS[slot] + " with minecraft:" + material + "_" + PIECES[slot]
                + "[damage=" + damage(maxDamage[slot], percents[slot]) + "]");
        }
    }

    /** Eight armor stands facing the camera, from full to almost broken armor. */
    private static void spawnRow(TestSingleplayerContext world, String material, int[] maxDamage, int[] percents, int y) {
        for (int i = 0; i < percents.length; i++) {
            String tag = "dt" + i;
            double x = 4.0 - i;
            command(world, "summon minecraft:armor_stand " + x + " " + y + " 4.5 {Rotation:[180f,0f],NoGravity:1b,ShowArms:1b,Tags:[\"" + tag + "\"]}");
            for (int slot = 0; slot < SLOTS.length; slot++) {
                command(world, "item replace entity @e[tag=" + tag + ",limit=1] armor." + SLOTS[slot]
                    + " with minecraft:" + material + "_" + PIECES[slot] + "[damage=" + damage(maxDamage[slot], percents[i]) + "]");
            }
        }
    }

    private static void shoot(ClientGameTestContext context, String name, ColorStyle style, NumberMode numberMode) {
        style(context, style, numberMode);
        context.takeScreenshot(name);
    }

    private static void style(ClientGameTestContext context, ColorStyle style, NumberMode numberMode) {
        context.runOnClient(client -> {
            DurabilityConfig.get().style = style;
            DurabilityConfig.get().numberMode = numberMode;
        });
        context.waitTicks(3);
    }
}
