"""Writes the hairstyle JSON files (original designs). Units: model pixels and degrees.

Design rules (voxel anime style): strands are wide flat plates, not sticks (w much larger than d);
the hair hugs the skull and spills out to the sides; wide bangs cover the top of the forehead;
longer strands toward the back. Every style is our own layout, not any existing character's.

Spike fields: p = root (x, y, z; y negative is up, z negative is the face side), back = sweep toward
the back (0 up, 90 back, 180 down; negative leans forward), side = tilt outward, w/d = root width and
depth (tapers in 4 steps), len = length, bend = extra sweep of the outer half, mirror = also place it
mirrored. "mass" [x, y, z, w, h, d] is a block of volume the strands grow from.
Preview with tools/preview_hair.py after editing.
"""
import json, os, sys

out = sys.argv[1]
os.makedirs(out, exist_ok=True)


def s(x, y, z, back, side, w, length, bend=0, d=2.4, mirror=False):
    e = {"p": [x, y, z], "back": back, "side": side, "w": w, "len": length, "bend": bend, "d": d}
    if mirror:
        e["mirror"] = True
    return e


def fringe(widths_x, length, lean=-150, w=4.2):
    """Wide plates falling over the forehead, angled a little outward."""
    return [s(x, -8.0, -3.2, lean, x * 6, w, length - abs(x) * 0.4, bend=-18, d=1.8) for x in widths_x]


def temples(length, back=165, side=18, w=3.6):
    """Strands over the ears framing the face."""
    return [s(4.2, -6.4, -1.8, back, side, w, length, bend=-10, d=1.8, mirror=True)]


SKULL = [-4.8, -9.6, -4.2, 9.6, 2.2, 9.2]      # volume hugging the skull
TALL = [-4.9, -10.4, -4.0, 9.8, 3.0, 9.0]

