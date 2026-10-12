package com.betongoat.durabilitytweaks.gui;

import com.betongoat.durabilitytweaks.DurabilityTweaks;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * Settings screen. Left: settings split into three tabs, the two most important choices (style and
 * number placement) as picture tiles. Right: live preview of netherite and diamond armor with a
 * durability slider, and the colour scale of both armor tiers.
 */
public class DurabilityConfigScreen extends Screen {
    private static final int PANEL = 0xD20E1117;
    private static final int PANEL_BORDER = 0xFF343B48;
    private static final int TAB_IDLE = 0xFF171B22;
    private static final int TAB_HOVER = 0xFF222834;
    private static final int TILE = 0xFF1A1F28;
    private static final int TILE_HOVER = 0xFF242B37;
    private static final int TILE_SELECTED = 0xFF2C2A1E;
    private static final int ACCENT = 0xFFF2C14E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9AA3B2;
    private static final int DIM = 0xFF5D6575;
    private static final int STAGE_TOP = 0xFF8DB4EA;
    private static final int STAGE_BOTTOM = 0xFFD5E4F7;

    private static final int TOP = 32;
    private static final int TAB_HEIGHT = 18;
    private static final int ROW = 22;
    private static final int BUTTON_HEIGHT = 20;
    /** Durability shown on the style and number tiles, so they always show colour. */
    private static final int TILE_PERCENT = 45;

    private static final PreviewArmor NETHERITE = new PreviewArmor("netherite", DurabilityTier.NETHERITE, 592);
    private static final PreviewArmor DIAMOND = new PreviewArmor("diamond", DurabilityTier.DIAMOND, 528);
    private static final PreviewArmor[] PREVIEW_ARMOR = {NETHERITE, DIAMOND};

    private static int tab;
    private static int previewPercent = 45;

    private final Screen parent;
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();

    private int panelWidth;
    private int leftX;
    private int rightX;
    private int panelBottom;
    private int contentTop;
    private int legendTop;
    private int stageBottom;

