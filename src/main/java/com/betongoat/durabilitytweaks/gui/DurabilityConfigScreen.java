package com.betongoat.durabilitytweaks.gui;

import com.betongoat.durabilitytweaks.DurabilityTweaks;
import com.betongoat.durabilitytweaks.config.ArmorPiece;
import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.IconMode;
import com.betongoat.durabilitytweaks.config.NumberMode;
import com.betongoat.durabilitytweaks.config.OptionEnum;
import com.betongoat.durabilitytweaks.core.ArmorLayout;
import com.betongoat.durabilitytweaks.core.DurabilityInfo;
import com.betongoat.durabilitytweaks.core.DurabilityTier;
import com.betongoat.durabilitytweaks.core.Palette;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import com.betongoat.durabilitytweaks.render.LegMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * Settings screen. Left: settings split into four tabs (colours, number, icons, general), the most
 * important choices as picture tiles, and a box that explains whatever the mouse is over.
 * Right: live preview of netherite and diamond armor with a durability slider, and the colour
 * scale of both armor tiers.
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

    private static final String[] TABS = {"look", "number", "icons", "general"};
    private static final int TAB_HEIGHT = 18;
    private static final int ROW = 22;
    private static final int BUTTON_HEIGHT = 20;
    /** Durability shown on the style and number tiles, so they always show colour. */
    private static final int TILE_PERCENT = 45;
    /** Height of the description box at the bottom of the settings panel. */
    private static final int DESCRIPTION_HEIGHT = 50;
    private static final Item[] NETHERITE_SET = {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};

    private static final PreviewArmor NETHERITE = new PreviewArmor("netherite", DurabilityTier.NETHERITE, 592);
    private static final PreviewArmor DIAMOND = new PreviewArmor("diamond", DurabilityTier.DIAMOND, 528);
    private static final PreviewArmor[] PREVIEW_ARMOR = {NETHERITE, DIAMOND};

    private static int tab;
    private static int previewPercent = 45;

    private final Screen parent;
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final ItemStack[] iconStacks = new ItemStack[NETHERITE_SET.length];

    /** Top of the panels; the subtitle is left out on small screens to make room. */
    private int top;
    private int panelWidth;
    private int leftX;
    private int rightX;
    private int panelBottom;
    private int contentTop;
    private int legendTop;
    private int stageBottom;
    /** Top of the description box, or -1 when the screen is too small for it (tooltips are used instead). */
    private int descriptionTop;
    private int contentBottom;
    /** Where the current tab's settings end; the description box may grow up to here. */
    private int contentEnd;
    private int iconScale = 1;
    private int iconPreviewTop = -1;

    public DurabilityConfigScreen(Screen parent) {
        super(Component.translatable("durabilitytweaks.title"));
        this.parent = parent;
    }

    /** Opens the screen on a given tab (0 colours, 1 number, 2 icons, 3 general). */
    public DurabilityConfigScreen(Screen parent, int tab) {
        this(parent);
        DurabilityConfigScreen.tab = Math.floorMod(tab, TABS.length);
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        refreshers.clear();
        tiles.clear();
        labels.clear();
        rows.clear();
        iconPreviewTop = -1;

        int gap = 10;
        top = this.height >= 300 ? 32 : 20;
        panelWidth = Math.min(230, (this.width - 3 * gap) / 2);
        leftX = this.width / 2 - gap / 2 - panelWidth;
        rightX = this.width / 2 + gap / 2;
        panelBottom = this.height - 32;
        contentTop = top + TAB_HEIGHT + 6;
        boolean roomForDescription = panelBottom - contentTop >= 180;
        int descriptionHeight = panelBottom - contentTop >= 230 ? DESCRIPTION_HEIGHT : 36;
        descriptionTop = roomForDescription ? panelBottom - 6 - descriptionHeight : -1;
        contentBottom = roomForDescription ? descriptionTop - 4 : panelBottom - 6;

        switch (tab) {
            case 0 -> initLookTab();
            case 1 -> initNumberTab();
            case 2 -> initIconsTab();
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
        y = addTiles(y, 2, ColorStyle.values(), v -> config.style == v, v -> config.style = v, false);
        y += 6;
        // Palette: cycle through the presets, or open the editor for your own colours
        Button palette = row(y, "palette", cycleButton(() -> config.palette, v -> config.palette = v, "palette"), 22);
        Button edit = this.addRenderableWidget(Button.builder(Component.literal("✎"), b -> Screens.open(this.minecraft, new ColorEditorScreen(this)))
            .bounds(leftX + panelWidth - 8 - 20, y, 20, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.option.edit_colors.tooltip")))
            .build());
        rows.add(new Row(y, "edit_colors", leftX + panelWidth - 28, leftX + panelWidth - 8));
        y += ROW;
        Button outline = row(y, "outline", cycleButton(() -> config.outlineWidth, v -> config.outlineWidth = v, "outline"));
        contentEnd = y + BUTTON_HEIGHT;
        refreshers.add(() -> {
            palette.active = config.style != ColorStyle.NONE;
            edit.active = config.style != ColorStyle.NONE;
            outline.active = config.style == ColorStyle.OUTLINE;
        });
    }

    private void initNumberTab() {
        DurabilityConfig config = DurabilityConfig.get();
        int y = contentTop;
        labels.add(new Label(Component.translatable("durabilitytweaks.caption.number"), leftX + 8, y));
        y += 11;
        // Each piece is switched on and off on its own
        y = addTiles(y, 4, ArmorPiece.values(), piece -> config.numberMode().on(piece), config::toggleNumber, true);
        y += 6;
        List<Button> numberButtons = new ArrayList<>();
        numberButtons.add(row(y, "format", cycleButton(() -> config.numberFormat, v -> config.numberFormat = v, "format")));
        y += ROW;
        numberButtons.add(row(y, "size", cycleButton(() -> config.numberSize, v -> config.numberSize = v, "size")));
        y += ROW;
        numberButtons.add(row(y, "color", cycleButton(() -> config.numberColor, v -> config.numberColor = v, "color")));
        y += ROW;
        numberButtons.add(row(y, "show_at_full", toggleButton(() -> config.showAtFull, v -> config.showAtFull = v)));
        contentEnd = y + BUTTON_HEIGHT;
        refreshers.add(() -> numberButtons.forEach(button -> button.active = config.numberMode().any()));
    }

    private void initIconsTab() {
        DurabilityConfig config = DurabilityConfig.get();
        int y = contentTop + 2;
        Button icons = row(y, "icons", cycleButton(() -> config.iconMode, v -> config.iconMode = v, "icons"));
        refreshers.add(() -> icons.active = config.style != ColorStyle.NONE);
        if (this.minecraft.level == null) {
            // Item stacks need a loaded world on 26.x; from the title screen the preview is skipped
            labels.add(new Label(Component.translatable("durabilitytweaks.icons.preview.world"), leftX + 8, y + ROW + 6));
            contentEnd = y + ROW + 16;
            return;
        }
        iconPreviewTop = y + ROW + 6;
        // Two strips (label, slot row, gap) at double size when there is room
        iconScale = contentBottom - iconPreviewTop >= 2 * (11 + 36 + 8) ? 2 : 1;
        contentEnd = iconPreviewTop + 2 * (11 + 16 * iconScale + 4 + 8) - 8;
        for (int i = 0; i < NETHERITE_SET.length; i++) {
            iconStacks[i] = new ItemStack(NETHERITE_SET[i]);
        }
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
        labels.add(new Label(Component.translatable("durabilitytweaks.client_only"), leftX + 8, y + 11));
        contentEnd = y + 21;
    }

    /**
     * A row of picture tiles for an option with few values; returns the y below it.
     *
     * @param toggle each tile switches on and off on its own instead of picking one of the values
     */
    private <E extends Enum<E> & OptionEnum> int addTiles(int y, int rowsBelow, E[] values, Predicate<E> isSelected, Consumer<E> click, boolean toggle) {
        int count = values.length;
        int tileGap = 4;
        int tileWidth = (panelWidth - 16 - tileGap * (count - 1)) / count;
        int available = contentBottom - y - rowsBelow * ROW - 6;
        int thumbScale = Math.max(1, Math.min((tileWidth - 8) / 16, (available - 16) / 20));
        thumbScale = Math.min(thumbScale, 3);
        int tileHeight = 20 * thumbScale + 20;
        for (int i = 0; i < count; i++) {
            E value = values[i];
            int x = leftX + 8 + i * (tileWidth + tileGap);
            tiles.add(new Tile(x, y, tileWidth, tileHeight, thumbScale,
                Component.translatable("durabilitytweaks.option." + value.key()),
                Component.translatable("durabilitytweaks.option." + value.key() + ".tooltip"),
                () -> isSelected.test(value),
                () -> click.accept(value),
                toggle,
                value));
        }
        return y + tileHeight;
    }

    /** A settings row: label on the left, button on the right. */
    private Button row(int y, String option, ButtonSpec spec) {
        return row(y, option, spec, 0);
    }

    /** A settings row whose button leaves {@code reserved} pixels free on the right (for an extra button). */
    private Button row(int y, String option, ButtonSpec spec, int reserved) {
        int buttonWidth = Math.min(116, panelWidth / 2);
        labels.add(new Label(Component.translatable("durabilitytweaks.option." + option), leftX + 8, y + 6));
        rows.add(new Row(y, option, leftX, leftX + panelWidth - 8 - reserved));
        Button.Builder builder = spec.builder()
            .bounds(leftX + panelWidth - 8 - buttonWidth, y, buttonWidth - reserved, BUTTON_HEIGHT);
        if (descriptionTop < 0) {
            // No description box on small screens: explain the option in a tooltip instead
            builder.tooltip(Tooltip.create(Component.translatable("durabilitytweaks.option." + option + ".tooltip")));
        }
        Button button = builder.build();
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
        LegMirror.reloadIfNeeded(this.minecraft);
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
        for (int i = 0; i < TABS.length; i++) {
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
                if (tile.toggle || !tile.selected.get()) {
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
        gfx.fill(leftX, top, leftX + panelWidth, panelBottom, PANEL);
        gfx.outline(leftX - 1, top - 1, panelWidth + 2, panelBottom - top + 2, PANEL_BORDER);
        for (int i = 0; i < TABS.length; i++) {
            int[] r = tabRect(i);
            boolean selected = tab == i;
            boolean hovered = tabHit(i, mouseX, mouseY);
            gfx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], selected ? PANEL : hovered ? TAB_HOVER : TAB_IDLE);
            if (selected) {
                gfx.fill(r[0], r[1], r[0] + r[2], r[1] + 2, ACCENT);
            }
        }
        gfx.fill(leftX, top + TAB_HEIGHT, leftX + panelWidth, top + TAB_HEIGHT + 1, PANEL_BORDER);

        // Preview panel with a sky coloured stage for the armor
        gfx.fill(rightX, top, rightX + panelWidth, panelBottom, PANEL);
        gfx.outline(rightX - 1, top - 1, panelWidth + 2, panelBottom - top + 2, PANEL_BORDER);
        gfx.gradient(rightX + 1, top + 1, rightX + panelWidth - 1, stageBottom, STAGE_TOP, STAGE_BOTTOM);
    }

    private void drawContent(Gfx gfx, int mouseX, int mouseY) {
        gfx.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), this.width / 2, 7, ACCENT);
        if (top > 20) {
            gfx.centeredText(this.font, Component.translatable("durabilitytweaks.subtitle"), this.width / 2, 19, MUTED);
        }

        for (int i = 0; i < TABS.length; i++) {
            int[] r = tabRect(i);
            gfx.centeredText(this.font, Component.translatable("durabilitytweaks.tab." + TABS[i]),
                r[0] + r[2] / 2, r[1] + 6, tab == i ? ACCENT : MUTED);
        }
        for (Label label : labels) {
            gfx.text(this.font, label.text, label.x, label.y, MUTED, false);
        }

        Component tooltip = null;
        Component description = null;
        Tile hoveredTile = null;
        for (Tile tile : tiles) {
            drawTile(gfx, tile, mouseX, mouseY);
            if (tile.contains(mouseX, mouseY)) {
                hoveredTile = tile;
                description = tile.tooltip;
            }
        }
        for (Row row : rows) {
            if (row.contains(mouseX, mouseY)) {
                description = Component.translatable("durabilitytweaks.option." + row.option + ".tooltip");
            }
        }
        if (iconPreviewTop >= 0) {
            drawIconPreview(gfx);
        }
        if (descriptionTop >= 0) {
            Component text = description != null ? description : Component.translatable("durabilitytweaks.tab." + TABS[tab] + ".intro");
            if (!drawDescription(gfx, text) && description != null) {
                // Too long for the box: show all of it next to the mouse as well
                tooltip = description;
            }
        } else if (hoveredTile != null) {
            tooltip = hoveredTile.tooltip;
        }

        if (DurabilityConfig.get().enabled) {
            drawStage(gfx);
        } else {
            gfx.centeredText(this.font, Component.translatable("durabilitytweaks.disabled"), rightX + panelWidth / 2, (top + stageBottom) / 2, 0xFF30343C);
        }
        Component legendTooltip = drawLegend(gfx, mouseX, mouseY);
        if (legendTooltip != null) {
            tooltip = legendTooltip;
        }
        if (tooltip != null) {
            gfx.tooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /** Explanation of the hovered option (or of the whole tab), wrapped to the box. Returns whether all of it fit. */
    private boolean drawDescription(Gfx gfx, Component text) {
        int x = leftX + 14;
        int width = panelWidth - 26;
        int bottom = panelBottom - 6;
        List<FormattedCharSequence> lines = this.font.split(text, width);
        // The box grows upwards into free space for long texts, but never over the settings
        int lineHeight = 9;
        int boxTop = Math.max(Math.min(descriptionTop, bottom - lines.size() * lineHeight - 5), Math.min(descriptionTop, contentEnd + 4));
        gfx.fill(leftX + 6, boxTop, leftX + panelWidth - 6, bottom, TILE);
        gfx.fill(leftX + 6, boxTop, leftX + 8, bottom, ACCENT);
        int y = boxTop + 3;
        int maxLines = Math.max(1, (bottom - y + 1) / lineHeight);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            gfx.text(this.font, lines.get(i), x, y + i * lineHeight, 0xFFD8DCE4, false);
        }
        return lines.size() <= maxLines;
    }

    /** Netherite icons at the preview durability, as the HUD and as an inventory would show them. */
    private void drawIconPreview(Gfx gfx) {
        for (int i = 0; i < iconStacks.length; i++) {
            ItemStack stack = iconStacks[i];
            stack.setDamageValue((int) Math.round(stack.getMaxDamage() * (100 - previewPercent) / 100.0));
        }
        int scale = iconScale;
        int slot = 16 * scale + 4;
        int y = iconPreviewTop;
        String[] places = {"hud", "inventory"};
        for (int p = 0; p < places.length; p++) {
            boolean onHud = p == 0;
            gfx.text(this.font, Component.translatable("durabilitytweaks.icons.preview." + places[p]), leftX + 8, y, MUTED, false);
            y += 11;
            int x = leftX + 8;
            for (ItemStack stack : iconStacks) {
                gfx.fill(x, y, x + slot, y + slot, 0xFF8B8B8B);
                gfx.fill(x + 1, y + 1, x + slot - 1, y + slot - 1, 0xFF373737);
                drawIcon(gfx, stack, x + 2, y + 2, scale, onHud);
                x += slot + 3;
            }
            y += slot + 8;
        }
    }

    private static void drawIcon(Gfx gfx, ItemStack stack, int x, int y, int scale, boolean onHud) {
        ArmorTextureManager.Resolved icon = ArmorTextureManager.icon(stack, onHud);
        if (icon != null) {
            gfx.blit(icon.id(), x, y, 16 * scale, 16 * scale, 0, 0, icon.width(), icon.height(), icon.width(), icon.height());
            return;
        }
        ArmorTextureManager.vanillaIcons = true;
        try {
            gfx.item(stack, x, y, scale);
        } finally {
            ArmorTextureManager.vanillaIcons = false;
        }
    }

    private void drawTile(Gfx gfx, Tile tile, int mouseX, int mouseY) {
        boolean selected = tile.selected.get();
        boolean hovered = tile.contains(mouseX, mouseY);
        gfx.fill(tile.x, tile.y, tile.x + tile.width, tile.y + tile.height, selected ? TILE_SELECTED : hovered ? TILE_HOVER : TILE);
        gfx.outline(tile.x, tile.y, tile.width, tile.height, selected ? ACCENT : hovered ? MUTED : PANEL_BORDER);

        DurabilityConfig config = DurabilityConfig.get();
        DurabilityInfo info = NETHERITE.info(TILE_PERCENT);
        int scale = tile.thumbScale;
        int thumbX = tile.x + tile.width / 2 - 8 * scale;
        int thumbY = tile.y + 5;
        if (tile.value instanceof ArmorPiece piece) {
            drawPiece(gfx, piece, info, thumbX, thumbY, scale);
            if (!selected) {
                gfx.fill(tile.x + 1, tile.y + 1, tile.x + tile.width - 1, tile.y + tile.height - 14, 0x90101318);
            }
            gfx.centeredText(this.font, tile.label, tile.x + tile.width / 2, tile.y + tile.height - 12, selected ? ACCENT : MUTED);
            return;
        }

        // Thumbnail: helmet and chestplate of netherite armor in this style
        ColorStyle style = tile.value instanceof ColorStyle s ? s : config.style;
        ArmorTextureManager.Resolved outer = ArmorTextureManager.resolve(NETHERITE.outerTexture(), ArmorLayout.Kind.OUTER, info, style, config.numberMode());
        if (outer != null) {
            drawFace(gfx, outer, ArmorLayout.BODY, 4, 8, thumbX, thumbY, scale);
            drawFace(gfx, outer, ArmorLayout.ARM, 0, 8, thumbX, thumbY, scale);
            drawFace(gfx, outer, ArmorLayout.ARM, 12, 8, thumbX, thumbY, scale);
            drawFace(gfx, outer, ArmorLayout.HEAD, 4, 0, thumbX, thumbY, scale);
        }
        gfx.centeredText(this.font, tile.label, tile.x + tile.width / 2, tile.y + tile.height - 12, selected ? ACCENT : TEXT);
    }

    /** One netherite armor piece with its number, inside a 16x20 texel box. */
    private void drawPiece(Gfx gfx, ArmorPiece piece, DurabilityInfo info, int x, int y, int scale) {
        boolean leggings = piece == ArmorPiece.LEGGINGS;
        ArmorLayout.Kind kind = leggings ? ArmorLayout.Kind.LEGGINGS : ArmorLayout.Kind.OUTER;
        ArmorTextureManager.Resolved texture = ArmorTextureManager.resolve(leggings ? NETHERITE.leggingsTexture() : NETHERITE.outerTexture(),
            kind, info, DurabilityConfig.get().style, NumberMode.only(piece));
        if (texture == null) {
            return;
        }
        switch (piece) {
            case HELMET -> drawFace(gfx, texture, ArmorLayout.HEAD, 4, 6, x, y, scale);
            case CHESTPLATE -> {
                drawFace(gfx, texture, ArmorLayout.BODY, 4, 4, x, y, scale);
                drawFace(gfx, texture, ArmorLayout.ARM, 0, 4, x, y, scale);
                drawFace(gfx, texture, ArmorLayout.ARM, 12, 4, x, y, scale);
            }
            case LEGGINGS, BOOTS -> {
                drawFace(gfx, texture, ArmorLayout.LEG, 4, 4, x, y, scale);
                drawFace(gfx, texture, ArmorLayout.LEG, 8, 4, x, y, scale);
            }
        }
    }

    /** Netherite and diamond armor side by side at the slider's durability. */
    private void drawStage(Gfx gfx) {
        int areaTop = top + 8;
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
            int color = info.isFull() ? TEXT : DurabilityConfig.get().activePalette().color(armor.tier.paletteSlot(info.band()));
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
    static void drawFace(Gfx gfx, ArmorTextureManager.Resolved texture, ArmorLayout.Cube cube, int dx, int dy, int x, int y, int scale) {
        int[] face = cube.front();
        float texel = texture.width() / (float) ArmorLayout.TEXELS_WIDE;
        gfx.blit(texture.id(), x + dx * scale, y + dy * scale, face[2] * scale, face[3] * scale,
            face[0] * texel, face[1] * texel, Math.round(face[2] * texel), Math.round(face[3] * texel),
            texture.width(), texture.height());
    }

    /** Colour scale of both tiers; the band of the preview durability is framed. Returns the hovered band's tooltip. */
    private Component drawLegend(Gfx gfx, int mouseX, int mouseY) {
        Palette palette = DurabilityConfig.get().activePalette();
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
        int width = panelWidth / TABS.length;
        int x = leftX + index * width;
        int last = TABS.length - 1;
        return new int[] {x, top, index == last ? panelWidth - last * width : width, TAB_HEIGHT};
    }

    private boolean tabHit(int index, double mouseX, double mouseY) {
        int[] r = tabRect(index);
        return mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
    }

    // ------------------------------------------------------------------ positions for the in-game click test

    public static int currentTab() {
        return tab;
    }

    public int[] tabCenter(int index) {
        int[] r = tabRect(index);
        return new int[] {r[0] + r[2] / 2, r[1] + r[3] / 2};
    }

    public int[] tileCenter(int index) {
        Tile tile = tiles.get(index);
        return new int[] {tile.x + tile.width / 2, tile.y + tile.height / 2};
    }

    // ------------------------------------------------------------------ helpers

    private record Label(Component text, int x, int y) {
    }

    /** Where a settings row (label and button) is, for the description box. */
    private record Row(int y, String option, int left, int right) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        }
    }

    private record Tile(int x, int y, int width, int height, int thumbScale, Component label, Component tooltip,
                        Supplier<Boolean> selected, Runnable select, boolean toggle, Object value) {
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
