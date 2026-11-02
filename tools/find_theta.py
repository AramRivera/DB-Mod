import math

def get_tangent(s, turns=2.4, H=110.0, th0=0.0):
    ds = 0.001
    def pt(val):
        r = 26.0 + 8.0 * math.sin(math.pi * val)
        th = 2 * math.pi * turns * val + th0
        return (r * math.cos(th), 2.0 + H * val, r * math.sin(th))
    p1 = pt(s)
    p2 = pt(s + ds)
    dx = p2[0] - p1[0]
    dy = p2[1] - p1[1]
    dz = p2[2] - p1[2]
    dist = math.sqrt(dx*dx + dy*dy + dz*dz)
    return p1, (dx/dist, dy/dist, dz/dist)

# Find th0 such that at s=1.0, tangent points toward -Z (dx ≈ 0, dz < 0)
best_th0 = 0.0
best_score = 999
for i in range(360):
    th = math.radians(i)
    p, t = get_tangent(1.0, th0=th)
    # We want t_x ≈ 0 and t_z ≈ -1
    score = abs(t[0]) + (1.0 + t[2]) # min when t[0]=0, t[2]=-1
    if score < best_score:
        best_score = score
        best_th0 = th

print(f"Best th0: {math.degrees(best_th0):.1f} deg")
p, t = get_tangent(1.0, th0=best_th0)
print(f"Head pos: {p}")
print(f"Head tangent: {t}")
