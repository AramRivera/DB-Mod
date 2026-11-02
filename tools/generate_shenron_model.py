import json
import math

# Load dragon.json
with open('src/main/resources/assets/dragonblock/dragon.json') as f:
    dragon_data = json.load(f)

head_raw = dragon_data['head']
seg_raw = dragon_data['segment']
tail_raw = dragon_data['tail']
leg_raw = dragon_data['leg']

NUM_SEGS = 26
TURNS = 2.3
HEIGHT = 90.0
R_BASE = 28.0
R_VAR = 7.0
TH0 = math.radians(32.0)

def pt(s):
    r = R_BASE + R_VAR * math.sin(math.pi * s)
    th = 2 * math.pi * TURNS * s + TH0
    return (round(r * math.cos(th), 2), round(2.0 + HEIGHT * s, 2), round(r * math.sin(th), 2))

points = [pt(i / NUM_SEGS) for i in range(NUM_SEGS + 1)]

# Let's inspect head joint position
head_pt = points[-1]
torso_pt = points[-2]
body_pt = points[24]
tail_pt = points[18]
tail2_pt = points[9]
tail3_pt = points[0]

print("Joint positions:")
print("  Head:", head_pt)
print("  Torso:", torso_pt)
print("  Body:", body_pt)
print("  Tail:", tail_pt)
print("  Tail2:", tail2_pt)
print("  Tail3:", tail3_pt)
