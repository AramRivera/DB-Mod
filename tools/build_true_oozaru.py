import json

# ==============================================================================
# AUTHENTIC DRAGON BALL OOZARU (GREAT APE) GENERATOR
# ==============================================================================

groups = [
    {"name": "root", "origin": [0, 0, 0], "parent": None},
    {"name": "body", "origin": [0, 22, 0], "parent": "root"},
    {"name": "tail", "origin": [0, 21, 7], "parent": "body", "rotation": [-15, 0, 0]},
    {"name": "tail2", "origin": [0, 26, 17], "parent": "tail", "rotation": [35, 0, 0]},
    {"name": "tail3", "origin": [0, 40, 20], "parent": "tail2", "rotation": [45, 0, 0]},
    {"name": "torso", "origin": [0, 26, 2], "parent": "body", "rotation": [14, 0, 0]},
    {"name": "head", "origin": [0, 42, -5], "parent": "torso", "rotation": [-10, 0, 0]},
    {"name": "jaw", "origin": [0, 40, -7], "parent": "head", "rotation": [32, 0, 0]},
    {"name": "hair", "origin": [0, 49, -5], "parent": "head"},
    {"name": "right_arm", "origin": [13, 42, 0], "parent": "torso", "rotation": [18, 0, 18]},
    {"name": "right_forearm", "origin": [17, 28, -1], "parent": "right_arm", "rotation": [-35, -15, 0]},
    {"name": "right_hand", "origin": [18, 15, -3], "parent": "right_forearm", "rotation": [12, 0, 0]},
    {"name": "left_arm", "origin": [-13, 42, 0], "parent": "torso", "rotation": [18, 0, -18]},
    {"name": "left_forearm", "origin": [-17, 28, -1], "parent": "left_arm", "rotation": [-35, 15, 0]},
    {"name": "left_hand", "origin": [-18, 15, -3], "parent": "left_forearm", "rotation": [12, 0, 0]},
    {"name": "right_leg", "origin": [8, 20, 0], "parent": "body", "rotation": [-20, 15, 12]},
    {"name": "right_shin", "origin": [9, 10, 2], "parent": "right_leg", "rotation": [28, 0, -6]},
    {"name": "left_leg", "origin": [-8, 20, 0], "parent": "body", "rotation": [-20, -15, -12]},
    {"name": "left_shin", "origin": [-9, 10, 2], "parent": "left_leg", "rotation": [28, 0, 6]}
]

cubes = []

def c(name, from_pos, to_pos, parent, side=None):
    entry = {"name": name, "from": from_pos, "to": to_pos, "parent": parent}
    if side:
        entry["side"] = side
    cubes.append(entry)

# 1. BODY / PELVIS
c('pelvis_core', [-8, 17, -5], [8, 27, 6], 'body')
c('groin', [-4, 15, -4], [4, 20, 4], 'body')
c('belly_lower', [-7, 19, -7], [7, 27, -5], 'body')
c('hip_flank_right', [7, 19, -3], [10, 26, 4], 'body', 'right')
c('hip_flank_left', [-10, 19, -3], [-7, 26, 4], 'body', 'left')

# 2. TORSO & GORILLA CHEST & HUNCH
c('torso_chest_core', [-9.5, 27, -5.5], [9.5, 43, 6], 'torso')
c('torso_belly_upper', [-8, 27, -7.5], [8, 35, -5.5], 'torso')
c('pec_right', [0.5, 33, -8.5], [9, 42, -5.5], 'torso', 'right')
c('pec_left', [-9, 33, -8.5], [-0.5, 42, -5.5], 'torso', 'left')
c('hunchback_hump', [-6, 39, 3], [6, 48, 9], 'torso')
c('shoulder_trap_right', [6.5, 38, -1.5], [13, 46, 5], 'torso', 'right')
c('shoulder_trap_left', [-13, 38, -1.5], [-6.5, 46, 5], 'torso', 'left')
c('neck_base', [-5, 39, -5], [5, 44, 0], 'torso')

# 3. HEAD & FEROCIOUS OOZARU SNOUT & GLOWING RED EYES
c('head_skull', [-5, 42, -8], [5, 51, 0], 'head')
c('face_skin_mask', [-4.5, 42, -10.5], [4.5, 50, -7.5], 'head')

# Intense furrowed V-brow ridge
c('brow_ridge_main', [-5.2, 48.5, -12.5], [5.2, 51.5, -7.5], 'head')
c('brow_v_center', [-2, 49.5, -13], [2, 52, -8], 'head')

# GLOWING RED EYES (PROMINENT AND UNBLOCKED!)
c('eye_right', [1.2, 47.5, -12.2], [4.5, 49.8, -9.5], 'head', 'right')
c('eye_left', [-4.5, 47.5, -12.2], [-1.2, 49.8, -9.5], 'head', 'left')

# Facial fur ruff / flared sideburns
c('mane_top', [-5.5, 50, -8.5], [5.5, 53.5, -2], 'head')
c('mane_side_right', [4.2, 41, -8.5], [8.5, 50, -1.5], 'head', 'right')
c('mane_side_left', [-8.5, 41, -8.5], [-4.2, 50, -1.5], 'head', 'left')
c('mane_flair_right', [7.5, 42, -7.5], [11, 47.5, -2], 'head', 'right')
c('mane_flair_left', [-11, 42, -7.5], [-7.5, 47.5, -2], 'head', 'left')

