"""Draws the 16x16 technique icons into one atlas (8 per row, same order as the Technique enum).

Run: python tools/gen_technique_icons.py src/main/resources/assets/dragonblock/textures/gui/techniques.png
"""
import math
import sys
from PIL import Image, ImageDraw

ORDER = ["ki_blast", "kamehameha", "galick_gun", "masenko", "final_flash", "makankosappo", "death_beam",
         "kienzan", "genkidama", "big_bang", "kiai", "taiyoken", "teleport"]
S = 16
atlas = Image.new("RGBA", (S * 8, S * 2), (0, 0, 0, 0))


def lighten(c, k):
    return tuple(min(255, int(v + (255 - v) * k)) for v in c[:3]) + (255,)


def orb(d, cx, cy, r, color):
    d.ellipse((cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1), fill=lighten(color, -0.0)[:3] + (90,))
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color)
    d.ellipse((cx - r * 0.5, cy - r * 0.5, cx + r * 0.5, cy + r * 0.5), fill=lighten(color, 0.7))


def beam(d, y, x0, x1, w, color):
    d.rectangle((x0, y - w - 1, x1, y + w + 1), fill=color[:3] + (90,))
    d.rectangle((x0, y - w, x1, y + w), fill=color)
    d.rectangle((x0, y - max(0, w - 1), x1, y + max(0, w - 1)), fill=lighten(color, 0.7))


def icon(name):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if name == "ki_blast":
        orb(d, 7.5, 7.5, 4, (150, 190, 255, 255))
        for a in range(0, 360, 45):
            x, y = 7.5 + math.cos(math.radians(a)) * 6.5, 7.5 + math.sin(math.radians(a)) * 6.5
            d.point((x, y), fill=(200, 220, 255, 200))
    elif name == "kamehameha":
        beam(d, 8, 5, 15, 2, (80, 195, 255, 255))
        orb(d, 4.5, 8, 3.5, (80, 195, 255, 255))
    elif name == "galick_gun":
        beam(d, 8, 5, 15, 2, (184, 104, 255, 255))
        orb(d, 4.5, 8, 3.5, (184, 104, 255, 255))
        d.line((1, 2, 4, 5), fill=(230, 200, 255, 255))
        d.line((1, 14, 4, 11), fill=(230, 200, 255, 255))
    elif name == "masenko":
        beam(d, 9, 3, 15, 1, (255, 228, 90, 255))
        orb(d, 4, 4, 3, (255, 228, 90, 255))
    elif name == "final_flash":
        beam(d, 8, 2, 15, 4, (255, 242, 122, 255))
        d.rectangle((0, 2, 1, 14), fill=(255, 250, 200, 255))
    elif name == "makankosappo":
        beam(d, 8, 1, 15, 0, (255, 210, 74, 255))
        for x in range(1, 16, 2):
            y = 8 + int(round(math.sin(x * 0.9) * 3))
            d.point((x, y), fill=(184, 77, 255, 255))
            d.point((x, 16 - y), fill=(184, 77, 255, 255))
    elif name == "death_beam":
        d.line((2, 13, 14, 3), fill=(255, 90, 200, 120), width=3)
        d.line((2, 13, 14, 3), fill=(255, 200, 240, 255), width=1)
        orb(d, 2.5, 12.5, 1.5, (255, 90, 200, 255))
    elif name == "kienzan":
        d.ellipse((1, 5, 14, 10), fill=(255, 232, 120, 110))
        d.ellipse((2, 6, 13, 9), fill=(255, 232, 120, 255))
        d.ellipse((5, 7, 10, 8), fill=(255, 250, 220, 255))
    elif name == "genkidama":
        orb(d, 7.5, 7.5, 6, (154, 223, 255, 255))
        for a in range(0, 360, 60):
            x, y = 7.5 + math.cos(math.radians(a)) * 4, 7.5 + math.sin(math.radians(a)) * 4
            d.point((x, y), fill=(255, 255, 255, 255))
    elif name == "big_bang":
        orb(d, 8.5, 7.5, 5, (122, 184, 255, 255))
        d.rectangle((0, 6, 2, 9), fill=(240, 200, 160, 255))  # the palm
    elif name == "kiai":
        for r, a in ((7, 120), (5, 200), (3, 255)):
            d.ellipse((7.5 - r, 7.5 - r, 7.5 + r, 7.5 + r), outline=(255, 255, 255, a))
    elif name == "taiyoken":
        for a in range(0, 360, 30):
            x, y = 7.5 + math.cos(math.radians(a)) * 7, 7.5 + math.sin(math.radians(a)) * 7
            d.line((7.5, 7.5, x, y), fill=(255, 250, 200, 180))
        orb(d, 7.5, 7.5, 3, (255, 255, 230, 255))
    elif name == "teleport":
        # Two fingers raised, with afterimage streaks
        d.rectangle((6, 3, 7, 11), fill=(240, 200, 160, 255))
        d.rectangle((9, 2, 10, 11), fill=(240, 200, 160, 255))
        d.rectangle((5, 10, 11, 14), fill=(220, 180, 140, 255))
        for x in (2, 13):
            d.line((x, 4, x, 12), fill=(220, 235, 255, 150))
    return img


for i, name in enumerate(ORDER):
    atlas.paste(icon(name), ((i % 8) * S, (i // 8) * S))
atlas.save(sys.argv[1])
print("wrote", len(ORDER), "icons")
