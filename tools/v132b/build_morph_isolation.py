#!/usr/bin/env python3
"""Create REAL-human women GLB morph-isolation variants for Android native diagnosis.

Models use the same exact licensed skinned human base meshes, body skeleton,
materials, face textures, outfits and original 13 body animation clips.
Only *GPU morph targets and dependent face animation channels* are excluded.

This is NOT the final avatar (no mouth expression in diagnostic mode). Its
purpose is to determine whether older SceneView/Filament morph allocation is
responsible for a native crash, without switching to 2D, anime or a mannequin.

The output source is the 1.0.31 -mobile-lite.glb; originals stay untouched.
"""
from pathlib import Path
import json, struct

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "source/app/src/main/assets/avatars"
JSON_TAG, BIN_TAG = 0x4E4F534A, 0x004E4942

def read(path: Path):
    b = path.read_bytes()
    assert b[:4] == b"glTF"
    version,total=struct.unpack_from("<II",b,4)
    assert version==2 and total==len(b)
    length,kind=struct.unpack_from("<II",b,12)
    assert kind==JSON_TAG
    obj=json.loads(b[20:20+length])
    offset=20+length
    count,kind=struct.unpack_from("<II",b,offset)
    assert kind==BIN_TAG and offset+8+count<=len(b)
    return obj,b[offset+8:offset+8+count]

def write(path: Path, doc: dict, binary: bytes):
    js=json.dumps(doc,separators=(",",":"),ensure_ascii=False).encode()
    js+=b" " * ((-len(js))%4)
    binary+=b"\0"*((-len(binary))%4)
    path.write_bytes(struct.pack("<4sII",b"glTF",2,12+8+len(js)+8+len(binary)) +
                     struct.pack("<II",len(js),JSON_TAG)+js +
                     struct.pack("<II",len(binary),BIN_TAG)+binary)

def build(actor: str):
    orig=ASSETS/(actor+"-mobile-lite.glb")
    target=ASSETS/(actor+"-no-morph-diagnostic.glb")
    doc,binary=read(orig)
    bones=json.dumps(doc.get("skins",[]),sort_keys=True)
    nodes_before=[{k:v for k,v in n.items() if k!="weights"} for n in doc["nodes"]]
    mesh_before = len(doc["meshes"])
    material_before = json.dumps(doc["materials"],sort_keys=True)
    image_before = json.dumps(doc["images"],sort_keys=True)
    animations=doc.get("animations",[])
    assert len(animations)==28
    body_animations=[]
    for anim in animations:
        if anim.get("name","").startswith("Teacher_"):
            continue
        # Retain only channels affecting skeleton pose/transforms. Old Idle
        # and Talk body clips still move shoulders/arms naturally.
        channels=[ch for ch in anim.get("channels",[]) if
                  ch.get("target",{}).get("path") in ("rotation","translation","scale")]
        # Keep original sampler indexes: extra samplers are valid and untouched.
        if channels:
            anim["channels"]=channels
            body_animations.append(anim)
    assert len(body_animations)>=4
    assert any(a.get("name")=="Idle" for a in body_animations)
    doc["animations"]=body_animations
    all_morphs=sum(len(p.get("targets",[])) for mesh in doc["meshes"]
                   for p in mesh.get("primitives",[]))
    assert all_morphs>0
    for m in doc["meshes"]:
        m.pop("weights",None)
        if "extras" in m:
            m["extras"].pop("targetNames",None)
        for primitive in m.get("primitives",[]):
            primitive.pop("targets",None)
    for n in doc["nodes"]:
        n.pop("weights",None)
    # glTF BIN buffers are preserved for a strict apples-to-apples experiment
    # (only morph allocation/execution differs). A later optimization can
    # compact unreachable morph buffers when a device test shows it helps.
    write(target,doc,binary)
    verify,_=read(target)
    assert json.dumps(verify["skins"],sort_keys=True)==bones
    assert len(verify["meshes"])==mesh_before
    assert json.dumps(verify["materials"],sort_keys=True)==material_before
    assert json.dumps(verify["images"],sort_keys=True)==image_before
    assert [{k:v for k,v in n.items() if k!="weights"} for n in verify["nodes"]]==nodes_before
    assert all(not p.get("targets") for mesh in verify["meshes"] for p in mesh["primitives"])
    assert all(ch["target"]["path"]!="weights" for a in verify["animations"] for ch in a["channels"])
    assert any(a["name"]=="Idle" for a in verify["animations"])
    print(f"{actor}: real licensed human body+face base geometry and skin preserved; "
          f"{len(body_animations)} skeletal clips; disabled {all_morphs} referenced GPU morph targets; "
          f"GLB MiB {len(target.read_bytes())/1048576:.2f}")
    return target

if __name__=="__main__":
    for actor in ("emma","sophia"):
        build(actor)
