"""Writes the hairstyle JSON files (original designs). Units: model pixels and degrees.

Design rule: hair is built in rows from the hairline to the nape, like layered flames. Each row
leans further back and the rows get longer toward the back, so the silhouette sweeps back instead of
fanning out from the center. Sideburns frame the face and bangs fall over the forehead.

Spike fields: p = root (x, y, z; y negative is up, z negative is the face side), back = sweep toward
the back (0 up, 90 back, 180 down; negative leans forward), side = tilt outward, w/d = root width and
depth (tapers in 4 steps), len = length, bend = extra sweep of the outer half, mirror = also place it
mirrored. Each style may also have "mass" [x, y, z, w, h, d], a block of volume the spikes grow from.
Preview with tools/preview_hair.py after editing.
"""
import json, os, sys

out = sys.argv[1]
os.makedirs(out, exist_ok=True)


def s(x, y, z, back, side, w, length, bend=0, d=None, mirror=False):
    e = {"p": [x, y, z], "back": back, "side": side, "w": w, "len": length, "bend": bend}
    if d is not None:
        e["d"] = d
    if mirror:
        e["mirror"] = True
    return e


def row(z, y, back, lengths, width, depth, spread, bend, side_tilt):
    """One row of spikes across the head at depth z; lengths go center -> outside, mirrored."""
    spikes = [s(0, y, z, back, 0, width, lengths[0], bend, depth)]
    for i, length in enumerate(lengths[1:], start=1):
        x = spread * i
        spikes.append(s(x, y + 0.3 * i, z, back + 4 * i, side_tilt * i, width * (1 - 0.08 * i), length, bend, depth * 0.92, mirror=True))
    return spikes


def sideburns(length, back=170, w=2.8):
    return [s(4.1, -5.6, -2.2, back, 6, w, length, bend=-8, d=1.8, mirror=True)]


def bangs(xs, length, w=3.2, lean=-160):
    return [s(x, -7.8, -3.4, lean, x * 5, w, length - abs(x) * 0.35, bend=-14, d=2.0) for x in xs]


MASS_UP = [-4.7, -10.2, -3.8, 9.4, 2.6, 8.6]
MASS_LOW = [-4.7, -9.4, -3.8, 9.4, 1.4, 9.0]

