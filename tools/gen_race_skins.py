"""Paints the race body textures as tint layers (64x64 Minecraft skin layout, classic/wide arms).

Each race gets:
  <race>_base.png    grayscale shading where skin shows; the mod multiplies it by the chosen skin color
  <race>_accent.png  grayscale, multiplied by the race's second color (shorts, gi, patches, gems...)
  <race>_detail.png  fixed colors drawn on top (eye whites, lashes, mouth, belts, shoes)
  <race>_brows.png   grayscale, multiplied by the hair color (races with hair)
  <race>_scalp.png   grayscale, multiplied by the hair color, only when the character has hair
plus eyes.png (iris mask, multiplied by the eye color). Designs are our own takes on each race.
Run: python tools/gen_race_skins.py <out_dir> [preview.png]
"""
import os
import sys
from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

# Box UV layouts: (u, v, w, h, d)
PARTS = {
    "head": (0, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4),
}
FACE_SHADE = {"top": 236, "front": 222, "right": 206, "left": 206, "back": 196, "bottom": 176}


def faces(part):
    u, v, w, h, d = PARTS[part]
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def new():
    return Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def g(v, a=255):
    v = max(0, min(255, int(v)))
    return (v, v, v, a)


class Canvas:
    """Paints in face-local coordinates: x from the face's left edge, y from its top edge."""

    def __init__(self):
        self.img = new()

    def face(self, part, face, fn):
        fu, fv, fw, fh = faces(part)[face]
        for y in range(fh):
            for x in range(fw):
                c = fn(x, y, fw, fh)
                if c is not None:
                    self.img.putpixel((fu + x, fv + y), c)

    def part(self, part, fn, only=None):
        for name in faces(part):
            if only and name not in only:
                continue
            self.face(part, name, lambda x, y, w, h, n=name: fn(n, x, y, w, h))

    def save(self, name):
        self.img.save(os.path.join(OUT, name))


# ------------------------------------------------------------------ shading helpers

def skin_shade(part, face, x, y, w, h):
    """Base skin value with soft vertical falloff and muscle definition on the torso and limbs."""
    v = FACE_SHADE[face]
    if face in ("front", "back", "right", "left") and h >= 12:
        v += 6 - y * 0.9  # light from above
    if part == "head" and face == "front":
        if y == 6 and x in (3, 4):
            v -= 14  # nose shadow
        if y == 7 and x in (1, 6):
            v -= 8  # jaw
    if part == "head" and face in ("right", "left") and y in (4, 5) and x in (3, 4):
        v -= 16  # ear
    if part == "body" and face == "front":
        if y in (1, 2, 3) and x in (1, 2, 5, 6):
            v += 8  # pecs catch the light
        if y == 4 and 1 <= x <= 6 and x not in (3, 4):
            v -= 22  # under the pecs
        if 1 <= y <= 8 and x in (3, 4):
            v -= 6 if x == 4 else 0  # sternum line
        if y in (6, 8) and 2 <= x <= 5:
            v -= 14  # abs
        if y in (5, 7) and 2 <= x <= 5:
            v += 6
    if part == "body" and face == "back":
        if y in (1, 2, 3) and x in (1, 2, 5, 6):
            v += 6  # shoulder blades
        if 2 <= y <= 8 and x == 4:
            v -= 10  # spine
    if part in ("rarm", "larm") and face in ("front", "back", "right", "left"):
        if y in (0, 1):
            v += 10  # deltoid
        if y == 2:
            v -= 8
        if y == 5:
            v -= 10  # elbow crease
        if y >= 10:
            v -= 6  # hands
    if part in ("rleg", "lleg") and face in ("front", "back", "right", "left"):
        if y == 6:
            v -= 10  # knee
        if y == 11:
            v -= 8
    return v


def cloth(v, x, y, folds=True):
    """Fabric: slightly darker than skin shading with fold lines."""
    if folds and (x + y) % 5 == 0:
        v -= 12
    return v - 10


# ------------------------------------------------------------------ face

def paint_face(detail, eyes, brows, lash=(20, 18, 20, 255), white=(244, 244, 240, 255), mouth=(70, 30, 30, 120),
               brows_on=True):
    def d(x, y, w, h):
        if y == 4 and x in (1, 2, 5, 6):
            return lash
        if y == 5 and x in (1, 6):
            return white
        if y == 7 and x in (3, 4):
            return mouth
        return None
    detail.face("head", "front", d)
    eyes.face("head", "front", lambda x, y, w, h: g(255) if y == 5 and x in (2, 5) else None)
    if brows_on:
        # Brows a row above the lashes, angled down toward the nose
        brows.face("head", "front", lambda x, y, w, h: g(150) if (y == 2 and x in (1, 6)) or (y == 3 and x in (2, 5)) else None)


