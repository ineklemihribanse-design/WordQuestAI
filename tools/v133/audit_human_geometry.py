#!/usr/bin/env python3
"""Spec-level glTF validation against the Android staged Emma/Sophia/Ethan.

This targets mesh/skin/accessor corruption left possible when BOTH 52-morph
and zero-morph female variants SIGABRT inside gltfio's model-load path.
Do not change art based on suspicion; fail CI for *actual* invalid data.

Checks decoded indices and joints (not just accessor bounds), finite base
positions, primitive attribute lengths, skin inverse bind matrix counts,
and animation/morph target cardinality. This is not native GPU certification.
"""
from pathlib import Path
from collections import defaultdict
import json,struct,math

A=Path(__file__).resolve().parents[2]/"source/app/src/main/assets/avatars"
FORMAT={5120:("b",1),5121:("B",1),5122:("h",2),5123:("H",2),
        5125:("I",4),5126:("f",4)}
PARTS={"SCALAR":1,"VEC2":2,"VEC3":3,"VEC4":4,"MAT2":4,"MAT3":9,"MAT4":16}

def audit(path):
    raw=path.read_bytes()
    assert raw[:4]==b"glTF" and struct.unpack_from("<II",raw,4)==(2,len(raw)),path
    jsize,jtype=struct.unpack_from("<II",raw,12)
    assert jtype==0x4E4F534A
    doc=json.loads(raw[20:20+jsize])
    p=20+jsize
    bsize,btype=struct.unpack_from("<II",raw,p)
    assert btype==0x004E4942
    bin=raw[p+8:p+8+bsize]
    views=doc["bufferViews"]
    accs=doc["accessors"]
    nodes=doc["nodes"]
    meshes=doc["meshes"]
    skins=doc["skins"]

    for i,v in enumerate(views):
        off=v.get("byteOffset",0)
        assert v.get("buffer",0)==0
        assert off>=0 and v["byteLength"]>=0 and off+v["byteLength"]<=len(bin),(path,i)
    def decode(ix):
        acc=accs[ix]
        ch,numbytes=FORMAT[acc["componentType"]]
        width=PARTS[acc["type"]]
        unit=numbytes*width
        result=bytearray(acc["count"]*unit)
        if "bufferView" in acc:
            view=views[acc["bufferView"]]
            base=view.get("byteOffset",0)+acc.get("byteOffset",0)
            stride=view.get("byteStride",unit)
            assert stride>=unit
            for k in range(acc["count"]):
                piece=bin[base+k*stride:base+k*stride+unit]
                assert len(piece)==unit,(path,ix,"short accessor")
                result[k*unit:(k+1)*unit]=piece
        if "sparse" in acc:
            sp=acc["sparse"]
            iv=sp["indices"]
            vv=sp["values"]
            fmt,b=FORMAT[iv["componentType"]]
            assert fmt in ("B","H","I")
            view=views[iv["bufferView"]]
            start=view.get("byteOffset",0)+iv.get("byteOffset",0)
            view2=views[vv["bufferView"]]
            start2=view2.get("byteOffset",0)+vv.get("byteOffset",0)
            previous=-1
            for k in range(sp["count"]):
                index=struct.unpack_from("<"+fmt,bin,start+k*b)[0]
                assert previous<index<acc["count"],(path,ix,"sparse indices")
                previous=index
                piece=bin[start2+k*unit:start2+(k+1)*unit]
                assert len(piece)==unit
                result[index*unit:(index+1)*unit]=piece
        return list(struct.iter_unpack("<"+ch*width,result))

    for ix,skin in enumerate(skins):
        assert all(0<=joint<len(nodes) for joint in skin["joints"])
        assert len(set(skin["joints"]))==len(skin["joints"]), (path,ix,"duplicate skin joint")
        if "inverseBindMatrices" in skin:
            assert accs[skin["inverseBindMatrices"]]["count"]==len(skin["joints"]), (
                path,ix,"IBMs mismatch skin joint count")
    for ix,mesh in enumerate(meshes):
        primitive_targets={len(p.get("targets",[])) for p in mesh["primitives"]}
        # glTF morphs must have same target count across primitives in a mesh.
        assert len(primitive_targets)<=1,(path,"unequal morph count in mesh",ix)
        expected=next(iter(primitive_targets),0)
        if "weights" in mesh:
            assert len(mesh["weights"])==expected,(path,ix,"invalid mesh morph weights")
    for n,node in enumerate(nodes):
        for x in ("translation","rotation","scale","matrix","weights"):
            if x in node:
                assert all(math.isfinite(k) for k in node[x]),(path,"non-finite node",n,x)
        if "mesh" in node and "weights" in node:
            targets=len(meshes[node["mesh"]]["primitives"][0].get("targets",[]))
            assert len(node["weights"])==targets,(path,"wrong node morph weight count",n)
    owners=defaultdict(list)
    for node in nodes:
        if "mesh" in node and "skin" in node:
            owners[node["mesh"]].append(len(skins[node["skin"]]["joints"]))

    primitives=0
    vertices=0
    morphs=0
    for mi,mesh in enumerate(meshes):
        for prim in mesh["primitives"]:
            primitives+=1
            attrs=prim["attributes"]
            count=accs[attrs["POSITION"]]["count"]
            vertices+=count
            for semantic,ix in attrs.items():
                assert accs[ix]["count"]==count,(path,mi,"bad attribute count",semantic)
            positions=decode(attrs["POSITION"])
            assert all(all(math.isfinite(x) for x in row) for row in positions),(
                path,mi,"non-finite mesh vertex POSITION")
            if "indices" in prim:
                indices=decode(prim["indices"])
                max_index=max((row[0] for row in indices),default=0)
                assert max_index<count,(path,mi,"triangle indices out of bounds",max_index,count)
            for suffix in ("_0","_1"):
                ji=attrs.get("JOINTS"+suffix)
                wi=attrs.get("WEIGHTS"+suffix)
                assert (ji is None)==(wi is None),(path,mi,"unpaired joints/weights")
                if ji is not None:
                    joints=decode(ji)
                    limits=owners.get(mi)
                    if limits:
                        valid=max(limits)
                        assert max((max(row) for row in joints),default=0)<valid,(
                            path,mi,"invalid skin joint index")
                    weights=decode(wi)
                    assert all(all(math.isfinite(float(x)) for x in row) for row in weights), (
                        path,mi,"nonfinite skin weights")
            morphs+=len(prim.get("targets",[]))
            for t in prim.get("targets",[]):
                for semantic,ix in t.items():
                    assert accs[ix]["count"]==count,(path,mi,"wrong morph size",semantic)
                    if accs[ix]["componentType"]==5126:
                        assert all(all(math.isfinite(float(x)) for x in row)
                                   for row in decode(ix)),(path,mi,"nonfinite morph delta")
    print(f"PASS {path.name}: nodes={len(nodes)} meshes={len(meshes)} "
          f"primitives={primitives} baseVertices={vertices} "
          f"morphReferences={morphs} skins={[len(x['joints']) for x in skins]}")

for stem in ("emma-dense-face","emma-no-morph-diagnostic",
             "sophia-dense-face","sophia-no-morph-diagnostic",
             "ethan-mobile"):
    audit(A/(stem+".glb"))
print("GLB structural spec checks PASS; native Filament SIGABRT root cause needs OS tombstone.")
