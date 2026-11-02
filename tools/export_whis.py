import base64
import json
import os
import shutil

step_file = r'C:/Users/aramriv/.gemini/antigravity/brain/08a39671-921d-4d0d-8fa7-3a2a6c1f280a/.system_generated/steps/446/output.txt'
with open(step_file, 'r', encoding='utf-8') as f:
    content = f.read().strip()

if content.startswith('"') and content.endswith('"'):
    content = json.loads(content)

raw_bytes = base64.b64decode(content)

out_png_root = r'C:/Users/aramriv/Documents/MODS MC/DB Mod/whis.png'
out_png_assets = r'C:/Users/aramriv/Documents/MODS MC/DB Mod/src/main/resources/assets/dragonblock/textures/entity/whis.png'
out_geo_root = r'C:/Users/aramriv/Documents/MODS MC/DB Mod/whis.geo.json'
out_geo_assets = r'C:/Users/aramriv/Documents/MODS MC/DB Mod/src/main/resources/assets/dragonblock/geo/whis.geo.json'

os.makedirs(os.path.dirname(out_png_assets), exist_ok=True)
os.makedirs(os.path.dirname(out_geo_assets), exist_ok=True)

with open(out_png_root, 'wb') as f:
    f.write(raw_bytes)

with open(out_png_assets, 'wb') as f:
    f.write(raw_bytes)

shutil.copyfile(out_geo_root, out_geo_assets)

print("Saved Whis files successfully:")
print(" -", out_png_root, f"({len(raw_bytes)} bytes)")
print(" -", out_png_assets)
print(" -", out_geo_root)
print(" -", out_geo_assets)
