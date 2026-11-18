#!/usr/bin/env python3
"""Compare original female and working Ethan native-render memory surfaces.

Numbers here are GLB *data structure* counts, not Android GPU allocations.
They help prioritize cases to reproduce with Android logcat / tombstones.
"""
from pathlib import Path
import json,struct
from collections import Counter
from PIL import Image
from io import BytesIO

BASE=Path(__file__).resolve().parents[2]/"source/app/src/main/assets/avatars"
for actor,variant in (("emma","mobile-lite"),("sophia","mobile-lite"),("ethan","mobile")):
    b=(BASE/f"{actor}-{variant}.glb").read_bytes()
    n,tag=struct.unpack_from("<II",b,12);assert tag==0x4E4F534A
    doc=json.loads(b[20:20+n]); at=20+n
    blen,tag=struct.unpack_from("<II",b,at);assert tag==0x004E4942
    chunk=b[at+8:at+8+blen]
    views=doc["bufferViews"];accessors=doc["accessors"]
    primitives=[p for m in doc["meshes"] for p in m["primitives"]]
    morphs=[t for p in primitives for t in p.get("targets",[])]
    accessidx={ix for t in morphs for ix in t.values()}
    morph_bytes=sum(views[accessors[ix]["bufferView"]]["byteLength"] for ix in accessidx
        if "bufferView" in accessors[ix])
    morph_vertices=sum(accessors[p["attributes"]["POSITION"]]["count"]*len(p.get("targets",[]))
         for p in primitives if "POSITION" in p["attributes"])
    triangles=sum(accessors[p["indices"]]["count"]//3 for p in primitives if "indices" in p)
    texels=0;tex_bytes=0;alpha=[]
    for image in doc.get("images",[]):
        view=views[image["bufferView"]]
        d=chunk[view.get("byteOffset",0):view.get("byteOffset",0)+view["byteLength"]]
        im=Image.open(BytesIO(d));texels+=im.width*im.height
        tex_bytes+=im.width*im.height*4
    for mat in doc.get("materials",[]):
        if mat.get("alphaMode","OPAQUE")!="OPAQUE":
            alpha.append((mat.get("name"),mat.get("alphaMode")))
    print(f"== {actor} ==")
    print(json.dumps({
        "glbMiB":round(len(b)/1048576,2),
        "meshes":len(doc["meshes"]), "triangles":triangles,
        "morphTargetReferences":len(morphs),
        "morphAccessors":len(accessidx),
        "morphReferencedBufferViewMiB":round(morph_bytes/1048576,2),
        "morphVertexInstances":morph_vertices,
        "decodedTextureMiB_RGBA8_approx":round(tex_bytes/1048576,2),
        "totalTextureTexels":texels,
        "skins":[len(x["joints"]) for x in doc["skins"]],
        "animationClips":len(doc["animations"]),
        "alphaMaterials":alpha,
        "requiredExtensions":doc.get("extensionsRequired",[]),
    },ensure_ascii=False,indent=2))
print("Do not infer a native crash root cause from counts; obtain :teacher3d tombstone.")
