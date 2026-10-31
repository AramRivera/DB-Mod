"""Writes the hairstyle JSON files (original designs). Units: model pixels and degrees.

Each style has an optional "mass" (a block of hair volume on top of the skull the spikes grow out of)
and a list of spikes. Spike fields:
  p     root position (x, y, z); y negative is up, z negative is the face side
  back  sweep toward the back of the head: 0 = straight up, 90 = straight back, 180 = straight down;
        negative values lean forward (bangs use about -160 to fall over the forehead)
  side  tilt outward, in degrees
  w, d  root width and depth; the spike tapers in 4 steps to a point
  len   total length
  bend  extra sweep of the outer half (curls the tip)
  mirror  also place the spike mirrored on the other side
Preview with tools/preview_hair.py (Blender) after editing.
"""
import json, os, sys

out = sys.argv[1]
os.makedirs(out, exist_ok=True)

def s(x, y, z, back, side, w, length, bend=0, d=None, mirror=False):
    e = {"p": [x, y, z], "back": back, "side": side, "w": w, "len": length, "bend": bend}
    if d is not None: e["d"] = d
    if mirror: e["mirror"] = True
    return e

def bangs(xs, length, w=3.0):
    return [s(x, -7.6, -3.6, -158, x * 4, w, length - abs(x) * 0.25, bend=-12, d=1.8) for x in xs]

# Volume blocks the spikes rise out of: [x, y, z, w, h, d]
TALL_MASS = [-4.6, -10.6, -3.6, 9.2, 3.0, 8.4]
LOW_MASS = [-4.6, -9.6, -3.8, 9.2, 1.6, 8.8]

