import json
import math

# Load dragon.json
with open('src/main/resources/assets/dragonblock/dragon.json') as f:
    dragon_data = json.load(f)

head_data = dragon_data['head']
seg_data = dragon_data['segment']
tail_data = dragon_data['tail']

print("Head parts:", list(head_data.keys()))
for k, v in head_data.items():
    print(f"  {k}: {len(v)} boxes")
