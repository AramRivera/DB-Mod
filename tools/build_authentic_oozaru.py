import json

# Authentic Dragon Ball Oozaru (Great Ape) Blockbench Generator
# 16 px = 1 block. Facing North (-Z), feet at y = 0.
# Colossal proportions, beast/wolf-baboon snarling head, pointed demon ears,
# gaping maw with jagged fangs, Saiyan spiky mane, and massive gorilla musculature.

groups = [
    {"name": "root", "origin": [0, 0, 0], "parent": None},
    {"name": "body", "origin": [0, 22, 0], "parent": "root"},
    {"name": "tail", "origin": [0, 22, 7], "parent": "body"},
    {"name": "tail2", "origin": [0, 27, 16], "parent": "tail"},
    {"name": "tail3", "origin": [0, 40, 19], "parent": "tail2"},
    {"name": "torso", "origin": [0, 28, 2], "parent": "body"},
    {"name": "head", "origin": [0, 43, -4], "parent": "torso"},
    {"name": "jaw", "origin": [0, 40, -8], "parent": "head"},
    {"name": "hair", "origin": [0, 50, -4], "parent": "head"},
    {"name": "right_arm", "origin": [13, 43, 0], "parent": "torso"},
    {"name": "right_forearm", "origin": [16, 29, -1], "parent": "right_arm"},
    {"name": "right_hand", "origin": [17, 16, -2], "parent": "right_forearm"},
    {"name": "left_arm", "origin": [-13, 43, 0], "parent": "torso"},
    {"name": "left_forearm", "origin": [-16, 29, -1], "parent": "left_arm"},
    {"name": "left_hand", "origin": [-17, 16, -2], "parent": "left_forearm"},
    {"name": "right_leg", "origin": [8, 20, 0], "parent": "body"},
    {"name": "right_shin", "origin": [9, 9, 1], "parent": "right_leg"},
    {"name": "left_leg", "origin": [-8, 20, 0], "parent": "body"},
    {"name": "left_shin", "origin": [-9, 9, 1], "parent": "left_leg"}
]

cubes = []

def c(name, from_pos, to_pos, parent, side=None):
    entry = {"name": name, "from": from_pos, "to": to_pos, "parent": parent}
    if side:
        entry["side"] = side
    cubes.append(entry)

# ==========================================
# 1. BODY / PELVIS (body, origin [0, 22, 0])
# ==========================================
c('pelvis_main', [-8.5, 18, -5], [8.5, 28, 6], 'body')
c('groin_center', [-4.5, 16, -4], [4.5, 21, 4], 'body')
c('belly_lower', [-7.5, 20, -7], [7.5, 28, -5], 'body')
c('hip_flank_right', [7.5, 20, -3.5], [10.5, 27, 4.5], 'body', 'right')
c('hip_flank_left', [-10.5, 20, -3.5], [-7.5, 27, 4.5], 'body', 'left')

# ==========================================
# 2. TORSO & GORILLA CHEST & HUNCH (torso, origin [0, 28, 2])
# ==========================================
c('torso_core', [-10, 28, -6], [10, 44, 6], 'torso')
c('belly_upper', [-8.5, 28, -8], [8.5, 36, -6], 'torso')
# Chiseled pectorals
c('pec_right', [0.5, 34, -8.8], [9.5, 43, -5.8], 'torso', 'right')
c('pec_left', [-9.5, 34, -8.8], [-0.5, 43, -5.8], 'torso', 'left')
# Upper chest fur collar (Oozaru chest ruff)
c('chest_ruff_top', [-7, 41, -8.5], [7, 45, -6.5], 'torso')
# Massive primate hunchback traps
c('hunchback_spine', [-6.5, 40, 3], [6.5, 49, 9], 'torso')
c('shoulder_trap_right', [6.5, 39, -1.5], [13, 47, 5.5], 'torso', 'right')
c('shoulder_trap_left', [-13, 39, -1.5], [-6.5, 47, 5.5], 'torso', 'left')
c('neck_nape', [-5, 40, -5], [5, 45, 1], 'torso')

