"""Renders a color-group box model json (ship.json style: groups of [x, y, z, w, h, d] in model pixels, y down,
plus a "colors" map) from three angles, headless:
  blender -b --factory-startup --python tools/preview_box_model.py -- <model.json> <out.png>
"""
import bpy, json, sys
from mathutils import Vector

args = sys.argv[sys.argv.index("--") + 1:]
data = json.load(open(args[0]))
colors = data.pop("colors", {})
for o in list(bpy.data.objects):
    bpy.data.objects.remove(o, do_unlink=True)
for group, boxes in data.items():
    rgb = colors.get(group, 0xCCCCCC)
    m = bpy.data.materials.new(group)
    m.diffuse_color = (((rgb >> 16) & 255) / 255, ((rgb >> 8) & 255) / 255, (rgb & 255) / 255, 1)
    for k, (x, y, z, w, h, d) in enumerate(boxes):
        for copy, off in enumerate((-80, 0, 80)):
            # three copies seen from front, side and three-quarter by rotating each copy
            me = bpy.data.meshes.new("b")
            def f(X, Y, Z, c=copy):
                X, Z = (X, Z) if c == 0 else ((-Z, X) if c == 1 else (X * 0.707 - Z * 0.707, X * 0.707 + Z * 0.707))
                return (X + off, Z, -Y)
            vs = [f(x + a * w, y + b * h, z + c * d) for a in (0, 1) for b in (0, 1) for c in (0, 1)]
            me.from_pydata(vs, [], [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)])
            me.materials.append(m)
            o = bpy.data.objects.new("b", me)
            bpy.context.scene.collection.objects.link(o)
sc = bpy.context.scene
sc.render.engine = "BLENDER_WORKBENCH"
sc.display.shading.light = "STUDIO"
sc.display.shading.color_type = "MATERIAL"
sc.display.shading.show_cavity = True
sc.render.resolution_x, sc.render.resolution_y = 1500, 560
cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
sc.collection.objects.link(cam)
cam.data.type = "ORTHO"
cam.data.ortho_scale = 250
t = Vector((0, 0, 40))
cam.location = t + Vector((0, -300, 60))
cam.rotation_euler = (t - cam.location).to_track_quat("-Z", "Y").to_euler()
sc.camera = cam
sc.render.filepath = args[1]
bpy.ops.render.render(write_still=True)
