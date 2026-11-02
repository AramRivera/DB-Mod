import json

# Oozaru (Great Ape) Blockbench Model Generator
# 16 px = 1 block. Facing North (-Z), feet at y = 0.
# Massive scale: ~69 px tall, ~44 px wide, colossal primate proportions.

groups = [
    {"name": "root", "origin": [0, 0, 0], "parent": None},
    {"name": "body", "origin": [0, 24, 0], "parent": "root"},
    {"name": "tail", "origin": [0, 24, 7], "parent": "body"},
    {"name": "tail2", "origin": [0, 28, 17], "parent": "tail"},
    {"name": "tail3", "origin": [0, 42, 20], "parent": "tail2"},
    {"name": "torso", "origin": [0, 32, 2], "parent": "body"},
    {"name": "head", "origin": [0, 48, -4], "parent": "torso"},
    {"name": "jaw", "origin": [0, 44, -9], "parent": "head"},
    {"name": "hair", "origin": [0, 54, -4], "parent": "head"},
    {"name": "right_arm", "origin": [14, 47, 1], "parent": "torso"},
    {"name": "right_forearm", "origin": [18, 33, -1], "parent": "right_arm"},
    {"name": "right_hand", "origin": [20, 18, -3], "parent": "right_forearm"},
    {"name": "left_arm", "origin": [-14, 47, 1], "parent": "torso"},
    {"name": "left_forearm", "origin": [-18, 33, -1], "parent": "left_arm"},
    {"name": "left_hand", "origin": [-20, 18, -3], "parent": "left_forearm"},
    {"name": "right_leg", "origin": [8, 22, 1], "parent": "body"},
    {"name": "right_shin", "origin": [9, 10, 2], "parent": "right_leg"},
    {"name": "left_leg", "origin": [-8, 22, 1], "parent": "body"},
    {"name": "left_shin", "origin": [-9, 10, 2], "parent": "left_leg"}
]

cubes = []

def c(name, from_pos, to_pos, parent, side=None):
    entry = {"name": name, "from": from_pos, "to": to_pos, "parent": parent}
    if side:
        entry["side"] = side
    cubes.append(entry)

# ==========================================
# 1. BODY / PELVIS (body)
# ==========================================
c('pelvis_core', [-9, 20, -6], [9, 32, 7], 'body')
c('groin', [-5, 18, -5], [5, 23, 5], 'body')
c('belly_lower', [-8, 22, -8], [8, 32, -6], 'body')
c('hip_flank_right', [8, 22, -4], [11, 30, 5], 'body', 'right')
c('hip_flank_left', [-11, 22, -4], [-8, 30, 5], 'body', 'left')

# ==========================================
# 2. TORSO & CHEST & HUNCHBACK (torso)
# ==========================================
c('torso_chest_main', [-11, 32, -7], [11, 48, 7], 'torso')
c('torso_belly_upper', [-9.5, 32, -9], [9.5, 42, -7], 'torso')
c('pec_right', [0.5, 39, -9.5], [10.5, 47, -7], 'torso', 'right')
c('pec_left', [-10.5, 39, -9.5], [-0.5, 47, -7], 'torso', 'left')
c('hunchback_spine', [-7, 44, 4], [7, 53, 10], 'torso')
c('hunchback_upper', [-5, 48, 0], [5, 54, 7], 'torso')
c('shoulder_trap_right', [7, 44, -2], [14, 52, 6], 'torso', 'right')
c('shoulder_trap_left', [-14, 44, -2], [-7, 52, 6], 'torso', 'left')
c('neck_base', [-6, 44, -7], [6, 49, -2], 'torso')

# ==========================================
# 3. HEAD & MUZZLE (head)
# ==========================================
c('head_skull', [-6, 47, -10], [6, 56, 0], 'head')
c('brow_ridge', [-6.5, 52.8, -13.5], [6.5, 55.8, -9.5], 'head')
c('temple_fur_right', [5.5, 47, -7], [8.5, 54, -1], 'head', 'right')
c('temple_fur_left', [-8.5, 47, -7], [-5.5, 54, -1], 'head', 'left')
c('cheek_fur_right', [5.5, 43, -10], [9.5, 49, -4], 'head', 'right')
c('cheek_fur_left', [-9.5, 43, -10], [-5.5, 49, -4], 'head', 'left')

# Dedicated 3D Glowing Red Eyes
c('eye_right', [2.0, 50.8, -13.6], [5.5, 53.2, -11.0], 'head', 'right')
c('eye_left', [-5.5, 50.8, -13.6], [-2.0, 53.2, -11.0], 'head', 'left')

# Prominent Pointed Saiyan Ape ears (pointing out and up)
c('ear_base_right', [6.0, 49.5, -8.0], [10.5, 53.5, -4.5], 'head', 'right')
c('ear_mid_right', [10.0, 51.5, -7.5], [14.5, 56.0, -4.5], 'head', 'right')
c('ear_tip_right', [14.0, 54.5, -7.0], [18.5, 59.0, -5.0], 'head', 'right')
c('ear_inner_right', [8.0, 50.5, -7.8], [13.5, 54.5, -5.5], 'head', 'right')

