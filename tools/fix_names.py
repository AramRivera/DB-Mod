import re

with open('tools/build_oozaru.py', 'r', encoding='utf-8') as f:
    text = f.read()

text = re.sub(r"_r(['\"])", r"_right\1", text)
text = re.sub(r"_l(['\"])", r"_left\1", text)

with open('tools/build_oozaru.py', 'w', encoding='utf-8') as f:
    f.write(text)

print("Updated names successfully!")
