package com.betongoat.durabilitytweaks.gui;

import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.NumberMode;
import com.betongoat.durabilitytweaks.config.OptionEnum;
import com.betongoat.durabilitytweaks.core.ArmorLayout;
import com.betongoat.durabilitytweaks.core.DurabilityInfo;
import com.betongoat.durabilitytweaks.core.DurabilityTier;
import com.betongoat.durabilitytweaks.core.Palette;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Settings screen: options on the left, a live preview of netherite and diamond armor on the right,
 * and the colour scale of both armor tiers underneath the preview.
 */
public class DurabilityConfigScreen extends Screen {
    private static final int CARD_BG = 0xC80E1016;
    private static final int CARD_BORDER = 0xFF3A4150;
    private static final int CARD_HEADER = 0xF01C2029;
    private static final int ACCENT = 0xFFF2C14E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA7B0C0;
    private static final int HEADER_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TOP = 34;
    private static final int BOTTOM_BAR = 30;

    private static final PreviewArmor[] PREVIEW_ARMOR = {
        new PreviewArmor("netherite", DurabilityTier.NETHERITE, 592),
        new PreviewArmor("diamond", DurabilityTier.DIAMOND, 528),
    };

    private final Screen parent;
    private final List<Runnable> refreshers = new ArrayList<>();
    private int previewPercent = 45;

    private int leftX;
    private int rightX;
    private int cardWidth;
    private int cardBottom;
    private int legendTop;

