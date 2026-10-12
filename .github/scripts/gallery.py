"""Turns the game test screenshots into framed 1920x1080 gallery images (Modrinth / README).

usage: gallery.py <folder with gallery_*.png screenshots> <output folder>
"""
import pathlib
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

WIDTH, HEIGHT = 1920, 1080
CAPTIONS = {
    "gallery_1_netherite_full": ("Armor colour follows durability",
                                 "Netherite changes colour every 10% down to 20%, then every 5%"),
    "gallery_2_netherite_numbers": ("Durability numbers",
                                    "Optional number on the helmet and the chestplate"),
    "gallery_3_diamond_full": ("Diamond armor",
                               "Every 10% down to 10%, plus two extra colours for intense PvP"),
    "gallery_4_diamond_outline": ("Outline style",
                                  "Keep the look of your armor, colour only its edges"),
    "gallery_5_diamond_number_only": ("Numbers only",
                                      "Vanilla armor with the durability painted on"),
    "gallery_6_player": ("Every piece on its own",
                         "Each armor piece shows its own durability"),
    "gallery_7_player_numbers": ("Read your opponent at a glance",
                                 "Full colour together with numbers"),
    "gallery_8_settings": ("Simple settings",
                           "Live preview, 15 languages, Mod Menu support"),
}
GLOW = [(31, 214, 42), (255, 210, 31), (224, 30, 30), (155, 43, 232), (46, 91, 255)]


def font(size, bold):
    names = ["DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"]
    for folder in ["/usr/share/fonts/truetype/dejavu/", ""]:
        for name in names:
            try:
                return ImageFont.truetype(folder + name, size)
            except OSError:
                pass
    return ImageFont.load_default(size=size)


def background(index):
    base = Image.new("RGB", (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(base)
    top, bottom = (16, 18, 28), (34, 26, 52)
    for y in range(HEIGHT):
        t = y / HEIGHT
        draw.line([(0, y), (WIDTH, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bottom)))
    glow = Image.new("RGB", (WIDTH, HEIGHT))
    glow_draw = ImageDraw.Draw(glow)
    for i, color in enumerate(GLOW):
        x = int(WIDTH * (0.08 + 0.21 * i))
        y = HEIGHT - 120 if (i + index) % 2 else 140
        glow_draw.ellipse([x - 260, y - 200, x + 260, y + 200], fill=tuple(c // 3 for c in color))
    glow = glow.filter(ImageFilter.GaussianBlur(160))
    return Image.blend(base, Image.composite(glow, base, Image.new("L", (WIDTH, HEIGHT), 255)), 0.55)


def rounded(image, radius):
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, image.width - 1, image.height - 1], radius, fill=255)
    return mask


def frame(shot_path, out_path, index):
    title, subtitle = CAPTIONS.get(shot_path.stem, (shot_path.stem, ""))
    canvas = background(index).convert("RGBA")
    shot = Image.open(shot_path).convert("RGBA")
    shot_width = 1560
    shot = shot.resize((shot_width, round(shot.height * shot_width / shot.width)), Image.LANCZOS)
    x = (WIDTH - shot.width) // 2
    y = HEIGHT - shot.height - 48

    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([x - 6, y + 10, x + shot.width + 6, y + shot.height + 22], 30, fill=(0, 0, 0, 170))
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(22)))
    border = Image.new("RGBA", (shot.width + 6, shot.height + 6), (255, 255, 255, 40))
    canvas.paste(border, (x - 3, y - 3), rounded(border, 27))
    canvas.paste(shot, (x, y), rounded(shot, 24))

    draw = ImageDraw.Draw(canvas)
    title_font, subtitle_font, brand_font = font(54, True), font(28, False), font(24, True)
    draw.text((x, 34), title, font=title_font, fill=(255, 255, 255))
    draw.text((x, 100), subtitle, font=subtitle_font, fill=(176, 184, 200))
    brand = "DURABILITY TWEAKS"
    brand_width = draw.textlength(brand, font=brand_font)
    draw.rounded_rectangle([x + shot.width - brand_width - 36, 44, x + shot.width, 84], 20, fill=(242, 193, 78))
    draw.text((x + shot.width - brand_width - 18, 50), brand, font=brand_font, fill=(24, 20, 10))
    canvas.convert("RGB").save(out_path, optimize=True)


def main():
    source, output = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
    output.mkdir(parents=True, exist_ok=True)
    shots = sorted(p for p in source.rglob("gallery_*.png"))
    for index, shot in enumerate(shots):
        frame(shot, output / f"{shot.stem}.png", index)
        print(f"framed {shot.name}")
    if not shots:
        sys.exit("no gallery screenshots found")


if __name__ == "__main__":
    main()
