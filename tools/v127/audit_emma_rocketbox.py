#!/usr/bin/env python3
"""Download and audit one actual high-detail adult Emma avatar, FOR VISUAL QA ONLY.

Origin: Microsoft Rocketbox Avatar Library (MIT, copyright 2020 Microsoft),
converted and animated by Rohit Biradar's MIT Avatar Studio.
The model stays a candidate; no APK or release is created from this script.
"""
from pathlib import Path
from urllib.request import Request, urlopen
import hashlib,json,struct

PIN="2fd9ec82fbe1613d40ae38aadcca3f410725f740"
FILE="models/emma_casual.glb"
SOURCE=f"https://raw.githubusercontent.com/memoriesrohit/avatar-studio/{PIN}/{FILE}"
OUTPUT=Path("candidate-emma-rocketbox")
OUTPUT.mkdir(parents=True,exist_ok=True)

request=Request(SOURCE,headers={"User-Agent":"WordQuestAI-Emma-Quality-Research/1.0"})
with urlopen(request,timeout=110) as response:
    payload=response.read(22_000_000)
if len(payload)!=12722000:
    raise RuntimeError(f"Unexpected file length {len(payload)}, refusing unpinned binary")
if len(payload)<32 or payload[:4]!=b"glTF":
    raise RuntimeError("Invalid .glb header")
version,size=struct.unpack_from("<II",payload,4)
assert version==2 and size==len(payload)
data_len,data_kind=struct.unpack_from("<II",payload,12)
assert data_kind==0x4e4f534a
info=json.loads(payload[20:20+data_len].decode("utf-8"))
assert len(info.get("skins",[]))>=1,"No human bone rig"
assert len(info.get("meshes",[]))>=1
assert len(info.get("animations",[]))>=8,"Expected animated face/voice/idle model"
assert len(info.get("images",[]))>=3,"No high-detail textures"
animations=[item.get("name","") for item in info.get("animations",[])]
targets=[name for mesh in info.get("meshes",[]) for name in mesh.get("extras",{}).get("targetNames",[])]
attrs=[p.get("attributes",{}) for mesh in info.get("meshes",[]) for p in mesh.get("primitives",[])]
assert any("JOINTS_0" in x and "WEIGHTS_0" in x for x in attrs),"Skinned vertices missing"
# Facial expressions may use mesh extra names or node extras depending on exporter.
mesh_morphs=sum(len(p.get("targets",[])) for mesh in info["meshes"] for p in mesh.get("primitives",[]))
assert mesh_morphs>0,"No morph-target facial expressions"
(OUTPUT/"emma.glb").write_bytes(payload)
result={
 "status":"CANDIDATE_ONLY_VISUAL_REVIEW_REQUIRED",
 "label":"Emma",
 "source":SOURCE,
 "repository":"https://github.com/memoriesrohit/avatar-studio",
 "original_upstream":"https://github.com/microsoft/Microsoft-Rocketbox",
 "license":"MIT",
 "credit":"Microsoft Rocketbox Avatar Library, © Microsoft Corporation 2020; model conversion © Rohit Biradar 2026",
 "license_notice_files":["https://github.com/memoriesrohit/avatar-studio/blob/main/THIRD_PARTY_NOTICES.md","https://github.com/microsoft/Microsoft-Rocketbox/blob/master/LICENSE.md"],
 "sha256":hashlib.sha256(payload).hexdigest(),
 "size":len(payload),
 "skins":len(info["skins"]),
 "meshes":len(info["meshes"]),
 "textures":len(info.get("textures",[])),
 "images":len(info["images"]),
 "animation_names":animations,
 "number_of_morph_targets":mesh_morphs,
 "named_targets":sorted(set(targets))[:100],
}
(OUTPUT/"PROVENANCE.json").write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps({k:result[k] for k in ("label","size","skins","meshes","textures","images","animation_names","number_of_morph_targets")},indent=2))
print("Only a comparison candidate; this does NOT modify or publish the mobile app.")
