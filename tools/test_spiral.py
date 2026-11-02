import json
import math

with open('src/main/resources/assets/dragonblock/dragon.json') as f:
    dragon_data = json.load(f)

head_raw = dragon_data['head']
seg_raw = dragon_data['segment']
tail_raw = dragon_data['tail']
leg_raw = dragon_data['leg']

# Parametric helical path
NUM_SEGS = 26
TURNS = 2.3
HEIGHT = 90.0
R_BASE = 26.0
R_VAR = 7.0
TH0 = math.radians(32.0)

def pt(s):
    r = R_BASE + R_VAR * math.sin(math.pi * s)
    th = 2 * math.pi * TURNS * s + TH0
    return (r * math.cos(th), 2.0 + HEIGHT * s, r * math.sin(th))

points = [pt(i / (NUM_SEGS)) for i in range(NUM_SEGS + 1)]
print(f"Computed {len(points)} points along the spiral coil.")
print(f"Tail tip: {points[0]}")
print(f"Head joint: {points[-1]}")
