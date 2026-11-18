#!/usr/bin/env python3
"""Add *facial-only* glTF morph animations for TTS range-aligned lip cues.

The animated adult teacher models are the licensed 1.0.27 candidates.
For every teacher, the VRM mouth-blendshape metadata is converted into
SceneView/Filament-compatible clips named Teacher_Viseme_A/E/I/O/U/MBP/NEUTRAL.
Only face morphs are keyed; skeleton, animations, hair, hands remain intact.

IMPORTANT: Android onRangeStart is WORD/RANGE timing, not phoneme timing.
This enables a *rough, event-aligned mouth cue*, not proven phoneme lip-sync.
"""
from pathlib import Path
import json,struct
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/"source/app/src/main/assets/avatars"
JSON=0x4E4F534A
BIN=0x004E4942

def pad(data,fill=b"\x00"):
    return data+fill*((-len(data))%4)

def add_file(file:Path):
    blob=file.read_bytes()
    assert blob[:4]==b"glTF"
    version,length=struct.unpack_from("<II",blob,4)
    assert version==2 and length==len(blob)
    at=12;chunks={}
    while at<len(blob):
        count,kind=struct.unpack_from("<II",blob,at);at+=8
        chunks[kind]=blob[at:at+count];at+=count
    doc=json.loads(chunks[JSON].decode("utf-8"))
    binary=bytearray(chunks[BIN])
    assert len(doc.get("skins",[]))>0
    old_names=[a.get("name") for a in doc.get("animations",[])]
    assert len(old_names)>=5 and "Teacher_Talk" in old_names
    groups=doc.get("extensions",{}).get("VRM",{}).get("blendShapeMaster",{}).get("blendShapeGroups",[])
    group={str(g.get("presetName","")).lower():g for g in groups}
    for name in ("a","e","i","o","u"):
        assert name in group,f"{file.name}: missing VRM mouth preset {name}"
    target_mesh=group["a"]["binds"][0]["mesh"]
    mesh=doc["meshes"][target_mesh]
    targets=len(mesh["primitives"][0].get("targets",[]))
    assert 0<targets<128,(file.name,targets)
    face_nodes=[i for i,n in enumerate(doc["nodes"]) if n.get("mesh")==target_mesh]
    assert face_nodes
    node=face_nodes[0]
    positions={}
    for key in ("a","e","i","o","u"):
        refs=group[key].get("binds",[])
        assert refs and refs[0]["mesh"]==target_mesh
        idx=refs[0]["index"]
        assert isinstance(idx,int) and idx<targets
        positions[key]=idx

    buffers=doc.setdefault("bufferViews",[])
    acc=doc.setdefault("accessors",[])
    def floats(data):
        binary.extend(b"\0"*((-len(binary))%4))
        offset=len(binary)
        binary.extend(struct.pack("<"+"f"*len(data),*data))
        v=len(buffers)
        buffers.append({"buffer":0,"byteOffset":offset,"byteLength":len(data)*4})
        a=len(acc)
        acc.append({"bufferView":v,"componentType":5126,"count":len(data),
                    "type":"SCALAR"})
        return a

    time_index=floats([0.0,0.4])
    clips=[]
    for viseme in ("a","e","i","o","u","mbp","neutral"):
        w=[0.0]*targets
        if viseme in positions:
            w[positions[viseme]]=.66
        # Single facial-only channel, no effect on body bones.
        output=floats(w+w)
        clip={"name":"Teacher_Viseme_"+viseme.upper(),
              "channels":[{"sampler":0,"target":{"node":node,"path":"weights"}}],
              "samplers":[{"input":time_index,"output":output,"interpolation":"STEP"}]}
        doc["animations"].append(clip)
        clips.append(clip["name"])
    doc["buffers"][0]["byteLength"]=len(binary)
    compact=pad(json.dumps(doc,separators=(",",":"),ensure_ascii=False).encode("utf-8"),b" ")
    binary=pad(bytes(binary))
    out=(struct.pack("<4sII",b"glTF",2,12+8+len(compact)+8+len(binary))+
         struct.pack("<II",len(compact),JSON)+compact+
         struct.pack("<II",len(binary),BIN)+binary)
    assert len(out)==struct.unpack_from("<I",out,8)[0]
    reopened=json.loads(out[20:20+len(compact)].decode("utf-8"))
    assert [a["name"] for a in reopened["animations"][-7:]]==clips
    assert [a.get("name") for a in reopened["animations"][:len(old_names)]]==old_names
    file.write_bytes(out)
    return {"actor":file.stem,"faceNode":node,"targetCount":targets,
            "mouthIndices":positions,"clips":clips,"rigs":len(doc["skins"])}

def main():
    records=[]
    for actor in ("emma","sophia","ethan"):
        records.append(add_file(ASSETS/(actor+".glb")))
    (ASSETS/"AVATAR_LIPSYNC_METADATA.json").write_text(
        json.dumps({"quality":"TEXT_RANGE_APPROXIMATION","actors":records},
                   indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps(records,indent=2))
    print("7 viseme clips per actor baked; phoneme-level alignment NOT claimed")
if __name__=="__main__":main()