# ==========================================
# 3. HEAD & BEAST MUZZLE & DEMON EARS (head, origin [0, 43, -4])
# ==========================================
# Cranium
c('head_skull', [-5, 43, -8], [5, 52, 0], 'head')
# Mane / Lion-like facial fur ruff framing the face
c('mane_top', [-5.5, 51, -9], [5.5, 54.5, -3], 'head')
c('mane_cheek_right', [4.5, 42, -9], [8.5, 51, -2], 'head', 'right')
c('mane_cheek_left', [-8.5, 42, -9], [-4.5, 51, -2], 'head', 'left')
c('mane_flair_right', [7.5, 43, -8], [11, 48.5, -3], 'head', 'right')
c('mane_flair_left', [-11, 43, -8], [-7.5, 48.5, -3], 'head', 'left')

# Furious V-Brow Ridge
c('brow_ridge_main', [-5.5, 49, -12], [5.5, 51.8, -8], 'head')
c('brow_crease_v', [-2.5, 50, -12.5], [2.5, 52.5, -9], 'head')

# Intense Glowing Red Eyes (deeply recessed under brow)
c('eye_right', [1.5, 47.5, -11.8], [4.8, 49.8, -9.5], 'head', 'right')
c('eye_left', [-4.8, 47.5, -11.8], [-1.5, 49.8, -9.5], 'head', 'left')

# Long, sharply pointed Saiyan Demon/Ape Ears (angled 45 degrees up and out)
c('ear_base_right', [4.5, 46.5, -6], [8.5, 50.5, -3], 'head', 'right')
c('ear_mid_right', [8, 49, -5.5], [12.5, 54, -3], 'head', 'right')
c('ear_tip_right', [12, 52.5, -5], [17, 57.5, -3.5], 'head', 'right')
c('ear_inner_right', [6.5, 47.5, -5.8], [11.5, 52.5, -4], 'head', 'right')

c('ear_base_left', [-8.5, 46.5, -6], [-4.5, 50.5, -3], 'head', 'left')
c('ear_mid_left', [-12.5, 49, -5.5], [-8, 54, -3], 'head', 'left')
c('ear_tip_left', [-17, 52.5, -5], [-12, 57.5, -3.5], 'head', 'left')
c('ear_inner_left', [-11.5, 47.5, -5.8], [-6.5, 52.5, -4], 'head', 'left')

# Sloping Beast Muzzle / Snout (tan/flesh tone)
c('snout_bridge', [-3.2, 45, -14.5], [3.2, 48.5, -8.5], 'head')
c('snout_nose_tip', [-2.2, 45.8, -16], [2.2, 48.0, -13.5], 'head')
c('snout_upper_lip', [-3.8, 43.5, -15], [3.8, 45.8, -9.5], 'head')

# Upper Fangs & Teeth
c('upper_teeth_plate', [-3.4, 42.5, -14.8], [3.4, 44.0, -10.5], 'head')
c('canine_upper_right', [2.4, 40.8, -14.5], [3.5, 43.2, -13.2], 'head', 'right')
c('canine_upper_left', [-3.5, 40.8, -14.5], [-2.4, 43.2, -13.2], 'head', 'left')

# ==========================================
# 4. LOWER JAW & GAPING MAW (jaw, origin [0, 40, -8])
# ==========================================
c('jaw_frame', [-3.6, 37.5, -14], [3.6, 41.5, -8], 'jaw')
c('jaw_chin_point', [-2.2, 36.2, -14.5], [2.2, 39.0, -11], 'jaw')
c('lower_teeth_plate', [-3.2, 41.0, -13.8], [3.2, 42.5, -9.5], 'jaw')
c('canine_lower_right', [2.2, 42.0, -13.8], [3.3, 44.5, -12.5], 'jaw', 'right')
c('canine_lower_left', [-3.3, 42.0, -13.8], [-2.2, 44.5, -12.5], 'jaw', 'left')
c('mouth_interior', [-2.8, 38.5, -12.5], [2.8, 41.0, -8.5], 'jaw')
c('mouth_tongue', [-1.8, 38.5, -12.5], [1.8, 39.8, -9], 'jaw')

