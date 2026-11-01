"""Builds the NPC skins (textures/entity/npc/<kind>.png) and their 3D extras (assets/dragonblock/npc_extras.json).

Skins are composed from the race skin layers (run gen_race_skins.py first) with each character's colors,
then painted with their own details. Extras are boxes in model pixels attached to a body part
(head pivot at the neck, y negative up, face toward -z), grouped by color like dragon.json.
Original characters on genre archetypes. Run: python tools/gen_npc_assets.py <resources_dir> [preview_dir]
"""
import json
import os
import sys
from PIL import Image

RES = sys.argv[1]
SKIN = os.path.join(RES, "assets/dragonblock/textures/skin")
OUT = os.path.join(RES, "assets/dragonblock/textures/entity/npc")
os.makedirs(OUT, exist_ok=True)


def layer(name):
    p = os.path.join(SKIN, name + ".png")
    return Image.open(p).convert("RGBA") if os.path.exists(p) else None


def tint(img, rgb):
    out = Image.new("RGBA", img.size)
    px, po = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            v, _, _, a = px[x, y]
            if a:
                add = max(0, v - 200) / 55 * 36
                po[x, y] = tuple(min(255, int(c * v / 255 + add)) for c in rgb) + (a,)
    return out


def rgb(h):
    return (h >> 16 & 255, h >> 8 & 255, h & 255)


def compose(template, skin, accent, eyes, hair=None):
    img = tint(layer(template + "_base"), rgb(skin))
    if hair is not None and layer(template + "_scalp"):
        img.alpha_composite(tint(layer(template + "_scalp"), rgb(hair)))
    img.alpha_composite(tint(layer(template + "_accent"), rgb(accent)))
    img.alpha_composite(layer(template + "_detail"))
    img.alpha_composite(tint(layer(template + "_brows"), rgb(hair if hair is not None else 0x2A2420)))
    img.alpha_composite(tint(layer("eyes"), rgb(eyes)))
    return img


def paint(img, rects, color):
    """rects: (x0, y0, x1, y1) inclusive in skin pixels."""
    for x0, y0, x1, y1 in rects:
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                img.putpixel((x, y), color + (255,) if len(color) == 3 else color)


# Face is (8,8)-(15,15); body front (20,20)-(27,31); arms front (44,20) / (36,52); legs (4,20) / (20,52)
skins = {}

# Maestro Kaido: old, tanned, bald, red martial robe with a dark sash, white brows
img = compose("human", 0xD9A877, 0xB0302A, 0x2A2420, hair=0xEDEDED)
paint(img, [(8, 0, 15, 7)], (217, 168, 119))          # bald crown
paint(img, [(9, 10, 10, 10), (13, 10, 14, 10)], (240, 240, 240))  # bushy white brows
skins["master"] = img

# Doctora Vela: fair, white lab coat over a teal top, dark eyes (hair is a 3D bun)
img = compose("human", 0xF2D2B4, 0xEDEDF0, 0x3A2A5A, hair=0x6A3A9A)
paint(img, [(23, 20, 24, 23)], (40, 150, 160))         # teal top under the open coat
skins["scientist"] = img

# El Gran Juez: blue-grey giant in a deep purple judge's robe, fierce yellow eyes
img = compose("human", 0x7F8FB0, 0x3A1E5A, 0xE8C040, hair=0x1A1A1A)
skins["judge"] = img

# Maestro Celeste: pale violet skin, white robe with a gold sash, kind blue eyes
img = compose("human", 0xC9B8E8, 0xF4F0E6, 0x4A80D0, hair=0xF4F4F4)
paint(img, [(20, 29, 27, 29), (32, 29, 39, 29)], (232, 192, 64))
skins["celestial"] = img

for name, img in skins.items():
    img.save(os.path.join(OUT, name + ".png"))


def mirror(boxes):
    out = []
    for x, y, z, w, h, d in boxes:
        out += [[x, y, z, w, h, d], [-x - w, y, z, w, h, d]]
    return out


extras = {
    "master": {
        "head": {
            "beard": [[-3, -1.5, -4.4, 6, 3, 1], [-2.5, 1.5, -4.4, 5, 3, 1], [-1.8, 4.5, -4.3, 3.6, 3, 1], [-1, 7.5, -4.2, 2, 2, 1]],
            "beard_side": mirror([[-4.3, -3, -4.2, 1, 3.5, 2]]),
            "hat": [[-8, -9, -8, 16, 1, 16], [-6, -10, -6, 12, 1, 12], [-4, -11, -4, 8, 1, 8], [-2, -12, -2, 4, 1, 4]],
            "hat_band": [[-4.6, -9, -4.6, 9.2, 0.8, 9.2]],
        },
        "rightArm": {"staff": [[-1.6, -8, -2.8, 1.2, 30, 1.2]]},
    },
    "scientist": {
        "head": {
            "hair": [[-4.4, -8.6, -4.4, 8.8, 3, 8.8], [-4.4, -5.6, 0, 8.8, 4, 4.4], [-4.4, -6, -4.4, 1.2, 4, 4], [3.2, -6, -4.4, 1.2, 4, 4],
                     [-2, -11, 1, 4, 3, 4]],
            "goggles": [[-4.3, -7.6, -4.5, 8.6, 1, 0.6]],
            "lens": [[-3.4, -8.2, -4.9, 2.6, 2, 0.6], [0.8, -8.2, -4.9, 2.6, 2, 0.6]],
        },
    },
    "judge": {
        "head": {
            "beard": [[-4.4, -3, -4.6, 8.8, 4, 1.4], [-3.6, 1, -4.6, 7.2, 3, 1.4], [-2.4, 4, -4.5, 4.8, 2, 1.2]],
            "beard_side": mirror([[-4.6, -5, -4.4, 1, 6, 6]]),
            "hat": [[-4.6, -13, -4.6, 9.2, 5, 9.2], [-5, -8.6, -5, 10, 1, 10]],
            "hat_plate": [[-1.5, -12, -4.9, 3, 3, 0.5]],
        },
    },
    "celestial": {
        "head": {
            "hair": [[-4.4, -8.6, -4.4, 8.8, 2, 8.8], [-4.4, -7, 1, 8.8, 9, 3.6], [-3, 2, 2, 6, 5, 2.4]],
            "beard": [[-2, -1, -4.4, 4, 3, 1], [-1.2, 2, -4.3, 2.4, 2, 1]],
        },
    },
}
# Tint of every group, per character (the game multiplies a plain white texture by these)
COLORS = {
    "master": {"beard": 0xF2F2F2, "beard_side": 0xF2F2F2, "hat": 0xD9B86B, "hat_band": 0x802620, "staff": 0x734720},
    "scientist": {"hair": 0x6A3A9A, "goggles": 0x333338, "lens": 0x4CC0F2},
    "judge": {"beard": 0x1A1A1A, "beard_side": 0x1A1A1A, "hat": 0x1E1A24, "hat_plate": 0xE6C040},
    "celestial": {"hair": 0xF6F6F6, "beard": 0xF6F6F6},
}
for kind, parts in extras.items():
    parts["colors"] = COLORS[kind]
json.dump(extras, open(os.path.join(RES, "assets/dragonblock/npc_extras.json"), "w"), indent=1)
print("wrote", len(skins), "npc skins")