# Pointed Saiyan demon ears
c('ear_base_right', [4.5, 45.5, -5.5], [8.5, 49.5, -2.5], 'head', 'right')
c('ear_mid_right', [8, 48, -5], [12.5, 53, -2.5], 'head', 'right')
c('ear_tip_right', [12, 51.5, -4.5], [17, 56.5, -3], 'head', 'right')
c('ear_inner_right', [6.5, 46.5, -5.3], [11.5, 51.5, -3.5], 'head', 'right')

c('ear_base_left', [-8.5, 45.5, -5.5], [-4.5, 49.5, -2.5], 'head', 'left')
c('ear_mid_left', [-12.5, 48, -5], [-8, 53, -2.5], 'head', 'left')
c('ear_tip_left', [-17, 51.5, -4.5], [-12, 56.5, -3], 'head', 'left')
c('ear_inner_left', [-11.5, 46.5, -5.3], [-6.5, 51.5, -3.5], 'head', 'left')

# Snarling upper muzzle (sits BELOW the eyes, y = 43.5 to 47.2)
c('snout_base', [-3.5, 43.5, -13.5], [3.5, 47.2, -8], 'head')
c('snout_bridge', [-2.2, 45.2, -14.2], [2.2, 47.2, -10.5], 'head')
c('snout_nostril_plate', [-2.5, 44.2, -14.5], [2.5, 46.0, -12.5], 'head')
c('upper_lip', [-3.8, 43.0, -13.8], [3.8, 44.8, -9.0], 'head')

# Upper needle teeth & massive canine fangs
c('upper_teeth_plate', [-3.2, 42.0, -13.5], [3.2, 43.5, -9.5], 'head')
c('canine_upper_right', [2.2, 39.5, -13.2], [3.4, 42.8, -11.8], 'head', 'right')
c('canine_upper_left', [-3.4, 39.5, -13.2], [-2.2, 42.8, -11.8], 'head', 'left')

# 4. LOWER JAW & ROARING MAW (jaw)
c('jaw_mandible', [-3.5, 37.0, -13.0], [3.5, 41.0, -7.5], 'jaw')
c('jaw_chin_point', [-2.2, 35.5, -13.5], [2.2, 38.5, -9.5], 'jaw')
c('lower_teeth_plate', [-3.0, 40.5, -12.8], [3.0, 42.2, -8.5], 'jaw')
c('canine_lower_right', [2.0, 41.5, -12.8], [3.2, 44.8, -11.2], 'jaw', 'right')
c('canine_lower_left', [-3.2, 41.5, -12.8], [-2.0, 44.8, -11.2], 'jaw', 'left')
c('mouth_throat_cavity', [-2.8, 38.0, -12.2], [2.8, 41.0, -7.8], 'jaw')
c('mouth_tongue', [-1.6, 38.0, -12.0], [1.6, 39.5, -8.5], 'jaw')

# 5. SAIYAN SPIKY HAIR CREST (hair)
c('hair_crown_tuft', [-2.8, 51, -5], [2.8, 57, 0], 'hair')
c('hair_mid_crest', [-2, 56, -3], [2, 62, 1], 'hair')
c('hair_top_spike', [-1.2, 61, -1], [1.2, 67, 3], 'hair')
c('hair_nape_spikes', [-3.2, 45, 0], [3.2, 53, 4], 'hair')
c('hair_side_spike_right', [2.5, 52, -4], [5.5, 57.5, 0], 'hair', 'right')
c('hair_side_spike_left', [-5.5, 52, -4], [-2.5, 57.5, 0], 'hair', 'left')

# 6. RIGHT ARM
c('shoulder_boulder_right', [9, 35, -3.5], [17.5, 45.5, 5], 'right_arm', 'right')
c('bicep_right', [10.5, 27, -2.5], [16.5, 37, 4], 'right_arm', 'right')
c('elbow_right', [11, 26, -1.5], [16, 31, 3.5], 'right_forearm', 'right')
c('forearm_mass_right', [11.5, 16, -4], [19, 28, 4], 'right_forearm', 'right')
c('wrist_right', [12.5, 13, -3], [18, 17, 3], 'right_forearm', 'right')
c('fist_palm_right', [12.5, 6, -4.5], [18.5, 14, 2.5], 'right_hand', 'right')
c('fist_knuckles_right', [13, 4, -6], [18, 10, -4], 'right_hand', 'right')
c('fist_fingers_right', [13.5, 5, -5.5], [17.5, 12, -1.5], 'right_hand', 'right')
c('fist_thumb_right', [11, 7, -4], [13.5, 11, -1.5], 'right_hand', 'right')