styles = {
    # Golden fan: big triangular clumps rising and sweeping back together
    "flare": {"mass": TALL_MASS, "spikes": [
        s(0, -10.0, -2.2, 14, 0, 5.6, 10, bend=10, d=4.4),
        s(0, -10.0, 0.6, 32, 0, 5.8, 11.5, bend=12, d=4.6),
        s(0, -9.6, 3.0, 55, 0, 5.4, 10.5, bend=14, d=4.2),
        s(2.4, -9.8, -1.4, 18, 22, 4.8, 9, bend=10, d=3.8, mirror=True),
        s(2.5, -9.6, 1.6, 40, 20, 5.0, 10, bend=12, d=4.0, mirror=True),
        s(3.8, -7.6, 0.0, 32, 48, 4.2, 7.5, bend=10, d=3.4, mirror=True),
        s(1.8, -6.0, 3.8, 138, 14, 4.6, 7.5, bend=10, d=3.4, mirror=True),
    ] + bangs([-2.0, 0.2, 2.1], 4.6)},
    # Bulkier fan for the muscled grades
    "heavy": {"mass": [-4.8, -11.0, -3.8, 9.6, 3.4, 8.8], "spikes": [
        s(0, -10.4, -2.2, 12, 0, 6.6, 10.5, bend=8, d=5.0),
        s(0, -10.4, 0.8, 30, 0, 6.8, 12, bend=10, d=5.2),
        s(0, -10.0, 3.2, 54, 0, 6.2, 11, bend=12, d=4.8),
        s(2.6, -10.0, -1.2, 16, 24, 5.6, 10, bend=8, d=4.4, mirror=True),
        s(2.7, -9.8, 1.8, 38, 22, 5.8, 11, bend=10, d=4.6, mirror=True),
        s(4.0, -7.8, 0.2, 30, 50, 4.8, 8.5, bend=8, d=3.8, mirror=True),
        s(1.9, -6.2, 3.9, 138, 16, 5.2, 8.5, bend=8, d=3.8, mirror=True),
    ] + bangs([-2.0, 0.2, 2.1], 4.2, w=3.4)},
    # Sharp vertical crest, one loose strand hanging over the brow
    "ridge": {"mass": TALL_MASS, "spikes": [
        s(0, -10.0, -1.8, 4, 0, 5.2, 13.5, bend=8, d=4.0),
        s(0, -10.0, 1.0, 20, 0, 5.4, 14, bend=10, d=4.2),
        s(0, -9.6, 3.2, 42, 0, 5.0, 12, bend=12, d=3.8),
        s(2.4, -9.8, -1.0, 6, 16, 4.6, 12, bend=8, d=3.6, mirror=True),
        s(2.5, -9.6, 1.8, 26, 15, 4.8, 12.5, bend=10, d=3.8, mirror=True),
        s(3.8, -7.6, 0.0, 18, 40, 4.0, 9, bend=8, d=3.2, mirror=True),
        s(1.8, -6.0, 3.8, 138, 14, 4.4, 8, bend=10, d=3.2, mirror=True),
        s(-1.2, -8.0, -3.8, -170, -6, 2.6, 8.5, bend=10, d=1.6),
    ]},
    # Mane: thick curtain to the waist, low crown
    "mane": {"mass": LOW_MASS, "spikes": [
        s(0, -9.2, -1.6, 22, 0, 5.2, 7, bend=10, d=4.0),
        s(2.4, -9.0, -1.0, 24, 24, 4.4, 6.5, bend=10, d=3.4, mirror=True),
        s(0, -7.8, 3.4, 172, 0, 6.0, 30, bend=4, d=3.2),
        s(2.0, -7.0, 3.8, 174, 5, 5.6, 31, bend=4, d=3.0, mirror=True),
        s(3.8, -6.0, 3.0, 172, 10, 5.0, 28, bend=4, d=2.8, mirror=True),
        s(4.3, -5.4, 0.6, 174, 12, 4.0, 19, bend=4, d=2.4, mirror=True),
    ] + bangs([-1.8, 1.6], 5.2)},
    # Untamed: outward clumps and a long back down to the shoulder blades
    "wild": {"mass": TALL_MASS, "spikes": [
        s(-1.6, -10.0, -2.0, 18, -26, 5.0, 9.5, bend=12, d=4.0),
        s(1.6, -10.0, -1.8, 20, 28, 5.0, 9, bend=12, d=4.0),
        s(0, -10.0, 0.6, 38, 0, 5.6, 10.5, bend=14, d=4.4),
        s(3.9, -7.6, 0.4, 40, 60, 4.4, 8, bend=12, d=3.4, mirror=True),
        s(0, -6.4, 3.9, 146, 0, 5.8, 16, bend=8, d=3.6),
        s(2.4, -6.2, 3.8, 140, 16, 5.2, 14.5, bend=8, d=3.4, mirror=True),
        s(4.0, -5.4, 2.8, 134, 30, 4.4, 11.5, bend=8, d=3.0, mirror=True),
    ] + bangs([-2.2, 0.0, 2.2], 4.8)},
    # Slicked back, flat to the skull
    "sleek": {"mass": LOW_MASS, "spikes": [
        s(0, -9.4, -2.8, 76, 0, 5.4, 9, bend=10, d=2.6),
        s(2.4, -9.2, -2.4, 74, 14, 4.6, 8.5, bend=10, d=2.4, mirror=True),
        s(0, -8.8, 0.8, 86, 0, 5.4, 8.5, bend=12, d=2.6),
        s(2.5, -8.6, 1.0, 84, 16, 4.6, 8, bend=12, d=2.4, mirror=True),
        s(3.9, -6.8, 0.4, 66, 34, 3.8, 6, bend=10, d=2.2, mirror=True),
    ] + bangs([-1.6, 1.5], 3.6)},
    # Tall central blade flanked by shorter ones
    "spire": {"mass": TALL_MASS, "spikes": [
        s(0, -10.2, -0.4, 2, 0, 6.0, 19, bend=6, d=4.4),
        s(2.2, -9.8, -0.8, 8, 16, 4.8, 13, bend=8, d=3.8, mirror=True),
        s(2.4, -9.6, 2.2, 32, 18, 4.6, 10.5, bend=12, d=3.6, mirror=True),
        s(3.9, -7.6, 0.2, 20, 46, 3.8, 8, bend=10, d=3.0, mirror=True),
        s(1.8, -6.0, 3.8, 138, 14, 4.2, 7, bend=10, d=3.0, mirror=True),
    ] + bangs([0.3], 5.4)},
    # Neat silver crest: calm, upright, few strands
    "crest": {"mass": TALL_MASS, "spikes": [
        s(0, -10.0, -1.8, 8, 0, 5.4, 9.5, bend=8, d=4.2),
        s(0, -10.0, 1.0, 24, 0, 5.6, 10, bend=10, d=4.4),
        s(2.4, -9.8, -1.0, 8, 18, 4.6, 8.5, bend=8, d=3.6, mirror=True),
        s(2.5, -9.6, 1.8, 30, 16, 4.6, 9, bend=10, d=3.6, mirror=True),
        s(3.8, -7.6, 0.0, 20, 42, 3.8, 6.5, bend=8, d=3.0, mirror=True),
        s(1.8, -6.0, 3.8, 138, 14, 4.2, 6.5, bend=8, d=3.0, mirror=True),
    ] + bangs([-1.7, 1.6], 3.8)},
}

for name, style in styles.items():
    with open(os.path.join(out, name + ".json"), "w") as f:
        json.dump(style, f, indent=1)
print("wrote", len(styles))
