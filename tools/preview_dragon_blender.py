"""Assembles the eternal dragon from dragon.json along the same coil ShenlongRenderer uses, inside Blender.

Exec it in Blender (or through the MCP) with DRAGON set to the json path. Units: blocks (1 px = SCALE/16).
Re-running replaces the previous preview. Keep path(), SEGMENTS and SCALE in sync with ShenlongRenderer.
"""
import bpy
import json
import math
from mathutils import Matrix, Vector

DRAGON = globals().get("DRAGON", "")
SCALE = 3.4 / 16.0
SEGMENTS = 44
COLORS = {"scales": (0.22, 0.58, 0.28), "belly": (0.9, 0.84, 0.6), "horn": (0.78, 0.62, 0.4), "whisker": (0.15, 0.22, 0.15),
          "teeth": (0.96, 0.94, 0.9), "eye": (1.0, 0.1, 0.1), "fin": (0.88, 0.38, 0.16)}
# Minecraft world axes (y up) -> Blender (z up)
W2B = Matrix(((1, 0, 0, 0), (0, 0, -1, 0), (0, 1, 0, 0), (0, 0, 0, 1)))
# Model pixels are y-down like every Minecraft model: turn them upright (the same scale(-1, -1, 1) entity renderers use)
FLIP = Matrix.Diagonal((-1, -1, 1, 1))


def path(s, t=0.0):
    """Point on the coil in Minecraft world axes (x, y up, z), s = 0 at the balls, 1 at the head."""
    turns, height = 2.2, 34.0
    r = 7.0 + 3.0 * math.sin(math.pi * s)
    th = 2 * math.pi * turns * s + 0.15 * math.sin(t * 0.05 + s * 12)
    return Vector((r * math.cos(th), 2.0 + height * s + 0.6 * math.sin(t * 0.07 + s * 9), r * math.sin(th)))


def material(name):
    m = bpy.data.materials.get("Dragon_" + name) or bpy.data.materials.new("Dragon_" + name)
    m.use_nodes = True
    bsdf = next(n for n in m.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    bsdf.inputs[0].default_value = (*COLORS[name], 1)
    if name == "eye":
        bsdf.inputs["Emission Color"].default_value = (1, 0.1, 0.1, 1)
        bsdf.inputs["Emission Strength"].default_value = 4.0
    return m


def add_part(parent, part, model):
    """model: Matrix from the part's pixel space to Minecraft world axes."""
    for group, boxes in part.items():
        mat = material(group)
        for x, y, z, w, h, d in boxes:
            me = bpy.data.meshes.new("db")
            vs = [(x + a * w, y + b * h, z + c * d) for a in (0, 1) for b in (0, 1) for c in (0, 1)]
            me.from_pydata(vs, [], [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)])
            me.materials.append(mat)
            o = bpy.data.objects.new("DragonBox", me)
            bpy.context.scene.collection.objects.link(o)
            o.matrix_world = W2B @ model
            o.parent = parent
            o.matrix_parent_inverse = parent.matrix_world.inverted()


def orient(at, toward):
    """Matrix placing a part at `at` with its -z (forward) facing `toward` (Minecraft axes)."""
    d = (toward - at).normalized()
    yaw = math.atan2(-d.x, -d.z)
    pitch = math.asin(max(-1.0, min(1.0, d.y)))
    return Matrix.Translation(at) @ Matrix.Rotation(yaw, 4, "Y") @ Matrix.Rotation(pitch, 4, "X")


def build(emerge=1.0, t=0.0):
    for o in list(bpy.data.objects):
        if o.name.startswith("Dragon"):
            bpy.data.objects.remove(o, do_unlink=True)
    data = json.load(open(DRAGON))
    root = bpy.data.objects.new("DragonRoot", None)
    bpy.context.scene.collection.objects.link(root)
    ds = 1.0 / SEGMENTS
    for i in range(SEGMENTS):
        s = emerge - (SEGMENTS - i) * ds
        if s < 0:
            continue
        size = 0.55 + 0.45 * min(1.0, i / 10.0)
        m = orient(path(s, t), path(s + ds, t)) @ Matrix.Scale(SCALE * size, 4) @ FLIP
        add_part(root, data["tail" if i == 0 else "segment"], m)
        if i in (SEGMENTS - 7, SEGMENTS - 20):
            for side in (-1, 1):
                # Hanging from the belly, splayed outward
                add_part(root, data["leg"], m @ Matrix.Translation((side * 3.2, 2.5, -1)) @ Matrix.Rotation(-side * 0.6, 4, "Z"))
    head_at = path(emerge, t)
    look = head_at + Vector((0, -6, 16))  # looking down toward the summoner in front
    add_part(root, data["head"], orient(head_at, look) @ Matrix.Scale(SCALE, 4) @ FLIP)
    return root


build()