def paint_scalp(scalp):
    """Hair color under the hair model: top, back and the upper sides of the head, and a fringe line."""
    def s(face, x, y, w, h):
        if face == "top":
            return g(200 + (x * 7 + y * 3) % 30)
        if face == "back" and y < 6:
            return g(190 + (x * 5) % 25)
        if face in ("right", "left") and (y < 3 or (y < 5 and (x < 3 if face == "right" else x > 4))):
            return g(185)
        if face == "front" and y == 0:
            return g(180)
        return None
    scalp.part("head", s)


# ------------------------------------------------------------------ races

def humanoid_base(base):
    for part in PARTS:
        base.part(part, lambda face, x, y, w, h, p=part: g(skin_shade(p, face, x, y, w, h)))


def saiyan():
    """Bare-chested warrior in dark shorts, barefoot. The accent tints the shorts."""
    base, accent, detail, brows, scalp, eyes = Canvas(), Canvas(), Canvas(), Canvas(), Canvas(), Canvas()
    humanoid_base(base)
    paint_face(detail, eyes, brows)
    paint_scalp(scalp)

    def shorts_body(face, x, y, w, h):
        if face == "bottom":
            return g(160)
        if face in ("front", "back", "right", "left") and y >= 9:
            return g(cloth(FACE_SHADE[face] + (12 if y == 9 else 0), x, y, folds=False))
        return None
    accent.part("body", shorts_body)

    def shorts_leg(face, x, y, w, h):
        if face == "top":
            return g(200)
        if face in ("front", "back", "right", "left") and y <= 4:
            v = cloth(FACE_SHADE[face], x, y)
            if y == 4:
                v -= 18  # hem
            return g(v)
        return None
    for leg in ("rleg", "lleg"):
        accent.part(leg, shorts_leg)
        # Toes and soles of bare feet
        base.part(leg, lambda face, x, y, w, h: g(150) if face == "bottom" else None, only=["bottom"])
        detail.face(leg, "front", lambda x, y, w, h: (0, 0, 0, 40) if y == 11 and x in (1, 2) else None)
    return {"base": base, "accent": accent, "detail": detail, "brows": brows, "scalp": scalp, "eyes": eyes}


def human():
    """Martial artist's gi (accent) over a dark undershirt, dark sash, wristbands and boots."""
    base, accent, detail, brows, scalp, eyes = Canvas(), Canvas(), Canvas(), Canvas(), Canvas(), Canvas()
    humanoid_base(base)
    paint_face(detail, eyes, brows)
    paint_scalp(scalp)
    under, sash, boot, band = (30, 38, 70, 255), (26, 28, 40, 255), (40, 34, 30, 255), (34, 40, 74, 255)

    def gi_body(face, x, y, w, h):
        if face == "top":
            return g(220)
        if face == "bottom":
            return g(170)
        return g(cloth(FACE_SHADE[face], x, y))
    accent.part("body", gi_body)

    def body_detail(x, y, w, h):
        if y == 9:
            return sash
        if y <= 3 and x in (3, 4) and y >= abs(x - 3.5) - 0.5:
            return under  # V of the undershirt
        return None
    detail.face("body", "front", body_detail)
    for face in ("back", "right", "left"):
        detail.face("body", face, lambda x, y, w, h: sash if y == 9 else None)

    for arm in ("rarm", "larm"):
        accent.part(arm, lambda face, x, y, w, h: g(cloth(FACE_SHADE[face], x, y)) if face == "top" or (face not in ("bottom",) and y <= 3) else None)
        for face in ("front", "back", "right", "left"):
            detail.face(arm, face, lambda x, y, w, h: band if y in (9, 10) else None)
    for leg in ("rleg", "lleg"):
        accent.part(leg, lambda face, x, y, w, h: g(cloth(FACE_SHADE[face], x, y)) if face == "top" or (face != "bottom" and y <= 8) else None)
        for face in ("front", "back", "right", "left", "bottom"):
            detail.face(leg, face, lambda x, y, w, h, f=face: (boot if f == "bottom" or y >= 9 else None))
        detail.face(leg, "front", lambda x, y, w, h: (70, 60, 52, 255) if y == 9 else None)  # boot cuff
    return {"base": base, "accent": accent, "detail": detail, "brows": brows, "scalp": scalp, "eyes": eyes}


