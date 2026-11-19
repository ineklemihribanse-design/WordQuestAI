#!/usr/bin/env python3
"""Give Lena a real speaking mouth and blinking eyelids in its skinned glTF mesh.

Original Lena v6 has 201 bones and 16 animation clips, but facial bones are
animated WITHOUT any vertex weights. It also has zero morph targets. No
amount of calling those animation clips can make its lips move.

Add TWO native glTF shape targets (mouth open, eye blink) to the existing
single skinned mesh and TWO loop animations of their morph weights. Body
animations remain intact. This works without Blender or API changes,
and without adding a second mesh/texture/skeleton or modifying source GLB.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

from paint_lena_162 import read_chunks, encode_glb, accessor_values

def append_bin_view(doc,blob,data,target=None):
    blob+=b"\x00"*((-len(blob))%4)
    offset=len(blob)
    d={"buffer":0,"byteOffset":offset,"byteLength":len(data)}
    if target is not None: d["target"]=target
    vi=len(doc["bufferViews"])
    doc["bufferViews"].append(d)
    return blob+data,vi

def append_accessor(doc,blob,raw,count,typ,component=5126,normalized=False,target=None):
    blob,view=append_bin_view(doc,blob,raw,target)
    i=len(doc["accessors"])
    a={"bufferView":view,"componentType":component,"count":count,"type":typ}
    if normalized:a["normalized"]=True
    doc["accessors"].append(a)
    return blob,i

def mouth_delta(x,y,z):
    # The earlier y=1.56..1.64 mask included nose + philtrum. Actual
    # lower mouth surface is y≈1.55..1.58; nose protrudes above y≈1.60.
    # Use narrow, depth-weighted masks for the visible lip center ONLY.
    if z<0.080 or abs(x)>=0.048 or not 1.545<y<1.582:
        return (0.,0.,0.)
    sx=max(0.,1.-(x/0.048)**2)
    sy=max(0.,1.-((y-1.562)/0.020)**2)
    sz=min(1.,max(0.,(z-0.080)/0.032))
    amount=sx*sy*sz
    if amount<0.10:return (0.,0.,0.)
    # Separate lower-lip downstroke and tiny upper lip raise.
    dy=(-0.010 if y<=1.565 else 0.004)*amount
    return (0.,dy,0.0010*amount)

def blink_delta(x,y,z):
    # Upper/lower eyelids are near y≈1.67, centered around x±0.064.
    # Previous mask extended as low as y=1.627 and animated cheeks.
    if z<0.072 or not 0.038<abs(x)<0.095 or not 1.659<y<1.687:
        return (0.,0.,0.)
    sx=max(0.,1.-((abs(x)-0.064)/0.028)**2)
    sy=max(0.,1.-((y-1.673)/0.014)**2)
    sz=min(1.,max(0.,(z-0.072)/0.022))
    amount=sx*sy*sz
    if amount<0.12:return (0.,0.,0.)
    return (0.,(-0.006 if y>1.673 else 0.004)*amount,0.)


def add_animation(doc,blob,node_index,name,frames):
    # glTF animation morph target weights are stored as one SCALAR for each
    # target at each keyframe, not as VEC2. Two targets = 2 * len(frames).
    times=[float(t) for t,_,_ in frames]
    weights=[float(w) for _,mouth,blink in frames for w in (mouth,blink)]
    assert len(times)>=4 and len(weights)==2*len(times)
    assert all(a<b for a,b in zip(times,times[1:]))
    assert all(0<=w<=1 for w in weights)
    blob,time_index=append_accessor(
        doc,blob,struct.pack("<"+"f"*len(times),*times),len(times),"SCALAR"
    )
    blob,weight_index=append_accessor(
        doc,blob,struct.pack("<"+"f"*len(weights),*weights),len(weights),"SCALAR"
    )
    doc["animations"].append({
        "name":name,
        "samplers":[{"input":time_index,"output":weight_index,"interpolation":"LINEAR"}],
        "channels":[{"sampler":0,"target":{"node":node_index,"path":"weights"}}],
    })
    return blob

def morph(source):
    doc,blob=read_chunks(source)
    meshes=doc.get("meshes",[])
    assert len(meshes)==1 and len(meshes[0]["primitives"])==1
    mesh=meshes[0]
    prim=mesh["primitives"][0]
    if prim.get("targets"):
        raise ValueError("Source already has morph targets; do not overwrite")
    if len(doc.get("animations",[]))!=16:
        raise ValueError("Unexpected source clips; review geometry")
    positions=accessor_values(doc,blob,prim["attributes"]["POSITION"])
    mouth=[mouth_delta(*p) for p in positions]
    blink=[blink_delta(*p) for p in positions]
    mouth_count=sum(any(abs(c)>1e-7 for c in d) for d in mouth)
    blink_count=sum(any(abs(c)>1e-7 for c in d) for d in blink)
    print("Lena facial morph influenced vertices:",{"mouth":mouth_count,"blink":blink_count})
    if mouth_count<30 or blink_count<30 or mouth_count>1100 or blink_count>1200:
        raise ValueError("Face region not present in source geometry; stop for manual review")
    prim["targets"]=[]
    for deltas in (mouth,blink):
        flat=[f for row in deltas for f in row]
        raw=struct.pack("<"+"f"*len(flat),*flat)
        blob,accessor=append_accessor(doc,blob,raw,len(positions),"VEC3",target=34962)
        prim["targets"].append({"POSITION":accessor})
    mesh["weights"]=[0.0,0.0]
    mesh.setdefault("extras",{})["targetNames"]=["Lena_MouthOpen","Lena_EyeBlink"]
    node_index=next(i for i,n in enumerate(doc["nodes"]) if n.get("mesh")==0)
    # Blink is used in IDLE/LISTENING/THINKING and TALK modes. No "always
    # mouth moving" behavior: SPEAKING begins only on actual audio onStart.
    rest=[
        (0.00,0.,0.),(1.38,0.,0.),(1.48,0.,1.),
        (1.59,0.,0.),(3.92,0.,0.),(4.04,0.,1.),
        (4.15,0.,0.),(4.85,0.,0.),
    ]
    talk=[
        (0.00,0.,0.),(.12,.85,0.),(.25,.25,0.),(.40,.95,0.),
        (.53,.16,0.),(.70,.75,0.),(.84,.33,0.),
        (1.01,.95,0.),(1.18,.22,0.),(1.35,.80,0.),
        (1.52,.06,0.),(1.70,.92,0.),(1.90,.20,0.),
        (2.08,.80,0.),(2.25,.12,0.),(2.38,.75,0.),
        (2.48,.30,0.),(2.56,.12,1.),(2.68,.95,0.),
        (2.86,.33,0.),(3.04,.85,0.),(3.22,.15,0.),
        (3.40,.65,0.),(3.58,.29,0.),(3.76,.96,0.),
        (3.98,.08,0.),(4.16,.72,0.),(4.40,.23,0.),
        (4.61,.86,0.),(4.85,0.,0.),
    ]
    blob=add_animation(doc,blob,node_index,"Lena_V162_Face_IdleBlink",rest)
    blob=add_animation(doc,blob,node_index,"Lena_V162_Face_TalkBlink",talk)
    doc["buffers"][0]["byteLength"]=len(blob)
    output=encode_glb(doc,blob)
    valid,_=read_chunks(output)
    assert len(valid["animations"])==18
    assert len(valid["skins"][0]["joints"])==len(doc["skins"][0]["joints"])==201
    assert len(valid["meshes"][0]["primitives"][0]["targets"])==2
    assert valid["meshes"][0]["primitives"][0]["attributes"]["COLOR_0"]==prim["attributes"]["COLOR_0"]
    return output,mouth_count,blink_count

def main():
    p=argparse.ArgumentParser()
    p.add_argument("source")
    p.add_argument("output")
    p.add_argument("--manifest")
    args=p.parse_args()
    source=Path(args.source).read_bytes()
    result,nmouth,nblink=morph(source)
    out=Path(args.output)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_bytes(result)
    print("Lena 3D facial morphs saved:",len(source),"->",len(result),"bytes, 18 animations")
    if args.manifest:
        f=Path(args.manifest)
        d=json.loads(f.read_text("utf-8"))
        d["sha256"]=hashlib.sha256(result).hexdigest()
        d["facialMorphTargets"]=["Lena_MouthOpen","Lena_EyeBlink"]
        d["facialAnimations"]=["Lena_V161_Face_IdleBlink","Lena_V161_Face_TalkBlink"]
        d["note"]="Mouth/blink masks geometrically localized to real facial regions; TTS approximate."
        f.write_text(json.dumps(d,indent=2,ensure_ascii=False)+"\n","utf-8")

if __name__=="__main__":
    main()
