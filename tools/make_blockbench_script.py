import json
import math

with open('src/main/resources/assets/dragonblock/dragon.json') as f:
    dragon_data = json.load(f)

head_raw = dragon_data['head']
seg_raw = dragon_data['segment']
tail_raw = dragon_data['tail']
leg_raw = dragon_data['leg']

NUM_SEGS = 26
TURNS = 2.5
HEIGHT = 85.0
R_BASE = 28.0
R_VAR = 6.0
TH0 = math.pi / 2.0

def pt(s):
    r = R_BASE + R_VAR * math.sin(math.pi * s)
    th = 2 * math.pi * TURNS * s + TH0
    return [round(r * math.cos(th), 2), round(2.0 + HEIGHT * s, 2), round(r * math.sin(th), 2)]

points = [pt(i / NUM_SEGS) for i in range(NUM_SEGS + 1)]

# Force head and torso to be centered on X=0
points[-1] = [0.0, 88.0, -26.0]
points[-2] = [0.0, 80.0, -22.0]

head_pos = points[-1]
torso_pos = points[-2]
body_pos = points[23]
tail_pos = points[17]
tail2_pos = points[9]
tail3_pos = points[0]

js = []
js.append("Project.texture_width = 512;")
js.append("Project.texture_height = 512;")

# Groups
groups_def = [
    {"name": "root", "origin": [0, 0, 0], "parent": None},
    {"name": "body", "origin": body_pos, "parent": "root"},
    {"name": "torso", "origin": torso_pos, "parent": "body"},
    {"name": "head", "origin": head_pos, "parent": "torso"},
    {"name": "jaw", "origin": [0, head_pos[1] - 2, head_pos[2] - 8], "parent": "head"},
    {"name": "right_arm", "origin": [14, torso_pos[1], torso_pos[2]], "parent": "torso"},
    {"name": "right_forearm", "origin": [18, torso_pos[1] - 8, torso_pos[2] - 2], "parent": "right_arm"},
    {"name": "right_hand", "origin": [20, torso_pos[1] - 16, torso_pos[2] - 4], "parent": "right_forearm"},
    {"name": "left_arm", "origin": [-14, torso_pos[1], torso_pos[2]], "parent": "torso"},
    {"name": "left_forearm", "origin": [-18, torso_pos[1] - 8, torso_pos[2] - 2], "parent": "left_arm"},
    {"name": "left_hand", "origin": [-20, torso_pos[1] - 16, torso_pos[2] - 4], "parent": "left_forearm"},
    {"name": "wing_right", "origin": [10, torso_pos[1] + 4, torso_pos[2] + 4], "parent": "torso"},
    {"name": "wing_left", "origin": [-10, torso_pos[1] + 4, torso_pos[2] + 4], "parent": "torso"},
    {"name": "right_leg", "origin": [8, body_pos[1] - 4, body_pos[2]], "parent": "body"},
    {"name": "right_shin", "origin": [10, body_pos[1] - 10, body_pos[2] + 2], "parent": "right_leg"},
    {"name": "left_leg", "origin": [-8, body_pos[1] - 4, body_pos[2]], "parent": "body"},
    {"name": "left_shin", "origin": [-10, body_pos[1] - 10, body_pos[2] + 2], "parent": "left_leg"},
    {"name": "tail", "origin": tail_pos, "parent": "body"},
    {"name": "tail2", "origin": tail2_pos, "parent": "tail"},
    {"name": "tail3", "origin": tail3_pos, "parent": "tail2"},
]

js.append(f"const gDefs = {json.dumps(groups_def)};")
js.append("""
const groupMap = {};
for (const g of gDefs) {
    const grp = new Group({ name: g.name, origin: g.origin }).init();
    groupMap[g.name] = grp;
    if (g.parent && groupMap[g.parent]) {
        grp.addTo(groupMap[g.parent]);
    }
}
""")

# Locators
js.append(f"""
new Locator({{ name: 'mouth_fx', from: [0, {head_pos[1] - 2}, {round(head_pos[2] - 22, 2)}] }}).init().addTo(groupMap['head']);
new Locator({{ name: 'right_hand_fx', from: [20, {torso_pos[1] - 20}, {round(torso_pos[2] - 6, 2)}] }}).init().addTo(groupMap['right_hand']);
new Locator({{ name: 'left_hand_fx', from: [-20, {torso_pos[1] - 20}, {round(torso_pos[2] - 6, 2)}] }}).init().addTo(groupMap['left_hand']);
""")

head_cubes = []
HEAD_SCALE = 1.25

