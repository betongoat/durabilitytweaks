package com.betongoat.durabilitytweaks.gui;

import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.PaletteType;
import com.betongoat.durabilitytweaks.core.ArmorLayout;
import com.betongoat.durabilitytweaks.core.DurabilityInfo;
import com.betongoat.durabilitytweaks.core.DurabilityTier;
import com.betongoat.durabilitytweaks.core.Palette;
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Editor for the player's own palette: pick one of the twelve durability levels on the left,
 * change its colour on the right with sliders, a hex code or a preset colour. Changes show live;
 * Cancel restores the palette from before the editor was opened.
 */
public class ColorEditorScreen extends Screen {
    private static final int PANEL = 0xD20E1117;
    private static final int PANEL_BORDER = 0xFF343B48;
    private static final int ACCENT = 0xFFF2C14E;
    private static final int MUTED = 0xFF9AA3B2;
    private static final int STAGE_TOP = 0xFF8DB4EA;
    private static final int STAGE_BOTTOM = 0xFFD5E4F7;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SWATCH_WIDTH = 90;
    private static final int RIGHT_WIDTH = 170;
    private static final int TOP = 40;
    private static final int[] PRESETS = {
        0xFFFFFF, 0x1E1E1E, 0x2ECC40, 0x9BE15D, 0xFFE14D, 0xFF9F1A,
        0xFF4136, 0xF012BE, 0xB10DC9, 0x7FDBFF, 0x0074D9, 0x001F8F,
    };
    private static final PaletteType[] LOADABLE = {PaletteType.CLASSIC, PaletteType.GRADIENT, PaletteType.COLORBLIND};

    private static int selected;

    private final Screen parent;
    private final PaletteType previousType;
    private final int[] previousColors;
    private final List<Button> swatches = new ArrayList<>();
    private final List<Button> presets = new ArrayList<>();
    private final List<Button> loadButtons = new ArrayList<>();
    private ChannelSlider[] sliders;
    private EditBox hex;
    private boolean syncing;
    private int leftX;
    private int rightX;
    private int bottom;

    public ColorEditorScreen(Screen parent) {
        super(Component.translatable("durabilitytweaks.editor.title"));
        this.parent = parent;
        DurabilityConfig config = DurabilityConfig.get();
        previousType = config.palette;
        previousColors = config.customColors.clone();
        config.palette = PaletteType.CUSTOM;
        config.customColorsChanged();
    }

    private static int[] colors() {
        return DurabilityConfig.get().customColors;
    }