    public DurabilityConfigScreen(Screen parent) {
        super(Component.translatable("durabilitytweaks.title"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        refreshers.clear();
        tiles.clear();
        labels.clear();

        int gap = 10;
        panelWidth = Math.min(230, (this.width - 3 * gap) / 2);
        leftX = this.width / 2 - gap / 2 - panelWidth;
        rightX = this.width / 2 + gap / 2;
        panelBottom = this.height - 32;
        contentTop = TOP + TAB_HEIGHT + 6;

        switch (tab) {
            case 0 -> initLookTab();
            case 1 -> initNumberTab();
            default -> initGeneralTab();
        }

        // Preview: slider at the bottom, colour scale above it, the armor stage fills the rest
        int sliderY = panelBottom - BUTTON_HEIGHT - 6;
        this.addRenderableWidget(new PreviewSlider(rightX + 6, sliderY, panelWidth - 12, BUTTON_HEIGHT));
        legendTop = sliderY - 6 - 2 * 13;
        stageBottom = legendTop - 6;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
            .bounds(this.width / 2 - 75, this.height - 26, 150, BUTTON_HEIGHT)
            .build());

        refresh();
    }

    private void initLookTab() {
        DurabilityConfig config = DurabilityConfig.get();
        int y = contentTop;
        labels.add(new Label(Component.translatable("durabilitytweaks.caption.style"), leftX + 8, y));
        y += 11;
        y = addTiles(y, ColorStyle.values(), () -> config.style, v -> config.style = v, "style");
        y += 6;
        Button palette = row(y, "palette", cycleButton(() -> config.palette, v -> config.palette = v, "palette"));
        y += ROW;
        Button outline = row(y, "outline", cycleButton(() -> config.outlineWidth, v -> config.outlineWidth = v, "outline"));
        refreshers.add(() -> {
            palette.active = config.style != ColorStyle.NONE;
            outline.active = config.style == ColorStyle.OUTLINE;
        });
    }

    private void initNumberTab() {
        DurabilityConfig config = DurabilityConfig.get();
        int y = contentTop;
        labels.add(new Label(Component.translatable("durabilitytweaks.caption.number"), leftX + 8, y));
        y += 11;
        y = addTiles(y, NumberMode.values(), () -> config.numberMode, v -> config.numberMode = v, "number");
        y += 6;
        Button format = row(y, "format", cycleButton(() -> config.numberFormat, v -> config.numberFormat = v, "format"));
        y += ROW;
        Button full = row(y, "show_at_full", toggleButton(() -> config.showAtFull, v -> config.showAtFull = v));
        refreshers.add(() -> {
            format.active = config.numberMode != NumberMode.OFF;
            full.active = config.numberMode != NumberMode.OFF;
        });
    }

    private void initGeneralTab() {
        DurabilityConfig config = DurabilityConfig.get();
        int y = contentTop + 2;
        row(y, "enabled", toggleButton(() -> config.enabled, v -> config.enabled = v));
        y += ROW;
        row(y, "filter", cycleButton(() -> config.armorFilter, v -> config.armorFilter = v, "filter"));
        y += ROW;
        row(y, "owners", cycleButton(() -> config.owners, v -> config.owners = v, "owners"));
        y += ROW;
        row(y, "reset", new ButtonSpec(Button.builder(Component.translatable("durabilitytweaks.reset"), b -> {
            DurabilityConfig.reset();
            this.rebuildWidgets();
        }), button -> { }));
        y += ROW + 4;
        labels.add(new Label(Component.translatable("durabilitytweaks.keybind_hint",
            DurabilityTweaks.openSettingsKey().getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW)), leftX + 8, y));
        labels.add(new Label(Component.translatable("durabilitytweaks.inspect_hint",
            DurabilityTweaks.inspectArmorKey().getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW)), leftX + 8, y + 11));
        labels.add(new Label(Component.translatable("durabilitytweaks.client_only"), leftX + 8, y + 22));
    }

    /** A row of picture tiles for an option with few values; returns the y below it. */
    private <E extends Enum<E> & OptionEnum> int addTiles(int y, E[] values, Supplier<E> getter, Consumer<E> setter, String option) {
        int count = values.length;
        int tileGap = 4;
        int tileWidth = (panelWidth - 16 - tileGap * (count - 1)) / count;
        int available = panelBottom - y - 2 * ROW - 14;
        int thumbScale = Math.max(1, Math.min((tileWidth - 8) / 16, (available - 16) / 20));
        thumbScale = Math.min(thumbScale, 3);
        int tileHeight = 20 * thumbScale + 20;
        for (int i = 0; i < count; i++) {
            E value = values[i];
            int x = leftX + 8 + i * (tileWidth + tileGap);
            tiles.add(new Tile(x, y, tileWidth, tileHeight, thumbScale,
                Component.translatable("durabilitytweaks.option." + value.key()),
                Component.translatable("durabilitytweaks.option." + value.key() + ".tooltip"),
                () -> getter.get() == value,
                () -> setter.accept(value),
                value));
        }
        return y + tileHeight;
    }

    /** A settings row: label on the left, button on the right. */
    private Button row(int y, String option, ButtonSpec spec) {
        int buttonWidth = Math.min(116, panelWidth / 2);
        labels.add(new Label(Component.translatable("durabilitytweaks.option." + option), leftX + 8, y + 6));
        Button button = spec.builder()
            .bounds(leftX + panelWidth - 8 - buttonWidth, y, buttonWidth, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.option." + option + ".tooltip")))
            .build();
        spec.bind().accept(button);
        return this.addRenderableWidget(button);
    }

    /** A button under construction plus what to do with the finished button (keep its label up to date). */
    private record ButtonSpec(Button.Builder builder, Consumer<Button> bind) {
    }

    private <E extends Enum<E> & OptionEnum> ButtonSpec cycleButton(Supplier<E> getter, Consumer<E> setter, String option) {
        Button.Builder builder = Button.builder(Component.empty(), b -> {
            setter.accept(OptionEnum.next(getter.get()));
            changed();
        });
        return new ButtonSpec(builder, button -> refreshers.add(() -> button.setMessage(
            Component.translatable("durabilitytweaks.option." + getter.get().key()).withStyle(ChatFormatting.YELLOW))));
    }

    private ButtonSpec toggleButton(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        Button.Builder builder = Button.builder(Component.empty(), b -> {
            setter.accept(!getter.get());
            changed();
        });
        return new ButtonSpec(builder, button -> refreshers.add(() -> button.setMessage(getter.get()
            ? Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
            : Component.translatable("options.off").withStyle(ChatFormatting.RED))));
    }

    private void changed() {
        DurabilityConfig.save();
        refresh();
    }

    private void refresh() {
        refreshers.forEach(Runnable::run);
    }

    @Override
    public void onClose() {
        DurabilityConfig.save();
        Screens.open(this.minecraft, parent);
    }

    // ------------------------------------------------------------------ input (version specific signature)

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && clickCustom(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickCustom(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?}

    /** Tabs and picture tiles are drawn by the screen itself, so it also handles their clicks. */
    private boolean clickCustom(double mouseX, double mouseY) {
        for (int i = 0; i < 3; i++) {
            if (tabHit(i, mouseX, mouseY)) {
                if (tab != i) {
                    tab = i;
                    clickSound();
                    this.rebuildWidgets();
                }
                return true;
            }
        }
        for (Tile tile : tiles) {
            if (tile.contains(mouseX, mouseY)) {
                if (!tile.selected.get()) {
                    tile.select.run();
                    clickSound();
                    changed();
                }
                return true;
            }
        }
        return false;
    }

    private void clickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ------------------------------------------------------------------ rendering (version specific entry points)

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        drawPanels(new Gfx(graphics), mouseX, mouseY);
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
        drawPanels(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        drawContent(new Gfx(graphics), mouseX, mouseY);
    }
    *///?}

    // ------------------------------------------------------------------ drawing

    private void drawPanels(Gfx gfx, int mouseX, int mouseY) {
        // Settings panel with its tab strip
        gfx.fill(leftX, TOP, leftX + panelWidth, panelBottom, PANEL);
        gfx.outline(leftX - 1, TOP - 1, panelWidth + 2, panelBottom - TOP + 2, PANEL_BORDER);
        for (int i = 0; i < 3; i++) {
            int[] r = tabRect(i);
            boolean selected = tab == i;
            boolean hovered = tabHit(i, mouseX, mouseY);
            gfx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], selected ? PANEL : hovered ? TAB_HOVER : TAB_IDLE);
            if (selected) {
                gfx.fill(r[0], r[1], r[0] + r[2], r[1] + 2, ACCENT);
            }
        }
        gfx.fill(leftX, TOP + TAB_HEIGHT, leftX + panelWidth, TOP + TAB_HEIGHT + 1, PANEL_BORDER);

        // Preview panel with a sky coloured stage for the armor
        gfx.fill(rightX, TOP, rightX + panelWidth, panelBottom, PANEL);
        gfx.outline(rightX - 1, TOP - 1, panelWidth + 2, panelBottom - TOP + 2, PANEL_BORDER);
        gfx.gradient(rightX + 1, TOP + 1, rightX + panelWidth - 1, stageBottom, STAGE_TOP, STAGE_BOTTOM);
    }

    private void drawContent(Gfx gfx, int mouseX, int mouseY) {
        gfx.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), this.width / 2, 7, ACCENT);
        gfx.centeredText(this.font, Component.translatable("durabilitytweaks.subtitle"), this.width / 2, 19, MUTED);

        String[] tabKeys = {"look", "number", "general"};
        for (int i = 0; i < 3; i++) {
            int[] r = tabRect(i);
            gfx.centeredText(this.font, Component.translatable("durabilitytweaks.tab." + tabKeys[i]),
                r[0] + r[2] / 2, r[1] + 6, tab == i ? ACCENT : MUTED);
        }
        for (Label label : labels) {
            gfx.text(this.font, label.text, label.x, label.y, MUTED, false);
        }

        Component tooltip = null;
        for (Tile tile : tiles) {
            drawTile(gfx, tile, mouseX, mouseY);
            if (tile.contains(mouseX, mouseY)) {
                tooltip = tile.tooltip;
            }
        }

        if (DurabilityConfig.get().enabled) {
            drawStage(gfx);
        } else {
            gfx.centeredText(this.font, Component.translatable("durabilitytweaks.disabled"), rightX + panelWidth / 2, (TOP + stageBottom) / 2, 0xFF30343C);
        }
        Component legendTooltip = drawLegend(gfx, mouseX, mouseY);
        if (legendTooltip != null) {
            tooltip = legendTooltip;
        }
        if (tooltip != null) {
            gfx.tooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void drawTile(Gfx gfx, Tile tile, int mouseX, int mouseY) {
        boolean selected = tile.selected.get();
        boolean hovered = tile.contains(mouseX, mouseY);
        gfx.fill(tile.x, tile.y, tile.x + tile.width, tile.y + tile.height, selected ? TILE_SELECTED : hovered ? TILE_HOVER : TILE);
        gfx.outline(tile.x, tile.y, tile.width, tile.height, selected ? ACCENT : hovered ? MUTED : PANEL_BORDER);

        // Thumbnail: helmet and chestplate of netherite armor with this option applied
        DurabilityConfig config = DurabilityConfig.get();
        ColorStyle style = tile.value instanceof ColorStyle s ? s : config.style;
        NumberMode numberMode = tile.value instanceof NumberMode n ? n : config.numberMode;
        DurabilityInfo info = NETHERITE.info(TILE_PERCENT);
        ArmorTextureManager.Resolved outer = ArmorTextureManager.resolve(NETHERITE.outerTexture(), ArmorLayout.Kind.OUTER, info, style, numberMode);
        int scale = tile.thumbScale;
        int thumbX = tile.x + tile.width / 2 - 8 * scale;
        int thumbY = tile.y + 5;
        if (outer != null) {
            drawFace(gfx, outer, ArmorLayout.BODY, 4, 8, thumbX, thumbY, scale);
            drawFace(gfx, outer, ArmorLayout.ARM, 0, 8, thumbX, thumbY, scale);
            drawFace(gfx, outer, ArmorLayout.ARM, 12, 8, thumbX, thumbY, scale);
            drawFace(gfx, outer, ArmorLayout.HEAD, 4, 0, thumbX, thumbY, scale);
        }
        gfx.centeredText(this.font, tile.label, tile.x + tile.width / 2, tile.y + tile.height - 12, selected ? ACCENT : TEXT);
    }

    /** Netherite and diamond armor side by side at the slider's durability. */
    private void drawStage(Gfx gfx) {
        int areaTop = TOP + 8;
        int labelHeight = 12;
        int areaHeight = stageBottom - areaTop - labelHeight - 4;
        int slotWidth = (panelWidth - 12) / PREVIEW_ARMOR.length;
        int scale = Math.max(1, Math.min(areaHeight / 32, (slotWidth - 12) / 16));
        int dollHeight = 32 * scale;
        int dollTop = areaTop + Math.max(0, (areaHeight - dollHeight) / 2);

        for (int i = 0; i < PREVIEW_ARMOR.length; i++) {
            PreviewArmor armor = PREVIEW_ARMOR[i];
            int centerX = rightX + 6 + slotWidth * i + slotWidth / 2;
            DurabilityInfo info = armor.info(previewPercent);
            // Soft shadow under the armor
            gfx.fill(centerX - 7 * scale, dollTop + dollHeight - 1, centerX + 7 * scale, dollTop + dollHeight + 1, 0x30000000);
            drawArmorSet(gfx, armor, info, centerX - 8 * scale, dollTop, scale);

            // Durability points left on the chestplate, like the number on the armor
            Component name = Component.translatable("durabilitytweaks.material." + armor.name).append(" " + info.remaining() + "/" + info.max());
            int width = this.font.width(name) + 8;
            int labelY = dollTop + dollHeight + 4;
            gfx.fill(centerX - width / 2, labelY - 2, centerX + width / 2, labelY + 10, 0xA0101318);
            int color = info.isFull() ? TEXT : DurabilityConfig.get().palette.palette.color(armor.tier.paletteSlot(info.band()));
            gfx.centeredText(this.font, name, centerX, labelY, color);
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

    /** Colour scale of both tiers; the band of the preview durability is framed. Returns the hovered band's tooltip. */
    private Component drawLegend(Gfx gfx, int mouseX, int mouseY) {
        Palette palette = DurabilityConfig.get().palette.palette;
        int labelWidth = 0;
        for (PreviewArmor armor : PREVIEW_ARMOR) {
            labelWidth = Math.max(labelWidth, this.font.width(Component.translatable("durabilitytweaks.material." + armor.name)));
        }
        int barLeft = rightX + 8 + labelWidth + 6;
        int barRight = rightX + panelWidth - 8;
        Component hovered = null;

        for (int row = 0; row < PREVIEW_ARMOR.length; row++) {
            PreviewArmor armor = PREVIEW_ARMOR[row];
            DurabilityTier tier = armor.tier;
            int y = legendTop + row * 13;
            Component name = Component.translatable("durabilitytweaks.material." + armor.name);
            gfx.text(this.font, name, rightX + 8, y + 1, MUTED, false);

            int current = armor.info(previewPercent).band();
            int bands = tier.bandCount();
            for (int band = 0; band < bands; band++) {
                int x0 = barLeft + (barRight - barLeft) * band / bands;
                int x1 = barLeft + (barRight - barLeft) * (band + 1) / bands;
                gfx.fill(x0, y, x1 - 1, y + 10, palette.color(tier.paletteSlot(band)));
                if (band == current) {
                    gfx.outline(x0 - 1, y - 1, x1 - x0 + 1, 12, TEXT);
                }
                if (mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + 10) {
                    hovered = Component.translatable("durabilitytweaks.legend.range", name, tier.lowerBound(band), tier.upperBound(band));
                }
            }
        }
        return hovered;
    }

    private int[] tabRect(int index) {
        int width = panelWidth / 3;
        int x = leftX + index * width;
        return new int[] {x, TOP, index == 2 ? panelWidth - 2 * width : width, TAB_HEIGHT};
    }

    private boolean tabHit(int index, double mouseX, double mouseY) {
        int[] r = tabRect(index);
        return mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
    }

    // ------------------------------------------------------------------ helpers

    private record Label(Component text, int x, int y) {
    }

    private record Tile(int x, int y, int width, int height, int thumbScale, Component label, Component tooltip,
                        Supplier<Boolean> selected, Runnable select, Object value) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record PreviewArmor(String name, DurabilityTier tier, int maxDamage) {
        DurabilityInfo info(int percent) {
            int damage = (int) Math.round(maxDamage * (100 - percent) / 100.0);
            return DurabilityInfo.of(tier, damage, maxDamage);
        }

        Identifier outerTexture() {
            return ArmorTextureManager.vanillaTexture(name, ArmorLayout.Kind.OUTER);
        }

        Identifier leggingsTexture() {
            return ArmorTextureManager.vanillaTexture(name, ArmorLayout.Kind.LEGGINGS);
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
