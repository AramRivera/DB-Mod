"""Writes assets/dragonblock/dragon.json: the eternal dragon as groups of boxes (our own eastern-dragon design).

Units are model pixels; the game draws them at DRAGON_SCALE (see ShenlongRenderer). Every part points
toward -z (forward) with y negative = up, like Minecraft models. Each part maps group -> list of boxes
[x, y, z, w, h, d]; groups are tinted separately (scales, belly, horn, whisker, teeth, eye, fin).
Preview in Blender with tools/preview_dragon_blender.py.
Run: python tools/gen_dragon_json.py src/main/resources/assets/dragonblock/dragon.json
"""
import json
import sys


def mirror(boxes):
    """Adds the left-right mirror of every box (x -> -x - w)."""
    out = []
    for x, y, z, w, h, d in boxes:
        out.append([x, y, z, w, h, d])
        out.append([-x - w, y, z, w, h, d])
    return out


head = {
    "scales": [
        [-5, -6, -10, 10, 7, 10],        # skull
        [-3.5, -4.5, -18, 7, 5, 8],      # snout
        [-4.2, -5.2, -14, 8.4, 2, 4],    # bridge of the snout
        [-5.5, -7.2, -11.5, 11, 2.2, 4], # brow ridge
        [-3, -5.4, -19.4, 6, 2.4, 1.6],  # nose pad
        [-4.5, -5, -2, 9, 7, 3],         # back of the head into the neck
    ],
    "belly": [
        [-3, 0.6, -17.5, 6, 1.8, 8],     # lower jaw, slightly open
        [-4.5, 0.4, -10, 9, 2.2, 9],     # throat
    ],
    "teeth": [
        [-3.2, 0.4, -17.8, 0.9, 1.2, 0.9], [2.3, 0.4, -17.8, 0.9, 1.2, 0.9],   # fangs
    ] + mirror([[-2.6, 0.5, -16, 0.6, 0.6, 0.6], [-2.6, 0.5, -14.4, 0.6, 0.6, 0.6], [-2.8, 0.5, -12.8, 0.6, 0.6, 0.6]]),
    "eye": mirror([[-5.25, -5.6, -11, 0.6, 1.6, 2.6]]),
    "horn": mirror([
        [-4, -9.5, -6, 1.6, 3.6, 1.6],        # antler base
        [-4.6, -12.8, -4.6, 1.3, 3.6, 1.3],   # main tine, sweeping back
        [-4.9, -15.6, -3.0, 1.0, 3.0, 1.0],
        [-6.6, -11.8, -5.4, 2.4, 1.0, 1.0],   # side tine
        [-7.4, -13.2, -5.4, 1.0, 1.6, 1.0],
        [-4.4, -11.6, -7.2, 1.0, 1.0, 1.8],   # front tine
    ]),
    "whisker": mirror([
        [-8, -3.4, -18.4, 4.6, 0.6, 0.6],     # from the snout, out and back
        [-12, -2.6, -17, 4.2, 0.6, 0.6],
        [-15.4, -1.4, -15, 3.6, 0.6, 0.6],
        [-17.6, 0.2, -12.4, 2.6, 0.6, 0.6],
        [-18.4, 1.6, -10, 1.4, 0.6, 0.6],
    ]),
    "fin": [
        [-1, -9.6, -7, 2, 3.6, 7],           # crest along the skull
        [-0.8, -8.6, 0, 1.6, 2.8, 4],
        [-1.6, 1.8, -15, 3.2, 3.2, 2.4],     # chin tuft
    ] + mirror([
        [-6.6, -5.6, -7, 1.2, 4.6, 6],       # cheek frills
        [-7.6, -4.6, -4, 1.2, 4, 4],
    ]),
}

segment = {
    "scales": [[-4, -4, -6.5, 8, 8, 13]],
    "belly": [[-3, 3.1, -6.2, 6, 1.4, 12.4]],
    "fin": [[-0.6, -6.6, -3.5, 1.2, 2.8, 7], [-0.4, -5.6, 2.5, 0.8, 1.8, 3]],
}

tail = {
    "scales": [[-2.6, -2.6, -6.5, 5.2, 5.2, 13]],
    "belly": [[-1.8, 2, -6.2, 3.6, 1.0, 12.4]],
    "fin": [[-0.5, -5, -3, 1.0, 2.6, 6], [-3.4, -1.4, 4.5, 6.8, 2.8, 5], [-2.2, -4.6, 5.2, 4.4, 2.4, 4]],
}

leg = {
    "scales": [[-1.6, 0, -1.6, 3.2, 8, 3.2], [-1.3, 7, -3.4, 2.6, 2.4, 3.6]],
    "belly": [],
    "teeth": [[-1.5, 8.6, -5, 0.7, 0.7, 1.8], [-0.35, 8.6, -5.4, 0.7, 0.7, 2.2], [0.8, 8.6, -5, 0.7, 0.7, 1.8]],
    "fin": [[-0.4, 1, 1.4, 0.8, 4, 2]],
}

json.dump({"head": head, "segment": segment, "tail": tail, "leg": leg}, open(sys.argv[1], "w"), indent=1)
print("wrote dragon")