styles = {
    # Golden tier: wide plates blown up and back, flaring out at the sides
    "flare": {"mass": TALL, "spikes": [
        s(0, -10.0, -2.6, 16, 0, 7.0, 10, 16, 2.6),
        s(0, -10.0, 0.4, 36, 0, 7.4, 12, 18, 2.8),
        s(0, -9.4, 3.0, 64, 0, 7.0, 11, 20, 2.6),
        s(2.8, -9.8, -1.6, 22, 30, 6.0, 9.5, 16, 2.4, mirror=True),
        s(3.2, -9.4, 1.6, 44, 34, 6.2, 10.5, 18, 2.4, mirror=True),
        s(4.4, -7.6, -0.2, 40, 62, 5.4, 8, 14, 2.2, mirror=True),
        s(4.0, -6.2, 3.0, 110, 46, 5.0, 7, 12, 2.2, mirror=True),
        s(0, -5.6, 4.2, 140, 0, 6.4, 8, 12, 2.4),
    ] + temples(5.5) + fringe([-2.4, 0.0, 2.4], 5.5)},
    # Massive version for the muscled forms: thicker and wider
    "heavy": {"mass": [-5.1, -10.8, -4.2, 10.2, 3.4, 9.4], "spikes": [
        s(0, -10.4, -2.6, 14, 0, 8.4, 11, 14, 3.0),
        s(0, -10.4, 0.4, 34, 0, 8.8, 13, 16, 3.2),
        s(0, -9.8, 3.0, 62, 0, 8.4, 12, 18, 3.0),
        s(3.0, -10.2, -1.6, 20, 32, 7.2, 10.5, 14, 2.8, mirror=True),
        s(3.4, -9.8, 1.6, 42, 36, 7.4, 11.5, 16, 2.8, mirror=True),
        s(4.6, -7.8, -0.2, 38, 64, 6.4, 9, 12, 2.6, mirror=True),
        s(4.2, -6.2, 3.0, 108, 48, 6.0, 8, 12, 2.6, mirror=True),
        s(0, -5.6, 4.4, 138, 0, 7.6, 9, 12, 2.8),
    ] + temples(6, w=4.2) + fringe([-2.4, 0.0, 2.4], 5.2, w=4.8)},
    # Sharp upright blades, one long loose strand over the brow
    "ridge": {"mass": TALL, "spikes": [
        s(0, -10.0, -2.2, 6, 0, 6.4, 13.5, 10, 2.4),
        s(0, -10.0, 0.6, 22, 0, 6.8, 14.5, 12, 2.6),
        s(0, -9.4, 3.2, 50, 0, 6.4, 12.5, 14, 2.4),
        s(2.7, -9.8, -1.2, 8, 22, 5.6, 12, 10, 2.2, mirror=True),
        s(3.1, -9.4, 1.8, 28, 26, 5.8, 12.5, 12, 2.2, mirror=True),
        s(4.4, -7.6, 0.0, 26, 50, 5.0, 9, 10, 2.0, mirror=True),
        s(0, -5.6, 4.2, 136, 0, 6.0, 8, 10, 2.2),
        s(-1.6, -8.2, -3.4, -168, -4, 3.2, 10, 16, 1.6),
    ] + temples(6)},
    # Mane falling to the waist, low crown, heavy fringe
    "mane": {"mass": SKULL, "spikes": [
        s(0, -9.2, -2.0, 24, 0, 7.0, 7.5, 14, 2.4),
        s(2.8, -9.0, -1.2, 28, 30, 6.0, 7, 12, 2.2, mirror=True),
        s(0, -8.2, 3.6, 172, 0, 8.0, 32, 5, 2.6),
        s(2.6, -7.4, 3.8, 174, 6, 7.4, 33, 4, 2.4, mirror=True),
        s(4.4, -6.4, 2.4, 172, 12, 6.0, 29, 4, 2.2, mirror=True),
        s(4.6, -5.6, 0.0, 176, 14, 4.6, 21, 3, 2.0, mirror=True),
    ] + fringe([-2.6, 0.0, 2.6], 6.5, w=4.6)},
    # Untamed dark style: ragged plates and a long back to the shoulder blades
    "wild": {"mass": TALL, "spikes": [
        s(-1.8, -10.0, -2.2, 18, -30, 6.2, 9.5, 20, 2.6),
        s(1.8, -10.0, -2.0, 22, 32, 6.2, 9, 20, 2.6),
        s(0, -9.8, 0.6, 40, 0, 7.0, 11, 22, 2.8),
        s(4.4, -7.6, 0.4, 46, 68, 5.6, 8.5, 18, 2.4, mirror=True),
        s(0, -6.6, 4.2, 152, 0, 7.6, 17, 8, 2.6),
        s(2.8, -6.4, 4.0, 146, 18, 6.8, 16, 8, 2.4, mirror=True),
        s(4.4, -5.6, 2.8, 140, 34, 5.6, 13, 8, 2.2, mirror=True),
    ] + temples(6.5, w=4.0) + fringe([-2.6, 0.0, 2.6], 6.0, w=4.4)},
    # Slicked back: flat plates lying along the skull
    "sleek": {"mass": SKULL, "spikes": [
        s(0, -9.4, -3.0, 76, 0, 7.4, 9.5, 12, 2.0),
        s(2.8, -9.2, -2.6, 74, 16, 6.2, 9, 12, 1.8, mirror=True),
        s(0, -8.8, 0.6, 88, 0, 7.4, 9, 14, 2.0),
        s(2.9, -8.6, 0.8, 86, 18, 6.2, 8.5, 14, 1.8, mirror=True),
        s(4.4, -6.8, 0.2, 70, 36, 5.0, 6.5, 10, 1.8, mirror=True),
    ] + temples(4.5, w=3.2) + fringe([-2.0, 2.0], 4.0, lean=-160, w=3.6)},
    # Tall central blade over swept sides
    "spire": {"mass": TALL, "spikes": [
        s(0, -10.2, -0.8, 2, 0, 7.6, 19, 6, 2.8),
        s(2.6, -9.8, -1.2, 10, 22, 5.8, 13, 10, 2.4, mirror=True),
        s(3.0, -9.4, 2.0, 34, 28, 5.8, 11, 14, 2.4, mirror=True),
        s(4.4, -7.6, 0.2, 26, 54, 5.0, 8.5, 12, 2.2, mirror=True),
        s(0, -5.6, 4.2, 138, 0, 6.0, 8, 12, 2.2),
    ] + temples(5.5) + fringe([0.2], 6.5, w=4.6)},
    # Calm silver crest: neat, upright, few plates
    "crest": {"mass": TALL, "spikes": [
        s(0, -10.0, -2.2, 10, 0, 6.8, 9.5, 10, 2.4),
        s(0, -10.0, 0.6, 28, 0, 7.0, 10.5, 12, 2.6),
        s(2.8, -9.8, -1.2, 12, 24, 5.8, 8.5, 10, 2.2, mirror=True),
        s(3.1, -9.4, 1.8, 32, 26, 5.8, 9, 12, 2.2, mirror=True),
        s(0, -5.6, 4.2, 136, 0, 6.0, 7, 10, 2.2),
    ] + temples(5) + fringe([-2.2, 2.2], 4.5, w=4.0)},
}

for name, style in styles.items():
    with open(os.path.join(out, name + ".json"), "w") as f:
        json.dump(style, f, indent=1)
print("wrote", len(styles))
