#!/usr/bin/env python3
"""Remove unused overlapping outfit meshes from the Emma Rocketbox GLB.

The authoring GLB ships four *mutually exclusive* outfits for the web editor.
Only one outfit may be active in WordQuest AI. Preserve its original animations,
morph targets, skin joints, texture buffers, license provenance and human rig.
"""
from pathlib import Path
import struct,json,hashlib

folder=Path("candidate-emma-rocketbox")
path=folder/"emma.glb"
blob=path.read_bytes()
assert blob[:4]==b"glTF"
v,total=struct.unpack_from("<II",blob,4)
assert v==2 and total==len(blob)
jlen,jkind=struct.unpack_from("<II",blob,12)
assert jkind==0x4E4F534A
doc=json.loads(blob[20:20+jlen].decode("utf-8"))
# Standard GLB chunk following JSON. Keep it unchanged to preserve all meshes.
tail=blob[20+jlen:]
exclusive={
    "Outfit_formal","Outfit_original","Skin_formal","Skin_original",
    "Top_shirt","Skin_shirt","Top_tee","Skin_tee","Trousers","Shoes"
}
selected={"Outfit_original","Skin_original"}
removed=[]
active=[]
for node in doc["nodes"]:
    name=node.get("name")
    if name in exclusive:
        if name not in selected:
            if "mesh" in node:
                removed.append(name);node.pop("mesh")
        else:active.append(name)
assert set(active)==selected,(active,removed)
assert set(removed)==exclusive-selected,(active,removed)
# Render passes must continue to see Body, Hair, Eyelashes and Eye meshes.
assert any(n.get("name")=="Body" and "mesh" in n for n in doc["nodes"])
assert any(n.get("name")=="Hair" and "mesh" in n for n in doc["nodes"])
# No animation may target an excluded outfit's mesh weights.
excluded_nodes={i for i,n in enumerate(doc["nodes"]) if n.get("name") in removed}
assert not any(ch.get("target",{}).get("node") in excluded_nodes
               for clip in doc["animations"] for ch in clip.get("channels",[]))
text=json.dumps(doc,separators=(",",":"),ensure_ascii=False).encode("utf-8")
text+=b" "*((-len(text))%4)
result=(struct.pack("<4sII",b"glTF",2,12+8+len(text)+len(tail))
        +struct.pack("<II",len(text),0x4E4F534A)+text+tail)
assert len(result)==struct.unpack_from("<I",result,8)[0]
# Audit the selected nodes survive round-trip.
decoded=json.loads(result[20:20+len(text)].decode("utf-8"))
assert sum("mesh" in n and n.get("name") in exclusive for n in decoded["nodes"])==2
path.write_bytes(result)
provenance=folder/"PROVENANCE.json"
record=json.loads(provenance.read_text("utf-8"))
record["original_sha256"]=record["sha256"]
record["sha256"]=hashlib.sha256(result).hexdigest()
record["model_variant"]="Original casual outfit only; 8 unused outfit meshes suppressed; all animations and morph targets kept"
record["removed_outfit_mesh_nodes"]=removed
record["size"]=len(result)
provenance.write_text(json.dumps(record,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
print("Emma candidate now contains a single casual outfit. Retained:",active,"Suppressed:",removed)
