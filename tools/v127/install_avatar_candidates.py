#!/usr/bin/env python3
"""Quality-gated VRoid glTF/VRM 0.x candidate loader for 1.0.27.
This is a prototype pipeline: no public release until actual Android visual QA.
"""
from __future__ import annotations
import hashlib, json, math, struct
from pathlib import Path
from urllib.request import Request, urlopen

BASE=Path(__file__).resolve().parents[2]
CONFIG=json.loads((BASE/"tools/v127/avatar_candidates.json").read_text("utf-8"))
DEST=BASE/"source/app/src/main/assets/avatars"
DEST.mkdir(parents=True,exist_ok=True)
REPORT={"version":"1.0.27-candidate","status":"UNAPPROVED_VISUAL_QA","models":[]}
GLTF_JSON=0x4E4F534A
GLTF_BIN=0x004E4942

def pad(data: bytes, char=b"\x00"):
    return data + char*((-len(data))%4)

def parse_glb(blob,label):
    if len(blob)<32 or blob[:4]!=b"glTF": raise ValueError(f"{label}: not a GLB/VRM")
    ver,total=struct.unpack_from("<II",blob,4)
    if ver!=2 or total!=len(blob):raise ValueError(f"{label}: invalid GLB 2 header")
    chunks=[];at=12
    while at<len(blob):
        length,kind=struct.unpack_from("<II",blob,at);at+=8
        chunks.append((kind,blob[at:at+length]));at+=length
    model=json.loads(next(data for kind,data in chunks if kind==GLTF_JSON).decode("utf-8"))
    binary=next(data for kind,data in chunks if kind==GLTF_BIN)
    if len(model.get("buffers",[]))!=1: raise ValueError(f"{label}: expected one embedded buffer")
    return model,bytearray(binary)

def unit_quat(q):
    l=math.sqrt(sum(n*n for n in q))
    return tuple(n/l for n in q)
def rotate_z(degrees):
    a=math.radians(degrees)/2
    return (0.,0.,math.sin(a),math.cos(a))
def rotate_x(degrees):
    a=math.radians(degrees)/2
    return (math.sin(a),0.,0.,math.cos(a))
def quat_mul(a,b):
    x,y,z,w=a;X,Y,Z,W=b
    return unit_quat((w*X+x*W+y*Z-z*Y,w*Y-x*Z+y*W+z*X,
                      w*Z+x*Y-y*X+z*W,w*W-x*X-y*Y-z*Z))

def animate(gltf,data,label):
    vrm=gltf.get("extensions",{}).get("VRM",{})
    human=vrm.get("humanoid",{}).get("humanBones",[])
    bone={x.get("bone"):x.get("node") for x in human}
    for name in ("head","leftUpperArm","rightUpperArm"):
        if not isinstance(bone.get(name),int):
            raise ValueError(f"{label}: missing rigged bone {name}")
    expressions=vrm.get("blendShapeMaster",{}).get("blendShapeGroups",[])
    groups={}
    for group in expressions:
        key=(group.get("presetName") or group.get("name") or "").strip().lower()
        groups.setdefault(key,[]).extend(group.get("binds",[]))
    if not groups.get("blink") or not any(groups.get(name) for name in ("a","aa")):
        raise ValueError(f"{label}: blink and A-vowel facial expressions required")
    skins=gltf.get("skins",[])
    if not skins or not gltf.get("materials") or not gltf.get("textures"):
        raise ValueError(f"{label}: missing skin, materials or textures")
    nodes=gltf["nodes"]
    views=gltf.setdefault("bufferViews",[])
    accessors=gltf.setdefault("accessors",[])
    # Ensure first played clip is a neutral, arm-lowered full-body stance,
    # rather than returning to GLB authoring T-pose between animation cycles.
    gltf["animations"]=[]
    def add_accessor(values,unit):
        while len(data)%4:data.append(0)
        offset=len(data)
        data.extend(struct.pack("<"+"f"*len(values),*values))
        view=len(views);views.append({"buffer":0,"byteOffset":offset,"byteLength":len(values)*4})
        result={"bufferView":view,"componentType":5126,"count":len(values)//unit,
                "type":"SCALAR" if unit==1 else "VEC4"}
        if unit==1:result["min"]=[min(values)];result["max"]=[max(values)]
        idx=len(accessors);accessors.append(result)
        return idx
    # Collect target index for each face mesh. VRM 0.x binds name morph indices by mesh.
    target_counts={}
    for i,mesh in enumerate(gltf.get("meshes",[])):
        count=len(mesh.get("primitives",[{}])[0].get("targets",[]))
        if count:target_counts[i]=count
    mapping={}
    for preset,binds in groups.items():
        for bind in binds:
            m=bind.get("mesh");idx=bind.get("index")
            if m in target_counts and isinstance(idx,int) and idx<target_counts[m]:
                mapping.setdefault(m,{}).setdefault(preset,[]).append(idx)
    if not any("blink" in group for group in mapping.values()):
        raise ValueError(f"{label}: blink morph target not resolved")
    def profile(kind, time):
        if kind=="idle":
            return {"blink":max(0.,1.-abs(time-.71)/.08)}
        if kind=="talk":
            return {"a":(.18+.62*abs(math.sin(time*math.pi*4))),
                    "o":(.12+.45*abs(math.cos(time*math.pi*3))),
                    "blink":max(0.,1.-abs(time-.84)/.05)}
        if kind=="listen":
            return {"blink":max(0.,1.-abs(time-.64)/.09)}
        if kind=="think":
            return {"blink":max(0.,1.-abs(time-.7)/.09)}
        return {"blink":max(0.,1.-abs(time-.78)/.08)}
    names=[("Teacher_Idle","idle",2.7),
           ("Teacher_Talk","talk",1.8),
           ("Teacher_Listen","listen",2.5),
           ("Teacher_Think","think",2.7),
           ("Teacher_Greet","greet",2.2)]
    for clip_name,kind,duration in names:
        steps=20
        f=[i/steps for i in range(steps+1)]
        times=add_accessor([t*duration for t in f],1)
        clip={"name":clip_name,"channels":[],"samplers":[]}
        def channel(node,path,values,unit):
            output=add_accessor(values,unit)
            sampler=len(clip["samplers"])
            clip["samplers"].append({"input":times,"output":output,"interpolation":"LINEAR"})
            clip["channels"].append({"sampler":sampler,"target":{"node":node,"path":path}})
        # With the arm bones in T-pose, rotate them toward a natural rest position.
        # Both first and last keyframe match the lowered stance, never authoring T.
        for joint_name,offset,axis in [
                ("leftUpperArm",52.,rotate_z),
                ("rightUpperArm",-52.,rotate_z),
                ("head",0.,rotate_x)]:
            index=bone[joint_name]
            base=nodes[index].get("rotation",[0.,0.,0.,1.])
            vals=[]
            for t in f:
                if joint_name=="head":
                    angle=math.sin(math.pi*2*t)*(3.5 if kind=="talk" else 1.)
                else:
                    angle=offset + (math.sin(2*math.pi*t)*(4. if kind=="talk" else 1.5))
                    if kind=="greet" and joint_name=="rightUpperArm":
                        angle=offset+22+11*math.sin(2*math.pi*t*2)
                vals.extend(quat_mul(base,axis(angle)))
            channel(index,"rotation",vals,4)
        # VRM facial shapes -> normal glTF weights animation, supported by Filament.
        for node_idx,node in enumerate(nodes):
            mesh_idx=node.get("mesh")
            if mesh_idx not in mapping:continue
            count=target_counts[mesh_idx]
            values=[]
            for t in f:
                weights=[0.]*count
                choices=profile(kind,t)
                for name,value in choices.items():
                    for ix in mapping[mesh_idx].get(name,[]):
                        weights[ix]=min(1.,max(weights[ix],value))
                values.extend(weights)
            if any(value>0 for value in values):
                channel(node_idx,"weights",values,1)
        gltf["animations"].append(clip)
    gltf["buffers"][0]["byteLength"]=len(data)
    return len(skins),len(target_counts),len(expressions)

