#!/usr/bin/env python3
"""Compare only model source portraits. No app modification, no APK publishing."""
from pathlib import Path
from io import BytesIO
from urllib.request import urlopen, Request
from PIL import Image, ImageDraw, ImageFont

PIN="2fd9ec82fbe1613d40ae38aadcca3f410725f740"
NAMES=["emma","mia","olivia","hannah","aisha","priya","mei","sophia"]
OUT=Path("candidate-emma-rocketbox")
OUT.mkdir(exist_ok=True)
tw,th=260,310
sheet=Image.new("RGB",(tw*4,th*2),(14,30,35))
draw=ImageDraw.Draw(sheet)
for index,name in enumerate(NAMES):
    url=f"https://raw.githubusercontent.com/memoriesrohit/avatar-studio/{PIN}/models/thumbs/{name}.jpg"
    with urlopen(Request(url,headers={"User-Agent":"WordQuest-AI-Emma-Reference-Review"}),timeout=30) as response:
        src=Image.open(BytesIO(response.read())).convert("RGB")
    src.thumbnail((240,265))
    x=(index%4)*tw+(tw-src.width)//2
    y=(index//4)*th+6
    sheet.paste(src,(x,y))
    draw.text(((index%4)*tw+16,(index//4)*th+276),name.upper(),fill=(238,211,165))
path=OUT/"female-hair-face-compare.jpg"
sheet.save(path,quality=92)
print("Eight MIT-licensed female preview variants in",path)
