package com.betongoat.durabilitytweaks.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?}

/**
 * Thin wrapper over Minecraft's GUI drawing API, which was renamed and reshaped several times
 * between 1.21 and 26.x. Everything version specific in the settings screen goes through here.
 */
public final class Gfx {
    //? if >=26.1 {
    public final GuiGraphicsExtractor graphics;

    public Gfx(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }
    //?} else {
    /*public final GuiGraphics graphics;

    public Gfx(GuiGraphics graphics) {
        this.graphics = graphics;
    }
    *///?}

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    public void gradient(int x0, int y0, int x1, int y1, int topArgb, int bottomArgb) {
        graphics.fillGradient(x0, y0, x1, y1, topArgb, bottomArgb);
    }

    /** One pixel wide rectangle border. */
    public void outline(int x, int y, int width, int height, int argb) {
        fill(x, y, x + width, y + 1, argb);
        fill(x, y + height - 1, x + width, y + height, argb);
        fill(x, y + 1, x + 1, y + height - 1, argb);
        fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public void text(Font font, Component text, int x, int y, int argb, boolean shadow) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, argb, shadow);
        //?} else {
        /*graphics.drawString(font, text, x, y, argb, shadow);
        *///?}
    }

    public void centeredText(Font font, Component text, int centerX, int y, int argb) {
        //? if >=26.1 {
        graphics.centeredText(font, text, centerX, y, argb);
        //?} else {
        /*graphics.drawCenteredString(font, text, centerX, y, argb);
        *///?}
    }

    public void tooltip(Font font, Component text, int mouseX, int mouseY) {
        //? if >=1.21.6 {
        graphics.setTooltipForNextFrame(font, text, mouseX, mouseY);
        //?} else {
        /*graphics.renderTooltip(font, text, mouseX, mouseY);
        *///?}
    }

    /**
     * Draws the region (u, v, regionWidth, regionHeight) of a texture stretched to (x, y, width, height).
     * All coordinates in texture pixels.
     */
    public void blit(Identifier texture, int x, int y, int width, int height,
                     float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        //? if >=1.21.6 {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
        //?} else if >=1.21.2 {
        /*graphics.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
        *///?} else {
        /*graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        *///?}
    }
}
