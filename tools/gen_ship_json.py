"""Writes assets/dragonblock/ship.json: the round capsule spaceship as color groups of boxes (model pixels,
y negative = up, front toward -z, feet at y = 0). Our own design: a white sphere with an orange band, a big
round window, a hatch, three landing legs and an antenna. Preview: tools/preview_box_model.py.
Run: python tools/gen_ship_json.py src/main/resources/assets/dragonblock/ship.json
"""
import json
import math
import sys

R = 30          # hull radius
CY = -38        # hull center height (feet at 0)
hull, band, window, rim, door, legs, dark = [], [], [], [], [], [], []

# Sphere as horizontal slices; each slice is a plus of two slabs and a square, which reads as an octagon
for y0 in range(-R, R, 4):
    yc = y0 + 2
    half = math.sqrt(max(0.0, R * R - yc * yc))
    if half < 4:
        continue
    w = round(half * 2)
    s = round(half * 1.42)
    target = band if -3 <= yc <= 3 else hull
    target.append([-w / 2, CY + y0, -s / 2, w, 4, s])
    target.append([-s / 2, CY + y0, -w / 2, s, 4, w])
    sq = round(half * 1.7)
    target.append([-sq / 2, CY + y0, -sq / 2, sq, 4, sq])

# Front window, a glass disc with a dark rim, above the band
rim.append([-11, CY - 25.3, -R - 0.8, 22, 14.3, 1.4])        # frame, flush with the hull
window.append([-9, CY - 24.7, -R - 1.4, 18, 11.7, 1.0])     # glass, standing just proud of the frame
# Hatch below the band and a dark hull underside
door.append([-6, CY + 6, -R + 1.8, 12, 14, 1.2])
dark.append([-14, CY + R - 4, -14, 28, 4, 28])
# Three legs angled out with round pads, and an antenna on top
for k in range(3):
    a = math.radians(90 + k * 120)
    x, z = math.cos(a) * 20, math.sin(a) * 20
    legs.append([x - 1.5, CY + R - 6, z - 1.5, 3, 12, 3])
    legs.append([x - 3.5, -1, z - 3.5, 7, 1, 7])
legs.append([-0.8, CY - R - 10, -0.8, 1.6, 10, 1.6])
dark.append([-2, CY - R - 13, -2, 4, 3, 4])

ship = {"hull": hull, "band": band, "window": window, "rim": rim, "door": door, "legs": legs, "dark": dark,
        "colors": {"hull": 0xF2F2F0, "band": 0xF28C28, "window": 0x6FC8F2, "rim": 0x2A2E36, "door": 0xC8CCD4, "legs": 0x8A8E96, "dark": 0x3A3E46}}
json.dump(ship, open(sys.argv[1], "w"), indent=1)
print("wrote ship:", sum(len(v) for k, v in ship.items() if k != "colors"), "boxes")
