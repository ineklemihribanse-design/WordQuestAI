#!/usr/bin/env python3
"""Audit two MIT-licensed long-brown-hair female faces for WordQuest Emma.

Unpack only the worn outfit; keep all rigged expressions and 13 animation
clips. These are comparison candidates, not automatically accepted assets.
"""
from pathlib import Path
import hashlib,json,struct
from urllib.request import Request,urlopen
ROOT=Path("candidate-emma-brown")
PIN="2fd9ec82fbe1613d40ae38aadcca3f410725f740"
SOURCES={"olivia":("models/olivia_smart.glb",13332100),
         "olivia_white":("models/olivia_smart.glb",13332100),
         "priya":("models/priya_casual.glb",13471776),
         "olivia_tee":("models/olivia_smart.glb",13332100),
         "olivia_formal":("models/olivia_smart.glb",13332100)}
EXCLUSIVE={"Outfit_formal","Outfit_original","Skin_formal","Skin_original",
 "Top_shirt","Skin_shirt","Top_tee","Skin_tee","Trousers","Shoes"}
WORN={"Outfit_original","Skin_original"}
for label,(source,expected_size) in SOURCES.items():
    dest=ROOT/label
    dest.mkdir(parents=True,exist_ok=True)
    url=f"https://raw.githubusercontent.com/memoriesrohit/avatar-studio/{PIN}/{source}"
    with urlopen(Request(url,headers={"User-Agent":"WordQuestEmmaBrownHairQA/1.0"}),timeout=100) as response:
        blob=response.read(23_000_000)
    if len(blob)!=expected_size:raise RuntimeError(f"{label}: wrong source size")
    if blob[:4]!=b"glTF":raise RuntimeError(f"{label}: not GLB")
    v,total=struct.unpack_from("<II",blob,4)
    assert v==2 and total==len(blob)
    jlen,kind=struct.unpack_from("<II",blob,12)
    assert kind==0x4E4F534A
    scene=json.loads(blob[20:20+jlen].decode("utf-8"))
    assert scene.get("skins") and scene.get("meshes") and len(scene.get("animations",[]))>=8
    assert len(scene.get("images",[]))>=3
    morphs=sum(len(p.get("targets",[])) for mesh in scene["meshes"] for p in mesh.get("primitives",[]))
    assert morphs>0
    choice=(
        {"Top_shirt","Skin_shirt","Trousers","Shoes"} if label=="olivia_white" else
        {"Top_tee","Skin_tee","Trousers","Shoes"} if label=="olivia_tee" else
        {"Outfit_formal","Skin_formal"} if label=="olivia_formal" else
        WORN
    )
    active=set()
    removed=[]
    for node in scene["nodes"]:
        name=node.get("name")
        if name in EXCLUSIVE:
            if name in choice:
                assert "mesh" in node
                active.add(name)
            elif "mesh" in node:
                node.pop("mesh")
                removed.append(name)
    assert active==choice,(label,active)
    if label in ("olivia_white","olivia_tee"):
        # Simple office attire using source rigs: white blouse and muted denim blue trousers.
        for piece,rgb in [(("Top_shirt" if label=="olivia_white" else "Top_tee"),[0.97,0.96,0.93,1]),("Trousers",[0.44,0.63,0.84,1])]:
            node=next(n for n in scene["nodes"] if n.get("name")==piece)
            for primitive in scene["meshes"][node["mesh"]]["primitives"]:
                material=scene["materials"][primitive["material"]]
                material.setdefault("pbrMetallicRoughness",{})["baseColorFactor"]=rgb
    encoded=json.dumps(scene,separators=(",",":"),ensure_ascii=False).encode("utf-8")
    encoded+=b" "*((-len(encoded))%4)
    suffix=blob[20+jlen:]
    result=struct.pack("<4sII",b"glTF",2,12+8+len(encoded)+len(suffix))+struct.pack("<II",len(encoded),0x4E4F534A)+encoded+suffix
    assert len(result)==struct.unpack_from("<I",result,8)[0]
    (dest/(label+".glb")).write_bytes(result)
    review={"status":"MODEL_PREVIEW_ONLY","source":url,"upstream_license":"MIT, Microsoft Rocketbox ©2020 and Avatar Studio ©2026","sha256":hashlib.sha256(result).hexdigest(),"bytes":len(result),"animations":[a.get("name") for a in scene["animations"]],"morphs":morphs,"only_visible_clothes":sorted(active),"hidden_clothing":removed,"reference_goal":"adult warm long dark brown hair semi-realistic stylized teacher"}
    (dest/"PROVENANCE.json").write_text(json.dumps(review,indent=2)+"\n")
    print(label,"bytes",len(result),"morphs",morphs,"images",len(scene["images"]),"clips",len(scene["animations"]))
print("No application update; Emma face options are review-only")