def namekian():
    """Green skin with colored muscle patches on the arms (accent), sleeveless gi, sash, shoes."""
    base, accent, detail, brows, eyes = Canvas(), Canvas(), Canvas(), Canvas(), Canvas()
    humanoid_base(base)
    # Brow ridge instead of eyebrows
    base.face("head", "front", lambda x, y, w, h: g(192) if (y == 2 and x in (1, 6)) or (y == 3 and x in (2, 5)) else None)
    paint_face(detail, eyes, brows, mouth=(40, 30, 40, 130), brows_on=False)
    gi, sash, shoe = (78, 52, 120, 255), (90, 160, 216, 255), (100, 70, 44, 255)

    def neckline(x, y):
        return (y == 0 and 2 <= x <= 5) or (y in (1, 2) and x in (3, 4))

    def gi_shade(face, x, y):
        s = (FACE_SHADE[face] - 222) // 2
        return tuple(max(0, min(255, c + s - (10 if (x + y) % 5 == 0 else 0))) for c in gi[:3]) + (255,)

    for face in ("front", "back", "right", "left", "top", "bottom"):
        detail.face("body", face, lambda x, y, w, h, f=face: sash if y == 9 and f not in ("top", "bottom")
                    else gi_shade(f, x, y) if f != "front" or not neckline(x, y) else None)
    for leg in ("rleg", "lleg"):
        for face in ("front", "back", "right", "left", "top", "bottom"):
            detail.face(leg, face, lambda x, y, w, h, f=face: shoe if f == "bottom" or y >= 10 else gi_shade(f, x, y))
    # Patches on the forearms and upper arms
    for arm in ("rarm", "larm"):
        # Two solid bands per arm, brighter in the middle like rounded muscle
        accent.part(arm, lambda face, x, y, w, h: g(FACE_SHADE[face] + (14 if y in (2, 7) else 0))
                    if face in ("front", "back", "right", "left") and (1 <= y <= 3 or 6 <= y <= 8) else None)
    return {"base": base, "accent": accent, "detail": detail, "brows": brows, "eyes": eyes}


def arcosian():
    """Smooth armored body (the skin color is the plating) with glossy gems (accent) on head, shoulders and chest."""
    base, accent, detail, brows, eyes = Canvas(), Canvas(), Canvas(), Canvas(), Canvas()
    for part in PARTS:
        def plate(face, x, y, w, h, p=part):
            v = FACE_SHADE[face] + 8
            if p in ("rarm", "larm", "rleg", "lleg") and face != "top" and y in (4, 8):
                v -= 20  # plate seams
            if p == "body" and face == "front" and y in (6, 8) and 1 <= x <= 6:
                v -= 14  # segmented belly
            if p == "body" and face == "front" and y == 9:
                v -= 22
            return g(v)
        base.part(part, plate)
    base.face("head", "front", lambda x, y, w, h: g(170) if y == 3 and x in (1, 2, 5, 6) else None)
    paint_face(detail, eyes, brows, lash=(30, 10, 20, 255), mouth=(70, 40, 80, 150), brows_on=False)
    # Purple-black lips line under the eyes like a mask edge
    detail.face("head", "front", lambda x, y, w, h: (60, 40, 70, 255) if y == 6 and x in (0, 7) else None)

    def gem(cx, cy, r):
        def f(x, y, w, h):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy > r * r:
                return None
            return g(250 if dx <= -0.2 and dy <= -0.2 else 200)
        return f
    accent.face("head", "top", gem(3.5, 3.5, 2.6))
    accent.face("head", "front", lambda x, y, w, h: g(230) if y == 0 and 2 <= x <= 5 else None)
    accent.face("body", "front", lambda x, y, w, h: g(250 if (x, y) in ((2, 1), (3, 1)) else 205)
                if (y == 1 and 2 <= x <= 5) or (y == 2 and 1 <= x <= 6) or (y == 3 and 2 <= x <= 5) else None)
    for arm in ("rarm", "larm"):
        accent.face(arm, "top", gem(1.5, 1.5, 1.6))
        accent.face(arm, "front", lambda x, y, w, h: g(210) if y in (0, 1) else None)
    for leg in ("rleg", "lleg"):
        accent.face(leg, "front", lambda x, y, w, h: g(220) if y in (5, 6) and x in (1, 2) else None)  # knee gems
    return {"base": base, "accent": accent, "detail": detail, "brows": brows, "eyes": eyes}


