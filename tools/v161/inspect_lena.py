#!/usr/bin/env python3
"""Inspect source Lena GLB structure before generating colored runtime variant.
Read-only, never rewrites the original model or the app's assets.
"""
import json,struct,sys
from pathlib import Path

def read_glb(path):
    data=Path(path).read_bytes()
    magic,version,length=struct.unpack_from("<4sII",data,0)
    assert magic==b"glTF" and version==2 and length==len(data)
    n,typ=struct.unpack_from("<I4s",data,12)
    assert typ==b"JSON"
    doc=json.loads(data[20:20+n])
    return doc

if __name__=="__main__":
    d=read_glb(sys.argv[1])
    print("LENA AUDIT meshes=",len(d.get("meshes",[])),"nodes=",len(d.get("nodes",[])),"materials=",len(d.get("materials",[])),"animations=",len(d.get("animations",[])))
    print("Materials:",[(i,m.get("name"),m.get("pbrMetallicRoughness",{}).get("baseColorFactor")) for i,m in enumerate(d.get("materials",[]))])
    for i,m in enumerate(d.get("meshes",[])):
        prims=m.get("primitives",[])
        print("MESH",i,repr(m.get("name")),"primitives",len(prims),"materials",[p.get("material") for p in prims],"targets",[len(p.get("targets",[])) for p in prims],"weights",m.get("weights"),"extras",str(m.get("extras",{}))[:300])
        for j,p in enumerate(prims):
            acc=d.get("accessors",[])[p["attributes"]["POSITION"]]
            print(" primitive",j,"position_bounds",acc.get("min"),acc.get("max"),"vertex_count",acc.get("count"),"channels",list(p.get("attributes",{}).keys()))
    print("NODES: ",[(i,n.get("name"),n.get("mesh"),n.get("skin")) for i,n in enumerate(d.get("nodes",[])) if n.get("mesh") is not None][:70])
    for si,skin in enumerate(d.get("skins",[])):
        print("SKIN",si,"joint_count",len(skin.get("joints",[])))
        print("JOINT NAMES:",[(i,d["nodes"][j].get("name"),j) for i,j in enumerate(skin.get("joints",[]))])
    print("ACCESSOR TYPES:",[(i,a.get("type"),a.get("componentType"),a.get("count")) for i,a in enumerate(d.get("accessors",[])[:12])])
    from paint_lena_glb import accessor_values, dominant_bone
    from collections import Counter
    blob=Path(sys.argv[1]).read_bytes()
    at=12
    while at<len(blob):
        n,t=struct.unpack_from("<I4s",blob,at)
        if t==bytes((66,73,78,0)):
            bin_data=blob[at+8:at+8+n]
            break
        at+=8+n
    else: raise ValueError("no BIN chunk")
    prim=d["meshes"][0]["primitives"][0]
    attr=prim["attributes"]
    pts=accessor_values(d,bin_data,attr["POSITION"])
    jj=accessor_values(d,bin_data,attr["JOINTS_0"])
    ww=accessor_values(d,bin_data,attr["WEIGHTS_0"])
    bones=[d["nodes"][k].get("name","") for k in d["skins"][0]["joints"]]
    primary=Counter(dominant_bone(j,w,bones) for j,w in zip(jj,ww))
    weighted=Counter()
    for j,w in zip(jj,ww):
        for idx,wt in zip(j,w):
            if wt>0.001: weighted[bones[idx]]+=1
    print("TOP DOMINANT BONES:",primary.most_common(25))
    print("WEIGHTED FACE BONES:",[(name,n) for name,n in weighted.items() if any(t in name.lower() for t in ["lip","brow","lid","eye","jaw"])][:50])
    for a in d.get("animations",[]):
        chans=a.get("channels",[])
        print("ANIM",a.get("name"),"channels",len(chans),"targets",sorted(set(str((c.get("target") or {}).get("path")) for c in chans)),"node_refs",sorted(set((c.get("target") or {}).get("node") for c in chans if (c.get("target") or {}).get("node") is not None))[:14])
