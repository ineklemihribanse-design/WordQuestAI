#!/usr/bin/env python3
"""Fail-closed quality check: all staged app avatars MUST be real Rocketbox humans."""
from pathlib import Path
import json,struct,hashlib
ROOT=Path(__file__).resolve().parents[2]
A=ROOT/"source/app/src/main/assets/avatars"
data=json.loads((A/"AVATAR_PROVENANCE.json").read_text())
assert data["status"]=="UNAPPROVED_VISUAL_QA"
assert data["family"]=="Microsoft Rocketbox / Avatar Studio"
assert data["no_anime_or_vroid_assets"] is True
assert data["user_review_required"] is True
assert len(data["models"])==3
assert {m["actor"] for m in data["models"]}=={"emma","sophia","ethan"}
mouth=json.loads((A/"AVATAR_LIPSYNC_METADATA.json").read_text())
assert mouth["quality"]=="TEXT_RANGE_APPROXIMATION"
assert {m["actor"] for m in mouth["actors"]}=={"emma","sophia","ethan"}
notice=(A/"HUMAN_3D_LICENSE_NOTICES.md").read_text()
assert "Microsoft Rocketbox" in notice and "Avatar Studio" in notice
for item in data["models"]:
    actor=item["actor"]
    asset=A/(actor+".glb")
    b=asset.read_bytes()
    assert len(b)>3_000_000 and b[:4]==b"glTF"
    version,length=struct.unpack_from("<II",b,4)
    assert version==2 and length==len(b)
    assert item["sha256"]==hashlib.sha256(b).hexdigest()
    n,tag=struct.unpack_from("<II",b,12)
    assert tag==0x4E4F534A
    gltf=json.loads(b[20:20+n])
    assert "VRM" not in gltf.get("extensions",{})
    assert "VRM" not in gltf.get("extensionsUsed",[])
    assert "EXT_texture_webp" not in gltf.get("extensionsRequired",[])
    assert all(i.get("mimeType") in ("image/png","image/jpeg") for i in gltf.get("images",[]))
    assert len(gltf.get("skins",[]))>0
    clips=[a.get("name") for a in gltf.get("animations",[])]
    assert len(clips)==28 and set(["Idle","Talk","Wave","Think"]).issubset(clips)
    assert all("Teacher_Viseme_"+v in clips
               for v in ("A","E","I","O","U","MBP","NEUTRAL"))
    assert "Teacher_Blink" in clips
    assert "Teacher_Blink" in [x["blink_clip"] for x in mouth["actors"]]
    matching=next(x for x in mouth["actors"] if x["actor"]==actor)
    assert matching["composite_blink_clips"]==item["composite_blink_clips"]
    assert all(x in clips for x in item["composite_blink_clips"])
    assert len(item["composite_blink_clips"])==7
    for clip in gltf["animations"][-15:]:
        assert clip["channels"] and clip["samplers"]
        assert all(ch["target"]["path"]=="weights" for ch in clip["channels"])
        assert all(ch["target"]["node"] < len(gltf["nodes"]) for ch in clip["channels"])
    # Validate the actual embedded FLOAT32 morph values, not just filenames.
    # A blinking vowel MUST preserve the mouth opening while closing the eyes.
    # Otherwise Filmament's last animation could silently erase one expression.
    byte_offset=20+n
    chunk_length,chunk_type=struct.unpack_from("<II",b,byte_offset)
    assert chunk_type==0x004E4942
    payload_start=byte_offset+8
    clips_by_name={clip["name"]:clip for clip in gltf["animations"]}
    def weight_vector(clip_name):
        clip=clips_by_name[clip_name]
        channel=clip["channels"][0]
        node=gltf["nodes"][channel["target"]["node"]]
        mesh=gltf["meshes"][node["mesh"]]
        names=mesh.get("extras",{}).get("targetNames",[])
        accessor=gltf["accessors"][clip["samplers"][channel["sampler"]]["output"]]
        view=gltf["bufferViews"][accessor["bufferView"]]
        assert accessor["componentType"]==5126 and accessor["type"]=="SCALAR"
        assert accessor["count"]%len(names)==0
        count=accessor["count"]
        values=struct.unpack_from("<"+"f"*count,b,payload_start+
                 view.get("byteOffset",0)+accessor.get("byteOffset",0))
        step=len(names)
        return [{name:values[k*step+i] for i,name in enumerate(names)}
                for k in range(count//step)]
    for vowel in ("A","E","I","O","U","MBP","NEUTRAL"):
        still=weight_vector("Teacher_Viseme_"+vowel)
        blink=weight_vector("Teacher_Viseme_"+vowel+"_Blink")
        assert len(still)==2 and len(blink)==4
        for part in blink:
            for key,value in still[0].items():
                if key not in ("eyeBlinkLeft","eyeBlinkRight"):
                    assert abs(part[key]-value)<1e-4,(actor,vowel,key,part[key],value)
        for side in ("eyeBlinkLeft","eyeBlinkRight"):
            assert blink[0][side]==0.0
            assert abs(blink[1][side]-1.0)<1e-4
            assert abs(blink[2][side]-1.0)<1e-4
            assert blink[3][side]==0.0
    assert weight_vector("Teacher_Viseme_A")[0]["jawOpen"]>0.6
    assert weight_vector("Teacher_Viseme_NEUTRAL")[0]["jawOpen"]==0.0
    assert item["facial_morph_targets"]>=52
    assert item["visual_approved"] is False
    print(actor,"pass - humanoid 3D",len(b),"bytes",len(clips),"animations,",
          len(gltf["skins"]),"skins, license retained, no VRoid")
print("PASS: no-anime guard and mouth-only tracks; NOT a visual approval")
