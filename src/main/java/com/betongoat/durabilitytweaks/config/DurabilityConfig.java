package com.betongoat.durabilitytweaks.config;

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
    public OutlineWidth outlineWidth = OutlineWidth.THIN;
    public NumberMode numberMode = NumberMode.OFF;
    public NumberFormat numberFormat = NumberFormat.POINTS;
    /** Paint the number even on undamaged armor (full armor otherwise looks vanilla). */
    public boolean showAtFull = false;
    public ArmorFilter armorFilter = ArmorFilter.ALL;
    public ArmorOwners owners = ArmorOwners.EVERYONE;
    /** Settings file format; files without it come from 1.0.x. */
    public int configVersion = CURRENT_VERSION;

    private static final int CURRENT_VERSION = 2;

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
                    if (!json.has("configVersion")) {
                        // 1.0.x saved percent as the default; the number now shows durability points left
                        loaded.numberFormat = NumberFormat.POINTS;
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
        if (numberMode == null) numberMode = defaults.numberMode;
        if (numberFormat == null) numberFormat = defaults.numberFormat;
        if (armorFilter == null) armorFilter = defaults.armorFilter;
        if (owners == null) owners = defaults.owners;
        return this;
    }
}