    public DurabilityConfigScreen(Screen parent) {
        super(Component.translatable("durabilitytweaks.title"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ layout & widgets

    @Override
    protected void init() {
        refreshers.clear();
        int gap = 8;
        cardWidth = Math.min(220, (this.width - 3 * gap) / 2);
        leftX = this.width / 2 - gap / 2 - cardWidth;
        rightX = this.width / 2 + gap / 2;
        cardBottom = this.height - BOTTOM_BAR - 4;

        DurabilityConfig config = DurabilityConfig.get();
        int buttonX = leftX + 6;
        int buttonWidth = cardWidth - 12;
        int optionCount = 7;
        int available = cardBottom - (TOP + HEADER_HEIGHT + 4) - 4;
        int spacing = BUTTON_HEIGHT + Math.max(0, Math.min(4, (available - optionCount * BUTTON_HEIGHT) / (optionCount - 1)));
        int y = TOP + HEADER_HEIGHT + 5;

        Button style = cycle("style", () -> config.style, v -> config.style = v, buttonX, y, buttonWidth);
        y += spacing;
        Button palette = cycle("palette", () -> config.palette, v -> config.palette = v, buttonX, y, buttonWidth);
        y += spacing;
        Button outline = cycle("outline", () -> config.outlineWidth, v -> config.outlineWidth = v, buttonX, y, buttonWidth);
        y += spacing;
        Button number = cycle("number", () -> config.numberMode, v -> config.numberMode = v, buttonX, y, buttonWidth);
        y += spacing;
        Button format = cycle("format", () -> config.numberFormat, v -> config.numberFormat = v, buttonX, y, buttonWidth);
        y += spacing;
        Button showAtFull = toggle("show_at_full", () -> config.showAtFull, v -> config.showAtFull = v, buttonX, y, buttonWidth);
        y += spacing;
        cycle("filter", () -> config.armorFilter, v -> config.armorFilter = v, buttonX, y, buttonWidth);

        // Options that do nothing with the current settings are greyed out
        refreshers.add(() -> {
            DurabilityConfig c = DurabilityConfig.get();
            boolean on = c.enabled;
            style.active = on;
            palette.active = on && c.style != ColorStyle.NONE;
            outline.active = on && c.style == ColorStyle.OUTLINE;
            number.active = on;
            format.active = on && c.numberMode != NumberMode.OFF;
            showAtFull.active = on && c.numberMode != NumberMode.OFF;
        });

        // Preview durability slider at the bottom of the preview card
        int sliderY = cardBottom - BUTTON_HEIGHT - 5;
        this.addRenderableWidget(new PreviewSlider(rightX + 6, sliderY, cardWidth - 12, BUTTON_HEIGHT));
        legendTop = sliderY - 4 - 2 * 13;

        // Bottom bar: master switch, reset, done
        int barY = this.height - BOTTOM_BAR + 5;
        int barWidth = Math.min(100, (this.width - 4 * gap) / 3);
        int barX = this.width / 2 - (3 * barWidth + 2 * gap) / 2;
        toggle("enabled", () -> config.enabled, v -> config.enabled = v, barX, barY, barWidth);
        this.addRenderableWidget(Button.builder(Component.translatable("durabilitytweaks.reset"), b -> {
                DurabilityConfig.reset();
                this.rebuildWidgets();
            })
            .bounds(barX + barWidth + gap, barY, barWidth, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.reset.tooltip")))
            .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
            .bounds(barX + 2 * (barWidth + gap), barY, barWidth, BUTTON_HEIGHT)
            .build());

        refresh();
    }

    private <E extends Enum<E> & OptionEnum> Button cycle(String option, Supplier<E> getter, Consumer<E> setter, int x, int y, int width) {
        Button button = Button.builder(Component.empty(), b -> {
                setter.accept(OptionEnum.next(getter.get()));
                DurabilityConfig.save();
                refresh();
            })
            .bounds(x, y, width, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.option." + option + ".tooltip")))
            .build();
        refreshers.add(() -> button.setMessage(label(option,
            Component.translatable("durabilitytweaks.option." + getter.get().key()).withStyle(ChatFormatting.YELLOW))));
        return this.addRenderableWidget(button);
    }

    private Button toggle(String option, Supplier<Boolean> getter, Consumer<Boolean> setter, int x, int y, int width) {
        Button button = Button.builder(Component.empty(), b -> {
                setter.accept(!getter.get());
                DurabilityConfig.save();
                refresh();
            })
            .bounds(x, y, width, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.option." + option + ".tooltip")))
            .build();
        refreshers.add(() -> button.setMessage(label(option, getter.get()
            ? Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
            : Component.translatable("options.off").withStyle(ChatFormatting.RED))));
        return this.addRenderableWidget(button);
    }

    private static Component label(String option, Component value) {
        return Component.translatable("durabilitytweaks.option." + option).append(": ").append(value);
    }

    private void refresh() {
        refreshers.forEach(Runnable::run);
    }

    @Override
    public void onClose() {
        DurabilityConfig.save();
        Screens.open(this.minecraft, parent);
    }

    // ------------------------------------------------------------------ rendering (version specific entry points)

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        drawCards(new Gfx(graphics));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawContent(new Gfx(graphics), mouseX, mouseY);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        drawCards(new Gfx(graphics));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        drawContent(new Gfx(graphics), mouseX, mouseY);
    }
    *///?}

    // ------------------------------------------------------------------ drawing

    private void drawCards(Gfx gfx) {
        drawCard(gfx, leftX, Component.translatable("durabilitytweaks.section.settings"));
        drawCard(gfx, rightX, Component.translatable("durabilitytweaks.section.preview"));
    }

    private void drawCard(Gfx gfx, int x, Component title) {
        gfx.fill(x, TOP, x + cardWidth, cardBottom, CARD_BG);
        gfx.fill(x, TOP, x + cardWidth, TOP + HEADER_HEIGHT, CARD_HEADER);
        gfx.fill(x, TOP + HEADER_HEIGHT, x + cardWidth, TOP + HEADER_HEIGHT + 1, ACCENT);
        gfx.outline(x - 1, TOP - 1, cardWidth + 2, cardBottom - TOP + 2, CARD_BORDER);
        gfx.text(this.font, title, x + 6, TOP + 3, TEXT, false);
    }

    private void drawContent(Gfx gfx, int mouseX, int mouseY) {
        // Title and author
        MutableComponent title = this.title.copy().withStyle(ChatFormatting.BOLD);
        gfx.centeredText(this.font, title, this.width / 2, 8, ACCENT);
        gfx.centeredText(this.font, Component.translatable("durabilitytweaks.subtitle"), this.width / 2, 20, MUTED);

        if (!DurabilityConfig.get().enabled) {
            gfx.centeredText(this.font, Component.translatable("durabilitytweaks.disabled"), rightX + cardWidth / 2,
                (TOP + legendTop) / 2, MUTED);
        } else {
            drawPreview(gfx);
        }
        drawLegend(gfx, mouseX, mouseY);
    }

    /** Two front-facing armor sets (netherite, diamond) at the slider's durability. */
    private void drawPreview(Gfx gfx) {
        int areaTop = TOP + HEADER_HEIGHT + 6;
        int labelHeight = 11;
        int areaHeight = legendTop - 6 - areaTop - labelHeight;
        int slotWidth = (cardWidth - 12) / PREVIEW_ARMOR.length;
        int scale = Math.max(1, Math.min(areaHeight / 32, (slotWidth - 8) / 16));
        int dollHeight = 32 * scale;
        int dollTop = areaTop + Math.max(0, (areaHeight - dollHeight) / 2);

        for (int i = 0; i < PREVIEW_ARMOR.length; i++) {
            PreviewArmor armor = PREVIEW_ARMOR[i];
            int centerX = rightX + 6 + slotWidth * i + slotWidth / 2;
            DurabilityInfo info = armor.info(previewPercent);
            drawArmorSet(gfx, armor, info, centerX - 8 * scale, dollTop, scale);

            Component name = Component.translatable("durabilitytweaks.material." + armor.name);
            int color = info.isFull() ? TEXT : DurabilityConfig.get().palette.palette.color(armor.tier.paletteSlot(info.band()));
            gfx.centeredText(this.font, name.copy().append(" " + info.percentDisplay() + "%"), centerX, dollTop + dollHeight + 3, color);
        }
    }

    private void drawArmorSet(Gfx gfx, PreviewArmor armor, DurabilityInfo info, int x, int y, int scale) {
        ArmorTextureManager.Resolved legs = ArmorTextureManager.resolve(armor.leggingsTexture(), ArmorLayout.Kind.LEGGINGS, info);
        ArmorTextureManager.Resolved outer = ArmorTextureManager.resolve(armor.outerTexture(), ArmorLayout.Kind.OUTER, info);
        if (legs != null) {
            drawFace(gfx, legs, ArmorLayout.BODY, 4, 8, x, y, scale);
            drawFace(gfx, legs, ArmorLayout.LEG, 4, 20, x, y, scale);
            drawFace(gfx, legs, ArmorLayout.LEG, 8, 20, x, y, scale);
        }
        if (outer != null) {
            drawFace(gfx, outer, ArmorLayout.LEG, 4, 20, x, y, scale);   // boots
            drawFace(gfx, outer, ArmorLayout.LEG, 8, 20, x, y, scale);
            drawFace(gfx, outer, ArmorLayout.BODY, 4, 8, x, y, scale);   // chestplate
            drawFace(gfx, outer, ArmorLayout.ARM, 0, 8, x, y, scale);
            drawFace(gfx, outer, ArmorLayout.ARM, 12, 8, x, y, scale);
            drawFace(gfx, outer, ArmorLayout.HEAD, 4, 0, x, y, scale);   // helmet
        }
    }

    /** Draws the front face of a cube at texel offset (dx, dy) of the armor stand-in. */
    private static void drawFace(Gfx gfx, ArmorTextureManager.Resolved texture, ArmorLayout.Cube cube, int dx, int dy, int x, int y, int scale) {
        int[] face = cube.front();
        float texel = texture.width() / (float) ArmorLayout.TEXELS_WIDE;
        gfx.blit(texture.id(), x + dx * scale, y + dy * scale, face[2] * scale, face[3] * scale,
            face[0] * texel, face[1] * texel, Math.round(face[2] * texel), Math.round(face[3] * texel),
            texture.width(), texture.height());
    }

    /** Colour scale of both tiers; the band of the preview durability is framed, hovering shows its range. */
    private void drawLegend(Gfx gfx, int mouseX, int mouseY) {
        Palette palette = DurabilityConfig.get().palette.palette;
        int labelWidth = 0;
        for (PreviewArmor armor : PREVIEW_ARMOR) {
            labelWidth = Math.max(labelWidth, this.font.width(Component.translatable("durabilitytweaks.material." + armor.name)));
        }
        int barLeft = rightX + 6 + labelWidth + 6;
        int barRight = rightX + cardWidth - 6;
        Component hovered = null;

        for (int row = 0; row < PREVIEW_ARMOR.length; row++) {
            PreviewArmor armor = PREVIEW_ARMOR[row];
            DurabilityTier tier = armor.tier;
            int y = legendTop + row * 13;
            Component name = Component.translatable("durabilitytweaks.material." + armor.name);
            gfx.text(this.font, name, rightX + 6, y + 1, MUTED, false);

            int current = armor.info(previewPercent).band();
            int bands = tier.bandCount();
            for (int band = 0; band < bands; band++) {
                int x0 = barLeft + (barRight - barLeft) * band / bands;
                int x1 = barLeft + (barRight - barLeft) * (band + 1) / bands;
                gfx.fill(x0, y, x1, y + 10, palette.color(tier.paletteSlot(band)));
                if (band == current) {
                    gfx.outline(x0 - 1, y - 1, x1 - x0 + 2, 12, TEXT);
                }
                if (mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + 10) {
                    hovered = Component.translatable("durabilitytweaks.legend.range", name, tier.lowerBound(band), tier.upperBound(band));
                }
            }
        }
        if (hovered != null) {
            gfx.tooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ helpers

    private record PreviewArmor(String name, DurabilityTier tier, int maxDamage) {
        DurabilityInfo info(int percent) {
            int damage = (int) Math.round(maxDamage * (100 - percent) / 100.0);
            return DurabilityInfo.of(tier, damage, maxDamage);
        }

        Identifier outerTexture() {
            //? if >=1.21.2 {
            return Identifier.withDefaultNamespace("textures/entity/equipment/humanoid/" + name + ".png");
            //?} else {
            /*return Identifier.withDefaultNamespace("textures/models/armor/" + name + "_layer_1.png");
            *///?}
        }

        Identifier leggingsTexture() {
            //? if >=1.21.2 {
            return Identifier.withDefaultNamespace("textures/entity/equipment/humanoid_leggings/" + name + ".png");
            //?} else {
            /*return Identifier.withDefaultNamespace("textures/models/armor/" + name + "_layer_2.png");
            *///?}
        }
    }

    private final class PreviewSlider extends AbstractSliderButton {
        PreviewSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), previewPercent / 100.0);
            updateMessage();
            setTooltip(Tooltip.create(Component.translatable("durabilitytweaks.preview.tooltip")));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("durabilitytweaks.preview.durability", (int) Math.round(this.value * 100)));
        }

        @Override
        protected void applyValue() {
            previewPercent = (int) Math.round(this.value * 100);
        }
    }
}