def save_glb(gltf,binary):
    json_part=pad(json.dumps(gltf,separators=(",",":"),ensure_ascii=False).encode("utf-8"),b" ")
    bin_part=pad(bytes(binary))
    total=12+8+len(json_part)+8+len(bin_part)
    return (struct.pack("<4sII",b"glTF",2,total) +
            struct.pack("<II",len(json_part),GLTF_JSON)+json_part +
            struct.pack("<II",len(bin_part),GLTF_BIN)+bin_part)

for spec in CONFIG["models"]:
    url=("https://raw.githubusercontent.com/madjin/vrm-samples/" +
         CONFIG["source_commit"] + "/" + spec["source"])
    request=Request(url,headers={"User-Agent":"WordQuestAI-PremiumAvatar-QA/1.0"})
    with urlopen(request,timeout=110) as response:
        blob=response.read(25_000_000)
    if len(blob)!=spec["bytes"]:
        raise ValueError(f'{spec["label"]}: unexpected upstream file size {len(blob)}')
    model,data=parse_glb(blob,spec["label"])
    skins,morph_meshes,expressions=animate(model,data,spec["label"])
    # Ensure fallback PBR materials are usable when proprietary VRM MToon is ignored.
    for mat in model.get("materials",[]):
        pbr=mat.setdefault("pbrMetallicRoughness",{})
        pbr["metallicFactor"]=0.
        pbr["roughnessFactor"]=.88
        if "hair" in mat.get("name","").lower():
            mat["doubleSided"]=True
    output=save_glb(model,data)
    # Check the generated GLB can be reopened before embedding in APK.
    verified,_=parse_glb(output,spec["label"])
    if len(verified["animations"])!=5:raise RuntimeError("bad animation count")
    (DEST/spec["target"]).write_bytes(output)
    REPORT["models"].append({
        "label":spec["label"],"file":spec["target"],"licence":spec["license"],
        "sourceUrl":url,"sha256":hashlib.sha256(output).hexdigest(),
        "size":len(output),"skins":skins,"facialMeshes":morph_meshes,
        "expressionGroups":expressions,"animations":[a["name"] for a in verified["animations"]],
        "meshes":len(model.get("meshes",[])),
        "textures":len(model.get("textures",[])),
    })
    print(f'{spec["label"]}: models={len(model["meshes"])} textures={len(model["textures"])} expressions={expressions} clips=5')
(DEST/"AVATAR_PROVENANCE.json").write_text(
    json.dumps(REPORT,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
print("1.0.27 candidate GLBs packaged; visual approval required before release")