def conv_box(box, offset, scale=1.0):
    x, y, z, w, h, d = box
    xmin = round(offset[0] + x * scale, 2)
    xmax = round(offset[0] + (x + w) * scale, 2)
    ymin = round(offset[1] + (-y - h) * scale, 2)
    ymax = round(offset[1] + (-y) * scale, 2)
    zmin = round(offset[2] + z * scale, 2)
    zmax = round(offset[2] + (z + d) * scale, 2)
    return [min(xmin, xmax), min(ymin, ymax), min(zmin, zmax)], [max(xmin, xmax), max(ymin, ymax), max(zmin, zmax)]

# Add head boxes
for sub, boxes in head_raw.items():
    for idx, b in enumerate(boxes):
        parent_name = 'jaw' if sub == 'belly' and idx == 0 else ('jaw' if sub == 'teeth' and idx >= 2 else 'head')
        f_pt, t_pt = conv_box(b, head_pos, HEAD_SCALE)
        cube_spec = {
            "name": f"head_{sub}_{idx}",
            "from": f_pt,
            "to": t_pt,
            "parent": parent_name
        }
        head_cubes.append(cube_spec)

# Serpentine body segments
coil_cubes = []
for i in range(NUM_SEGS):
    p1 = points[i]
    p2 = points[i + 1]
    dx = p2[0] - p1[0]
    dy = p2[1] - p1[1]
    dz = p2[2] - p1[2]
    dist = math.sqrt(dx*dx + dy*dy + dz*dz)
    cx_seg = round((p1[0] + p2[0]) / 2, 2)
    cy_seg = round((p1[1] + p2[1]) / 2, 2)
    cz_seg = round((p1[2] + p2[2]) / 2, 2)
    
    yaw = round(math.degrees(math.atan2(-dx, -dz)), 1)
    
    if i < 9:
        pbone = 'tail3'
    elif i < 18:
        pbone = 'tail2'
    elif i < 24:
        pbone = 'tail'
    else:
        pbone = 'body'
    
    seg_w = round(8.0 * (0.6 + 0.4 * min(1.0, i / 8.0)), 2)
    seg_h = seg_w
    seg_l = round(max(10.0, dist + 2.0), 2)
    
    coil_cubes.append({
        "name": f"seg_body_{i}",
        "from": [round(cx_seg - seg_w/2, 2), round(cy_seg - seg_h/2, 2), round(cz_seg - seg_l/2, 2)],
        "to": [round(cx_seg + seg_w/2, 2), round(cy_seg + seg_h/2, 2), round(cz_seg + seg_l/2, 2)],
        "origin": [cx_seg, cy_seg, cz_seg],
        "rotation": [0, yaw, 0],
        "parent": pbone
    })
    coil_cubes.append({
        "name": f"seg_belly_{i}",
        "from": [round(cx_seg - seg_w*0.4, 2), round(cy_seg - seg_h/2 - 1.2, 2), round(cz_seg - seg_l/2, 2)],
        "to": [round(cx_seg + seg_w*0.4, 2), round(cy_seg - seg_h/2, 2), round(cz_seg + seg_l/2, 2)],
        "origin": [cx_seg, cy_seg, cz_seg],
        "rotation": [0, yaw, 0],
        "parent": pbone
    })
    coil_cubes.append({
        "name": f"seg_spine_{i}",
        "from": [round(cx_seg - 0.8, 2), round(cy_seg + seg_h/2, 2), round(cz_seg - 2.5, 2)],
        "to": [round(cx_seg + 0.8, 2), round(cy_seg + seg_h/2 + 3.5, 2), round(cz_seg + 2.5, 2)],
        "origin": [cx_seg, cy_seg, cz_seg],
        "rotation": [0, yaw, 0],
        "parent": pbone
    })

# Add tail fin fan on segment 0 (tail tip resting at y=0, perfectly centered at X=0!)
coil_cubes.append({
    "name": "tail3_fin_blade",
    "from": [-1.0, 0, round(points[0][2] + 4, 2)],
    "to": [1.0, 12, round(points[0][2] + 18, 2)],
    "origin": points[0],
    "parent": "tail3"
})
coil_cubes.append({
    "name": "tail3_fin_side_right",
    "from": [1.0, 0, round(points[0][2] + 3, 2)],
    "to": [8.0, 5, round(points[0][2] + 13, 2)],
    "origin": points[0],
    "parent": "tail3",
    "side": "right"
})
coil_cubes.append({
    "name": "tail3_fin_side_left",
    "from": [-8.0, 0, round(points[0][2] + 3, 2)],
    "to": [-1.0, 5, round(points[0][2] + 13, 2)],
    "origin": points[0],
    "parent": "tail3",
    "side": "left"
})

