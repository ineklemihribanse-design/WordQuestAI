#!/usr/bin/env python3
"""Small offline glTF studio geometry for the existing live Lena SceneView.

Only generates new background geometry. Never changes the user's Lena GLB,
skin, animations or installed data. Zero network calls and no dependencies.
"""
from pathlib import Path
import json, math, struct, sys

PALETTE = {
    "wall": ((0.070, 0.17, 0.145, 1), (0, 0, 0)),
    "panel": ((0.105, 0.245, 0.19, 1), (0, 0, 0)),
    "paneltrim": ((0.24, 0.36, 0.25, 1), (0, 0, 0)),
    "oak": ((0.54, 0.40, 0.26, 1), (0, 0, 0)),
    "oaklight": ((0.68, 0.53, 0.36, 1), (0, 0, 0)),
    "warm": ((1, .75, .39, 1), (.8, .45, .16)),
    "shelfgold": ((.72, .58, .34, 1), (.12, .10, .02)),
    "bookcream": ((.84, .83, .69, 1), (0, 0, 0)),
    "bookmint": ((.39, .56, .46, 1), (0, 0, 0)),
    "bookdark": ((.13, .30, .23, 1), (0, 0, 0)),
    "bookrose": ((.52, .40, .35, 1), (0, 0, 0)),
    "ceramic": ((.87, .84, .69, 1), (0, 0, 0)),
    "leaf": ((.15, .43, .25, 1), (0, 0, 0)),
    "leaflight": ((.29, .57, .31, 1), (0, 0, 0)),
    "stem": ((.15, .27, .17, 1), (0, 0, 0)),
    "window": ((.59, .79, .67, 1), (.24, .33, .23)),
    "windowtrim": ((.65, .63, .46, 1), (0, 0, 0)),
    "glass": ((.70, .70, .56, 1), (0, 0, 0)),
}
MESH = {name: [[], [], []] for name in PALETTE}

def face(name, corners, normal):
    p, n, idx = MESH[name]
    start = len(p)//3
    for x,y,z in corners:
        p.extend((x,y,z)); n.extend(normal)
    idx.extend((start, start+1, start+2, start, start+2, start+3))

def box(name, x1,y1,z1, x2,y2,z2):
    assert x1<x2 and y1<y2 and z1<z2
    face(name, [(x1,y1,z2),(x2,y1,z2),(x2,y2,z2),(x1,y2,z2)], (0,0,1))
    face(name, [(x2,y1,z1),(x1,y1,z1),(x1,y2,z1),(x2,y2,z1)], (0,0,-1))
    face(name, [(x2,y1,z2),(x2,y1,z1),(x2,y2,z1),(x2,y2,z2)], (1,0,0))
    face(name, [(x1,y1,z1),(x1,y1,z2),(x1,y2,z2),(x1,y2,z1)], (-1,0,0))
    face(name, [(x1,y2,z2),(x2,y2,z2),(x2,y2,z1),(x1,y2,z1)], (0,1,0))
    face(name, [(x1,y1,z1),(x2,y1,z1),(x2,y1,z2),(x1,y1,z2)], (0,-1,0))

def sphere(name, x,y,z, radius, lat=10, lon=16):
    p,n,idx=MESH[name]
    start=len(p)//3
    for iy in range(lat+1):
        phi=math.pi*iy/lat
        for ix in range(lon+1):
            angle=2*math.pi*ix/lon
            nx=math.sin(phi)*math.cos(angle)
            ny=math.cos(phi)
            nz=math.sin(phi)*math.sin(angle)
            p.extend((x+radius*nx,y+radius*ny,z+radius*nz))
            n.extend((nx,ny,nz))
    for iy in range(lat):
        for ix in range(lon):
            a=start+iy*(lon+1)+ix
            b=a+lon+1
            idx.extend((a,b,a+1, a+1,b,b+1))

def plant(x,y,z,side=1):
    box("ceramic",x-.13,y-.22,z-.1,x+.13,y,z+.1)
    box("stem",x-.009,y,z-.009,x+.009,y+.45,z+.009)
    for j in range(9):
        a=j*2.39996
        dx=math.cos(a)*(.13+.03*(j%3))
        dz=math.sin(a)*.13
        py=y+.12+.042*j
        sphere("leaf" if j%2 else "leaflight",x+dx,py,z+dz,.095,6,8)

