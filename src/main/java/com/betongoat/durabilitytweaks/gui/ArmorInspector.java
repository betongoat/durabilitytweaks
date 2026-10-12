package com.betongoat.durabilitytweaks.gui;

import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.core.DurabilityInfo;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Prints the armor durability the client knows for the player or mob under the crosshair, into the
 * local chat only. Shows quickly whether a server hides other players' durability.
 */
public final class ArmorInspector {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] SLOT_KEYS = {"head", "chest", "legs", "feet"};

    public static void inspect(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        Entity target = minecraft.crosshairPickEntity;
        if (!(target instanceof LivingEntity living)) {
            say(minecraft, prefix().append(Component.translatable("durabilitytweaks.inspect.nothing")));
            return;
        }

        MutableComponent message = prefix().append(living.getDisplayName().copy()).append(": ");
        int known = 0;
        int full = 0;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = living.getItemBySlot(SLOTS[i]);
            Component value;
            if (stack.isEmpty()) {
                value = Component.literal("-").withStyle(ChatFormatting.DARK_GRAY);
            } else {
                DurabilityInfo info = ArmorTextureManager.durabilityOf(stack);
                if (info == null) {
                    value = Component.literal("∞").withStyle(ChatFormatting.GRAY);
                } else {
                    known++;
                    int rgb = info.isFull() ? 0xFFFFFF
                        : DurabilityConfig.get().palette.palette.color(info.tier().paletteSlot(info.band())) & 0xFFFFFF;
                    if (info.isFull()) {
                        full++;
                    }
                    value = Component.literal(info.percentDisplay() + "%").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
                }
            }
            message.append(Component.translatable("durabilitytweaks.inspect." + SLOT_KEYS[i]).withStyle(ChatFormatting.GRAY))
                .append(" ").append(value).append(i < SLOTS.length - 1 ? "   " : "");
        }
        say(minecraft, message);

        if (living instanceof Player && living != minecraft.player && known > 0 && full == known) {
            say(minecraft, prefix().append(
                Component.translatable("durabilitytweaks.inspect.all_full").withStyle(ChatFormatting.GRAY)));
        }
    }

    /** Shows a message in the local chat only; nothing is sent to the server. */
    private static void say(Minecraft minecraft, Component message) {
        //? if >=26.1 {
        minecraft.player.sendSystemMessage(message);
        //?} else {
        /*minecraft.player.displayClientMessage(message, false);
        *///?}
    }

    private static MutableComponent prefix() {
        return Component.literal("[Durability Tweaks] ").withStyle(ChatFormatting.GOLD);
    }

    private ArmorInspector() {
    }
}
