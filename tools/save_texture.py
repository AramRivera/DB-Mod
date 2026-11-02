import base64
import json
import shutil
import os

with open(r'C:\Users\aramriv\.gemini\antigravity\brain\08a39671-921d-4d0d-8fa7-3a2a6c1f280a\.system_generated\steps\575\output.txt', 'r', encoding='utf-8') as f:
    data = f.read().strip()

if data.startswith('"') and data.endswith('"'):
    data = json.loads(data)

if 'base64,' in data:
    data = data.split('base64,')[1]

img_bytes = base64.b64decode(data)

with open('whis.png', 'wb') as f:
    f.write(img_bytes)

print(f'Wrote whis.png ({len(img_bytes)} bytes)')

dest_tex_dir = r'src\main\resources\assets\dragonblock\textures\entity'
dest_geo_dir = r'src\main\resources\assets\dragonblock\geo'

if os.path.exists(dest_tex_dir):
    shutil.copy('whis.png', os.path.join(dest_tex_dir, 'whis.png'))
    print('Copied whis.png to mod resources')

if os.path.exists(dest_geo_dir):
    shutil.copy('whis.geo.json', os.path.join(dest_geo_dir, 'whis.geo.json'))
    print('Copied whis.geo.json to mod resources')
