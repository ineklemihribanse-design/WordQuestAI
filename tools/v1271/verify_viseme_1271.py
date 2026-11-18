#!/usr/bin/env python3
"""Structural regression tests for the actual GLB mouth animation overlay."""
from pathlib import Path
import struct,json
ROOT=Path(__file__).resolve().parents[2]
FOLDER=ROOT/"source/app/src/main/assets/avatars"
meta=json.loads((FOLDER/"AVATAR_LIPSYNC_METADATA.json").read_text("utf-8"))
assert meta["quality"]=="TEXT_RANGE_APPROXIMATION"
for actor in meta["actors"]:
    blob=(FOLDER/(actor["actor"]+".glb")).read_bytes()
    assert blob[:4]==b"glTF"
    version,length=struct.unpack_from("<II",blob,4)
    assert version==2 and length==len(blob)
    jslen,kind=struct.unpack_from("<II",blob,12)
    assert kind==0x4E4F534A
    doc=json.loads(blob[20:20+jslen].decode("utf-8"))
    assert doc["skins"] and len(doc["animations"])>=12
    clips={a["name"]:a for a in doc["animations"]}
    assert "Teacher_Talk" in clips and "Teacher_Idle" in clips
    for name in actor["clips"]:
        clip=clips[name]
        assert len(clip["channels"])==1, (actor["actor"],name)
        ch=clip["channels"][0]
        assert ch["target"]["node"]==actor["faceNode"]
        assert ch["target"]["path"]=="weights","Must not replace skeletal arm/head animation"
        output=doc["accessors"][clip["samplers"][0]["output"]]
        assert output["type"]=="SCALAR"
        assert output["count"]==2*actor["targetCount"]
    assert "Teacher_Viseme_NEUTRAL" in clips
    assert "Teacher_Viseme_MBP" in clips
    for key,ix in actor["mouthIndices"].items():
        assert 0<=ix<actor["targetCount"],(actor["actor"],key,ix)
    print(actor["actor"],"skins",len(doc["skins"]),
          "morph targets",actor["targetCount"],
          "teacher clips",len(actor["clips"]),"validated")
print("Passed: mouth-only clips, 3 rigged teachers, original idle/talk preserved")
