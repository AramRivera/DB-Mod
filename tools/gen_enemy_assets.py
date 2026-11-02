"""Builds the enemy skins (textures/entity/enemy/<kind>.png, plus emperor_gold for his second form) and their 3D
extras (assets/dragonblock/enemy_extras.json), the same way gen_npc_assets.py builds the characters.
Original designs on genre archetypes. Run after gen_race_skins.py:
  python tools/gen_enemy_assets.py src/main/resources
"""
import json
import os
import sys

sys.argv = [sys.argv[0], sys.argv[1]]
HERE = os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(HERE, "gen_npc_assets.py"), encoding="utf-8").read().split("# Face is (8,8)")[0])  # reuse compose/paint helpers

OUT = os.path.join(RES, "assets/dragonblock/textures/entity/enemy")
os.makedirs(OUT, exist_ok=True)
skins = {}

# Bandit: sun-tanned, dark red vest and trousers, a cloth over the mouth
img = compose("human", 0xB98058, 0x6A2420, 0x2A2420, hair=0x2A1A10)
paint(img, [(8, 14, 15, 15)], (180, 60, 50))
skins["bandit"] = img

# Imperial soldier: lilac-skinned alien in a navy bodysuit with white chest armor trimmed in purple
img = compose("human", 0xA890D0, 0x1E2440, 0x202020, hair=0x101010)
paint(img, [(20, 20, 27, 27), (32, 20, 39, 27)], (236, 236, 240))
paint(img, [(20, 27, 27, 27), (32, 27, 39, 27)], (120, 60, 170))
paint(img, [(16, 20, 19, 27), (28, 20, 31, 27)], (220, 220, 228))
skins["soldier"] = img

# Combat android: pale synthetic skin, black jumpsuit, a seam down the face
img = compose("human", 0xE8E0D8, 0x161618, 0x202020, hair=0x303030)
paint(img, [(11, 13, 12, 15)], (190, 184, 178))
skins["android"] = img

# Emperor: slate armor-skin with crimson gems; golden with violet gems once he transforms
skins["emperor"] = compose("arcosian", 0x3A3F5A, 0xC0203A, 0xFF3040)
skins["emperor_gold"] = compose("arcosian", 0xE8C040, 0x7A30C0, 0xFF3040)

for name, img in skins.items():
    img.save(os.path.join(OUT, name + ".png"))

extras = {
    "bandit": {
        "head": {"wrap": [[-4.4, -8.8, -4.4, 8.8, 3.6, 8.8], [-4.6, -6.6, -4.6, 9.2, 1.2, 9.2], [2.4, -6.4, 2, 2, 5, 2]]},
        "rightArm": {"blade": [[-1.4, 9, -2, 0.8, 1.6, 2], [-1.3, 9.4, -12, 0.6, 1, 10], [-1.6, 8.6, -2.6, 1.4, 2.6, 0.8]]},
        "colors": {"wrap": 0xE8DCC0, "blade": 0xC8CCD4},
    },
    "soldier": {
        "rightArm": {"pad": [[-4.4, -2.6, -2.6, 5.4, 2.4, 5.2]]},
        "leftArm": {"pad": [[-1, -2.6, -2.6, 5.4, 2.4, 5.2]]},
        "head": {"frame": [[0.4, -4.9, -4.95, 4.0, 0.4, 0.5], [4.2, -4.4, -4.9, 0.6, 0.9, 3.9], [4.0, -5.2, -1.4, 1.0, 3.4, 2.8]],
                 "lens": [[0.5, -4.6, -4.8, 3.8, 3.0, 0.2]]},
        "colors": {"pad": 0xECECF0, "frame": 0x2E3138, "lens": 0x40FF70},
    },
    "android": {
        "head": {"visor": [[-4.3, -5.2, -4.5, 8.6, 1.6, 0.6]], "plate": [[-4.4, -8.6, -4.4, 8.8, 1.4, 8.8]],
                 "antenna": [[4.2, -7, -0.5, 0.8, 3, 1], [-5, -7, -0.5, 0.8, 3, 1]]},
        "colors": {"visor": 0xFF2020, "plate": 0x8A8E96, "antenna": 0x5A5E66},
    },
    "emperor": {
        "head": {"horn": [[-5.6, -7, -1, 1.6, 1.6, 2], [-6.8, -8.2, -1, 1.6, 1.6, 2], [-7.4, -10, -0.8, 1.4, 2, 1.6], [-7.2, -11.6, -0.6, 1.2, 1.8, 1.2],
                          [4, -7, -1, 1.6, 1.6, 2], [5.2, -8.2, -1, 1.6, 1.6, 2], [6, -10, -0.8, 1.4, 2, 1.6], [6, -11.6, -0.6, 1.2, 1.8, 1.2]],
                 "gem": [[-1, -8.4, -1, 2, 0.6, 2]]},
        "body": {"tail": [[-1.6, 10, 2, 3.2, 3.2, 5], [-1.3, 11, 6.5, 2.6, 2.6, 5], [-1, 12.5, 11, 2, 2, 5], [-0.7, 14.5, 15.5, 1.4, 1.4, 4]],
                 "chest": [[-2, 2, -2.4, 4, 3, 0.6]]},
        "rightArm": {"gem": [[-2.4, -2.6, -1, 2, 0.6, 2]]},
        "leftArm": {"gem": [[0.4, -2.6, -1, 2, 0.6, 2]]},
        "colors": {"horn": 0x2A2430, "gem": 0xC0203A, "tail": 0x3A3F5A, "chest": 0xC0203A},
        "gold_colors": {"horn": 0xF2E2A0, "gem": 0x7A30C0, "tail": 0xE8C040, "chest": 0x7A30C0},
    },
}
json.dump(extras, open(os.path.join(RES, "assets/dragonblock/enemy_extras.json"), "w"), indent=1)
print("wrote", len(skins), "enemy skins")
