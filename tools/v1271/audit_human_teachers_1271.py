#!/usr/bin/env python3
"""Prepare adult 3D-human *review* GLBs for Emma, Sophia, Ethan.

No VRoid assets, no anime samples. All upstream inputs are pinned to a
specific MIT-licensed Avatar Studio commit using Rocketbox human scans.
Worn-outfit isolation preserves the meshes, skeleton, facial blendshapes and
original action clips. Actual 3D Blender previews are mandatory before merging.

This script NEVER updates the app's production avatar assets by itself.
"""
from pathlib import Path
from urllib.request import urlopen,Request
from collections import Counter
import json,struct,hashlib

PIN="2fd9ec82fbe1613d40ae38aadcca3f410725f740"
SOURCE="https://raw.githubusercontent.com/memoriesrohit/avatar-studio/"+PIN+"/models/"
OUTPUT=Path("candidate-human-teachers")
INPUTS={
 "emma":{"file":"olivia_smart.glb","size":13332100,
         "outfit":["Top_tee","Skin_tee","Trousers","Shoes"]},
 "sophia":{"file":"sophia_formal.glb","size":11097332,
           "outfit":["Outfit_original","Skin_original"]},
 "ethan":{"file":"ethan_casual.glb","size":13101540,
          "outfit":["Outfit_original","Skin_original"]},
}
MUTEX={"Outfit_formal","Outfit_original","Skin_formal","Skin_original",
       "Top_shirt","Skin_shirt","Top_tee","Skin_tee","Trousers","Shoes"}
def process(label,spec):
    dest=OUTPUT/label;dest.mkdir(parents=True,exist_ok=True)
    source=SOURCE+spec["file"]
    with urlopen(Request(source,headers={"User-Agent":"WordQuest-HumanTeachers-QualityAudit/1.0.27.1"}),timeout=90) as f:
        blob=f.read(23_000_000)
    assert len(blob)==spec["size"],(label,"unexpected upstream bytes",len(blob))
    assert blob[:4]==b"glTF"
    ver,total=struct.unpack_from("<II",blob,4)
    assert ver==2 and total==len(blob)
    jlen,jtag=struct.unpack_from("<II",blob,12)
    assert jtag==0x4E4F534A
    doc=json.loads(blob[20:20+jlen].decode("utf-8"))
    clips=[a.get("name","") for a in doc["animations"]]
    assert {"Idle","Talk","Wave","Think"}.issubset(set(clips)),(label,clips)
    assert doc.get("skins") and doc.get("meshes") and len(doc.get("images",[]))>=5
    assert all("VRM" not in x for x in doc.get("extensionsUsed",[])),label
    assert "EXT_texture_webp" in doc.get("extensionsUsed",[])
    selected=set(spec["outfit"])
    present=set();removed=[]
    for n in doc["nodes"]:
        name=n.get("name")
        if name in MUTEX:
            if name in selected:
                assert "mesh" in n,(label,name)
                present.add(name)
            elif "mesh" in n:
                n.pop("mesh")
                removed.append(name)
    # Some Rocketbox conversions have naming variants: fail closed for
    # missing clothes rather than generating a floating head.
    assert present==selected,(label,"missing garment",sorted(selected-present))
    if label=="emma":
        for piece,factor in (("Top_tee",[.97,.96,.93,1.]),
                             ("Trousers",[.44,.63,.84,1.])):
            n=next(n for n in doc["nodes"] if n.get("name")==piece)
            for p in doc["meshes"][n["mesh"]]["primitives"]:
                doc["materials"][p["material"]].setdefault(
                    "pbrMetallicRoughness",{})["baseColorFactor"]=factor
    named=[]
    for i,mesh in enumerate(doc["meshes"]):
        c=sum(len(p.get("targets",[])) for p in mesh.get("primitives",[]))
        if c:
            names=mesh.get("extras",{}).get("targetNames") or []
            named.append({"mesh":i,"name":mesh.get("name"),"targets":c,
                          "names":names[:62]})
    assert sum(x["targets"] for x in named)>=52,(label,"missing facial morphs")
    assert len(doc["skins"])>=1
    # Only adjust glTF JSON; embedded buffers and weights stay byte-exact.
    encoded=json.dumps(doc,separators=(",",":"),ensure_ascii=False).encode("utf-8")
    encoded+=b" "*((-len(encoded))%4)
    suffix=blob[20+jlen:]
    out=(struct.pack("<4sII",b"glTF",2,12+8+len(encoded)+len(suffix))
         +struct.pack("<II",len(encoded),0x4E4F534A)+encoded+suffix)
    assert struct.unpack_from("<I",out,8)[0]==len(out)
    path=dest/(label+".glb");path.write_bytes(out)
    report={
      "actor":label,"source":source,"source_commit":PIN,
      "source_license":"MIT, Microsoft Rocketbox and Avatar Studio",
      "studio_preview_only":True,
      "visual_accepted":False,"bytes":len(out),
      "sha256":hashlib.sha256(out).hexdigest(),
      "skeletons":len(doc["skins"]),"animations":clips,
      "mouth_morph_targets":named,
      "selected_outfit":sorted(present),"hidden_outfits":sorted(removed),
      "visible_meshes":sum("mesh" in n for n in doc["nodes"]),
      "extensionsRequired":doc.get("extensionsRequired",[]),
      "source_art_styling":"real-human; semi-stylized goal not yet approved"
    }
    (dest/"AUDIT.json").write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n")
    print(label,report["selected_outfit"],len(clips),"clips",sum(x["targets"] for x in named),"morphs")
    for item in named:
        print(" MORPHS",item["name"],item["targets"],item["names"][:18])
    return report

def main():
    reports=[process(name,spec) for name,spec in INPUTS.items()]
    assert {p["actor"] for p in reports}=={"emma","sophia","ethan"}
    assert all(p["visual_accepted"] is False for p in reports)
    (OUTPUT/"AUDIT_SUMMARY.json").write_text(json.dumps(reports,indent=2,ensure_ascii=False)+"\n")
    print("Human 3D teacher QA candidates staged; NO APK and NO release")
if __name__=="__main__":main()
