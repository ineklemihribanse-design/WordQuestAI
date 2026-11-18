#!/usr/bin/env python3
"""Bake a real facial morph-only viseme system into adult Rocketbox teacher GLBs.

ARKit targetNames are read from each *actual* visible skinned mesh. No VRM /
VRoid extension is used. All 13 imported original animation clips are kept
unaltered. These clips are a VISUAL approximation triggered by TTS word-range
events, not real phoneme-aligned lip sync.

Usage: bake_human_visemes_1271.py input-mobile.glb output-lips.glb
"""
from pathlib import Path
import json,struct,sys,hashlib

JSON=0x4E4F534A; BIN=0x004E4942
VOWELS={
 "A":{"jawOpen":.76,"mouthLowerDownLeft":.24,"mouthLowerDownRight":.24},
 "E":{"jawOpen":.28,"mouthStretchLeft":.55,"mouthStretchRight":.55},
 "I":{"jawOpen":.21,"mouthSmileLeft":.52,"mouthSmileRight":.52},
 "O":{"jawOpen":.40,"mouthFunnel":.84},
 "U":{"jawOpen":.19,"mouthPucker":.88},
 "MBP":{"mouthClose":.91,"mouthPressLeft":.43,"mouthPressRight":.43},
 "NEUTRAL":{},
}
def pad(b:bytes,char=b"\x00"):
    return b+char*((-len(b))%4)
def parse(b):
    assert b[:4]==b"glTF"
    v,size=struct.unpack_from("<II",b,4);assert v==2 and size==len(b)
    chunks={};i=12
    while i<len(b):
        length,kind=struct.unpack_from("<II",b,i);i+=8
        chunks[kind]=b[i:i+length];i+=length
    doc=json.loads(chunks[JSON].decode("utf-8"))
    return doc,bytearray(chunks[BIN])
