package com.betongoat.durabilitytweaks.render;

import com.betongoat.durabilitytweaks.DurabilityTweaks;
import com.betongoat.durabilitytweaks.config.ArmorFilter;
import com.betongoat.durabilitytweaks.config.ColorStyle;
import com.betongoat.durabilitytweaks.config.DurabilityConfig;
import com.betongoat.durabilitytweaks.config.IconMode;
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
    /** Set while the settings screen draws an icon that has to look vanilla (preview of inventories in HUD mode). */
    public static boolean vanillaIcons;

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
        return infoFor(stack, own, true);
    }

    private static DurabilityInfo infoFor(ItemStack stack, boolean own, boolean checkOwner) {
        DurabilityConfig config = DurabilityConfig.get();
        if (!config.enabled || (checkOwner && !config.owners.includes(own))) {
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
        return resolve(original, kind, info, config.style, config.numberMode());
    }

    /** Like {@link #resolve(Identifier, ArmorLayout.Kind, DurabilityInfo)} with the style and number placement overridden (settings screen previews). */
    public static Resolved resolve(Identifier original, ArmorLayout.Kind kind, DurabilityInfo info, ColorStyle style, NumberMode numberMode) {
        TextureProcessor.Request request = request(info, kind, style, numberMode);
        if (request == null) {
            Source source = source(original, true);
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
        // Undamaged armor looks vanilla, unless the player wants to see the number on it too
        if (full && !(config.showAtFull && numberMode.any())) {
            return null;
        }
        ColorStyle style = full ? ColorStyle.NONE : colorStyle;
        int color = colorOf(info);

        // Every piece only draws its own part of the texture, so all enabled spots can be painted:
        // the outer texture holds helmet, chestplate and boots, the leggings texture the leggings
        boolean outer = kind == ArmorLayout.Kind.OUTER;
        boolean head = outer && numberMode.helmet();
        boolean chest = outer && numberMode.chestplate();
        boolean legs = outer ? numberMode.boots() : numberMode.leggings();
        String text = head || chest || legs
            ? String.valueOf(config.numberFormat == NumberFormat.POINTS ? info.remaining() : info.percentDisplay())
            : null;
        TextureProcessor.Request request = new TextureProcessor.Request(style, color, config.outlineWidth.texels, text, head, chest,
            legs, outer, config.numberSize.faceShare, numberColor(style, color));
        return request.isNoop() ? null : request;
    }

    /** Durability colour of the armor's current level, white for undamaged armor. */
    public static int colorOf(DurabilityInfo info) {
        return info.isFull() ? 0xFFFFFFFF : DurabilityConfig.get().activePalette().color(info.tier().paletteSlot(info.band()));
    }

    private static int numberColor(ColorStyle style, int durabilityColor) {
        return switch (DurabilityConfig.get().numberColor) {
            case AUTO -> style == ColorStyle.FULL ? TextureProcessor.TEXT_LIGHT : durabilityColor;
            case DURABILITY -> durabilityColor;
            default -> DurabilityConfig.get().numberColor.argb;
        };
    }

    /** Whether an item counts as humanoid armor (helmet, chestplate, leggings, boots). */
    public static boolean isArmor(ItemStack stack) {
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("elytra")) {
            return false;
        }
        //? if >=1.21.2 {
        net.minecraft.world.item.equipment.Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR;
        //?} else {
        /*return stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
        *///?}
    }

    /**
     * Recoloured icon for an armor item in a GUI (HUD, armor HUD mods, inventories), or null when the
     * icon should stay vanilla. {@code onHud} tells whether it is drawn outside of any screen.
     */
    public static Resolved icon(ItemStack stack, boolean onHud) {
        DurabilityConfig config = DurabilityConfig.get();
        if (vanillaIcons || !config.enabled || config.style == ColorStyle.NONE || config.iconMode == IconMode.OFF
            || (config.iconMode == IconMode.HUD && !onHud) || !isArmor(stack)) {
            return null;
        }
        DurabilityInfo info = infoFor(stack, true, false);
        if (info == null || info.isFull()) {
            return null;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier texture = Identifier.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
        int color = colorOf(info);
        String key = "icon|" + texture + "|" + config.style + "|" + color + "|" + config.outlineWidth;
        Generated generated = GENERATED.get(key);
        if (generated == null) {
            Source source = source(texture, false);
            if (source == null) {
                return null;
            }
            TextureProcessor.Image image = TextureProcessor.processIcon(source.argb(), source.width(), source.height(),
                config.style, color, config.outlineWidth.texels);
            generated = new Generated(upload(image));
            GENERATED.put(key, generated);
        }
        generated.lastUsed = System.nanoTime();
        return generated.texture;
    }

    private static Resolved generate(Identifier original, TextureProcessor.Request request) {
        Source source = source(original, true);
        if (source == null) {
            return null;
        }
        return upload(TextureProcessor.process(source.argb(), source.width(), source.height(), request));
    }

    private static Resolved upload(TextureProcessor.Image image) {
        NativeImage nativeImage = new NativeImage(image.width(), image.height(), false);
        int[] pixels = image.argb();
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                setPixel(nativeImage, x, y, pixels[y * image.width() + x]);
            }
        }

        Identifier id = Identifier.fromNamespaceAndPath(DurabilityTweaks.MOD_ID, "generated/" + nextId++);
        //? if >=1.21.5 {
        DynamicTexture texture = new DynamicTexture(id::toString, nativeImage);
        //?} else {
        /*DynamicTexture texture = new DynamicTexture(nativeImage);
        *///?}
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return new Resolved(id, image.width(), image.height());
    }

    /** @param armorLayout whether this is an armor texture (only the standard 2:1 layout can be processed) */
    private static Source source(Identifier location, boolean armorLayout) {
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
                boolean supported = armorLayout ? width >= ArmorLayout.TEXELS_WIDE && width == height * 2 : width > 0 && width == height;
                if (supported) {
                    int[] argb = new int[width * height];
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            argb[y * width + x] = getPixel(image, x, y);
                        }
                    }
                    source = new Source(argb, width, height);
                } else {
                    DurabilityTweaks.LOGGER.warn("Skipping texture {} with unsupported size {}x{}", location, width, height);
                }
            } catch (Exception e) {
                DurabilityTweaks.LOGGER.warn("Could not read texture {}", location, e);
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