def majin():
    """Soft pink body, baggy trousers (accent), gold belt and cuffs, dark boots."""
    base, accent, detail, brows, eyes = Canvas(), Canvas(), Canvas(), Canvas(), Canvas()
    for part in PARTS:
        base.part(part, lambda face, x, y, w, h, p=part: g(skin_shade(p, face, x, y, w, h) + 6))
    paint_face(detail, eyes, brows, mouth=(110, 40, 60, 150), brows_on=False)
    gold, boot = (232, 192, 64, 255), (28, 26, 36, 255)
    accent.part("body", lambda face, x, y, w, h: g(cloth(FACE_SHADE[face], x, y)) if face == "bottom" or (face != "top" and y >= 10) else None)
    for face in ("front", "back", "right", "left"):
        detail.face("body", face, lambda x, y, w, h: gold if y == 9 else None)
    for leg in ("rleg", "lleg"):
        accent.part(leg, lambda face, x, y, w, h: g(cloth(FACE_SHADE[face] + (6 if x in (1, 2) else 0), x, y)) if face == "top" or (face != "bottom" and y <= 8) else None)
        for face in ("front", "back", "right", "left", "bottom"):
            detail.face(leg, face, lambda x, y, w, h, f=face: boot if f == "bottom" or y >= 9 else None)
    for arm in ("rarm", "larm"):
        for face in ("front", "back", "right", "left"):
            detail.face(arm, face, lambda x, y, w, h: gold if y in (8, 9) else None)
    return {"base": base, "accent": accent, "detail": detail, "brows": brows, "eyes": eyes}


RACES = {"saiyan": saiyan, "human": human, "namekian": namekian, "arcosian": arcosian, "majin": majin}

layers_by_race = {}
for race, fn in RACES.items():
    layers = fn()
    layers_by_race[race] = layers
    for name, canvas in layers.items():
        if name == "eyes":
            continue
        canvas.save(f"{race}_{name}.png")
layers_by_race["saiyan"]["eyes"].save("eyes.png")


# ------------------------------------------------------------------ preview (same compositing as RaceSkins.java)

def tint(img, rgb, sheen=True):
    out = new()
    for y in range(64):
        for x in range(64):
            v, _, _, a = img.getpixel((x, y))
            if a == 0:
                continue
            add = max(0, v - 200) / 55 * 36 if sheen else 0
            out.putpixel((x, y), tuple(min(255, int(c * v / 255 + add)) for c in rgb) + (a,))
    return out


def compose(race, skin, accent, hair, eyes, has_hair=True):
    L = layers_by_race[race]
    img = tint(L["base"].img, skin)
    if has_hair and "scalp" in L:
        img.alpha_composite(tint(L["scalp"].img, hair))
    img.alpha_composite(tint(L["accent"].img, accent))
    img.alpha_composite(L["detail"].img)
    img.alpha_composite(tint(L["brows"].img, hair))
    img.alpha_composite(tint(layers_by_race["saiyan"]["eyes"].img, eyes, sheen=False))
    return img


def hexrgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


if len(sys.argv) > 2:
    samples = [
        ("saiyan", 0xF2CFA6, 0x16161C, 0x15120F, 0x1A1A1A),
        ("human", 0xE7B98A, 0x2A4A9A, 0x3A2416, 0x2A4A8A),
        ("namekian", 0x6BBF4A, 0xE59AA4, 0, 0x1A1A1A),
        ("arcosian", 0xF2EEF6, 0x8A3CC8, 0, 0xB02030),
        ("majin", 0xF59AC8, 0xF4F0F0, 0, 0x1A1A1A),
    ]
    sheet = Image.new("RGBA", (64 * 5 * 6, 64 * 6), (40, 44, 56, 255))
    for i, (race, skin, acc, hair, eye) in enumerate(samples):
        img = compose(race, hexrgb(skin), hexrgb(acc), hexrgb(hair), hexrgb(eye), has_hair=race in ("saiyan", "human"))
        sheet.paste(img.resize((64 * 6, 64 * 6), Image.NEAREST), (i * 64 * 6, 0), img.resize((64 * 6, 64 * 6), Image.NEAREST))
        img.save(os.path.join(os.path.dirname(sys.argv[2]), f"sample_{race}.png"))
    sheet.save(sys.argv[2])
print("wrote", len(RACES), "races")