def bake(src,dest):
    doc,data=parse(Path(src).read_bytes())
    assert "VRM" not in doc.get("extensions",{}),"Anime/VRoid asset blocked"
    assert "EXT_texture_webp" not in doc.get("extensionsRequired",[]),"Convert WebP before baking"
    existing=[x.get("name") for x in doc.get("animations",[])]
    assert len(existing)==13 and {"Idle","Talk","Wave","Think"}.issubset(existing),existing
    assert doc.get("skins") and doc.get("meshes")
    mesh_ids={n["mesh"] for n in doc["nodes"] if "mesh" in n}
    morphed=[]
    for node_id,node in enumerate(doc["nodes"]):
        mesh_id=node.get("mesh")
        if mesh_id not in mesh_ids:continue
        mesh=doc["meshes"][mesh_id]
        prims=mesh.get("primitives",[])
        if not prims or not prims[0].get("targets"):continue
        count=len(prims[0]["targets"])
        names=mesh.get("extras",{}).get("targetNames",[])
        if len(names)!=count:continue
        indices={x:i for i,x in enumerate(names)}
        if {"jawOpen","mouthClose","mouthPucker","mouthFunnel"}.issubset(indices):
            morphed.append((node_id,count,indices,node.get("name",str(node_id))))
    assert morphed,"ARKit face blendshape targets missing in visible meshes"
    # Retain the skinned body, hair and skin morphs. Some models have multiple
    # active facial nodes which must animate together.
    views=doc.setdefault("bufferViews",[])
    accessors=doc.setdefault("accessors",[])
    def floats(values):
        data.extend(b"\x00"*((-len(data))%4))
        start=len(data)
        data.extend(struct.pack("<"+"f"*len(values),*values))
        idx=len(views);views.append({"buffer":0,"byteOffset":start,"byteLength":4*len(values)})
        accessor=len(accessors)
        accessors.append({"bufferView":idx,"componentType":5126,
                          "count":len(values),"type":"SCALAR"})
        return accessor
    frame_times=floats([0.0,.48])
    clips=[]
    for viseme,weights in VOWELS.items():
        clip={"name":"Teacher_Viseme_"+viseme,"channels":[],"samplers":[]}
        for node_id,count,indices,name in morphed:
            array=[0.0]*count
            for key,val in weights.items():
                assert key in indices,(src,key)
                array[indices[key]]=val
            sampler=len(clip["samplers"])
            clip["samplers"].append({
                "input":frame_times,"output":floats(array+array),
                "interpolation":"STEP"})
            clip["channels"].append({
                "sampler":sampler,
                "target":{"node":node_id,"path":"weights"}})
        doc["animations"].append(clip)
        clips.append(viseme)
    # The upstream Talk/Idle clips animate bones but do NOT contain ARKit
    # blink morphs. Bake a dedicated 260 ms blink cycle to avoid staring.
    blink_times=floats([0.0,.09,.18,.26])
    blink={"name":"Teacher_Blink","channels":[],"samplers":[]}
    for node_id,count,indices,name in morphed:
        assert "eyeBlinkLeft" in indices and "eyeBlinkRight" in indices
        shut=[0.0]*count
        shut[indices["eyeBlinkLeft"]]=1.0
        shut[indices["eyeBlinkRight"]]=1.0
        zeros=[0.0]*count
        output=floats(zeros+shut+shut+zeros)
        sampler=len(blink["samplers"])
        blink["samplers"].append({
            "input":blink_times,"output":output,"interpolation":"LINEAR"})
        blink["channels"].append({"sampler":sampler,
                                   "target":{"node":node_id,"path":"weights"}})
    doc["animations"].append(blink)
    # Mouth and blink target the exact same glTF 52-weight vector.
    # Two independently applied face clips would overwrite each other's
    # values in Filament. Precompose one facial track per mouth shape instead.
    # SceneView switches between regular and blinking viseme variants.
    composite=[]
    for vowel,base in VOWELS.items():
        clip={"name":"Teacher_Viseme_"+vowel+"_Blink",
              "channels":[],"samplers":[]}
        for node_id,count,indices,name in morphed:
            base_weights=[0.0]*count
            for key,value in base.items():
                base_weights[indices[key]]=value
            shut=base_weights.copy()
            shut[indices["eyeBlinkLeft"]]=1.0
            shut[indices["eyeBlinkRight"]]=1.0
            output=floats(base_weights+shut+shut+base_weights)
            sm=len(clip["samplers"])
            clip["samplers"].append({"input":blink_times,
                "output":output,"interpolation":"LINEAR"})
            clip["channels"].append({"sampler":sm,
                "target":{"node":node_id,"path":"weights"}})
        doc["animations"].append(clip)
        composite.append(clip["name"])
    doc["buffers"][0]["byteLength"]=len(data)
    j=pad(json.dumps(doc,separators=(",",":"),ensure_ascii=False).encode(),b" ")
    data=pad(bytes(data))
    out=(struct.pack("<4sII",b"glTF",2,12+8+len(j)+8+len(data))
         +struct.pack("<II",len(j),JSON)+j
         +struct.pack("<II",len(data),BIN)+data)
    p=Path(dest);p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(out)
    verify,_=parse(out)
    assert len(verify["animations"])==len(existing)+15
    assert [x["name"] for x in verify["animations"][:len(existing)]]==existing
    assert all(set(c["target"])=={"node","path"} and c["target"]["path"]=="weights"
               for animation in verify["animations"][-15:] for c in animation["channels"])
    assert all(len(a["channels"])==len(morphed) for a in verify["animations"][-15:])
    report={"source":str(src),"file":str(dest),"sha256":hashlib.sha256(out).hexdigest(),
            "original_animations":existing,"viseme_clips":clips,
            "blink_clip":"Teacher_Blink",
            "composite_blink_clips":composite,
            "active_face_nodes":[{"id":i,"name":n,"targets":c} for i,c,_,n in morphed],
            "skin_count":len(verify["skins"]),"visual_approved":False,
            "accuracy":"TTS word-range-driven approximation; NOT phoneme-level"}
    report_file=p.with_suffix(".visemes.json")
    report_file.write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n")
    print(json.dumps(report,indent=2,ensure_ascii=False))
    return report

def main():
    if len(sys.argv)!=3:raise SystemExit("Usage: bake_human_visemes_1271.py in.glb out.glb")
    bake(sys.argv[1],sys.argv[2])
if __name__=="__main__":main()