# Super Shenron Wings attached to torso (centered on X=0)
wing_cubes = [
    # Right wing (+X)
    {"name": "wing_strut1_right", "from": [6, torso_pos[1] - 2, torso_pos[2] + 2], "to": [28, torso_pos[1] + 12, torso_pos[2] + 8], "parent": "wing_right", "side": "right"},
    {"name": "wing_strut2_right", "from": [26, torso_pos[1] + 10, torso_pos[2] + 6], "to": [58, torso_pos[1] + 28, torso_pos[2] + 12], "parent": "wing_right", "side": "right"},
    {"name": "wing_claw_right", "from": [56, torso_pos[1] + 26, torso_pos[2] + 8], "to": [62, torso_pos[1] + 36, torso_pos[2] + 14], "parent": "wing_right", "side": "right"},
    {"name": "wing_finger1_right", "from": [58, torso_pos[1] + 14, torso_pos[2] + 8], "to": [86, torso_pos[1] + 32, torso_pos[2] + 11], "parent": "wing_right", "side": "right"},
    {"name": "wing_finger2_right", "from": [56, torso_pos[1] - 8, torso_pos[2] + 6], "to": [62, torso_pos[1] + 16, torso_pos[2] + 9], "parent": "wing_right", "side": "right"},
    {"name": "wing_membrane_outer_right", "from": [58, torso_pos[1], torso_pos[2] + 8.5], "to": [84, torso_pos[1] + 26, torso_pos[2] + 9.5], "parent": "wing_right", "side": "right"},
    {"name": "wing_membrane_inner_right", "from": [12, torso_pos[1] - 4, torso_pos[2] + 4.5], "to": [58, torso_pos[1] + 18, torso_pos[2] + 5.5], "parent": "wing_right", "side": "right"},

    # Left wing (-X)
    {"name": "wing_strut1_left", "from": [-28, torso_pos[1] - 2, torso_pos[2] + 2], "to": [-6, torso_pos[1] + 12, torso_pos[2] + 8], "parent": "wing_left", "side": "left"},
    {"name": "wing_strut2_left", "from": [-58, torso_pos[1] + 10, torso_pos[2] + 6], "to": [-26, torso_pos[1] + 28, torso_pos[2] + 12], "parent": "wing_left", "side": "left"},
    {"name": "wing_claw_left", "from": [-62, torso_pos[1] + 26, torso_pos[2] + 8], "to": [-56, torso_pos[1] + 36, torso_pos[2] + 14], "parent": "wing_left", "side": "left"},
    {"name": "wing_finger1_left", "from": [-86, torso_pos[1] + 14, torso_pos[2] + 8], "to": [-58, torso_pos[1] + 32, torso_pos[2] + 11], "parent": "wing_left", "side": "left"},
    {"name": "wing_finger2_left", "from": [-62, torso_pos[1] - 8, torso_pos[2] + 6], "to": [-56, torso_pos[1] + 16, torso_pos[2] + 9], "parent": "wing_left", "side": "left"},
    {"name": "wing_membrane_outer_left", "from": [-84, torso_pos[1], torso_pos[2] + 8.5], "to": [-58, torso_pos[1] + 26, torso_pos[2] + 9.5], "parent": "wing_left", "side": "left"},
    {"name": "wing_membrane_inner_left", "from": [-58, torso_pos[1] - 4, torso_pos[2] + 4.5], "to": [-12, torso_pos[1] + 18, torso_pos[2] + 5.5], "parent": "wing_left", "side": "left"},
]