# ==========================================
# 5. SAIYAN SPIKY HAIR CREST (hair, origin [0, 50, -4])
# ==========================================
c('hair_spike_crown', [-3, 52, -5], [3, 58, 0], 'hair')
c('hair_spike_mid', [-2, 57, -3], [2, 63, 1], 'hair')
c('hair_spike_top', [-1.2, 62, -1], [1.2, 68, 3], 'hair')
c('hair_spike_back', [-3.5, 46, 0], [3.5, 54, 4], 'hair')
c('hair_side_spike_right', [2.5, 53, -4], [5.5, 58.5, 0], 'hair', 'right')
c('hair_side_spike_left', [-5.5, 53, -4], [-2.5, 58.5, 0], 'hair', 'left')

# ==========================================
# 6. RIGHT ARM (right_arm, right_forearm, right_hand)
# ==========================================
# Shoulder Deltoid Boulder
c('shoulder_deltoid_right', [9, 36, -3.5], [17.5, 46.5, 5], 'right_arm', 'right')
c('bicep_right', [10.5, 28, -2.5], [16.5, 38, 4], 'right_arm', 'right')
c('tricep_right', [11, 29, 3], [16, 37, 6], 'right_arm', 'right')

# Massive Forearm
c('elbow_right', [11.5, 27, -1.5], [16.5, 32, 3.5], 'right_forearm', 'right')
c('forearm_thick_right', [12, 17, -4], [19.5, 29, 4], 'right_forearm', 'right')
c('wrist_right', [13, 14, -3], [18.5, 18, 3], 'right_forearm', 'right')

# Ape Fist
c('palm_right', [13, 7, -4.5], [19, 15, 2.5], 'right_hand', 'right')
c('knuckles_right', [13.5, 5, -6], [18.5, 11, -4], 'right_hand', 'right')
c('fingers_right', [14, 6, -5.5], [18, 13, -1.5], 'right_hand', 'right')
c('thumb_right', [11.5, 8, -4], [14, 12, -1.5], 'right_hand', 'right')

# ==========================================
# 7. LEFT ARM (left_arm, left_forearm, left_hand)
# ==========================================
# Shoulder Deltoid Boulder
c('shoulder_deltoid_left', [-17.5, 36, -3.5], [-9, 46.5, 5], 'left_arm', 'left')
c('bicep_left', [-16.5, 28, -2.5], [-10.5, 38, 4], 'left_arm', 'left')
c('tricep_left', [-16, 29, 3], [-11, 37, 6], 'left_arm', 'left')

# Massive Forearm
c('elbow_left', [-16.5, 27, -1.5], [-11.5, 32, 3.5], 'left_forearm', 'left')
c('forearm_thick_left', [-19.5, 17, -4], [-12, 29, 4], 'left_forearm', 'left')
c('wrist_left', [-18.5, 14, -3], [-13, 18, 3], 'left_forearm', 'left')

# Ape Fist
c('palm_left', [-19, 7, -4.5], [-13, 15, 2.5], 'left_hand', 'left')
c('knuckles_left', [-18.5, 5, -6], [-13.5, 11, -4], 'left_hand', 'left')
c('fingers_left', [-18, 6, -5.5], [-14, 13, -1.5], 'left_hand', 'left')
c('thumb_left', [-14, 8, -4], [-11.5, 12, -1.5], 'left_hand', 'left')

