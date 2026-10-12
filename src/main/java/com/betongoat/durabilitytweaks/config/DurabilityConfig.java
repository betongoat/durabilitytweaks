package com.betongoat.durabilitytweaks.config;

import com.betongoat.durabilitytweaks.core.Palette;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** User settings, stored as JSON in {@code config/durabilitytweaks.json}. */
public final class DurabilityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("durabilitytweaks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public ColorStyle style = ColorStyle.FULL;
    public PaletteType palette = PaletteType.CLASSIC;
    /** Colours of {@link PaletteType#CUSTOM}, one RGB value per durability level (see {@link Palette}). */
    public int[] customColors = Palette.CLASSIC.colors();
    public OutlineWidth outlineWidth = OutlineWidth.THIN;
    public boolean numberOnHelmet = true;
    public boolean numberOnChestplate = true;
    public boolean numberOnLeggings = false;
    public boolean numberOnBoots = false;
    public NumberFormat numberFormat = NumberFormat.POINTS;
    public NumberSize numberSize = NumberSize.MEDIUM;
    public NumberColor numberColor = NumberColor.AUTO;
    /** Paint the number even on undamaged armor (full armor otherwise looks vanilla). */
    public boolean showAtFull = false;
    public IconMode iconMode = IconMode.HUD;
    public ArmorFilter armorFilter = ArmorFilter.ALL;
    public ArmorOwners owners = ArmorOwners.EVERYONE;
    /** Settings file format: missing in 1.0.x, 2 in 1.1.x. */
    public int configVersion = CURRENT_VERSION;

    private static final int CURRENT_VERSION = 3;

    private transient Palette customPalette;
    private transient int[] customPaletteSource;

    public NumberMode numberMode() {
        return new NumberMode(numberOnHelmet, numberOnChestplate, numberOnLeggings, numberOnBoots);
    }

    public void setNumberMode(NumberMode mode) {
        numberOnHelmet = mode.helmet();
        numberOnChestplate = mode.chestplate();
        numberOnLeggings = mode.leggings();
        numberOnBoots = mode.boots();
    }

    public void toggleNumber(ArmorPiece piece) {
        switch (piece) {
            case HELMET -> numberOnHelmet = !numberOnHelmet;
            case CHESTPLATE -> numberOnChestplate = !numberOnChestplate;
            case LEGGINGS -> numberOnLeggings = !numberOnLeggings;
            case BOOTS -> numberOnBoots = !numberOnBoots;
        }
    }

    /** The palette in use, including the player's own colours. */
    public Palette activePalette() {
        if (palette != PaletteType.CUSTOM) {
            return palette.palette;
        }
        if (customPalette == null || customPaletteSource != customColors) {
            customPalette = Palette.of(customColors);
            customPaletteSource = customColors;
        }
        return customPalette;
    }

    /** Call after editing {@link #customColors} in place. */
    public void customColorsChanged() {
        customPalette = null;
    }

    private static DurabilityConfig instance = new DurabilityConfig();
    private static Path file;

    public static DurabilityConfig get() {
        return instance;
    }

    public static void load(Path configDir) {
        file = configDir.resolve("durabilitytweaks.json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    DurabilityConfig loaded = GSON.fromJson(json, DurabilityConfig.class);
                    int version = json.has("configVersion") ? json.get("configVersion").getAsInt() : 1;
                    if (version < 2) {
                        // 1.0.x saved percent as the default; the number now shows durability points left
                        loaded.numberFormat = NumberFormat.POINTS;
                    }
                    if (version < 3) {
                        // 1.1.x had one "numberMode" setting that defaulted to off; numbers are on by default now
                        String old = json.has("numberMode") ? json.get("numberMode").getAsString() : "OFF";
                        loaded.numberOnHelmet = true;
                        loaded.numberOnChestplate = !old.equals("HELMET");
                    }
                    loaded.configVersion = CURRENT_VERSION;
                    instance = loaded.sanitized();
                }
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Could not read {}, using defaults", file, e);
            }
        }
        save();
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save {}", file, e);
        }
    }

    public static void reset() {
        instance = new DurabilityConfig();
        save();
    }

    /** Unknown enum names in the JSON become null in Gson, fall back to defaults. */
    private DurabilityConfig sanitized() {
        DurabilityConfig defaults = new DurabilityConfig();
        if (style == null) style = defaults.style;
        if (palette == null) palette = defaults.palette;
        if (outlineWidth == null) outlineWidth = defaults.outlineWidth;
        if (numberSize == null) numberSize = defaults.numberSize;
        if (numberColor == null) numberColor = defaults.numberColor;
        if (iconMode == null) iconMode = defaults.iconMode;
        if (customColors == null || customColors.length != Palette.SLOTS) customColors = defaults.customColors;
        if (numberFormat == null) numberFormat = defaults.numberFormat;
        if (armorFilter == null) armorFilter = defaults.armorFilter;
        if (owners == null) owners = defaults.owners;
        return this;
    }
}