styles = {
    # Golden tier: three rows sweeping up and back, flame-like
    "flare": {"mass": MASS_UP, "spikes":
        row(-2.4, -9.8, 10, [9, 8, 6.5], 5.6, 4.0, 2.6, 14, 14)
        + row(0.6, -9.8, 34, [12, 11, 9], 5.8, 4.2, 2.6, 16, 16)
        + row(3.2, -8.8, 62, [11, 10, 8], 5.6, 4.0, 2.7, 18, 18)
        + [s(0, -6.0, 4.0, 128, 0, 5.4, 8, 12, 3.4), s(2.6, -5.8, 3.8, 132, 18, 4.8, 7, 12, 3.2, mirror=True)]
        + sideburns(4.5) + bangs([-2.1, 0.1, 2.0], 5.2)},
    # Heavier and denser for the muscled grades
    "heavy": {"mass": [-4.9, -10.6, -4.0, 9.8, 3.0, 9.0], "spikes":
        row(-2.4, -10.2, 8, [10, 9, 7], 6.8, 5.0, 2.8, 12, 16)
        + row(0.6, -10.2, 32, [13, 12, 10], 7.0, 5.2, 2.8, 14, 18)
        + row(3.2, -9.2, 60, [12, 11, 9], 6.6, 4.8, 2.9, 16, 20)
        + [s(0, -6.0, 4.0, 126, 0, 6.2, 9, 12, 3.8), s(2.7, -5.8, 3.8, 130, 20, 5.6, 8, 12, 3.6, mirror=True)]
        + sideburns(4.5, w=3.4) + bangs([-2.1, 0.1, 2.0], 4.8, w=3.6)},
    # Sharp, nearly vertical rows and a single long strand over the brow
    "ridge": {"mass": MASS_UP, "spikes":
        row(-2.2, -9.8, 4, [13, 11, 8.5], 5.2, 3.8, 2.6, 10, 10)
        + row(0.8, -9.8, 22, [15, 13, 10.5], 5.4, 4.0, 2.6, 12, 12)
        + row(3.3, -8.8, 48, [13, 11.5, 9], 5.2, 3.8, 2.7, 14, 14)
        + [s(0, -6.0, 4.0, 124, 0, 5.0, 8.5, 10, 3.2), s(2.6, -5.8, 3.8, 128, 18, 4.4, 7.5, 10, 3.0, mirror=True)]
        + sideburns(5)
        + [s(-1.3, -8.0, -3.5, -172, -6, 2.8, 9.5, bend=14, d=1.6)]},
    # Long mane falling to the waist; short crown, heavy brow fringe
    "mane": {"mass": MASS_LOW, "spikes":
        row(-1.8, -9.0, 22, [7, 6, 5], 5.4, 4.0, 2.6, 12, 18)
        + [s(0, -8.0, 3.6, 172, 0, 6.4, 31, 5, 3.4),
           s(2.2, -7.2, 3.9, 174, 5, 6.0, 32, 4, 3.2, mirror=True),
           s(4.0, -6.2, 2.8, 172, 11, 5.2, 28, 4, 3.0, mirror=True),
           s(4.4, -5.4, 0.4, 175, 12, 4.2, 20, 3, 2.4, mirror=True)]
        + bangs([-2.2, 0.0, 2.1], 5.8, w=3.4)},
    # Untamed: rows tilted outward plus a long back to the shoulder blades
    "wild": {"mass": MASS_UP, "spikes":
        row(-2.2, -9.8, 16, [8.5, 8, 7], 5.2, 4.0, 2.7, 18, 26)
        + row(0.8, -9.6, 40, [10, 9.5, 8], 5.4, 4.2, 2.7, 20, 28)
        + [s(0, -6.6, 3.9, 150, 0, 6.0, 16, 8, 3.6),
           s(2.4, -6.4, 3.8, 144, 16, 5.4, 15, 8, 3.4, mirror=True),
           s(4.0, -5.6, 2.8, 138, 30, 4.6, 12, 8, 3.0, mirror=True)]
        + sideburns(5, w=3.2) + bangs([-2.3, 0.0, 2.2], 5.0)},
    # Slicked back: flat rows hugging the skull
    "sleek": {"mass": MASS_LOW, "spikes":
        row(-2.8, -9.2, 74, [9, 8.5, 7], 5.4, 2.6, 2.5, 12, 10)
        + row(0.8, -8.6, 86, [8.5, 8, 6.5], 5.4, 2.6, 2.5, 14, 12)
        + sideburns(4) + bangs([-1.7, 1.6], 3.8)},
    # Central blade standing tall over shorter rows
    "spire": {"mass": MASS_UP, "spikes":
        [s(0, -10.0, -0.6, 2, 0, 6.2, 19, 6, 4.4)]
        + row(-2.2, -9.6, 12, [0.1, 9, 7], 5.0, 3.8, 2.5, 12, 14)[1:]
        + row(1.6, -9.4, 36, [0.1, 11, 9], 5.2, 3.8, 2.6, 14, 16)[1:]
        + [s(0, -6.0, 4.0, 128, 0, 5.0, 8, 12, 3.2)]
        + sideburns(4.5) + bangs([0.3], 5.6)},
    # Calm silver crest: neat, upright, few strands
    "crest": {"mass": MASS_UP, "spikes":
        row(-2.2, -9.8, 8, [9, 8, 6], 5.4, 4.0, 2.6, 10, 12)
        + row(0.8, -9.8, 28, [10.5, 9.5, 7.5], 5.6, 4.2, 2.6, 12, 14)
        + [s(0, -6.0, 4.0, 126, 0, 5.0, 7, 10, 3.2)]
        + sideburns(4) + bangs([-1.8, 1.7], 4.2)},
}

for name, style in styles.items():
    with open(os.path.join(out, name + ".json"), "w") as f:
        json.dump(style, f, indent=1)
print("wrote", len(styles))