# Arms & Claws (centered on X=0)
arm_cubes = [
    # Right arm (+X)
    {"name": "arm_upper_right", "from": [8, torso_pos[1] - 8, torso_pos[2] - 4], "to": [15, torso_pos[1] + 4, torso_pos[2] + 3], "parent": "right_arm", "side": "right"},
    {"name": "arm_fore_right", "from": [13, torso_pos[1] - 16, torso_pos[2] - 6], "to": [18, torso_pos[1] - 7, torso_pos[2]], "parent": "right_forearm", "side": "right"},
    {"name": "hand_palm_right", "from": [14, torso_pos[1] - 22, torso_pos[2] - 7], "to": [20, torso_pos[1] - 16, torso_pos[2] - 2], "parent": "right_hand", "side": "right"},
    {"name": "claw_thumb_right", "from": [19, torso_pos[1] - 22, torso_pos[2] - 6], "to": [22, torso_pos[1] - 18, torso_pos[2] - 3], "parent": "right_hand", "side": "right"},
    {"name": "claw_f1_right", "from": [14.2, torso_pos[1] - 26, torso_pos[2] - 7], "to": [15.8, torso_pos[1] - 22, torso_pos[2] - 5], "parent": "right_hand", "side": "right"},
    {"name": "claw_f2_right", "from": [16.2, torso_pos[1] - 27, torso_pos[2] - 7], "to": [17.8, torso_pos[1] - 22, torso_pos[2] - 5], "parent": "right_hand", "side": "right"},
    {"name": "claw_f3_right", "from": [18.2, torso_pos[1] - 26, torso_pos[2] - 6.5], "to": [19.8, torso_pos[1] - 22, torso_pos[2] - 4.5], "parent": "right_hand", "side": "right"},

    # Left arm (-X)
    {"name": "arm_upper_left", "from": [-15, torso_pos[1] - 8, torso_pos[2] - 4], "to": [-8, torso_pos[1] + 4, torso_pos[2] + 3], "parent": "left_arm", "side": "left"},
    {"name": "arm_fore_left", "from": [-18, torso_pos[1] - 16, torso_pos[2] - 6], "to": [-13, torso_pos[1] - 7, torso_pos[2]], "parent": "left_forearm", "side": "left"},
    {"name": "hand_palm_left", "from": [-20, torso_pos[1] - 22, torso_pos[2] - 7], "to": [-14, torso_pos[1] - 16, torso_pos[2] - 2], "parent": "left_hand", "side": "left"},
    {"name": "claw_thumb_left", "from": [-22, torso_pos[1] - 22, torso_pos[2] - 6], "to": [-19, torso_pos[1] - 18, torso_pos[2] - 3], "parent": "left_hand", "side": "left"},
    {"name": "claw_f1_left", "from": [-15.8, torso_pos[1] - 26, torso_pos[2] - 7], "to": [-14.2, torso_pos[1] - 22, torso_pos[2] - 5], "parent": "left_hand", "side": "left"},
    {"name": "claw_f2_left", "from": [-17.8, torso_pos[1] - 27, torso_pos[2] - 7], "to": [-16.2, torso_pos[1] - 22, torso_pos[2] - 5], "parent": "left_hand", "side": "left"},
    {"name": "claw_f3_left", "from": [-19.8, torso_pos[1] - 26, torso_pos[2] - 6.5], "to": [-18.2, torso_pos[1] - 22, torso_pos[2] - 4.5], "parent": "left_hand", "side": "left"},
]

# Legs & Claws on body (+X right, -X left)
leg_cubes = [
    {"name": "pelvic_spur_right", "from": [6, body_pos[1] - 6, body_pos[2] - 2], "to": [11, body_pos[1] + 1, body_pos[2] + 4], "parent": "right_leg", "side": "right"},
    {"name": "pelvic_fin_right", "from": [8, body_pos[1] - 14, body_pos[2] + 1], "to": [11, body_pos[1] - 6, body_pos[2] + 7], "parent": "right_shin", "side": "right"},
    {"name": "pelvic_spur_left", "from": [-11, body_pos[1] - 6, body_pos[2] - 2], "to": [-6, body_pos[1] + 1, body_pos[2] + 4], "parent": "left_leg", "side": "left"},
    {"name": "pelvic_fin_left", "from": [-11, body_pos[1] - 14, body_pos[2] + 1], "to": [-8, body_pos[1] - 6, body_pos[2] + 7], "parent": "left_shin", "side": "left"},
]

all_cubes = head_cubes + coil_cubes + wing_cubes + arm_cubes + leg_cubes

js.append(f"const cDefs = {json.dumps(all_cubes)};")
js.append("""
for (const c of cDefs) {
    const cube = new Cube({
        name: c.name,
        from: c.from,
        to: c.to,
        origin: c.origin || [(c.from[0] + c.to[0])/2, (c.from[1] + c.to[1])/2, (c.from[2] + c.to[2])/2],
        rotation: c.rotation || [0, 0, 0]
    }).init();
    if (c.parent && groupMap[c.parent]) {
        cube.addTo(groupMap[c.parent]);
    }
}
Canvas.updateAll();
return {
    cubeCount: Cube.all.length,
    groupCount: Group.all.length,
    locCount: Locator.all.length
};
""")

full_script = "\n".join(js)
with open('tools/run_bb_script.js', 'w', encoding='utf-8') as f:
    f.write(full_script)

print(f"Generated updated script with {len(all_cubes)} cubes!")