    @Override
    protected void init() {
        swatches.clear();
        presets.clear();
        loadButtons.clear();
        int total = 2 * SWATCH_WIDTH + 4 + 16 + RIGHT_WIDTH;
        leftX = (this.width - total) / 2;
        rightX = leftX + 2 * SWATCH_WIDTH + 4 + 16;

        // Twelve durability levels, netherite's ranges (diamond shares them, see the tooltip)
        for (int slot = 0; slot < Palette.SLOTS; slot++) {
            int column = slot / 6;
            int row = slot % 6;
            int index = slot;
            DurabilityTier tier = DurabilityTier.NETHERITE;
            Component range = Component.translatable("durabilitytweaks.editor.range", tier.lowerBound(slot), tier.upperBound(slot));
            swatches.add(this.addRenderableWidget(Button.builder(range, b -> select(index))
                .bounds(leftX + column * (SWATCH_WIDTH + 4), TOP + row * 22, SWATCH_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.editor.slot.tooltip")))
                .build()));
        }
        // Start from one of the built-in palettes
        int loadY = TOP + 6 * 22 + 16;
        int loadWidth = (2 * SWATCH_WIDTH + 4 - 8) / LOADABLE.length;
        for (int i = 0; i < LOADABLE.length; i++) {
            PaletteType type = LOADABLE[i];
            // The button shows the palette itself; its name is in the tooltip
            loadButtons.add(this.addRenderableWidget(Button.builder(Component.empty(), b -> load(type))
                .bounds(leftX + i * (loadWidth + 4), loadY, loadWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("durabilitytweaks.option." + type.key()).append(" - ")
                    .append(Component.translatable("durabilitytweaks.editor.load.tooltip"))))
                .build()));
        }

        int y = TOP + 76;
        sliders = new ChannelSlider[] {
            new ChannelSlider(rightX, y, 16, "red"),
            new ChannelSlider(rightX, y + 22, 8, "green"),
            new ChannelSlider(rightX, y + 44, 0, "blue"),
        };
        for (ChannelSlider slider : sliders) {
            this.addRenderableWidget(slider);
        }
        int hexWidth = 70;
        hex = new EditBox(this.font, rightX + RIGHT_WIDTH - hexWidth, y + 66 + 1, hexWidth, BUTTON_HEIGHT - 2, Component.translatable("durabilitytweaks.editor.hex"));
        hex.setMaxLength(7);
        hex.setResponder(this::hexChanged);
        this.addRenderableWidget(hex);

        int presetY = y + 90;
        int presetSize = (RIGHT_WIDTH - (PRESETS.length - 1)) / PRESETS.length;
        for (int i = 0; i < PRESETS.length; i++) {
            int color = PRESETS[i];
            presets.add(this.addRenderableWidget(Button.builder(Component.empty(), b -> setColor(color))
                .bounds(rightX + i * (presetSize + 1), presetY, presetSize, presetSize + 2)
                .build()));
        }
        bottom = Math.max(loadY + BUTTON_HEIGHT, presetY + presetSize + 2) + 8;

        int buttonY = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
            .bounds(this.width / 2 - 154, buttonY, 150, BUTTON_HEIGHT)
            .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> cancel())
            .bounds(this.width / 2 + 4, buttonY, 150, BUTTON_HEIGHT)
            .build());
        sync();
    }

    private void select(int slot) {
        selected = slot;
        sync();
    }

    private void load(PaletteType type) {
        System.arraycopy(type.palette.colors(), 0, colors(), 0, Palette.SLOTS);
        changed();
    }

    private void setColor(int rgb) {
        colors()[selected] = rgb & 0xFFFFFF;
        changed();
    }

    private void changed() {
        DurabilityConfig.get().customColorsChanged();
        sync();
    }

    /** Puts the selected colour into the sliders and the hex field. */
    private void sync() {
        int rgb = colors()[selected];
        syncing = true;
        for (ChannelSlider slider : sliders) {
            slider.set((rgb >> slider.shift) & 0xFF);
        }
        hex.setValue(String.format(Locale.ROOT, "#%06X", rgb));
        syncing = false;
    }

    private void hexChanged(String text) {
        if (syncing) {
            return;
        }
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6) {
            return;
        }
        try {
            colors()[selected] = Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            return;
        }
        DurabilityConfig.get().customColorsChanged();
        syncing = true;
        for (ChannelSlider slider : sliders) {
            slider.set((colors()[selected] >> slider.shift) & 0xFF);
        }
        syncing = false;
    }

    private void cancel() {
        DurabilityConfig config = DurabilityConfig.get();
        config.palette = previousType;
        config.customColors = previousColors;
        config.customColorsChanged();
        close();
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        DurabilityConfig.save();
        Screens.open(this.minecraft, parent);
    }

    // ------------------------------------------------------------------ rendering (version specific entry points)

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        drawPanels(new Gfx(graphics));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawContent(new Gfx(graphics));
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        drawPanels(new Gfx(graphics));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        drawContent(new Gfx(graphics));
    }
    *///?}

    private void drawPanels(Gfx gfx) {
        int x0 = leftX - 8;
        int x1 = rightX + RIGHT_WIDTH + 8;
        gfx.fill(x0, TOP - 8, x1, bottom, PANEL);
        gfx.outline(x0 - 1, TOP - 9, x1 - x0 + 2, bottom - TOP + 10, PANEL_BORDER);
        gfx.gradient(rightX, TOP, rightX + RIGHT_WIDTH, TOP + 70, STAGE_TOP, STAGE_BOTTOM);
    }

    private void drawContent(Gfx gfx) {
        gfx.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), this.width / 2, 8, ACCENT);
        gfx.centeredText(this.font, Component.translatable("durabilitytweaks.editor.subtitle"), this.width / 2, 19, MUTED);

        int[] colors = colors();
        for (int slot = 0; slot < swatches.size(); slot++) {
            Button button = swatches.get(slot);
            int x = button.getX();
            int y = button.getY();
            gfx.fill(x + 4, y + 4, x + 16, y + 16, 0xFF000000);
            gfx.fill(x + 5, y + 5, x + 15, y + 15, 0xFF000000 | colors[slot]);
            if (slot == selected) {
                gfx.outline(x - 1, y - 1, button.getWidth() + 2, button.getHeight() + 2, ACCENT);
            }
        }
        gfx.text(this.font, Component.translatable("durabilitytweaks.editor.load"), leftX, TOP + 6 * 22 + 5, MUTED, false);
        for (int i = 0; i < loadButtons.size(); i++) {
            Button button = loadButtons.get(i);
            int[] palette = LOADABLE[i].palette.colors();
            int x0 = button.getX() + 5;
            int width = button.getWidth() - 10;
            int y0 = button.getY() + 6;
            gfx.fill(x0 - 1, y0 - 1, x0 + width + 1, y0 + 9, 0xFF000000);
            for (int c = 0; c < palette.length; c++) {
                gfx.fill(x0 + width * c / palette.length, y0, x0 + width * (c + 1) / palette.length, y0 + 8, 0xFF000000 | palette[c]);
            }
        }
        for (int i = 0; i < presets.size(); i++) {
            Button button = presets.get(i);
            gfx.fill(button.getX() + 2, button.getY() + 2, button.getX() + button.getWidth() - 2, button.getY() + button.getHeight() - 2, 0xFF000000 | PRESETS[i]);
        }
        gfx.text(this.font, Component.translatable("durabilitytweaks.editor.hex"), rightX, TOP + 76 + 66 + 6, MUTED, false);
        gfx.fill(hex.getX() - 24, hex.getY() + 1, hex.getX() - 6, hex.getY() + hex.getHeight() - 1, 0xFF000000 | colors[selected]);

        drawPreview(gfx);
    }

    /** Netherite chestplate and helmet at the selected level, in the player's style. */
    private void drawPreview(Gfx gfx) {
        DurabilityConfig config = DurabilityConfig.get();
        DurabilityTier tier = DurabilityTier.NETHERITE;
        int percent = (tier.lowerBound(selected) + tier.upperBound(selected)) / 2;
        int max = 592;
        DurabilityInfo info = DurabilityInfo.of(tier, (int) Math.round(max * (100 - percent) / 100.0), max);
        ColorStyle style = config.style == ColorStyle.NONE ? ColorStyle.FULL : config.style;
        ArmorTextureManager.Resolved outer = ArmorTextureManager.resolve(ArmorTextureManager.vanillaTexture("netherite", ArmorLayout.Kind.OUTER),
            ArmorLayout.Kind.OUTER, info, style, config.numberMode());
        if (outer == null) {
            return;
        }
        int scale = 3;
        int x = rightX + RIGHT_WIDTH / 2 - 8 * scale;
        int y = TOP + 5;
        DurabilityConfigScreen.drawFace(gfx, outer, ArmorLayout.BODY, 4, 8, x, y - 4, scale);
        DurabilityConfigScreen.drawFace(gfx, outer, ArmorLayout.ARM, 0, 8, x, y - 4, scale);
        DurabilityConfigScreen.drawFace(gfx, outer, ArmorLayout.ARM, 12, 8, x, y - 4, scale);
        DurabilityConfigScreen.drawFace(gfx, outer, ArmorLayout.HEAD, 4, 0, x, y - 4, scale);
    }

    private final class ChannelSlider extends AbstractSliderButton {
        final int shift;
        private final String channel;

        ChannelSlider(int x, int y, int shift, String channel) {
            super(x, y, RIGHT_WIDTH, BUTTON_HEIGHT, Component.empty(), 0);
            this.shift = shift;
            this.channel = channel;
            updateMessage();
        }

        int channelValue() {
            return (int) Math.round(this.value * 255);
        }

        void set(int channelValue) {
            this.value = channelValue / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("durabilitytweaks.editor." + channel, channelValue()));
        }

        @Override
        protected void applyValue() {
            if (syncing) {
                return;
            }
            int rgb = colors()[selected];
            rgb = (rgb & ~(0xFF << shift)) | (channelValue() << shift);
            colors()[selected] = rgb;
            DurabilityConfig.get().customColorsChanged();
            syncing = true;
            hex.setValue(String.format(Locale.ROOT, "#%06X", rgb));
            syncing = false;
        }
    }
}
