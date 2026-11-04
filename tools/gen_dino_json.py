"""Writes assets/dragonblock/dino.json: the Earth dinosaur as a tree of jointed parts, each a set of boxes
grouped by color (model pixels, y negative = up, front toward -z, feet at y = 0). A big two-legged hunter
with a heavy head, small arms and a long tail, in olive green with a pale belly and a dark ridge.
Each part's pivot is relative to its parent's pivot, and its boxes are relative to its own pivot; the
renderer (DinoRenderer) swings the legs, sways the tail, turns the head and opens the jaw from these joints.
Preview: tools/preview_box_model.py understands the flattened "preview" section written alongside.
Run: python tools/gen_dino_json.py src/main/resources/assets/dragonblock/dino.json
"""
import json
import sys

parts = {}


def part(name, parent, pivot, rest=0.0, **boxes):
    """rest: the joint's resting pitch in radians (the legs bend like a bird's)."""
    parts[name] = {"parent": parent, "pivot": pivot, "rest": rest, "boxes": boxes}


# Hips 26px up; the torso leans slightly forward, chest ahead of the hips
part("body", None, [0, -26, 0],
     skin=[[-7, -9, -13, 14, 14, 24], [-6, -11, -16, 12, 12, 6]],
     belly=[[-6, 4, -14, 12, 2, 20]],
     ridge=[[-1, -12, -12, 2, 2, 3], [-1, -11.5, -6, 2, 2, 3], [-1, -11, 0, 2, 2, 3], [-1, -10.5, 6, 2, 2, 3]])
part("head", "body", [0, -6, -16],
     skin=[[-4, -6, -6, 8, 10, 7], [-5.5, -12, -17, 11, 9, 13], [-4.5, -10.5, -26, 9, 6, 10]],
     eye=[[-5.7, -10, -15, 0.6, 2, 2], [5.1, -10, -15, 0.6, 2, 2]],
     brow=[[-6, -12.6, -16, 2.5, 1.2, 4], [3.5, -12.6, -16, 2.5, 1.2, 4]],
     teeth=[[-4, -4.6, -25.6, 8, 1.2, 8]],
     ridge=[[-1, -13.4, -10, 2, 1.6, 4], [-1, -12.6, -4, 2, 1.6, 3]])
part("jaw", "head", [0, -4, -13],
     skin=[[-4.5, 0, -12, 9, 4, 13]],
     mouth=[[-3.5, -0.4, -11, 7, 0.6, 11]],
     teeth=[[-3.8, -1, -11.4, 7.6, 1, 7]])
part("tail", "body", [0, -5, 11],
     skin=[[-5.5, -4, 0, 11, 9, 12]], ridge=[[-1, -5.4, 2, 2, 1.6, 3], [-1, -5.2, 8, 2, 1.6, 3]])
part("tail2", "tail", [0, 0.5, 11],
     skin=[[-4, -3, 0, 8, 7, 12]], ridge=[[-1, -4.2, 3, 2, 1.4, 3]])
part("tail3", "tail2", [0, 1, 11],
     skin=[[-2.5, -2, 0, 5, 5, 12], [-1.5, -1, 11, 3, 3, 5]])
for side, sx in (("left", 1), ("right", -1)):
    part(side + "Leg", "body", [sx * 7, 1, 3], rest=-0.3,
         skin=[[-3.5, -4, -6, 7, 15, 11]])
    part(side + "Shin", side + "Leg", [0, 11, 1], rest=0.55,
         skin=[[-2.4, 0, -2.4, 4.8, 12, 4.8]])
    part(side + "Foot", side + "Shin", [0, 12, 0], rest=-0.25,
         skin=[[-3, 0, -7, 6, 3, 10]], claw=[[-3, 0.5, -8.4, 1.4, 2, 1.6], [-0.7, 0.5, -8.6, 1.4, 2, 1.8], [1.6, 0.5, -8.4, 1.4, 2, 1.6]])
    part(side + "Arm", "body", [sx * 6, 2, -12],
         skin=[[-1.2, 0, -1.2, 2.4, 6, 2.4], [-1, 5, -3, 2, 2, 3]], claw=[[-1, 6.4, -3.8, 2, 1.4, 1]])

colors = {"skin": 0x6E7F3A, "belly": 0xCDBE8E, "ridge": 0x485A26, "eye": 0xE8C030, "brow": 0x3E4A22,
          "teeth": 0xF2EEDC, "mouth": 0x8A2A2A, "claw": 0x2A2A22}


def flatten():
    """All boxes in model space at rest, for the generic box-model preview."""
    out = {c: [] for c in colors}

    def origin(name):
        p = parts[name]
        o = p["pivot"]
        if p["parent"]:
            po = origin(p["parent"])
            return [po[0] + o[0], po[1] + o[1], po[2] + o[2]]
        return o

    for name, p in parts.items():
        o = origin(name)
        for c, boxes in p["boxes"].items():
            for b in boxes:
                out[c].append([b[0] + o[0], b[1] + o[1], b[2] + o[2], b[3], b[4], b[5]])
    return out


model = {"parts": parts, "colors": colors, "preview": flatten()}
json.dump(model, open(sys.argv[1], "w"), indent=1)
print("wrote", len(parts), "dinosaur parts")
