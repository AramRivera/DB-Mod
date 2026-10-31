const fs = require('fs');
const path = require('path');

async function call(action, params = {}) {
    const res = await fetch("http://127.0.0.1:8787/command", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: "build_" + Date.now(), action, params })
    });
    return await res.json();
}

async function main() {
    console.log("Resetting project...");
    await call("new_project", {
        format: "modded_entity",
        name: "shenlong",
        texture_width: 128,
        texture_height: 128
    });

    const cubes = [];

    // ==========================================
    // 1. HEAD & JAW
    // ==========================================
    console.log("Adding head & jaw groups...");
    await call("add_group", { name: "head", origin: [0, 48, -14] });

    cubes.push(
        // Cranium
        { name: "head_base", from: [-5, 44, -20], to: [5, 52, -10], parent: "head" },
        // Upper Snout
        { name: "snout_upper", from: [-3.8, 45.5, -31], to: [3.8, 49.5, -20], parent: "head" },
        { name: "snout_ridge", from: [-1.8, 49.5, -30.5], to: [1.8, 50.8, -20], parent: "head" },
        { name: "snout_tip", from: [-3.2, 45.5, -32.5], to: [3.2, 49, -31], parent: "head" },
        { name: "nostril_left", from: [-2.8, 48.5, -32.2], to: [-1.2, 50, -30.5], parent: "head" },
        { name: "nostril_right", from: [1.2, 48.5, -32.2], to: [2.8, 50, -30.5], parent: "head" },

        // Brows & Eyes
        { name: "brow_left", from: [-5.2, 49.2, -24], to: [-1.8, 50.8, -19], parent: "head" },
        { name: "brow_right", from: [1.8, 49.2, -24], to: [5.2, 50.8, -19], parent: "head" },
        { name: "eye_left_glow", from: [-4.8, 47.5, -23.5], to: [-2.2, 49.2, -20.5], parent: "head" },
        { name: "eye_right_glow", from: [2.2, 47.5, -23.5], to: [4.8, 49.2, -20.5], parent: "head" },

        // Upper Fangs & Teeth
        { name: "fang_upper_left", from: [-3.4, 42.5, -29.5], to: [-2.2, 45.5, -27.5], parent: "head" },
        { name: "fang_upper_right", from: [2.2, 42.5, -29.5], to: [3.4, 45.5, -27.5], parent: "head" },
        { name: "teeth_upper_front", from: [-2.2, 44.2, -30.8], to: [2.2, 45.5, -29.5], parent: "head" },

        // Cheek Mane Tufts
        { name: "mane_cheek_left_1", from: [-7.5, 45, -18], to: [-5, 49, -14], parent: "head" },
        { name: "mane_cheek_left_2", from: [-8.5, 47, -14], to: [-5, 51, -10], parent: "head" },
        { name: "mane_cheek_right_1", from: [5, 45, -18], to: [7.5, 49, -14], parent: "head" },
        { name: "mane_cheek_right_2", from: [5, 47, -14], to: [8.5, 51, -10], parent: "head" },

        // Head Spines
        { name: "spine_head_1", from: [-0.8, 52, -18], to: [0.8, 55, -14], parent: "head" },
        { name: "spine_head_2", from: [-0.8, 52, -14], to: [0.8, 55.5, -10], parent: "head" }
    );

    // Roaring Lower Jaw
    await call("add_group", {
        name: "jaw",
        origin: [0, 45, -20],
        rotation: [-22, 0, 0],
        parent: "head"
    });

    cubes.push(
        { name: "jaw_base", from: [-3.8, 41.5, -31], to: [3.8, 45, -20], parent: "jaw" },
        { name: "jaw_chin", from: [-2.8, 39.5, -30], to: [2.8, 41.8, -22], parent: "jaw" },
        { name: "mouth_tongue", from: [-2.2, 44, -29], to: [2.2, 45.4, -21], parent: "jaw" },
        { name: "belly_throat", from: [-2.5, 41.8, -20], to: [2.5, 44.8, -12], parent: "jaw" },
        // Lower Fangs & Teeth
        { name: "fang_lower_left", from: [-3.3, 45, -29.5], to: [-2.1, 48, -27.5], parent: "jaw" },
        { name: "fang_lower_right", from: [2.1, 45, -29.5], to: [3.3, 48, -27.5], parent: "jaw" },
        { name: "teeth_lower_front", from: [-2.1, 45, -30.8], to: [2.1, 46.4, -29.5], parent: "jaw" }
    );

    // ==========================================
    // 2. GOLDEN ANTLER HORNS (Branching Stags)
    // ==========================================
    console.log("Adding horns...");
    // Left Horn
    await call("add_group", {
        name: "horn_left",
        origin: [-3, 51, -14],
        rotation: [22, -12, 18],
        parent: "head"
    });
    cubes.push(
        { name: "horn_stem_left", from: [-4.2, 51, -15.2], to: [-2.2, 59, -12.8], parent: "horn_left" },
        { name: "horn_tine_front_left", from: [-4, 55, -18.5], to: [-2.4, 59.5, -15.2], parent: "horn_left" },
        { name: "horn_tip_back_left", from: [-3.8, 58.5, -13.5], to: [-2.4, 64, -11], parent: "horn_left" },
        { name: "horn_tine_top_left", from: [-4.2, 58.5, -16], to: [-2.8, 65, -13.5], parent: "horn_left" }
    );

    // Right Horn
    await call("add_group", {
        name: "horn_right",
        origin: [3, 51, -14],
        rotation: [22, 12, -18],
        parent: "head"
    });
    cubes.push(
        { name: "horn_stem_right", from: [2.2, 51, -15.2], to: [4.2, 59, -12.8], parent: "horn_right" },
        { name: "horn_tine_front_right", from: [2.4, 55, -18.5], to: [4, 59.5, -15.2], parent: "horn_right" },
        { name: "horn_tip_back_right", from: [2.4, 58.5, -13.5], to: [3.8, 64, -11], parent: "horn_right" },
        { name: "horn_tine_top_right", from: [2.8, 58.5, -16], to: [4.2, 65, -13.5], parent: "horn_right" }
    );

    // ==========================================
    // 3. CURVED WHISKERS
    // ==========================================
    console.log("Adding whiskers...");
    // Whisker Left
    await call("add_group", {
        name: "whisker_left_base",
        origin: [-3.5, 47.5, -30],
        rotation: [0, -40, -15],
        parent: "head"
    });
    cubes.push(
        { name: "whisker_left_1", from: [-4.2, 46.8, -30.5], to: [-2.8, 48.2, -23], parent: "whisker_left_base" }
    );

    await call("add_group", {
        name: "whisker_left_mid",
        origin: [-3.5, 47.5, -23],
        rotation: [-12, -25, 20],
        parent: "whisker_left_base"
    });
    cubes.push(
        { name: "whisker_left_2", from: [-4.1, 46.9, -23], to: [-2.9, 48.1, -14], parent: "whisker_left_mid" }
    );

    await call("add_group", {
        name: "whisker_left_tip",
        origin: [-3.5, 47.5, -14],
        rotation: [15, -10, 25],
        parent: "whisker_left_mid"
    });
    cubes.push(
        { name: "whisker_left_3", from: [-4, 47, -14], to: [-3, 48, -3], parent: "whisker_left_tip" }
    );

    // Whisker Right
    await call("add_group", {
        name: "whisker_right_base",
        origin: [3.5, 47.5, -30],
        rotation: [0, 40, 15],
        parent: "head"
    });
    cubes.push(
        { name: "whisker_right_1", from: [2.8, 46.8, -30.5], to: [4.2, 48.2, -23], parent: "whisker_right_base" }
    );

    await call("add_group", {
        name: "whisker_right_mid",
        origin: [3.5, 47.5, -23],
        rotation: [-12, 25, -20],
        parent: "whisker_right_base"
    });
    cubes.push(
        { name: "whisker_right_2", from: [2.9, 46.9, -23], to: [4.1, 48.1, -14], parent: "whisker_right_mid" }
    );

    await call("add_group", {
        name: "whisker_right_tip",
        origin: [3.5, 47.5, -14],
        rotation: [15, 10, -25],
        parent: "whisker_right_mid"
    });
    cubes.push(
        { name: "whisker_right_3", from: [3, 47, -14], to: [4, 48, -3], parent: "whisker_right_tip" }
    );

    // ==========================================
    // 4. FRONT DRAGON CLAWS (Flaring wide framing the head!)
    // ==========================================
    console.log("Adding symmetrical dragon claws...");
    // Left Arm (model's left, -X)
    await call("add_group", {
        name: "arm_left",
        origin: [-14, 44, -10],
        rotation: [15, -45, -20]
    });
    cubes.push(
        { name: "arm_upper_left", from: [-16.5, 41.5, -12], to: [-13.5, 46.5, -5], parent: "arm_left" }
    );

    await call("add_group", {
        name: "forearm_left",
        origin: [-15, 44, -5],
        rotation: [-25, -15, 20],
        parent: "arm_left"
    });
    cubes.push(
        { name: "arm_fore_left", from: [-16.5, 41.5, -16], to: [-13.5, 46.5, -5], parent: "forearm_left" },
        { name: "paw_left", from: [-18, 41, -22], to: [-12.5, 46, -16], parent: "forearm_left" },
        // 4 Talons spreading outward
        { name: "claw_talon_l1", from: [-18.5, 42.5, -26.5], to: [-16.5, 45, -22], parent: "forearm_left" },
        { name: "claw_talon_l2", from: [-16, 42.8, -27], to: [-14, 45.2, -22], parent: "forearm_left" },
        { name: "claw_talon_l3", from: [-13.5, 42.5, -26.5], to: [-11.5, 45, -22], parent: "forearm_left" },
        { name: "claw_talon_l4_thumb", from: [-11, 41.5, -23.5], to: [-9.2, 44, -19.5], parent: "forearm_left" }
    );

    // Right Arm (model's right, +X)
    await call("add_group", {
        name: "arm_right",
        origin: [14, 44, -10],
        rotation: [15, 45, 20]
    });
    cubes.push(
        { name: "arm_upper_right", from: [13.5, 41.5, -12], to: [16.5, 46.5, -5], parent: "arm_right" }
    );

    await call("add_group", {
        name: "forearm_right",
        origin: [15, 44, -5],
        rotation: [-25, 15, -20],
        parent: "arm_right"
    });
    cubes.push(
        { name: "arm_fore_right", from: [13.5, 41.5, -16], to: [16.5, 46.5, -5], parent: "forearm_right" },
        { name: "paw_right", from: [12.5, 41, -22], to: [18, 46, -16], parent: "forearm_right" },
        // 4 Talons spreading outward
        { name: "claw_talon_r1", from: [16.5, 42.5, -26.5], to: [18.5, 45, -22], parent: "forearm_right" },
        { name: "claw_talon_r2", from: [14, 42.8, -27], to: [16, 45.2, -22], parent: "forearm_right" },
        { name: "claw_talon_r3", from: [11.5, 42.5, -26.5], to: [13.5, 45, -22], parent: "forearm_right" },
        { name: "claw_talon_r4_thumb", from: [9.2, 41.5, -23.5], to: [11, 44, -19.5], parent: "forearm_right" }
    );

    // ==========================================
    // 5. SERPENTINE COILED BODY
    // ==========================================
    console.log("Generating serpentine body...");
    await call("add_group", { name: "body", origin: [0, 46, -10] });

    // Smooth continuous spiral curve
    const bodyPoints = [
        [0, 46, -10],      // Neck base
        [0, 44, -1],       // Neck 2
        [-5, 41, 7],       // Body 1
        [-13, 37, 10],     // Body 2
        [-20, 33, 4],      // Body 3
        [-21, 28, -6],     // Body 4
        [-16, 24, -16],    // Body 5
        [-6, 20, -20],     // Body 6
        [6, 17, -17],      // Body 7
        [16, 14, -8],      // Body 8
        [20, 11, 2],       // Body 9
        [17, 8, 12],       // Body 10
        [8, 5, 18],        // Body 11
        [-2, 3, 17],       // Body 12
        [-11, 1, 11],      // Tail 1
        [-16, 0, 2]        // Tail tip
    ];

    for (let i = 0; i < bodyPoints.length - 1; i++) {
        const p1 = bodyPoints[i];
        const p2 = bodyPoints[i + 1];

        const dx = p2[0] - p1[0];
        const dy = p2[1] - p1[1];
        const dz = p2[2] - p1[2];
        const length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Yaw and pitch - correctly oriented towards next point!
        const yaw = (Math.atan2(-dx, -dz) * 180 / Math.PI);
        const horizDist = Math.sqrt(dx * dx + dz * dz);
        const pitch = (Math.atan2(dy, horizDist) * 180 / Math.PI);

        const segName = `body_seg_${i + 1}`;
        await call("add_group", {
            name: segName,
            origin: [p1[0], p1[1], p1[2]],
            rotation: [pitch, yaw, 0],
            parent: "body"
        });

        // Taper
        const progress = i / (bodyPoints.length - 2);
        const thickness = Math.max(2.2, 4.0 * (1.0 - progress * 0.45));
        const segLen = length + 0.6; // generous overlap to avoid any visual gaps

        // Torso cube
        cubes.push({
            name: `scale_body_${i + 1}`,
            from: [p1[0] - thickness, p1[1] - thickness, p1[2] - segLen],
            to: [p1[0] + thickness, p1[1] + thickness, p1[2]],
            parent: segName
        });

        // Ribbed Cream Belly Plate (on underside: -Y)
        cubes.push({
            name: `belly_plate_${i + 1}`,
            from: [p1[0] - thickness * 0.75, p1[1] - thickness - 0.4, p1[2] - segLen + 0.1],
            to: [p1[0] + thickness * 0.75, p1[1] - thickness * 0.6, p1[2] - 0.1],
            parent: segName
        });

        // Dark Green Dorsal Spine (+Y)
        if (i < bodyPoints.length - 2) {
            cubes.push({
                name: `spine_fin_${i + 1}`,
                from: [p1[0] - 0.6, p1[1] + thickness - 0.2, p1[2] - segLen + 0.5],
                to: [p1[0] + 0.6, p1[1] + thickness + 2.5, p1[2] - 0.5],
                parent: segName
            });
        }
    }

    // Tail tip fin
    const lastP = bodyPoints[bodyPoints.length - 1];
    cubes.push(
        { name: "spine_fin_tail_1", from: [lastP[0] - 0.5, lastP[1] - 1, lastP[2] - 4], to: [lastP[0] + 0.5, lastP[1] + 3.5, lastP[2]], parent: "body" },
        { name: "spine_fin_tail_2", from: [lastP[0] - 0.4, lastP[1] - 2, lastP[2] - 7], to: [lastP[0] + 0.4, lastP[1] + 2, lastP[2] - 3], parent: "body" }
    );

    // ==========================================
    // 6. CREATE ALL CUBES
    // ==========================================
    console.log(`Adding ${cubes.length} cubes in total...`);
    const addRes = await call("add_cubes", { cubes });
    if (!addRes.ok) {
        console.error("add_cubes error:", addRes.error);
        return;
    }
    console.log("All cubes created successfully!");

    // ==========================================
    // 7. TEXTURING PIPELINE
    // ==========================================
    console.log("Packing UVs...");
    await call("pack_uv", { padding: 1 });

    console.log("Creating single unified texture...");
    const texRes = await call("create_texture", {
        name: "shenlong",
        fill: "#249341"
    });
    console.log("create_texture result:", texRes);

    const texUuid = texRes.result.uuid;

    console.log("Applying smooth detail_cubes texturing...");
    const detailRes = await call("detail_cubes", {
        cubes: "all",
        texture: texUuid,
        base: "#249341", // Emerald green DBZ scales
        colors: [
            { match: "belly|throat", color: "#fae89d" },          // Cream yellow ribbed belly
            { match: "spine|fin|mane", color: "#0f5922" },        // Dark forest green dorsal fins & mane
            { match: "horn", color: "#c68f32" },                  // Golden amber antler horns
            { match: "eye", color: "#ff1818" },                   // Glowing red dragon eyes
            { match: "teeth|fang|talon|claw", color: "#f2f2f0" }, // Bone white teeth, fangs & talons
            { match: "mouth|tongue", color: "#be3852" },          // Deep crimson/pink mouth
            { match: "nostril", color: "#165926" },               // Dark green nostril holes
            { match: "paw", color: "#228b3b" },                   // Green paws
            { match: "arm", color: "#249341" }                    // Green arms
        ],
        noise: 0.05,
        blur: 0.5,
        top_light: 0.14,
        bottom_dark: 0.22
    });
    console.log("detail_cubes result:", detailRes.ok);

    // Select this texture in canvas
    await call("execute_script", {
        code: `
            const t = Texture.all.find(x => x.uuid === '${texUuid}');
            if (t) { t.select(); Canvas.updateAll(); }
            return { ok: true };
        `
    });

    // ==========================================
    // 8. SCREENSHOTS & EXPORT
    // ==========================================
    console.log("Taking screenshots...");
    const shotFront = await call("screenshot", { view: "front" });
    if (shotFront.result?.base64) {
        fs.writeFileSync("tools/shenlong_final_front.png", Buffer.from(shotFront.result.base64, "base64"));
    }
    const shotRight = await call("screenshot", { view: "front_right" });
    if (shotRight.result?.base64) {
        fs.writeFileSync("tools/shenlong_final_right.png", Buffer.from(shotRight.result.base64, "base64"));
    }
    const shotTop = await call("screenshot", { view: "top" });
    if (shotTop.result?.base64) {
        fs.writeFileSync("tools/shenlong_final_top.png", Buffer.from(shotTop.result.base64, "base64"));
    }

    // Ensure models directory exists
    fs.mkdirSync("src/main/resources/assets/dragonblock/models/entity", { recursive: true });

    // Save project as .bbmodel in mod repository
    const bbmodelPath = "C:/Users/aramriv/Documents/MODS MC/DB Mod/src/main/resources/assets/dragonblock/models/entity/shenlong.bbmodel";
    console.log("Saving project to " + bbmodelPath);
    const saveRes = await call("save_project", { path: bbmodelPath });
    console.log("save_project result:", saveRes);

    // Export Texture PNG to mod repository
    const texPath = "C:/Users/aramriv/Documents/MODS MC/DB Mod/src/main/resources/assets/dragonblock/textures/entity/shenlong.png";
    const texData = await call("get_texture", { texture: texUuid });
    if (texData.result?.data_url) {
        const base64 = texData.result.data_url.split(",")[1];
        fs.mkdirSync("src/main/resources/assets/dragonblock/textures/entity", { recursive: true });
        fs.writeFileSync("src/main/resources/assets/dragonblock/textures/entity/shenlong.png", Buffer.from(base64, "base64"));
        console.log("Saved texture to " + texPath);
    }

    // Compile Java Class
    const compileRes = await call("execute_script", {
        code: `return Codecs.modded_entity.compile();`
    });
    if (compileRes.ok && compileRes.result) {
        fs.writeFileSync("tools/raw_ShenlongModel.java", compileRes.result);
        console.log("Saved tools/raw_ShenlongModel.java, length:", compileRes.result.length);
    }

    console.log("Done generating Shenlong!");
}

main().catch(console.error);
