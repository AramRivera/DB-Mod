import json
import math
import os

with open('src/main/resources/assets/dragonblock/dragon.json') as f:
    dragon_data = json.load(f)

head_raw = dragon_data['head']
seg_raw = dragon_data['segment']
tail_raw = dragon_data['tail']
leg_raw = dragon_data['leg']

print("Successfully loaded dragon.json")
