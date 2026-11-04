"""Combines preview_hair.py renders into contact sheets (4 styles per sheet, 4 views per style)."""
import os, sys
from PIL import Image
d = sys.argv[1]
styles = sorted({f.rsplit('_', 1)[0] for f in os.listdir(d) if f.endswith('.png') and not f.startswith('sheet')})
for half, group in enumerate([styles[:4], styles[4:]]):
    sheet = Image.new('RGB', (4 * 240, len(group) * 240), 'white')
    for r, st in enumerate(group):
        for c, v in enumerate(['front', 'side', 'back', '3q']):
            sheet.paste(Image.open(f'{d}/{st}_{v}.png').resize((240, 240)), (c * 240, r * 240))
    sheet.save(f'{d}/sheet{half}.png')
print(styles)
