#!/usr/bin/env python3
"""Basic automated camera QA on *actual Blender renders*, not source coordinates.

Do not equate these checks with artistic acceptance. Detect the specific v127
regression in which Emma's scalp, forehead or mouth was cut by the top edge.
Produces a JSON report; exits nonzero if a preview plainly cuts through skin.
"""
from pathlib import Path
from PIL import Image
import json,sys

paths=[Path(p) for p in sys.argv[1:]]
if not paths:
    raise SystemExit("Usage: assert_emma_frame.py file.png [file2.png ...]")
results=[]
for p in paths:
    im=Image.open(p).convert("RGB")
    width,height=im.size
    # Skin has warmer R/G and R/B values than the dark studio backdrop.
    # This checks the central 60% where the face must be for default camera.
    xs=range(int(width*.26),int(width*.74),3)
    ys=range(0,int(height*.05),3)
    warm=0; total=0
    for y in ys:
        for x in xs:
            r,g,b=im.getpixel((x,y))
            if r>92 and r>g*1.11 and r>b*1.18 and r-g>12:
                warm+=1
            total+=1
    # Only inspect the extreme edge, not the full hairline (dark brown
    # hair can be warm-toned and enter the upper 9% without being clipped).
    is_clipped=warm/max(total,1)>.026
    result={"file":str(p),"width":width,"height":height,
            "topFivePercentWarmSkinPixels":warm,
            "topSkinFraction":round(warm/max(1,total),4),
            "cameraSkinClipped":is_clipped}
    results.append(result)
    print(json.dumps(result))
Path("candidate-emma-brown/EMMA_CAMERA_QA.json").write_text(
    json.dumps(results,indent=2)+"\n",encoding="utf-8")
if any(x["cameraSkinClipped"] for x in results):
    raise SystemExit("FAIL: Emma head/face appears clipped near top of frame")
print("Camera edge checks passed; visual face and clothing approval still required")
