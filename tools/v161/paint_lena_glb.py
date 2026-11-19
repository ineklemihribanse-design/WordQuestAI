#!/usr/bin/env python3
"""Non-destructive Lena GLB recoloring by skinning-bone and spatial vertex groups.

The source v6 GLB has ONE skinned mesh, ONE primitive and NO materials. A
regular material-per-mesh recolor would paint the entire human one color.
Instead add normalized RGBA8 COLOR_0 to that existing primitive, preserving
every original skin, joint, 16 animation clips and all original geometry.
No textures, model remeshing, Blender or runtime network fetch required.
"""
from collections import Counter
import argparse
import copy
import hashlib
import json
from pathlib import Path
import struct

PALETTE = {
    "skin": (238, 184, 151, 255),       # Warm, natural skin
    "face": (245, 194, 162, 255),
    "lips": (177, 81, 102, 255),
    "brows": (48, 55, 87, 255),
    "eyes": (66, 137, 201, 255),
    "hair": (39, 74, 134, 255),        # Sapphire-blue hair
    "shirt": (57, 133, 204, 255),      # Blue teaching jacket
    "shirt_light": (88, 174, 225, 255),
    "pants": (42, 57, 91, 255),        # Navy trousers
    "shoes": (218, 231, 236, 255),
}
COMPONENTS = {5120: ("b", 1), 5121: ("B", 1), 5122: ("h", 2),
              5123: ("H", 2), 5125: ("I", 4), 5126: ("f", 4)}
COMPS_IN_TYPE = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}

def read_chunks(data):
    if len(data)<28 or data[:4]!=b"glTF":
        raise ValueError("Not a GLB")
    magic,version,length=struct.unpack_from("<4sII",data)
    if version!=2 or length!=len(data):
        raise ValueError("GLB header mismatch")
    chunks=[]
    at=12
    while at<len(data):
        n,typ=struct.unpack_from("<I4s",data,at)
        payload=data[at+8:at+8+n]
        if len(payload)!=n: raise ValueError("Truncated GLB chunk")
        chunks.append((typ,payload))
        at+=8+n
    if len(chunks)!=2 or chunks[0][0]!=b"JSON" or chunks[1][0]!=b"BIN\x00":
        raise ValueError("Expected JSON and binary GLB chunks")
    return json.loads(chunks[0][1]),chunks[1][1]

def encode_glb(doc,bin_bytes):
    json_bytes=json.dumps(doc,separators=(",",":"),ensure_ascii=False).encode("utf-8")
    json_bytes+=b" "*((-len(json_bytes))%4)
    bin_bytes+=b"\x00"*((-len(bin_bytes))%4)
    total=12+8+len(json_bytes)+8+len(bin_bytes)
    return (struct.pack("<4sII",b"glTF",2,total)+
        struct.pack("<I4s",len(json_bytes),b"JSON")+json_bytes+
        struct.pack("<I4s",len(bin_bytes),b"BIN\x00")+bin_bytes)

def accessor_values(doc,blob,index):
    a=doc["accessors"][index]
    if a.get("sparse"): raise ValueError("Sparse accessors unsupported for Lena")
    view=doc["bufferViews"][a["bufferView"]]
    assert view.get("buffer",0)==0
    typ=a["componentType"]
    fmt,sz=COMPONENTS[typ]
    n=COMPS_IN_TYPE[a["type"]]
    offset=view.get("byteOffset",0)+a.get("byteOffset",0)
    stride=view.get("byteStride",sz*n)
    assert stride>=sz*n and offset+(a["count"]-1)*stride+sz*n<=len(blob)
    unpack=struct.Struct("<"+fmt*n).unpack_from
    return [unpack(blob,offset+i*stride) for i in range(a["count"])]

def dominant_bone(joints,weights,joint_names):
    idx=max(range(4),key=lambda i: weights[i])
    return joint_names[joints[idx]]

def vertex_group(xyz,bone):
    x,y,z=xyz
    b=bone.lower()
    # Named facial joints keep eyes, brows and lips distinguishable from skin.
    if "upper_lip" in b or "lower_lip" in b or b.startswith("jaw") or b=="tongue":
        return "lips"
    if "brow" in b: return "brows"
    if "lid" in b:
        # Inner iris-colored dots; neighboring eyelid vertices stay skin.
        if 1.575<y<1.68 and 0.033<abs(x)<0.11 and abs(z)>0.04:
            return "eyes"
        return "face"
    if "cheek" in b: return "face"
    if "finger" in b or "thumb" in b or "index" in b or "middle" in b or "pinky" in b or "ring" in b or "hand" in b:
        return "skin"
    if "forearm" in b: return "skin"
    if "upper_arm" in b: return "shirt" if y>1.36 else "skin"
    if "clavicle" in b: return "shirt"
    if "foot" in b or "toe" in b:
        return "shoes"
    if "shin" in b or "thigh" in b:
        return "shoes" if y<0.105 else "pants"
    if "neck" in b:
        return "skin"
    if b=="head":
        if y>1.718 or (y>1.545 and abs(x)>0.125):
            return "hair"
        return "face"
    if b in ("chest","spine_03","spine_02","spine_01","pelvis","root"):
        if y>1.36: return "shirt_light"
        if y>0.92: return "shirt"
        return "pants"
    # Some auxiliary vertices follow the head bone through facial controllers.
    if y>1.72: return "hair"
    if y>1.49: return "face"
    if y>0.92: return "shirt"
    if y>0.13: return "pants"
    return "shoes"

