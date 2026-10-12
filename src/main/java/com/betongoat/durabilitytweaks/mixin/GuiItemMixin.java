package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.gui.Gfx;
import com.betongoat.durabilitytweaks.gui.Screens;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Every item icon in a GUI goes through one private method: the hotbar, inventories and the armor
 * HUDs of other mods. Damaged armor is drawn with its durability colour there instead.
 */
//? if >=26.1 {
@Mixin(GuiGraphicsExtractor.class)
//?} else {
/*@Mixin(GuiGraphics.class)
*///?}
public abstract class GuiItemMixin {
    //? if >=26.1 {
    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), cancellable = true)
    private void durabilitytweaks$colorIcon(LivingEntity owner, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (durabilitytweaks$draw(new Gfx((GuiGraphicsExtractor) (Object) this), stack, x, y)) {
            ci.cancel();
        }
    }
    //?} else if >=1.21.6 {
    /*@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), cancellable = true)
    private void durabilitytweaks$colorIcon(LivingEntity owner, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (durabilitytweaks$draw(new Gfx((GuiGraphics) (Object) this), stack, x, y)) {
            ci.cancel();
        }
    }
    *///?} else {
    /*@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("HEAD"), cancellable = true)
    private void durabilitytweaks$colorIcon(LivingEntity owner, Level level, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        // Items sit in front of the slot background, like the vanilla item would
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 150.0F + z);
        boolean drawn = durabilitytweaks$draw(new Gfx(graphics), stack, x, y);
        graphics.pose().popPose();
        if (drawn) {
            ci.cancel();
        }
    }
    *///?}

    private static boolean durabilitytweaks$draw(Gfx gfx, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // "HUD only" leaves inventories alone, everything else (hotbar, armor HUDs, chat open) is coloured
        boolean onHud = !(Screens.current(minecraft) instanceof AbstractContainerScreen<?>);
        ArmorTextureManager.Resolved icon = ArmorTextureManager.icon(stack, onHud);
        if (icon == null) {
            return false;
        }
        gfx.blit(icon.id(), x, y, 16, 16, 0, 0, icon.width(), icon.height(), icon.width(), icon.height());
        return true;
    }
}
