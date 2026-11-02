import json
import math

def build_curve(num_segments=26, turns=2.2, height=90.0, r_base=24.0, r_var=8.0):
    points = []
    for i in range(num_segments + 1):
        s = i / num_segments
        # s = 0 at the tail tip, 1 at the head
        r = r_base + r_var * math.sin(math.pi * s)
        # angle: we can offset theta so head points toward -Z
        th = 2 * math.pi * turns * s + math.pi * 0.5
        x = r * math.cos(th)
        z = r * math.sin(th)
        y = 2.0 + height * s
        points.append((x, y, z))
    return points

pts = build_curve()
print(f"Generated {len(pts)} points:")
print(f"  Tail tip (s=0): {pts[0]}")
print(f"  Mid (s=0.5): {pts[len(pts)//2]}")
print(f"  Head (s=1): {pts[-1]}")