c('ear_base_left', [-10.5, 49.5, -8.0], [-6.0, 53.5, -4.5], 'head', 'left')
c('ear_mid_left', [-14.5, 51.5, -7.5], [-10.0, 56.0, -4.5], 'head', 'left')
c('ear_tip_left', [-18.5, 54.5, -7.0], [-14.0, 59.0, -5.0], 'head', 'left')
c('ear_inner_left', [-13.5, 50.5, -7.8], [-8.0, 54.5, -5.5], 'head', 'left')

# Snout & Upper Muzzle (tan/flesh colored)
c('snout_core', [-4.5, 46.5, -16.0], [4.5, 50.8, -10.0], 'head')
c('snout_bridge', [-2.5, 49.0, -15.5], [2.5, 51.0, -10.5], 'head')
c('snout_nostrils', [-3.2, 47.0, -16.8], [3.2, 49.0, -15.8], 'head')

# Upper teeth / fangs
c('fangs_upper_plate', [-4.0, 45.0, -15.8], [4.0, 46.8, -11.0], 'head')
c('fang_upper_right', [2.8, 43.0, -15.5], [4.0, 45.5, -14.2], 'head', 'right')
c('fang_upper_left', [-4.0, 43.0, -15.5], [-2.8, 45.5, -14.2], 'head', 'left')

# ==========================================
# 4. JAW & MOUTH (jaw)
# ==========================================
c('jaw_mandible', [-4.2, 40.5, -15.5], [4.2, 44.5, -9], 'jaw')
c('jaw_chin', [-3.2, 39.5, -16], [3.2, 42.5, -12], 'jaw')
c('jaw_teeth_lower', [-3.8, 44.2, -15.2], [3.8, 45.8, -10], 'jaw')
c('fang_lower_right', [2.7, 45.5, -15.0], [3.9, 47.5, -13.8], 'jaw', 'right')
c('fang_lower_left', [-3.9, 45.5, -15.0], [-2.7, 47.5, -13.8], 'jaw', 'left')
c('mouth_tongue', [-2.5, 42.5, -14], [2.5, 44.0, -10], 'jaw')

# ==========================================
# 5. HAIR CREST (hair)
# ==========================================
c('hair_crest_mid', [-3.5, 55, -8], [3.5, 62, -1], 'hair')
c('hair_crest_top', [-2.5, 60, -6], [2.5, 66, -1], 'hair')
c('hair_crest_peak', [-1.5, 64, -4], [1.5, 69, 0], 'hair')
c('hair_nape', [-4, 50, 0], [4, 58, 3], 'hair')
c('hair_side_spike_right', [3, 55, -5], [6.5, 61, 0], 'hair', 'right')
c('hair_side_spike_left', [-6.5, 55, -5], [-3, 61, 0], 'hair', 'left')

# ==========================================
# 6. RIGHT ARM (right_arm, right_forearm, right_hand)
# ==========================================
c('shoulder_deltoid_right', [10, 41, -4], [19, 52, 6], 'right_arm', 'right')
c('bicep_upper_right', [12, 33, -3.5], [19, 43, 4.5], 'right_arm', 'right')
c('tricep_right', [13, 34, 3], [18, 42, 6.5], 'right_arm', 'right')

c('elbow_right', [13.5, 31, -2], [18.5, 36, 4], 'right_forearm', 'right')
c('forearm_thick_right', [14, 21, -4.5], [22, 33, 4.5], 'right_forearm', 'right')
c('wrist_right', [15, 17, -4], [21, 22, 3], 'right_forearm', 'right')

c('palm_right', [15, 8, -5], [22, 17, 3], 'right_hand', 'right')
c('knuckles_front_right', [15.5, 6, -6.5], [21.5, 12, -4.5], 'right_hand', 'right')
c('fingers_fold_right', [16, 7, -6], [21, 14, -2], 'right_hand', 'right')
c('thumb_right', [13, 9, -5], [16, 14, -2], 'right_hand', 'right')

# ==========================================
# 7. LEFT ARM (left_arm, left_forearm, left_hand)
# ==========================================
c('shoulder_deltoid_left', [-19, 41, -4], [-10, 52, 6], 'left_arm', 'left')
c('bicep_upper_left', [-19, 33, -3.5], [-12, 43, 4.5], 'left_arm', 'left')
c('tricep_left', [-18, 34, 3], [-13, 42, 6.5], 'left_arm', 'left')

c('elbow_left', [-18.5, 31, -2], [-13.5, 36, 4], 'left_forearm', 'left')
c('forearm_thick_left', [-22, 21, -4.5], [-14, 33, 4.5], 'left_forearm', 'left')
c('wrist_left', [-21, 17, -4], [-15, 22, 3], 'left_forearm', 'left')

