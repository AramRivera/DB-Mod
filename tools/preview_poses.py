"""Renders the player poses from DBPlayerModel in first person (camera at the eyes, FOV 70) and third
person, so arm angles can be checked without launching the game. Keep POSES in sync with DBPlayerModel.

Usage: blender -b --factory-startup --python tools/preview_poses.py -- <out_dir>
"""
import bpy, math, os, sys
from mathutils import Matrix, Vector

out_dir = sys.argv[sys.argv.index("--") + 1]
os.makedirs(out_dir, exist_ok=True)
TO_BLENDER = Matrix(((1, 0, 0, 0), (0, 0, 1, 0), (0, -1, 0, 0), (0, 0, 0, 1)))

# (xRot, yRot, zRot) per part, radians, pitch 0. "fp" = first-person variant.
POSES = {
    "charge_fp": {"rightArm": (-1.38, -0.35, 0.0), "leftArm": (-1.38, 0.35, 0.0)},
    "powerup_fp": {"rightArm": (-1.28, -0.2, 0.25), "leftArm": (-1.28, 0.2, -0.25)},
    "wave_fp": {"rightArm": (-1.45, -0.6, 0.0), "leftArm": (-1.45, 0.6, 0.0)},
    "blast_fp": {"rightArm": (-1.62, -0.3, 0.0)},
    "release_fp": {"rightArm": (-1.62, -0.35, 0.0), "leftArm": (-1.62, 0.35, 0.0)},
    "guard_fp": {"rightArm": (-1.9, -0.95, 0.0), "leftArm": (-1.72, 0.95, 0.0)},
    "charge_3p": {"rightArm": (-0.25, 0.1, 0.45), "leftArm": (-0.25, -0.1, -0.45), "rightLeg": (0, 0, 0.12), "leftLeg": (0, 0, -0.12)},
    "powerup_3p": {"rightArm": (0.15, 0, 0.75), "leftArm": (0.15, 0, -0.75), "rightLeg": (0, 0, 0.18), "leftLeg": (0, 0, -0.18), "head": (-0.45, 0, 0)},
    "wave_3p": {"body": (0, -0.45, 0), "rightArm": (0.35, -0.35, 0.25), "leftArm": (-0.35, 1.0, 0.1), "rightLeg": (0.3, 0, 0.08), "leftLeg": (-0.3, 0, -0.08)},
    "guard_3p": {"rightArm": (-1.45, -0.75, 0.0), "leftArm": (-1.30, 0.85, 0.0)},
}

# name: (pivot, box origin, box size, color)
PARTS = {
    "head": ((0, 0, 0), (-4, -8, -4), (8, 8, 8), (0.85, 0.65, 0.5)),
    "body": ((0, 0, 0), (-4, 0, -2), (8, 12, 4), (0.2, 0.35, 0.7)),
    "rightArm": ((-5, 2, 0), (-3, -2, -2), (4, 12, 4), (0.85, 0.6, 0.45)),
    "leftArm": ((5, 2, 0), (-1, -2, -2), (4, 12, 4), (0.75, 0.55, 0.4)),
    "rightLeg": ((-1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (0.25, 0.25, 0.3)),
    "leftLeg": ((1.9, 12, 0), (-2, 0, -2), (4, 12, 4), (0.3, 0.3, 0.35)),
}


def rot(x, y, z):
    return Matrix.Rotation(z, 4, "Z") @ Matrix.Rotation(y, 4, "Y") @ Matrix.Rotation(x, 4, "X")


# First person: shoulders sit in front of and below the eyes (model px; eye at y=-3.6), like the vanilla hand
FP_SHOULDERS = {"rightArm": (-6.0, 0.8, -4.0), "leftArm": (6.0, 0.8, -4.0)}


def build(pose):
    for o in list(bpy.data.objects):
        bpy.data.objects.remove(o, do_unlink=True)
    for name, (pivot, origin, size, color) in PARTS.items():
        if pose.get("_fp"):
            if name not in FP_SHOULDERS:
                continue
            pivot = FP_SHOULDERS[name]
        m = Matrix.Translation(pivot) @ rot(*pose.get(name, (0, 0, 0)))
        bpy.ops.mesh.primitive_cube_add(size=1)
        o = bpy.context.object
        o.scale = size
        o.location = [origin[i] + size[i] / 2 for i in range(3)]
        bpy.ops.object.transform_apply(location=True, scale=True)
        o.matrix_world = TO_BLENDER @ m
        mat = bpy.data.materials.new(name)
        mat.diffuse_color = (*color, 1)
        o.data.materials.append(mat)
        if name == "head" and pose.get("_fp"):
            o.hide_render = True  # the camera sits inside the head


def render(path, first_person):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.render.resolution_x, scene.render.resolution_y = 480, 270
    cam_data = bpy.data.cameras.new("cam")
    cam = bpy.data.objects.new("cam", cam_data)
    scene.collection.objects.link(cam)
    if first_person:
        cam_data.angle_y = math.radians(70)
        cam_data.sensor_fit = "VERTICAL"
        cam_data.clip_start = 0.5
        cam.location = TO_BLENDER @ Vector((0, -3.6, 0))
        cam.rotation_euler = Vector((0, -1, 0)).to_track_quat("-Z", "Y").to_euler()  # look where the face looks
    else:
        cam_data.type = "ORTHO"
        cam_data.ortho_scale = 44
        target = Vector((0, 0, -4))
        cam.location = target + Vector((-0.7, -1, 0.35)).normalized() * 80
        cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    scene.render.filepath = path
    bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(cam, do_unlink=True)


for name, pose in POSES.items():
    fp = name.endswith("_fp")
    build({**pose, "_fp": fp})
    render(os.path.join(out_dir, name + ".png"), fp)
print("done")