def paint(source):
    doc,bin_bytes=read_chunks(source)
    meshes=doc.get("meshes",[])
    if len(meshes)!=1 or len(meshes[0].get("primitives",[]))!=1:
        raise ValueError("Lena v6 geometry changed; review before painting")
    primitive=meshes[0]["primitives"][0]
    a=primitive["attributes"]
    if "COLOR_0" in a: raise ValueError("Already colored: refuse double append")
    if not all(k in a for k in ("POSITION","JOINTS_0","WEIGHTS_0")):
        raise ValueError("Missing skinned vertex data")
    positions=accessor_values(doc,bin_bytes,a["POSITION"])
    joints=accessor_values(doc,bin_bytes,a["JOINTS_0"])
    weights=accessor_values(doc,bin_bytes,a["WEIGHTS_0"])
    skin=doc["skins"][doc["nodes"][231].get("skin",0)]
    bone_names=[doc["nodes"][i].get("name","") for i in skin["joints"]]
    if not (len(positions)==len(joints)==len(weights)):
        raise ValueError("Vertex accessor counts mismatch")
    assert len(positions)>10000 and len(bone_names)>190
    colors=bytearray()
    counts=Counter()
    for p,j,w in zip(positions,joints,weights):
        bone=dominant_bone(j,w,bone_names)
        group=vertex_group(p,bone)
        colors.extend(PALETTE[group])
        counts[group]+=1
    # Never ship a monochrome GLB under a colored release label.
    if any(counts.get(name,0)<60 for name in ("skin","face","hair","shirt","pants","shoes")):
        raise ValueError(f"Missing meaningful color groups: {dict(counts)}")
    if len(bin_bytes)%4:
        bin_bytes+=b"\x00"*((-len(bin_bytes))%4)
    offset=len(bin_bytes)
    buffer_view=len(doc["bufferViews"])
    doc["bufferViews"].append({
        "buffer":0,"byteOffset":offset,"byteLength":len(colors),
        "target":34962,
    })
    accessor=len(doc["accessors"])
    doc["accessors"].append({
        "bufferView":buffer_view,"componentType":5121,
        "count":len(positions),"type":"VEC4","normalized":True,
        "name":"Lena_Colored_Vertex_RGBA8",
    })
    primitive["attributes"]["COLOR_0"]=accessor
    material=len(doc.setdefault("materials",[]))
    doc["materials"].append({
        "name":"Lena_V161_PBR_VertexColors",
        "pbrMetallicRoughness":{
            "baseColorFactor":[1.0,1.0,1.0,1.0],
            "metallicFactor":0.0,"roughnessFactor":0.88,
        },
        "doubleSided":True,
    })
    primitive["material"]=material
    doc["buffers"][0]["byteLength"]=len(bin_bytes)+len(colors)
    output=encode_glb(doc,bin_bytes+colors)
    after,after_bin=read_chunks(output)
    assert len(after["animations"])==len(doc["animations"])==16
    assert len(after["skins"][0]["joints"])==len(doc["skins"][0]["joints"])
    assert after["meshes"][0]["primitives"][0]["attributes"]["COLOR_0"]==accessor
    assert after["materials"][material]["name"]=="Lena_V161_PBR_VertexColors"
    assert len(after_bin)>=len(bin_bytes)+len(colors)
    return output,counts

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("output")
    parser.add_argument("--manifest")
    args=parser.parse_args()
    source=Path(args.source)
    out=Path(args.output)
    raw=source.read_bytes()
    painted,counts=paint(raw)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_bytes(painted)
    print("Lena vertex paint groups:",dict(counts))
    print("Original GLB:",len(raw),"Colored GLB:",len(painted),
          "Added bytes:",len(painted)-len(raw))
    if args.manifest:
        f=Path(args.manifest)
        d=json.loads(f.read_text("utf-8"))
        d["baseAssetSha256"]=hashlib.sha256(raw).hexdigest()
        d["sha256"]=hashlib.sha256(painted).hexdigest()
        d["appearance"]="v1.0.61 blue-haired teacher with warm skin, blue uniform and navy trousers"
        d["materials"]="RGBA8 vertex colors, one glTF PBR material; no texture bloat"
        f.write_text(json.dumps(d,indent=2,ensure_ascii=False)+"\n","utf-8")

if __name__=="__main__":
    main()