c('palm_left', [-22, 8, -5], [-15, 17, 3], 'left_hand', 'left')
c('knuckles_front_left', [-21.5, 6, -6.5], [-15.5, 12, -4.5], 'left_hand', 'left')
c('fingers_fold_left', [-21, 7, -6], [-16, 14, -2], 'left_hand', 'left')
c('thumb_left', [-16, 9, -5], [-13, 14, -2], 'left_hand', 'left')

# ==========================================
# 8. RIGHT LEG & MONKEY FOOT (right_leg, right_shin)
# ==========================================
c('thigh_upper_right', [4, 16, -5], [13, 26, 5], 'right_leg', 'right')
c('thigh_outer_bulge_right', [11, 17, -4], [14.5, 25, 4], 'right_leg', 'right')
c('knee_right', [5, 11, -5.5], [12, 17, 3.5], 'right_leg', 'right')

c('calf_muscle_right', [5, 4, -3], [12.5, 13, 5], 'right_shin', 'right')
c('shin_front_right', [5.5, 3, -4.5], [11.5, 11, 2], 'right_shin', 'right')
c('ankle_right', [6, 2, -3.5], [11, 5, 3], 'right_shin', 'right')

c('heel_right', [5.5, 0, 1], [11.5, 3.5, 6], 'right_shin', 'right')
c('sole_mid_right', [5, 0, -4], [12, 3, 2], 'right_shin', 'right')
c('toes_outer_right', [4.5, 0, -9], [12.5, 2.8, -4], 'right_shin', 'right')
c('thumb_toe_right', [2.5, 0, -6], [5.5, 2.5, -2], 'right_shin', 'right')

# ==========================================
# 9. LEFT LEG & MONKEY FOOT (left_leg, left_shin)
# ==========================================
c('thigh_upper_left', [-13, 16, -5], [-4, 26, 5], 'left_leg', 'left')
c('thigh_outer_bulge_left', [-14.5, 17, -4], [-11, 25, 4], 'left_leg', 'left')
c('knee_left', [-12, 11, -5.5], [-5, 17, 3.5], 'left_leg', 'left')

c('calf_muscle_left', [-12.5, 4, -3], [-5, 13, 5], 'left_shin', 'left')
c('shin_front_left', [-11.5, 3, -4.5], [-5.5, 11, 2], 'left_shin', 'left')
c('ankle_left', [-11, 2, -3.5], [-6, 5, 3], 'left_shin', 'left')

c('heel_left', [-11.5, 0, 1], [-5.5, 3.5, 6], 'left_shin', 'left')
c('sole_mid_left', [-12, 0, -4], [-5, 3, 2], 'left_shin', 'left')
c('toes_outer_left', [-12.5, 0, -9], [-4.5, 2.8, -4], 'left_shin', 'left')
c('thumb_toe_left', [-5.5, 0, -6], [-2.5, 2.5, -2], 'left_shin', 'left')

# ==========================================
# 10. SAIYAN MONKEY TAIL (tail > tail2 > tail3)
# ==========================================
c('tail_base', [-2.5, 21, 5], [2.5, 27, 11], 'tail')
c('tail_seg1', [-2.2, 23, 10], [2.2, 29, 18], 'tail')

c('tail_seg2_lower', [-2, 28, 16], [2, 36, 21], 'tail2')
c('tail_seg2_upper', [-1.8, 35, 17], [1.8, 44, 22], 'tail2')

c('tail_seg3_curve', [-1.6, 43, 14], [1.6, 52, 19], 'tail3')
c('tail_seg3_tip', [-1.3, 50, 10], [1.3, 56, 15], 'tail3')

# Build Blockbench Script
js = []
js.append("Project.texture_width = 256;")
js.append("Project.texture_height = 256;")
js.append(f"const gDefs = {json.dumps(groups)};")
js.append("""
const groupMap = {};
for (const g of gDefs) {
    const grp = new Group({ name: g.name, origin: g.origin }).init();
    groupMap[g.name] = grp;
    if (g.parent && groupMap[g.parent]) {
        grp.addTo(groupMap[g.parent]);
    }
}

// Locators
new Locator({ name: 'mouth_fx', from: [0, 45, -16.5] }).init().addTo(groupMap['head']);
new Locator({ name: 'right_hand_fx', from: [18, 9, -5.5] }).init().addTo(groupMap['right_hand']);
new Locator({ name: 'left_hand_fx', from: [-18, 9, -5.5] }).init().addTo(groupMap['left_hand']);
""")

js.append(f"const cDefs = {json.dumps(cubes)};")
js.append("""
for (const c of cDefs) {
    const cube = new Cube({
        name: c.name,
        from: c.from,
        to: c.to,
        origin: [(c.from[0] + c.to[0])/2, (c.from[1] + c.to[1])/2, (c.from[2] + c.to[2])/2]
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

with open('tools/run_oozaru_script.js', 'w', encoding='utf-8') as f:
    f.write("\n".join(js))

print(f"Generated Oozaru script with {len(cubes)} cubes across {len(groups)} groups!")
