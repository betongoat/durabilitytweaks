<div align="center">

<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/modrinth/icon_512.png" width="128" alt="Durability Tweaks icon">

# Durability Tweaks

**A dura pack as a mod: armor changes colour as it loses durability.**

No resource pack, no OptiFine, no CIT. Works on every server.

<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/docs/gallery/gallery_1_netherite_full.png" width="800" alt="Netherite armor changing colour with durability">

</div>

---

## ✨ Features

- 🎨 **Armor colour follows durability.** Full armor looks like vanilla; damaged armor goes from green to red, then purple and blue.
- 🔢 **Durability numbers** on the armor show the durability points left, with percent as an option. Switch each piece on its own: helmet, chestplate, leggings and boots.
- 🛡️ **Coloured armor icons** in the hotbar and in **armor HUD mods**. They work out of the box, with no extra setup.
- 🖌️ **Three styles:** *Full* (the whole armor, like a classic dura pack), *Outline* (only the edges are coloured) or *None* (numbers only).
- 🌈 **Colour palettes:** Classic, Green → Red, colour blind friendly, or **your own colours** for every durability level, set with RGB sliders, a hex code or presets.
- 👥 **Whose armor:** everyone, only yours, or only other players and mobs.
- 🖥️ **Clear settings screen** with a live preview, and every option explained when you hover over it.
- 🌍 **15 languages.**

<div align="center">

<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/docs/gallery/gallery_7_player_numbers.png" width="800" alt="Players with coloured armor and durability numbers">

</div>

## 📉 How the colour changes

| Durability | Netherite | Diamond & other armor |
|:----------:|:---------:|:---------------------:|
| 100% | vanilla look | vanilla look |
| 90 – 20% | new colour every **10%** | new colour every **10%** |
| 20 – 10% | new colour every **5%** | one colour (10 – 19%) |
| 10 – 0% | new colour every **5%** | **2 extra colours** for intense PvP (10 – 5%, 5 – 0%) |

## 🖼️ Gallery

<div align="center">

<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/docs/gallery/gallery_11_all_numbers.png" width="390" alt="Numbers on every armor piece">
<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/docs/gallery/gallery_9_hud_icons.png" width="390" alt="Coloured armor icons in the hotbar">
<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/docs/gallery/gallery_4_diamond_outline.png" width="390" alt="Outline style">
<img src="https://raw.githubusercontent.com/betongoat/durabilitytweaks/main/docs/gallery/gallery_10_custom_colors.png" width="390" alt="Custom colour editor">

</div>

## ⚙️ Settings

Press <kbd>K</kbd> in game (you can change it in Controls) or use the button in **Mod Menu**.

<details>
<summary><b>All options</b></summary>

<br>

**Colours**
- Style: Full / Outline / None
- Palette: Classic / Green → Red / Colour blind / Own (✎ opens the colour editor)
- Outline width: Thin / Thick

**Number**
- Pieces: Helmet, Chest, Legs, Boots (each one on or off; helmet and chest by default)
- Shows: Points (durability left) / Percent
- Size: Small / Medium / Large
- Colour: Auto / White / Durability colour / Black / Yellow
- Number on undamaged armor: on / off

**Icons**
- Coloured armor icons: Off / HUD (hotbar and armor HUD mods) / Everywhere (inventories too)

**General**
- Mod on / off
- Armor: all, or only diamond & netherite
- Whose armor: everyone / only mine / only others
- Reset to defaults

Settings are saved in `config/durabilitytweaks.json`.

</details>

## 🧩 Compatibility

<details>
<summary><b>Versions, mods and servers</b></summary>

<br>

- **One jar for every version from 1.21 to 26.3.** Fabric Loader picks the right build for your game automatically.
- Needs **Fabric API**. **Mod Menu** is optional.
- Works with **Sodium**, **Iris** and other performance mods, **custom resource packs** (the colours are made from your pack's own armor textures) and **modded armor**.
- **Client-side only.** The server doesn't need the mod and can't see it: the mod sends no packets and blocks the known sign-based mod detection trick.
- Does other players' armor look full while they are clearly taking damage? Then the server hides other players' durability, and no mod or dura pack can show it.

</details>

<details>
<summary><b>Why does turning on leg numbers reload resources?</b></summary>

<br>

Minecraft draws the left leg of armor with a mirrored texture, so a number there would read backwards. When numbers on leggings or boots are on, the mod rebuilds the armor model with a normal left leg. This needs one quick resource reload when you close the settings. With leg numbers off, armor looks exactly like vanilla.

</details>

---

<div align="center">

Made by **betongoat** · [Source code on GitHub](https://github.com/betongoat/durabilitytweaks)

</div>