# 7. LEFT ARM
c('shoulder_boulder_left', [-17.5, 35, -3.5], [-9, 45.5, 5], 'left_arm', 'left')
c('bicep_left', [-16.5, 27, -2.5], [-10.5, 37, 4], 'left_arm', 'left')
c('elbow_left', [-16, 26, -1.5], [-11, 31, 3.5], 'left_forearm', 'left')
c('forearm_mass_left', [-19, 16, -4], [-11.5, 28, 4], 'left_forearm', 'left')
c('wrist_left', [-18, 13, -3], [-12.5, 17, 3], 'left_forearm', 'left')
c('fist_palm_left', [-18.5, 6, -4.5], [-12.5, 14, 2.5], 'left_hand', 'left')
c('fist_knuckles_left', [-18, 4, -6], [-13, 10, -4], 'left_hand', 'left')
c('fist_fingers_left', [-17.5, 5, -5.5], [-13.5, 12, -1.5], 'left_hand', 'left')
c('fist_thumb_left', [-13.5, 7, -4], [-11, 11, -1.5], 'left_hand', 'left')

# 8. RIGHT LEG & MONKEY FOOT
c('thigh_upper_right', [3.5, 13, -4.5], [11.5, 23, 4.5], 'right_leg', 'right')
c('thigh_outer_right', [10, 14, -3.5], [13, 22, 3.5], 'right_leg', 'right')
c('knee_right', [4, 9, -5], [10.5, 14, 3], 'right_leg', 'right')
c('calf_muscle_right', [4, 3, -2.5], [11, 11.5, 4.5], 'right_shin', 'right')
c('shin_bone_right', [4.5, 2, -4], [10, 9.5, 1.5], 'right_shin', 'right')
c('ankle_right', [5, 1.5, -3], [9.5, 4, 2.5], 'right_shin', 'right')
c('heel_right', [4.5, 0, 1], [10, 2.8, 5], 'right_shin', 'right')
c('sole_right', [4, 0, -3.5], [10.5, 2.4, 1.5], 'right_shin', 'right')
c('toes_outer_right', [3.5, 0, -8], [11, 2.2, -3.5], 'right_shin', 'right')
c('thumb_toe_right', [1.5, 0, -5.5], [4.2, 2, -1.8], 'right_shin', 'right')

# 9. LEFT LEG & MONKEY FOOT
c('thigh_upper_left', [-11.5, 13, -4.5], [-3.5, 23, 4.5], 'left_leg', 'left')
c('thigh_outer_left', [-13, 14, -3.5], [-10, 22, 3.5], 'left_leg', 'left')
c('knee_left', [-10.5, 9, -5], [-4, 14, 3], 'left_leg', 'left')
c('calf_muscle_left', [-11, 3, -2.5], [-4, 11.5, 4.5], 'left_shin', 'left')
c('shin_bone_left', [-10, 2, -4], [-4.5, 9.5, 1.5], 'left_shin', 'left')
c('ankle_left', [-9.5, 1.5, -3], [-5, 4, 2.5], 'left_shin', 'left')
c('heel_left', [-10, 0, 1], [-4.5, 2.8, 5], 'left_shin', 'left')
c('sole_left', [-10.5, 0, -3.5], [-4, 2.4, 1.5], 'left_shin', 'left')
c('toes_outer_left', [-11, 0, -8], [-3.5, 2.2, -3.5], 'left_shin', 'left')
c('thumb_toe_left', [-4.2, 0, -5.5], [-1.5, 2, -1.8], 'left_shin', 'left')

# 10. SAIYAN MONKEY TAIL
c('tail_base', [-2, 18, 5], [2, 24, 10], 'tail')
c('tail_seg1', [-1.8, 20, 9], [1.8, 26, 16], 'tail')
c('tail_seg2_lower', [-1.6, 25, 15], [1.6, 33, 20], 'tail2')
c('tail_seg2_upper', [-1.4, 32, 16], [1.4, 41, 21], 'tail2')
c('tail_seg3_curve', [-1.2, 40, 13], [1.2, 49, 18], 'tail3')
c('tail_seg3_tip', [-1.0, 47, 9], [1.0, 53, 14], 'tail3')

# Build Blockbench Script
js = []
js.append("Project.texture_width = 256;")
js.append("Project.texture_height = 256;")
js.append(f"const gDefs = {json.dumps(groups)};")
js.append("""
const groupMap = {};
for (const g of gDefs) {
    const grp = new Group({
        name: g.name,
        origin: g.origin,
        rotation: g.rotation || [0, 0, 0]
    }).init();
    groupMap[g.name] = grp;
    if (g.parent && groupMap[g.parent]) {
        grp.addTo(groupMap[g.parent]);
    }
}

// Locators
new Locator({ name: 'mouth_fx', from: [0, 41, -14.5] }).init().addTo(groupMap['head']);
new Locator({ name: 'right_hand_fx', from: [15.5, 7, -4.5] }).init().addTo(groupMap['right_hand']);
new Locator({ name: 'left_hand_fx', from: [-15.5, 7, -4.5] }).init().addTo(groupMap['left_hand']);
""")

js.append(f"const cDefs = {json.dumps(cubes)};")
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

with open('tools/run_true_oozaru.js', 'w', encoding='utf-8') as f:
    f.write("\n".join(js))

print(f"Generated True Oozaru script with {len(cubes)} cubes!")
