#!/usr/bin/env python3
"""Render Lena's real, currently shipped GLB head for her chat profile.

This deliberately does NOT generate another person's face and does NOT use a
stock/avatar image. All triangles/colors come from the signed WordQuest Lena
3D GLB itself. A tiny deterministic orthographic software renderer writes a
transparent-free pastel profile PNG with the exact current head geometry,
hair, irises and costume. No Blender/texture download/API dependency needed.
"""
from array import array
from pathlib import Path
import binascii
import hashlib
import json
import math
import struct
import sys
import zlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/"tools/v165"))
from build_lena_lip_open_165 import getacc,read_glb

SIZE=448
XMIN,XMAX=-.192,.192
YTOP,YBOTTOM=1.825,1.445

def png_chunk(kind,data):
    return (struct.pack(">I",len(data))+kind+data+
            struct.pack(">I",binascii.crc32(kind+data)&0xffffffff))

def save_png(path,w,h,rgba):
    rows=b"".join(b"\x00"+bytes(rgba[j*w*4:(j+1)*w*4]) for j in range(h))
    out=(b"\x89PNG\r\n\x1a\n"+
        png_chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,6,0,0,0))+
        png_chunk(b"IDAT",zlib.compress(rows,level=9))+
        png_chunk(b"IEND",b""))
    Path(path).parent.mkdir(parents=True,exist_ok=True)
    Path(path).write_bytes(out)
    return out

def srgb(linear):
    x=max(0.0,min(1.0,linear))
    y=12.92*x if x<=.0031308 else 1.055*(x**(1./2.4))-.055
    return round(255*y)

def generate(glb_path,png_path):
    source=Path(glb_path).read_bytes()
    doc,blob=read_glb(source)
    mesh=doc["meshes"][0]
    parts=mesh["primitives"]
    expected=["Body","Tshirt","Sneakers","Pants","Hair","Eyes","Eyelash","Eyebrow","Belt"]
    names=[p.get("extras",{}).get("originalPrisma3DObject") for p in parts]
    if names!=expected:raise ValueError(f"Expected actual Prisma3D Lena parts, got {names}")
    if len(doc["skins"][0]["joints"])!=201:
        raise ValueError("Unexpected character rig")
    attrs=parts[0]["attributes"]
    vertices=getacc(doc,blob,attrs["POSITION"])
    rgba=getacc(doc,blob,attrs["COLOR_0"])
    normals=getacc(doc,blob,attrs["NORMAL"])
    if len(vertices)!=31958 or len(rgba)!=31958:
        raise ValueError("Unexpected Lena geometry or vertex paint")

    w=h=SIZE
    img=bytearray(w*h*4)
    depth=array("f",[-10000.0])*(w*h)
    # Calm cream/mint background, suitable for round-clipped chat portrait.
    for y in range(h):
        for x in range(w):
            nx=(x-(w-1)*.5)/((w-1)*.5)
            ny=(y-(h-1)*.5)/((h-1)*.5)
            radius=min(1.0,math.hypot(nx,ny))
            f=(1.0-radius)*.18
            at=4*(y*w+x)
            img[at:at+4]=bytes((round(239+12*f),round(247+5*f),round(239+9*f),255))

    coords=[((p[0]-XMIN)/(XMAX-XMIN)*(w-1),
             (YTOP-p[1])/(YTOP-YBOTTOM)*(h-1),p[2]) for p in vertices]
    colors=[(srgb(c[0]/255),srgb(c[1]/255),srgb(c[2]/255)) for c in rgba]
    # Deterministic Lambertian studio shading; not a synthetic replacement.
    vertex_brightness=[]
    for nx,ny,nz in normals:
        diffuse=max(0.0,nx*(-.33)+ny*.44+nz*.83)
        vertex_brightness.append(.72+.34*diffuse)

    drawn=0
    for prim in parts:
        tris=[a[0] for a in getacc(doc,blob,prim["indices"])]
        if len(tris)%3:raise ValueError("Malformed Lena triangles")
        for k in range(0,len(tris),3):
            ia,ib,ic=tris[k:k+3]
            x0,y0,z0=coords[ia]
            x1,y1,z1=coords[ib]
            x2,y2,z2=coords[ic]
            if max(x0,x1,x2)<0 or min(x0,x1,x2)>=w or max(y0,y1,y2)<0 or min(y0,y1,y2)>=h:
                continue
            area=(x1-x0)*(y2-y0)-(y1-y0)*(x2-x0)
            if abs(area)<.02:continue
            lowx=max(0,math.floor(min(x0,x1,x2)))
            highx=min(w-1,math.ceil(max(x0,x1,x2)))
            lowy=max(0,math.floor(min(y0,y1,y2)))
            highy=min(h-1,math.ceil(max(y0,y1,y2)))
            # Skip wholly hidden rear regions during the headshot crop.
            if highx<lowx or highy<lowy:continue
            shade=sum(vertex_brightness[i] for i in (ia,ib,ic))/3
            cr=round(min(255,sum(colors[i][0] for i in (ia,ib,ic))/3*shade))
            cg=round(min(255,sum(colors[i][1] for i in (ia,ib,ic))/3*shade))
            cb=round(min(255,sum(colors[i][2] for i in (ia,ib,ic))/3*shade))
            face_color=bytes((cr,cg,cb,255))
            # Exact GLB mesh projection with depth occlusion.
            for yy in range(lowy,highy+1):
                py=yy+.5
                for xx in range(lowx,highx+1):
                    px=xx+.5
                    a=((x1-px)*(y2-py)-(y1-py)*(x2-px))/area
                    if a<0 or a>1:continue
                    b=((x2-px)*(y0-py)-(y2-py)*(x0-px))/area
                    if b<0 or b>1 or a+b>1:continue
                    c=1.0-a-b
                    z=a*z0+b*z1+c*z2
                    pos=yy*w+xx
                    if z<=depth[pos]:continue
                    depth[pos]=z
                    img[4*pos:4*pos+4]=face_color
                    drawn+=1
    if drawn<45000:
        raise ValueError(f"Real Lena face render contains too little geometry: {drawn}")
    encoded=save_png(png_path,w,h,img)
    unique=len(set(bytes(img[i:i+3]) for i in range(0,len(img),4*89)))
    if len(encoded)<14000 or unique<60:
        raise ValueError("Avatar image unexpectedly blank/flat")
    print("LENA_REAL_PORTRAIT source_glb_sha256",hashlib.sha256(source).hexdigest())
    print("LENA_REAL_PORTRAIT face_pixels",drawn,
          "unique_sampled_colors",unique,
          "png_bytes",len(encoded),
          "dimensions",f"{w}x{h}",
          "source_parts",",".join(names))
    return encoded

if __name__=="__main__":
    if len(sys.argv)!=3:sys.exit("usage: render_lena_real_portrait.py staged_lena.glb drawable.png")
    generate(sys.argv[1],sys.argv[2])
