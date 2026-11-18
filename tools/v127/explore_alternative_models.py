#!/usr/bin/env python3
"""External quality exploration only: four licensed CC BY 4.0 full-body GLBs.
NOT used by the application until visual and redistribution acceptance.
Source + attribution: https://vtubeme.com/free-vrm-avatars
"""
from pathlib import Path
import hashlib,json,struct
from urllib.request import Request,urlopen
ROOT=Path(__file__).resolve().parents[2]
DEST=ROOT/"alternate-avatar-review"
DEST.mkdir(exist_ok=True)
results={"status":"REVIEW_ONLY_NOT_RELEASED","license":"CC BY 4.0",
         "attribution":"VTubeMe — https://vtubeme.com/free-vrm-avatars",
         "models":[]}
for name in ("nova","sky","kai","ember"):
    url=f"https://vtubeme.com/media/free/{name}/model.glb"
    req=Request(url,headers={"User-Agent":"Mozilla/5.0 (compatible; WordQuestAvatarReview/1.0)"})
    with urlopen(req,timeout=90) as response:
        blob=response.read(40_000_000)
    if blob[:4]!=b"glTF" or len(blob)<100000:
        raise ValueError(f"{name}: missing official GLB")
    version,length=struct.unpack_from("<II",blob,4)
    if version!=2 or length!=len(blob): raise ValueError(f"{name}: invalid GLB header")
    jslen,jsmark=struct.unpack_from("<I4s",blob,12)
    if jsmark!=b"JSON":raise ValueError(f"{name}: no model JSON")
    data=json.loads(blob[20:20+jslen].decode("utf8"))
    if not data.get("meshes") or not data.get("skins"):
        raise ValueError(f"{name}: missing skinned human mesh")
    count=sum(len(mesh.get("primitives",[])) for mesh in data.get("meshes",[]))
    p=DEST/f"{name}.glb"
    p.write_bytes(blob)
    results["models"].append({"name":name,"source":url,"size":len(blob),
                              "sha256":hashlib.sha256(blob).hexdigest(),
                              "skins":len(data.get("skins",[])),
                              "meshes":len(data.get("meshes",[])),
                              "primitives":count,
                              "textures":len(data.get("textures",[]))})
    print(name,"mesh",len(data["meshes"]),"skin",len(data["skins"]),"textures",len(data.get("textures",[])))
(DEST/"PROVENANCE.json").write_text(json.dumps(results,indent=2)+"\n")
print("Alternative GLB visual review only — not approved or shipped")
