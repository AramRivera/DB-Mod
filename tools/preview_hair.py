"""Renders every hairstyle JSON on a Minecraft-sized head from 4 angles, using the same spike assembly
as DBModels.java, so hair can be checked without launching the game.

Usage (headless):
  blender -b --factory-startup --python tools/preview_hair.py -- <hair_json_dir> <out_dir>
"""
import bpy, json, math, os, sys
from mathutils import Matrix, Vector

args = sys.argv[sys.argv.index("--") + 1:]
hair_dir, out_dir = args[0], args[1]
os.makedirs(out_dir, exist_ok=True)

# Minecraft model space (y down, face toward -z) -> Blender (z up, face toward -y)
TO_BLENDER = Matrix(((1, 0, 0, 0), (0, 0, 1, 0), (0, -1, 0, 0), (0, 0, 0, 1)))


def clear():
    for o in list(bpy.data.objects):
        bpy.data.objects.remove(o, do_unlink=True)


def material(name, rgb):
    m = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    m.diffuse_color = (*rgb, 1)
    return m


def box(name, mat, matrix, x, y, z, w, h, d):
    """Axis-aligned box from (x, y, z) with size (w, h, d) in the part space given by matrix."""
    bpy.ops.mesh.primitive_cube_add(size=1)
    o = bpy.context.object
    o.name = name
    o.scale = (w, h, d)
    o.location = (x + w / 2, y + h / 2, z + d / 2)
    bpy.ops.object.transform_apply(location=True, scale=True)
    o.matrix_world = TO_BLENDER @ matrix
    o.data.materials.append(mat)
    return o


def part_matrix(x, y, z, xrot, yrot, zrot):
    # ModelPart.translateAndRotate: translate, then rotationZYX(z, y, x) = Rz * Ry * Rx
    return (Matrix.Translation((x, y, z)) @ Matrix.Rotation(zrot, 4, "Z")
            @ Matrix.Rotation(yrot, 4, "Y") @ Matrix.Rotation(xrot, 4, "X"))


# Four tapering steps (fraction of length, fraction of width); the outer two follow the bend
TIERS = [(0.32, 1.0), (0.26, 0.78), (0.2, 0.54), (0.13, 0.32), (0.09, 0.14)]


def spike(mat, s, mirror):
    x, y, z = s["p"]
    side = s["side"]
    if mirror:
        x, side = -x, -side
    w, length = s["w"], s["len"]
    d = s.get("d", w)
    m = part_matrix(x, y, z, -math.radians(s["back"]), 0, math.radians(side))
    start = 0.0
    for i, (lf, wf) in enumerate(TIERS):
        if i == 2:  # bend joint
            m = m @ part_matrix(0, -start, 0, -math.radians(s.get("bend", 0)), 0, 0)
            start = 0.0
        l = length * lf
        box("t", mat, m, -w * wf / 2, -start - l - 0.15, -d * wf / 2, w * wf, l + 0.3, d * wf)
        start += l


def build(style):
    clear()
    skin, shirt, hair = material("skin", (0.85, 0.65, 0.5)), material("shirt", (0.2, 0.35, 0.7)), material("hair", (1.0, 0.82, 0.2))
    eye = material("eye", (0.1, 0.1, 0.15))
    ident = Matrix.Identity(4)
    box("head", skin, ident, -4, -8, -4, 8, 8, 8)
    box("eyeL", eye, ident, -3, -4, -4.05, 1.2, 1, 0.1)
    box("eyeR", eye, ident, 1.8, -4, -4.05, 1.2, 1, 0.1)
    box("body", shirt, ident, -4, 0, -2, 8, 12, 4)
    box("armR", shirt, ident, -8, 0, -2, 4, 12, 4)
    box("armL", shirt, ident, 4, 0, -2, 4, 12, 4)
    box("legs", material("pants", (0.25, 0.25, 0.3)), ident, -4, 12, -2, 8, 12, 4)
    # cap (same as DBModels)
    box("cap", hair, ident, -4.4, -8.8, -4.4, 8.8, 3.4, 8.8)
    box("capBack", hair, ident, -4.4, -6.2, 0.6, 8.8, 5.4, 3.8)
    box("capL", hair, ident, -4.45, -6.0, -3.6, 0.9, 2.6, 4.4)
    box("capR", hair, ident, 3.55, -6.0, -3.6, 0.9, 2.6, 4.4)
    with open(os.path.join(hair_dir, style + ".json")) as f:
        data = json.load(f)
        if "mass" in data:
            box("mass", hair, ident, *data["mass"])
        for s in data["spikes"]:
            spike(hair, s, False)
            if s.get("mirror"):
                spike(hair, s, True)


def render(path, direction):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x = scene.render.resolution_y = 360
    scene.render.film_transparent = False
    target = Vector((0, 0, 6))
    cam_data = bpy.data.cameras.new("cam")
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = 46
    cam = bpy.data.objects.new("cam", cam_data)
    scene.collection.objects.link(cam)
    cam.location = target + Vector(direction).normalized() * 80
    cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    scene.render.filepath = path
    bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(cam, do_unlink=True)


VIEWS = {"front": (0, -1, 0.05), "side": (1, 0, 0.05), "back": (0, 1, 0.05), "3q": (0.8, -0.8, 0.45)}
for style in sorted(n[:-5] for n in os.listdir(hair_dir) if n.endswith(".json")):
    build(style)
    for view, d in VIEWS.items():
        render(os.path.join(out_dir, f"{style}_{view}.png"), d)
print("done")
