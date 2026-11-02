import json

js = []
js.append("Project.texture_width = 128;")
js.append("Project.texture_height = 128;")

# Groups
groups_def = [
    {"name": "root", "origin": [0, 0, 0], "parent": None},
    {"name": "body", "origin": [0, 20, 0], "parent": "root"},
    {"name": "robe_skirt", "origin": [0, 20, 0], "parent": "body"},
    {"name": "torso", "origin": [0, 24, 0], "parent": "body"},
    {"name": "halo", "origin": [0, 34, 0], "parent": "torso"},
    {"name": "head", "origin": [0, 36, 0], "parent": "torso"},
    {"name": "jaw", "origin": [0, 36.5, -1.5], "parent": "head"},
    {"name": "hair", "origin": [0, 42, 0], "parent": "head"},
    {"name": "right_arm", "origin": [4.5, 32, 0], "parent": "torso"},
    {"name": "right_forearm", "origin": [4.5, 26, 0], "parent": "right_arm"},
    {"name": "right_hand", "origin": [4.5, 20, 0], "parent": "right_forearm"},
    {"name": "staff", "origin": [4.5, 18, -4], "parent": "right_hand"},
    {"name": "left_arm", "origin": [-4.5, 32, 0], "parent": "torso"},
    {"name": "left_forearm", "origin": [-4.5, 26, 0], "parent": "left_arm"},
    {"name": "left_hand", "origin": [-4.5, 20, 0], "parent": "left_forearm"},
    {"name": "right_leg", "origin": [2.2, 20, 0], "parent": "body"},
    {"name": "right_shin", "origin": [2.2, 9, 0], "parent": "right_leg"},
    {"name": "left_leg", "origin": [-2.2, 20, 0], "parent": "body"},
    {"name": "left_shin", "origin": [-2.2, 9, 0], "parent": "left_leg"},
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
js.append("""
new Locator({ name: 'mouth_fx', from: [0, 37.5, -3.2] }).init().addTo(groupMap['head']);
new Locator({ name: 'right_hand_fx', from: [4.5, 16.5, 0] }).init().addTo(groupMap['right_hand']);
new Locator({ name: 'left_hand_fx', from: [-4.5, 16.5, 0] }).init().addTo(groupMap['left_hand']);
""")

# Cubes
cubes = [
    # Pelvis & Waist & Sash (body)
    {"name": "pelvis", "from": [-3.2, 18, -2.2], "to": [3.2, 22, 2.2], "parent": "body"},
    {"name": "sash_belt", "from": [-3.6, 20, -2.6], "to": [3.6, 23.5, 2.6], "parent": "body"},
    {"name": "sash_hanging", "from": [-1.6, 7.5, -2.8], "to": [1.6, 22, -2.2], "parent": "body"},

    # Robe Skirt (robe_skirt)
    {"name": "skirt_upper", "from": [-4, 13, -2.8], "to": [4, 19, 2.8], "parent": "robe_skirt"},
    {"name": "skirt_lower", "from": [-4.8, 7, -3.4], "to": [4.8, 14, 3.4], "parent": "robe_skirt"},
    {"name": "side_mantle_right", "from": [3.2, 10, -2.9], "to": [4.9, 20, 2.9], "parent": "robe_skirt", "side": "right"},
    {"name": "side_mantle_left", "from": [-4.9, 10, -2.9], "to": [-3.2, 20, 2.9], "parent": "robe_skirt", "side": "left"},

    # Torso, Chest, Shoulders, Collar (torso)
    {"name": "torso_chest", "from": [-3.2, 22, -2], "to": [3.2, 33, 2], "parent": "torso"},
    {"name": "neck", "from": [-1.2, 33, -1.2], "to": [1.2, 36.5, 1.2], "parent": "torso"},
    {"name": "collar_high_neck", "from": [-1.6, 33.5, -1.6], "to": [1.6, 35.8, 1.6], "parent": "torso"},
    {"name": "mantle_shoulder_right", "from": [2.5, 31, -3], "to": [7.2, 33.8, 3], "parent": "torso", "side": "right"},
    {"name": "mantle_shoulder_left", "from": [-7.2, 31, -3], "to": [-2.5, 33.8, 3], "parent": "torso", "side": "left"},
    {"name": "mantle_chest_yoke", "from": [-3.4, 29, -2.6], "to": [3.4, 33.5, -1.8], "parent": "torso"},
    {"name": "mantle_back_yoke", "from": [-3.4, 29, 1.8], "to": [3.4, 33.5, 2.6], "parent": "torso"},

    # Angelic Halo (halo)
    {"name": "halo_front", "from": [-6.5, 33.5, -6.5], "to": [6.5, 34.5, -5.5], "parent": "halo"},
    {"name": "halo_back", "from": [-6.5, 35.5, 5.5], "to": [6.5, 36.5, 6.5], "parent": "halo"},
    {"name": "halo_right", "from": [5.5, 34, -6], "to": [6.5, 36, 6], "parent": "halo", "side": "right"},
    {"name": "halo_left", "from": [-6.5, 34, -6], "to": [-5.5, 36, 6], "parent": "halo", "side": "left"},

    # Head & Jaw (head, jaw)
    {"name": "head_base", "from": [-2.8, 36, -2.8], "to": [2.8, 42, 2.8], "parent": "head"},
    {"name": "head_nose", "from": [-0.4, 38, -3.2], "to": [0.4, 39, -2.8], "parent": "head"},
    {"name": "jaw_main", "from": [-1.2, 35.8, -2.8], "to": [1.2, 37.2, -1.2], "parent": "jaw"},

    # Hair (hair)
    {"name": "hair_base", "from": [-3.2, 41.5, -3], "to": [3.2, 46, 3], "parent": "hair"},
    {"name": "hair_mid", "from": [-2.6, 45.5, -2.5], "to": [2.6, 51, 2.5], "parent": "hair"},
    {"name": "hair_top", "from": [-1.8, 50.5, -2], "to": [1.8, 55, 1.5], "parent": "hair"},
    {"name": "hair_curl", "from": [-1.2, 53.5, -3.8], "to": [1.2, 56.5, -1], "parent": "hair"},

    # Right Arm & Hand
    {"name": "arm_upper_right", "from": [3.5, 26, -1.5], "to": [5.5, 32, 1.5], "parent": "right_arm", "side": "right"},
    {"name": "arm_fore_right", "from": [3.5, 20, -1.5], "to": [5.5, 26, 1.5], "parent": "right_forearm", "side": "right"},
    {"name": "hand_palm_right", "from": [3.8, 16.5, -1.2], "to": [5.2, 20, 1.2], "parent": "right_hand", "side": "right"},

    # Left Arm & Hand
    {"name": "arm_upper_left", "from": [-5.5, 26, -1.5], "to": [-3.5, 32, 1.5], "parent": "left_arm", "side": "left"},
    {"name": "arm_fore_left", "from": [-5.5, 20, -1.5], "to": [-3.5, 26, 1.5], "parent": "left_forearm", "side": "left"},
    {"name": "hand_palm_left", "from": [-5.2, 16.5, -1.2], "to": [-3.8, 20, 1.2], "parent": "left_hand", "side": "left"},

    # Staff (staff, child of right_hand)
    {"name": "staff_shaft_lower", "from": [4.2, 0, -4.3], "to": [4.8, 12, -3.7], "parent": "staff", "side": "right"},
    {"name": "staff_shaft_mid", "from": [4.1, 12, -4.4], "to": [4.9, 36, -3.6], "parent": "staff", "side": "right"},
    {"name": "staff_shaft_upper", "from": [4, 36, -4.5], "to": [5, 43, -3.5], "parent": "staff", "side": "right"},
    {"name": "staff_cup", "from": [3.6, 43, -4.9], "to": [5.4, 46, -3.1], "parent": "staff", "side": "right"},
    {"name": "staff_halo_ring", "from": [2.6, 47, -5.9], "to": [6.4, 52, -2.1], "parent": "staff", "side": "right"},
    {"name": "staff_orb", "from": [3.5, 47.5, -5], "to": [5.5, 51.5, -3], "parent": "staff", "side": "right"},

    # Right Leg & Shin & Foot
    {"name": "thigh_right", "from": [1.2, 9, -1.2], "to": [3.2, 20, 1.2], "parent": "right_leg", "side": "right"},
    {"name": "shin_right", "from": [1.3, 4, -1], "to": [3.1, 9, 1], "parent": "right_shin", "side": "right"},
    {"name": "shoe_collar_right", "from": [1, 2.5, -1.8], "to": [3.4, 4.8, 1.8], "parent": "right_shin", "side": "right"},
    {"name": "shoe_foot_right", "from": [1.1, 0, -3.8], "to": [3.3, 2.8, 1.6], "parent": "right_shin", "side": "right"},

    # Left Leg & Shin & Foot
    {"name": "thigh_left", "from": [-3.2, 9, -1.2], "to": [-1.2, 20, 1.2], "parent": "left_leg", "side": "left"},
    {"name": "shin_left", "from": [-3.1, 4, -1], "to": [-1.3, 9, 1], "parent": "left_shin", "side": "left"},
    {"name": "shoe_collar_left", "from": [-3.4, 2.5, -1.8], "to": [-1, 4.8, 1.8], "parent": "left_shin", "side": "left"},
    {"name": "shoe_foot_left", "from": [-3.3, 0, -3.8], "to": [-1.1, 2.8, 1.6], "parent": "left_shin", "side": "left"},
]

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

full_script = "\n".join(js)
with open('tools/run_whis_script.js', 'w', encoding='utf-8') as f:
    f.write(full_script)

print(f"Generated Whis script with {len(cubes)} cubes!")
