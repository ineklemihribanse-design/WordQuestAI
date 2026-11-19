#!/usr/bin/env python3
"""Read-only measurements for Lena v6 face landmarks.
Shows XY occupancy and front depth; never guesses lips from animation labels.
"""
from pathlib import Path
import sys,math,statistics
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"v161"))
from paint_lena_glb import read_chunks,accessor_values

doc,b=read_chunks(Path(sys.argv[1]).read_bytes())
a=doc["meshes"][0]["primitives"][0]["attributes"]
pts=accessor_values(doc,b,a["POSITION"])
print("GEOMETRY",len(pts),"axes x,y,z; head front is +z")
print("BANDS: y range, vertices in face x±.13, front depth max, p90, front+z threshold vertex counts")
for j in range(145,177):
    yl=j/100.;yh=yl+.01
    vals=[p for p in pts if yl<=p[1]<yh and abs(p[0])<.13]
    if not vals: continue
    zs=sorted(p[2] for p in vals)
    print(f"{yl:.2f}-{yh:.2f}: n={len(vals):4d} zmax={max(zs):.3f} z90={zs[int(.9*(len(zs)-1))]:.3f} front>{.04}={sum(p[2]>.04 for p in vals):4d} xnear={sum(abs(p[0])<.055 and p[2]>.04 for p in vals):4d}")
print("Projected front-depth map; top=skull y1.76, bottom=chin/neck y1.46")
print("ASCII symbols encode maximum z in each XY bin. Blank = no vertex.")
chars=" .:-=+*#%@"
for j in range(75):
    ylo=1.755-j*.004
    line=[]
    for i in range(70):
        xlo=-.175+i*.005
        pp=[p[2] for p in pts if ylo-.004<=p[1]<ylo and xlo<=p[0]<xlo+.005]
        if not pp: line.append(" ");continue
        top=max(pp)
        k=max(0,min(9,round((top+.12)/.31*9)))
        line.append(chars[k])
    print(f"{ylo:.3f} "+ "".join(line))
