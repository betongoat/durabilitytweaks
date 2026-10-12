package com.betongoat.durabilitytweaks.render;

import com.betongoat.durabilitytweaks.DurabilityTweaks;
import com.betongoat.durabilitytweaks.config.ArmorFilter;
import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.NumberFormat;
import com.betongoat.durabilitytweaks.config.NumberMode;
import com.betongoat.durabilitytweaks.core.ArmorLayout;
import com.betongoat.durabilitytweaks.core.DurabilityInfo;
import com.betongoat.durabilitytweaks.core.DurabilityTier;
import com.betongoat.durabilitytweaks.core.TextureProcessor;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;

/**
 * Creates and caches the recoloured armor textures. Every texture is generated from the armor
 * texture of the active resource pack, so custom packs and modded armor keep working.
 */
public final class ArmorTextureManager {
    /** Generated textures that were not drawn for this long are freed again. */
    private static final long UNUSED_TIMEOUT_NANOS = 20_000_000_000L;

    private static final Map<String, Generated> GENERATED = new HashMap<>();
    /** Decoded source textures; null value means the texture could not be loaded. */
    private static final Map<Identifier, Source> SOURCES = new HashMap<>();
    private static int nextId;

    /** A texture as drawn: its location and pixel size. */
    public record Resolved(Identifier id, int width, int height) {
    }

    private record Source(int[] argb, int width, int height) {
    }

    private static final class Generated {
        final Resolved texture;
        long lastUsed;

        Generated(Resolved texture) {
            this.texture = texture;
        }
    }

    /** Location of a vanilla armor texture, e.g. ("diamond", OUTER). */
    public static Identifier vanillaTexture(String material, ArmorLayout.Kind kind) {
        boolean leggings = kind == ArmorLayout.Kind.LEGGINGS;
        //? if >=1.21.2 {
        return Identifier.withDefaultNamespace("textures/entity/equipment/" + (leggings ? "humanoid_leggings/" : "humanoid/") + material + ".png");
        //?} else {
        /*return Identifier.withDefaultNamespace("textures/models/armor/" + material + (leggings ? "_layer_2.png" : "_layer_1.png"));
        *///?}
    }

