#!/usr/bin/env python3
"""Read-only Lena armature and animation QA on the exact GLB assembled by CI.

Does not modify bones, mesh, textures, lips, face, or existing animation clips.
Rejects malformed joint references, quaternion output and missing core clips.
The device-reported distorted greeting remains disabled until manual retarget QA.
"""
from pathlib import Path
import json, math, struct, sys

def read_glb(path):
    data=Path(path).read_bytes()
    magic,version,total=struct.unpack_from("<4sII",data)
    assert magic==b"glTF" and version==2 and total==len(data)
    pos=12; doc=None;binary=None
    while pos<len(data):
        size,typ=struct.unpack_from("<I4s",data,pos)
        pos+=8
        chunk=data[pos:pos+size]
        if typ==b"JSON":doc=json.loads(chunk)
        if typ==b"BIN\x00":binary=chunk
        pos+=size
    assert doc is not None and binary is not None
    return doc,binary

def main(path):
    d,b=read_glb(path)
    nodes=d["nodes"]
    skins=d["skins"]
    assert len(skins)==1,"Unexpected number of Lena skins"
    joints=skins[0]["joints"]
    assert len(joints)==201,("Lena bone count changed",len(joints))
    assert len(set(joints))==201
    assert all(0<=i<len(nodes) for i in joints)
    animations=d.get("animations",[])
    assert len(animations)>=18,("Lena clips missing",len(animations))
    names={x.get("name","") for x in animations}
    for name in ("01_Idle_Natural_Breathing","02_Greeting_Gentle_Wave",
                 "03_Talking_Hand_Gestures","04_Listening_Nod"):
        assert name in names, ("Missing required original clip",name)
    issues=[]; inspected_rotations=0
    for clip in animations:
        for channel in clip["channels"]:
            assert 0 <= channel["target"]["node"] < len(nodes)
            path=channel["target"]["path"]
            sampler=clip["samplers"][channel["sampler"]]
            access=d["accessors"][sampler["output"]]
            assert access["count"]>0
            if path!="rotation":continue
            # Sample all float32 quaternions (including cubic spline tangents).
            # Cubic spline tangents need not be unit quaternions.
            if access["componentType"]!=5126 or access["type"]!="VEC4":
                issues.append((clip.get("name"),"unsupported quaternion output"))
                continue
            view=d["bufferViews"][access["bufferView"]]
            stride=view.get("byteStride",16)
            offset=view.get("byteOffset",0)+access.get("byteOffset",0)
            for i in range(access["count"]):
                q=struct.unpack_from("<4f",b,offset+i*stride)
                assert all(math.isfinite(v) for v in q),("Non-finite joint pose",clip["name"])
                if sampler.get("interpolation")!="CUBICSPLINE":
                    norm=math.sqrt(sum(v*v for v in q))
                    if not (.7<norm<1.3):
                        issues.append((clip.get("name"),"non-normalized joint rotation"))
                inspected_rotations+=1
    assert not issues, issues[:15]
    print("Lena original rig QA:",len(joints),"unique joints;",
          len(animations),"animation clips;",inspected_rotations,
          "finite rotation samples")
    print("Broken greeting wave is preserved as source data but disabled at runtime.")
    print("This validates GLB structure, NOT natural pose or device lip synchronization.")

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: audit_lena_rig_170.py path/to/Lena.glb")
    main(sys.argv[1])