def create_scene():
    # Wall, architectural arch and soft ambient light accents.
    box("wall",-3.6,-.35,-2.55,3.6,3.55,-2.43)
    box("panel",-3.18,.73,-2.41,-1.30,2.83,-2.34)
    box("paneltrim",-3.23,.72,-2.32,-3.16,2.71,-2.29)
    box("paneltrim",-1.36,.72,-2.32,-1.29,2.71,-2.29)
    box("paneltrim",-3.23,2.75,-2.32,-1.29,2.82,-2.29)
    box("warm",-3.20,2.75,-2.28,-1.34,2.77,-2.24)
    # Left wall shelves with carefully spaced multi-colour books.
    for y in (.48,1.13,1.82):
        box("oak",-3.45,y,-2.35,-1.85,y+.055,-1.72)
        box("warm",-3.43,y-.018,-1.91,-1.88,y-.011,-1.77)
        for i in range(10):
            x=-3.37+i*.118
            h=.28+.06*(i%3)
            box(("bookcream","bookmint","bookrose","bookdark")[i%4],
                x,y+.056,-2.19,x+.080,y+.056+h,-2.05)
    # Soft arched window at right with mullions and depth.
    box("window",1.78,.67,-2.42,3.14,3.03,-2.39)
    box("windowtrim",1.75,.67,-2.34,1.83,3.03,-2.29)
    box("windowtrim",3.09,.67,-2.34,3.17,3.03,-2.29)
    box("windowtrim",1.75,2.96,-2.34,3.17,3.04,-2.29)
    box("windowtrim",2.44,.67,-2.32,2.49,3.04,-2.28)
    box("windowtrim",1.76,1.7,-2.32,3.16,1.75,-2.28)
    # Desk, shelves and objects at far right/left, never obstruct Lena.
    box("oak",1.44,.72,-1.55,3.55,.80,-.87)
    box("oaklight",1.55,.79,-1.55,3.55,.82,-.87)
    box("ceramic",2.57,.82,-1.33,2.88,1.10,-1.09)
    sphere("bookmint",2.75,1.16,-1.26,.12)
    for y in (1.43,2.20):
        box("oak",1.40,y,-2.29,2.2,y+.06,-1.87)
        box("warm",1.45,y-.015,-1.95,2.15,y-.006,-1.87)
        for i in range(6):
            bx=1.50+i*.10
            box(("bookmint","bookcream","bookdark")[i%3],
                bx,y+.062,-2.16,bx+.074,y+.062+.19+.025*(i%2),-2.04)
    sphere("warm",-2.50,.94,-1.78,.18,12,20)
    box("shelfgold",-2.52,.48,-1.81,-2.48,.75,-1.77)
    plant(-1.73,2.25,-2.11)
    plant(1.72,2.95,-2.19)
    plant(3.21,.89,-1.28)
    # Grounding near lower camera boundary; the portrait never needs feet.
    box("oak",-3.5,-.22,-2.36,3.5,-.12,-.5)

def write_glb(path):
    create_scene()
    binary=bytearray()
    views=[]; accessors=[]; primitives=[]
    def append(data,component,count,shape,target):
        while len(binary)%4: binary.append(0)
        offset=len(binary); binary.extend(data)
        v=len(views);views.append({"buffer":0,"byteOffset":offset,"byteLength":len(data),"target":target})
        a=len(accessors); accessors.append({"bufferView":v,"componentType":component,"count":count,"type":shape})
        return a
    for material,(name,(rgba,emission)) in enumerate(PALETTE.items()):
        p,n,ind=MESH[name]
        if not ind: continue
        positions=append(struct.pack("<"+"f"*len(p),*p),5126,len(p)//3,"VEC3",34962)
        normals=append(struct.pack("<"+"f"*len(n),*n),5126,len(n)//3,"VEC3",34962)
        indices=append(struct.pack("<"+"I"*len(ind),*ind),5125,len(ind),"SCALAR",34963)
        primitives.append({"attributes":{"POSITION":positions,"NORMAL":normals},
            "indices":indices,"material":material,"mode":4})
    materials=[]
    for name,(rgba,emission) in PALETTE.items():
        materials.append({"name":name,"pbrMetallicRoughness":{
            "baseColorFactor":rgba,"metallicFactor":0.0,"roughnessFactor":.86},
            "emissiveFactor":emission,"doubleSided":True})
    doc={"asset":{"version":"2.0","generator":"WordQuest native stdlib studio"},
         "scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"name":"WordQuestStudio","mesh":0}],
         "meshes":[{"primitives":primitives}],"materials":materials,
         "accessors":accessors,"bufferViews":views,
         "buffers":[{"byteLength":len(binary)}]}
    head=json.dumps(doc,separators=(",",":")).encode("utf8")
    head+=b" "*((-len(head))%4)
    binary+=b"\0"*((-len(binary))%4)
    result=(struct.pack("<4sII",b"glTF",2,12+8+len(head)+8+len(binary))+
        struct.pack("<I4s",len(head),b"JSON")+head+
        struct.pack("<I4s",len(binary),b"BIN\0")+binary)
    dest=Path(path);dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(result)
    assert len(result)>1000 and result[:4]==b"glTF"
    assert len(primitives)>=12
    print("WordQuest studio GLB generated:",len(result),"bytes,",len(primitives),"materials")

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: build_lena_studio.py output.glb")
    write_glb(sys.argv[1])