    /**
     * Durability of a worn armor piece, or null when this mod should leave the stack alone.
     *
     * @param own whether the local player wears it
     */
    public static DurabilityInfo infoFor(ItemStack stack, boolean own) {
        DurabilityConfig config = DurabilityConfig.get();
        if (!config.enabled || !config.owners.includes(own)) {
            return null;
        }
        DurabilityInfo info = durabilityOf(stack);
        if (info != null && config.armorFilter == ArmorFilter.DIAMOND_NETHERITE) {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!path.contains("netherite") && !path.contains("diamond")) {
                return null;
            }
        }
        return info;
    }

    /** Durability of any damageable item, ignoring the settings; null when it has none. */
    public static DurabilityInfo durabilityOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return null;
        }
        boolean netherite = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("netherite");
        DurabilityTier tier = netherite ? DurabilityTier.NETHERITE : DurabilityTier.DIAMOND;
        return DurabilityInfo.of(tier, stack.getDamageValue(), stack.getMaxDamage());
    }

    /** Texture to draw instead of {@code original} for this armor piece. */
    public static Identifier remap(Identifier original, ItemStack stack, boolean own) {
        ArmorLayout.Kind kind = ArmorLayout.classify(original.getPath());
        if (kind == null) {
            return original;
        }
        DurabilityInfo info = infoFor(stack, own);
        if (info == null) {
            return original;
        }
        Resolved resolved = resolve(original, kind, info);
        return resolved == null ? original : resolved.id();
    }

    /**
     * Whether the armor is fully recoloured, in which case the leather dye tint has to be
     * dropped or it would muddy the durability colour.
     */
    public static boolean replacesTint(ItemStack stack, boolean own) {
        DurabilityInfo info = infoFor(stack, own);
        return info != null && !info.isFull() && DurabilityConfig.get().style == ColorStyle.FULL;
    }

    /**
     * The texture for an armor piece with the given durability, or the original texture when
     * nothing changes. Returns null if the original texture cannot be loaded.
     */
    public static Resolved resolve(Identifier original, ArmorLayout.Kind kind, DurabilityInfo info) {
        DurabilityConfig config = DurabilityConfig.get();
        return resolve(original, kind, info, config.style, config.numberMode);
    }

    /** Like {@link #resolve(Identifier, ArmorLayout.Kind, DurabilityInfo)} with the style and number placement overridden (settings screen previews). */
    public static Resolved resolve(Identifier original, ArmorLayout.Kind kind, DurabilityInfo info, ColorStyle style, NumberMode numberMode) {
        TextureProcessor.Request request = request(info, kind, style, numberMode);
        if (request == null) {
            Source source = source(original);
            return source == null ? null : new Resolved(original, source.width(), source.height());
        }
        String key = original + "|" + request;
        Generated generated = GENERATED.get(key);
        if (generated == null) {
            Resolved texture = generate(original, request);
            if (texture == null) {
                return null;
            }
            generated = new Generated(texture);
            GENERATED.put(key, generated);
        }
        generated.lastUsed = System.nanoTime();
        return generated.texture;
    }

    private static TextureProcessor.Request request(DurabilityInfo info, ArmorLayout.Kind kind, ColorStyle colorStyle, NumberMode numberMode) {
        DurabilityConfig config = DurabilityConfig.get();
        boolean full = info.isFull();
        // Undamaged armor looks vanilla, unless the player wants to see "100" on it
        if (full && !(config.showAtFull && numberMode.onHelmet())) {
            return null;
        }
        ColorStyle style = full ? ColorStyle.NONE : colorStyle;
        int color = full ? 0xFFFFFFFF : config.palette.palette.color(info.tier().paletteSlot(info.band()));

        String text = null;
        boolean head = false;
        boolean chest = false;
        if (kind == ArmorLayout.Kind.OUTER && numberMode.onHelmet()) {
            text = String.valueOf(config.numberFormat == NumberFormat.POINTS ? info.remaining() : info.percentDisplay());
            head = true;
            chest = numberMode.onChestplate();
        }
        TextureProcessor.Request request = new TextureProcessor.Request(style, color, config.outlineWidth.texels, text, head, chest);
        return request.isNoop() ? null : request;
    }

    private static Resolved generate(Identifier original, TextureProcessor.Request request) {
        Source source = source(original);
        if (source == null) {
            return null;
        }
        TextureProcessor.Image image = TextureProcessor.process(source.argb(), source.width(), source.height(), request);
        NativeImage nativeImage = new NativeImage(image.width(), image.height(), false);
        int[] pixels = image.argb();
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                setPixel(nativeImage, x, y, pixels[y * image.width() + x]);
            }
        }

        Identifier id = Identifier.fromNamespaceAndPath(DurabilityTweaks.MOD_ID, "armor/" + nextId++);
        //? if >=1.21.5 {
        DynamicTexture texture = new DynamicTexture(id::toString, nativeImage);
        //?} else {
        /*DynamicTexture texture = new DynamicTexture(nativeImage);
        *///?}
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return new Resolved(id, image.width(), image.height());
    }

    private static Source source(Identifier location) {
        if (SOURCES.containsKey(location)) {
            return SOURCES.get(location);
        }
        Source source = null;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                int width = image.getWidth();
                int height = image.getHeight();
                // Only the standard 2:1 armor layout (64x32, 128x64, ...) can be processed safely
                if (width >= ArmorLayout.TEXELS_WIDE && width == height * 2) {
                    int[] argb = new int[width * height];
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            argb[y * width + x] = getPixel(image, x, y);
                        }
                    }
                    source = new Source(argb, width, height);
                } else {
                    DurabilityTweaks.LOGGER.warn("Skipping armor texture {} with unsupported size {}x{}", location, width, height);
                }
            } catch (Exception e) {
                DurabilityTweaks.LOGGER.warn("Could not read armor texture {}", location, e);
            }
        }
        SOURCES.put(location, source);
        return source;
    }

    private static int getPixel(NativeImage image, int x, int y) {
        //? if >=1.21.2 {
        return image.getPixel(x, y);
        //?} else {
        /*return swapRedBlue(image.getPixelRGBA(x, y));
        *///?}
    }

    private static void setPixel(NativeImage image, int x, int y, int argb) {
        //? if >=1.21.2 {
        image.setPixel(x, y, argb);
        //?} else {
        /*image.setPixelRGBA(x, y, swapRedBlue(argb));
        *///?}
    }

    /** Converts between ARGB and the ABGR layout used by NativeImage before 1.21.2. */
    @SuppressWarnings("unused")
    private static int swapRedBlue(int color) {
        return (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
    }

    /** Frees textures nobody has drawn for a while (old durability values, changed settings). Runs every client tick. */
    public static void tick() {
        if (GENERATED.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Iterator<Generated> iterator = GENERATED.values().iterator();
        while (iterator.hasNext()) {
            Generated generated = iterator.next();
            if (now - generated.lastUsed > UNUSED_TIMEOUT_NANOS) {
                Minecraft.getInstance().getTextureManager().release(generated.texture.id());
                iterator.remove();
            }
        }
    }

    /** Drops everything; called after resource packs reload so the new armor textures are picked up. */
    public static void clear() {
        for (Generated generated : GENERATED.values()) {
            Minecraft.getInstance().getTextureManager().release(generated.texture.id());
        }
        GENERATED.clear();
        SOURCES.clear();
    }

    private ArmorTextureManager() {
    }
}