# ==========================================
# 8. RIGHT LEG & MONKEY FOOT (right_leg, right_shin)
# ==========================================
c('thigh_upper_right', [3.5, 14, -4.5], [12, 24, 4.5], 'right_leg', 'right')
c('thigh_outer_right', [10.5, 15, -3.5], [13.5, 23, 3.5], 'right_leg', 'right')
c('knee_right', [4.5, 10, -5], [11, 15, 3], 'right_leg', 'right')

c('calf_muscle_right', [4.5, 3.5, -2.5], [11.5, 12, 4.5], 'right_shin', 'right')
c('shin_bone_right', [5, 2.5, -4], [10.5, 10, 1.5], 'right_shin', 'right')
c('ankle_right', [5.5, 1.8, -3], [10, 4.5, 2.5], 'right_shin', 'right')

c('heel_right', [5, 0, 1], [10.5, 3, 5], 'right_shin', 'right')
c('sole_right', [4.5, 0, -3.5], [11, 2.6, 1.5], 'right_shin', 'right')
c('toes_outer_right', [4, 0, -8], [11.5, 2.4, -3.5], 'right_shin', 'right')
c('thumb_toe_right', [2, 0, -5.5], [4.8, 2.2, -1.8], 'right_shin', 'right')

# ==========================================
# 9. LEFT LEG & MONKEY FOOT (left_leg, left_shin)
# ==========================================
c('thigh_upper_left', [-12, 14, -4.5], [-3.5, 24, 4.5], 'left_leg', 'left')
c('thigh_outer_left', [-13.5, 15, -3.5], [-10.5, 23, 3.5], 'left_leg', 'left')
c('knee_left', [-11, 10, -5], [-4.5, 15, 3], 'left_leg', 'left')

c('calf_muscle_left', [-11.5, 3.5, -2.5], [-4.5, 12, 4.5], 'left_shin', 'left')
c('shin_bone_left', [-10.5, 2.5, -4], [-5, 10, 1.5], 'left_shin', 'left')
c('ankle_left', [-10, 1.8, -3], [-5.5, 4.5, 2.5], 'left_shin', 'left')

c('heel_left', [-10.5, 0, 1], [-5, 3, 5], 'left_shin', 'left')
c('sole_left', [-11, 0, -3.5], [-4.5, 2.6, 1.5], 'left_shin', 'left')
c('toes_outer_left', [-11.5, 0, -8], [-4, 2.4, -3.5], 'left_shin', 'left')
c('thumb_toe_left', [-4.8, 0, -5.5], [-2, 2.2, -1.8], 'left_shin', 'left')

# ==========================================
# 10. SAIYAN MONKEY TAIL (tail > tail2 > tail3)
# ==========================================
c('tail_base', [-2.2, 19, 5], [2.2, 25, 10], 'tail')
c('tail_seg1', [-2.0, 21, 9], [2.0, 27, 16], 'tail')

c('tail_seg2_lower', [-1.8, 26, 15], [1.8, 34, 20], 'tail2')
c('tail_seg2_upper', [-1.6, 33, 16], [1.6, 42, 21], 'tail2')

c('tail_seg3_curve', [-1.4, 41, 13], [1.4, 50, 18], 'tail3')
c('tail_seg3_tip', [-1.2, 48, 9], [1.2, 54, 14], 'tail3')

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
new Locator({ name: 'mouth_fx', from: [0, 42.5, -15.5] }).init().addTo(groupMap['head']);
new Locator({ name: 'right_hand_fx', from: [16, 8, -4.5] }).init().addTo(groupMap['right_hand']);
new Locator({ name: 'left_hand_fx', from: [-16, 8, -4.5] }).init().addTo(groupMap['left_hand']);
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

with open('tools/run_authentic_oozaru.js', 'w', encoding='utf-8') as f:
    f.write("\n".join(js))

print(f"Generated Authentic Oozaru script with {len(cubes)} cubes across {len(groups)} groups!")
