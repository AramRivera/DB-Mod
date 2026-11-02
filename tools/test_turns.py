import math

# We want points[0][0] = 0 (tail tip at X=0)
# and points[-1][0] = 0 (head at X=0)
# Let's test turns:
# If turns = 2.0 or 2.5:
# At s=0: th = th0
# At s=1: th = 2*pi*turns + th0
# If turns is an integer, like 2.0: th(1) - th(0) = 4*pi, so th(1) = th(0)!
# That means if x(0) = 0, then x(1) = 0 automatically!
# If th0 = pi/2: cos(pi/2) = 0, so x(0) = 0 and x(1) = 0!
# At s=0: z = R_0 * sin(pi/2) = +R_0 (pointing back +Z).
# At s=1: z = R_1 * sin(pi/2) = +R_1.
# What if turns = 2.5?
# At s=0: th = pi/2 -> x(0) = 0, z(0) = +R_0 (back).
# At s=1: th = 5*pi + pi/2 = 5.5*pi -> cos(5.5*pi) = 0, sin(5.5*pi) = -1!
# So x(1) = 0, and z(1) = -R_1 (pointing front -Z)!
# Look at that: with turns = 2.5 and th0 = pi/2:
# Tail tip is at x = 0, z = +R_0 (back, on ground)!
# Head is at x = 0, z = -R_1 (front, facing north -Z)!
# Perfectly centered on X=0!

turns = 2.5
th0 = math.pi / 2.0

for s in [0.0, 0.25, 0.5, 0.75, 1.0]:
    r = 26.0 + 6.0 * math.sin(math.pi * s)
    th = 2 * math.pi * turns * s + th0
    x = round(r * math.cos(th), 2)
    y = round(2.0 + 85.0 * s, 2)
    z = round(r * math.sin(th), 2)
    print(f"s={s:.2f}: x={x}, y={y}, z={z}")
