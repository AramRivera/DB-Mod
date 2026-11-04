"""Renders the giant-ape head (src/main/resources/assets/dragonblock/ape_head.json, also loaded by the game) from 3 angles.

Usage: blender -b --factory-startup --python tools/preview_ape.py -- <ape_head.json> <out_dir>
"""
import bpy, json, os, sys
from mathutils import Matrix, Vector

args = sys.argv[sys.argv.index("--") + 1:]
src, out_dir = args
os.makedirs(out_dir, exist_ok=True)
TO_BLENDER = Matrix(((1, 0, 0, 0), (0, 0, 1, 0), (0, -1, 0, 0), (0, 0, 0, 1)))
COLORS = {"fur": (0.35, 0.22, 0.13), "skin": (0.78, 0.6, 0.44), "dark": (0.12, 0.08, 0.06),
          "fangs": (0.95, 0.93, 0.88), "eyes": (1.0, 0.1, 0.1), "body": (0.35, 0.22, 0.13)}


def box(group, x, y, z, w, h, d):
    bpy.ops.mesh.primitive_cube_add(size=1)
    o = bpy.context.object
    o.scale = (w, h, d)
    o.location = (x + w / 2, y + h / 2, z + d / 2)
    bpy.ops.object.transform_apply(location=True, scale=True)
    o.matrix_world = TO_BLENDER
    m = bpy.data.materials.get(group) or bpy.data.materials.new(group)
    m.diffuse_color = (*COLORS[group], 1)
    o.data.materials.append(m)


for o in list(bpy.data.objects):
    bpy.data.objects.remove(o, do_unlink=True)
data = json.load(open(src))
box("fur", -4, -8, -4, 8, 8, 8)          # the player's head under the fur
box("body", -4.45, 0, -2.45, 8.9, 12, 4.9)  # fur suit torso
for group in ("fur", "skin", "dark", "fangs", "eyes"):
    for b in data[group]:
        box(group, *b)

scene = bpy.context.scene
scene.render.engine = "BLENDER_WORKBENCH"
scene.display.shading.color_type = "MATERIAL"
scene.display.shading.show_cavity = True
scene.render.resolution_x = scene.render.resolution_y = 360
for name, d in {"front": (0, -1, 0.1), "side": (1, 0, 0.1), "3q": (0.8, -0.8, 0.3)}.items():
    cam_data = bpy.data.cameras.new("cam")
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = 22
    cam = bpy.data.objects.new("cam", cam_data)
    scene.collection.objects.link(cam)
    target = Vector((0, 0, 3))
    cam.location = target + Vector(d).normalized() * 60
    cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    scene.render.filepath = os.path.join(out_dir, f"ape_{name}.png")
    bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(cam, do_unlink=True)
print("done")
