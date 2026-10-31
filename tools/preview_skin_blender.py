"""Builds a Minecraft player (classic arms) in Blender textured with a 64x64 skin, for reviewing race skins.

Run inside Blender (or through the MCP): set SKIN to the PNG path, then exec this file. The character faces
-Y with its right side at -X, units are skin pixels / 10. Re-running replaces the previous preview.
"""
import bpy

SKIN = globals().get("SKIN", "")
NAME = globals().get("NAME", "SkinPreview")

PARTS = {  # name: (u, v, w, h, d, center x, bottom z)
    "head": (0, 0, 8, 8, 8, 0, 24),
    "body": (16, 16, 8, 12, 4, 0, 12),
    "rarm": (40, 16, 4, 12, 4, -6, 12),
    "larm": (32, 48, 4, 12, 4, 6, 12),
    "rleg": (0, 16, 4, 12, 4, -2, 0),
    "lleg": (16, 48, 4, 12, 4, 2, 0),
}


def uv(px, py):
    return (px / 64.0, 1.0 - py / 64.0)


def box(name, u, v, w, h, d, cx, z0, mat):
    s = 0.1
    x0, x1 = (cx - w / 2) * s, (cx + w / 2) * s
    y0, y1 = -d / 2 * s, d / 2 * s
    za, zb = z0 * s, (z0 + h) * s
    verts, faces, uvs = [], [], []

    def quad(corners, rect):
        # corners: top-left, top-right, bottom-right, bottom-left as seen on the texture
        ru, rv, rw, rh = rect
        i = len(verts)
        verts.extend(corners)
        faces.append((i, i + 3, i + 2, i + 1))
        uvs.extend([uv(ru, rv), uv(ru + rw, rv), uv(ru + rw, rv + rh), uv(ru, rv + rh)])

    quad([(x0, y0, zb), (x1, y0, zb), (x1, y0, za), (x0, y0, za)], (u + d, v + d, w, h))              # front (-Y)
    quad([(x1, y1, zb), (x0, y1, zb), (x0, y1, za), (x1, y1, za)], (u + 2 * d + w, v + d, w, h))      # back
    quad([(x0, y1, zb), (x0, y0, zb), (x0, y0, za), (x0, y1, za)], (u, v + d, d, h))                  # right (-X)
    quad([(x1, y0, zb), (x1, y1, zb), (x1, y1, za), (x1, y0, za)], (u + d + w, v + d, d, h))          # left (+X)
    quad([(x0, y1, zb), (x1, y1, zb), (x1, y0, zb), (x0, y0, zb)], (u + d, v, w, d))                  # top
    quad([(x0, y0, za), (x1, y0, za), (x1, y1, za), (x0, y1, za)], (u + d + w, v, w, d))              # bottom

    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    layer = mesh.uv_layers.new()
    for poly in mesh.polygons:
        for k, li in enumerate(poly.loop_indices):
            # every face has its own 4 vertices, so uvs line up with vertex indices
            corner = poly.vertices[k]
            layer.data[li].uv = uvs[corner]
    mesh.materials.append(mat)
    obj = bpy.data.objects.new(name, mesh)
    return obj


def build():
    for o in list(bpy.data.objects):
        if o.name.startswith(NAME):
            bpy.data.objects.remove(o, do_unlink=True)
    img = bpy.data.images.load(SKIN, check_existing=False)
    mat = bpy.data.materials.new(NAME + "Mat")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = next(n for n in nodes if n.type == "BSDF_PRINCIPLED")
    tex = nodes.new("ShaderNodeTexImage")
    tex.image = img
    tex.interpolation = "Closest"
    mat.node_tree.links.new(tex.outputs[0], bsdf.inputs[0])
    mat.node_tree.links.new(tex.outputs[1], bsdf.inputs["Alpha"])
    root = bpy.data.objects.new(NAME, None)
    bpy.context.scene.collection.objects.link(root)
    for part, (u, v, w, h, d, cx, z0) in PARTS.items():
        obj = box(NAME + "_" + part, u, v, w, h, d, cx, z0, mat)
        bpy.context.scene.collection.objects.link(obj)
        obj.parent = root
    return root


build()
