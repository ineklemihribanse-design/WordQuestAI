#!/usr/bin/env python3
"""Densify *only sparse facial morph accessors* in female Android GLBs.

Reason: 1.0.31 shrank embedded textures but both Emma and Sophia still
crash in native Filament. GLB audit showed 208 facial morph accessors per
actor and Emma's morph accessors have no direct bufferView. Some older
gltfio pipelines are sensitive to sparse morph decoding.

This is a controlled representation change, not an art downgrade:
- same 52 ARKit face shapes, same body, textures, 80 bones and 28 clips
- byte-identical decoded morph delta arrays (verified below)
- input GLBs retained; output .glb separate for Android QA
- no claim of phone compatibility until logcat/tombstone and visual QA

This grows encoded bytes but can bypass sparse morph handling in the native
loader. DO NOT call this a final fix before real-device confirmation.
"""
from pathlib import Path
import json, struct, hashlib

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/"source/app/src/main/assets/avatars"
JSON_TAG=0x4E4F534A
BIN_TAG=0x004E4942
SIZE={5120:1,5121:1,5122:2,5123:2,5125:4,5126:4}
COMP={"SCALAR":1,"VEC2":2,"VEC3":3,"VEC4":4,"MAT2":4,"MAT3":9,"MAT4":16}
INDEX_FORMAT={5121:"B",5123:"H",5125:"I"}

def read(path):
    b=path.read_bytes()
    assert b[:4]==b"glTF"
    v,total=struct.unpack_from("<II",b,4)
    assert v==2 and total==len(b)
    n,tag=struct.unpack_from("<II",b,12)
    assert tag==JSON_TAG
    doc=json.loads(b[20:20+n])
    start=20+n
    bin_len,tag=struct.unpack_from("<II",b,start)
    assert tag==BIN_TAG and start+8+bin_len<=len(b)
    return doc,bytes(b[start+8:start+8+bin_len])

def expand(accessor,views,data):
    count=accessor["count"]
    unit=SIZE[accessor["componentType"]]*COMP[accessor["type"]]
    result=bytearray(count*unit)
    if "bufferView" in accessor:
        view=views[accessor["bufferView"]]
        stride=view.get("byteStride",unit)
        assert stride>=unit
        start=view.get("byteOffset",0)+accessor.get("byteOffset",0)
        for index in range(count):
            chunk=data[start+index*stride:start+index*stride+unit]
            assert len(chunk)==unit
            result[index*unit:(index+1)*unit]=chunk

    sparse=accessor.get("sparse")
    if sparse:
        quantity=sparse["count"]
        assert 0<=quantity<=count
        indexes=sparse["indices"]
        values=sparse["values"]
        fmt=INDEX_FORMAT[indexes["componentType"]]
        size=SIZE[indexes["componentType"]]
        indexview=views[indexes["bufferView"]]
        valueview=views[values["bufferView"]]
        indexbase=indexview.get("byteOffset",0)+indexes.get("byteOffset",0)
        valuebase=valueview.get("byteOffset",0)+values.get("byteOffset",0)
        last=-1
        for i in range(quantity):
            raw=data[indexbase+i*size:indexbase+(i+1)*size]
            assert len(raw)==size
            target=struct.unpack("<"+fmt,raw)[0]
            assert 0<=target<count and target>last,("bad sparse index",target,last)
            last=target
            block=data[valuebase+i*unit:valuebase+(i+1)*unit]
            assert len(block)==unit
            result[target*unit:(target+1)*unit]=block
    return bytes(result)

def pack(doc,binary):
    js=json.dumps(doc,ensure_ascii=False,separators=(",",":")).encode()
    js+=b" " * ((-len(js))%4)
    binary+=b"\0" * ((-len(binary))%4)
    return (struct.pack("<4sII",b"glTF",2,12+8+len(js)+8+len(binary))+
            struct.pack("<II",len(js),JSON_TAG)+js+
            struct.pack("<II",len(binary),BIN_TAG)+binary)

def densify(actor):
    src=ASSETS/(actor+"-mobile-lite.glb")
    dst=ASSETS/(actor+"-dense-face.glb")
    doc,old_binary=read(src)
    old_views=json.loads(json.dumps(doc["bufferViews"]))
    original_accessors=json.loads(json.dumps(doc["accessors"]))
    selected={idx for mesh in doc["meshes"] for prim in mesh.get("primitives",[])
              for morph in prim.get("targets",[]) for idx in morph.values()}
    assert len(selected)>=50,(actor,"no ARKit morph accessors")
    old_doc=json.loads(json.dumps(doc))
    newbin=bytearray(old_binary)
    converted=0
    direct=0
    sparse_items=0
    for ix in sorted(selected):
        acc=doc["accessors"][ix]
        if "sparse" not in acc:
            direct+=1
            continue
        assert acc["componentType"]==5126,("morph component",actor,ix)
        assert acc["type"]=="VEC3",("morph vector",actor,ix,acc["type"])
        dense=expand(original_accessors[ix],old_views,old_binary)
        sparse_items+=acc["sparse"]["count"]
        newbin.extend(b"\0"*((-len(newbin))%4))
        new_view=len(doc["bufferViews"])
        doc["bufferViews"].append({"buffer":0,"byteOffset":len(newbin),"byteLength":len(dense)})
        newbin.extend(dense)
        acc.pop("sparse")
        acc["bufferView"]=new_view
        acc["byteOffset"]=0
        # Fail closed: decoded output must be byte-identical to original sparse.
        assert expand(acc,doc["bufferViews"],newbin)==dense
        converted+=1

    doc["buffers"][0]["byteLength"]=len(newbin)
    result=pack(doc,bytes(newbin))
    final,finalbin=read_from_bytes(result)
    for key in ("nodes","meshes","skins","animations","materials","textures","images"):
        assert final.get(key)==old_doc.get(key),(actor,key)
    assert len(final["accessors"])==len(old_doc["accessors"])
    for ix in sorted(selected):
        assert expand(final["accessors"][ix],final["bufferViews"],finalbin)==expand(
            original_accessors[ix],old_views,old_binary),(actor,"morph value change",ix)
    assert len(final["animations"])==28
    assert any(a.get("name")=="Teacher_Viseme_A" for a in final["animations"])
    dst.write_bytes(result)
    print(f"{actor}: sparse morph accessors={converted}, direct={direct}, "
          f"sparse entries={sparse_items}, source MiB={len(src.read_bytes())/1048576:.2f}, "
          f"dense QA MiB={len(result)/1048576:.2f}, "
          f"52 ARKit shapes, 28 animation clips and humanoid base unchanged")
    return converted

def read_from_bytes(data):
    from tempfile import NamedTemporaryFile
    # Parse in memory; no temporary artifacts needed in CI.
    n,kind=struct.unpack_from("<II",data,12)
    assert kind==JSON_TAG
    doc=json.loads(data[20:20+n])
    offset=20+n
    binary_length,kind=struct.unpack_from("<II",data,offset)
    assert kind==BIN_TAG
    return doc,bytes(data[offset+8:offset+8+binary_length])

if __name__=="__main__":
    totals=[densify(a) for a in ("emma","sophia")]
    print("QA: sparse-to-dense representation-only experiment; NOT proof of Android crash resolution")
    assert any(x>0 for x in totals), "No sparse female morph targets; investigate other renderer differences"
