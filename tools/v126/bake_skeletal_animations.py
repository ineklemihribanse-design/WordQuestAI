"""Generate small, deterministic skeletal idle/talk/listen/think glTF clips.

Only creates bone rotations on existing humanoid skin joints.
No procedural mesh generation, no paid model pipeline and no runtime synthesis.
"""
from __future__ import annotations
import json
import math
import re
import struct

JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942

def pad4(data: bytes, fill=b"\x00"):
    return data + fill * ((-len(data)) % 4)

def normalize(name: str):
    return re.sub(r"[^a-z]", "", name.casefold())

def mul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    q = (aw*bx+ax*bw+ay*bz-az*by,
         aw*by-ax*bz+ay*bw+az*bx,
         aw*bz+ax*by-ay*bx+az*bw,
         aw*bw-ax*bx-ay*by-az*bz)
    norm = math.sqrt(sum(v*v for v in q))
    return tuple(v/norm for v in q)

def rotation(axis, angle):
    angle = math.radians(angle)/2
    s = math.sin(angle)
    return (axis[0]*s,axis[1]*s,axis[2]*s,math.cos(angle))

def bake_clips(blob: bytes, actor: str):
    if blob[:4] != b"glTF":
        raise ValueError("invalid GLB")
    pos=12
    chunks=[]
    while pos<len(blob):
        length, typ = struct.unpack_from("<II",blob,pos)
        pos += 8
        chunks.append((typ,blob[pos:pos+length]))
        pos += length
    gltf = json.loads(next(b for t,b in chunks if t==JSON_CHUNK).decode("utf8"))
    data = bytearray(next((b for t,b in chunks if t==BIN_CHUNK),b""))
    if len(gltf.get("buffers",[])) != 1:
        raise ValueError("Only single-file embedded GLBs can be animated")
    animated_joint_ids = set()
    for skin in gltf.get("skins",[]):
        animated_joint_ids.update(skin.get("joints",[]))
    nodes=gltf.get("nodes",[])
    def find(*candidates):
        for candidate in candidates:
            eligible=[]
            for idx in animated_joint_ids:
                value=normalize(nodes[idx].get("name",""))
                if value == candidate or value.endswith(candidate):
                    eligible.append(idx)
            if eligible:
                return sorted(eligible)[0]
        return None
    head = find("head","headbone","neck","spine2","spine1")
    spine = find("spine","chest","spine1","upperchest")
    left = find("leftupperarm","leftarm","upperarml","upperarmleft")
    right = find("rightupperarm","rightarm","upperarmr","upperarmright")
    jaw = find("jaw","lowerjaw")
    if head is None:
        raise RuntimeError(f"{actor}: not a named humanoid head rig")
    print(f"{actor}: matched skeleton head={nodes[head].get('name')},"
          f" spine={nodes[spine].get('name') if spine is not None else '-'},"
          f" leftArm={nodes[left].get('name') if left is not None else '-'},"
          f" rightArm={nodes[right].get('name') if right is not None else '-'},"
          f" jaw={nodes[jaw].get('name') if jaw is not None else '-'}")
    clips=gltf.setdefault("animations",[])
    views=gltf.setdefault("bufferViews",[])
    accessors=gltf.setdefault("accessors",[])
    def accessor(values, shape, components):
        while len(data)%4:data.append(0)
        start=len(data)
        data.extend(struct.pack("<"+len(values)*"f",*values))
        vid=len(views)
        views.append({"buffer":0,"byteOffset":start,"byteLength":len(values)*4})
        aid=len(accessors)
        item={"bufferView":vid,"byteOffset":0,"componentType":5126,
              "count":len(values)//components,"type":shape}
        if shape=="SCALAR":
            item["min"]=[min(values)];item["max"]=[max(values)]
        accessors.append(item)
        return aid
    # Maintain the source bind/rest quaternion; animation always returns
    # to the bind pose at clip loop boundaries.
    channels={
        "Teacher_Idle": [
            (head,(0,0,1),2.6),
            *(([(spine,(1,0,0),1.2)] if spine is not None else [])),
        ],
        "Teacher_Talk": [
            (head,(1,0,0),5.0),
            *(([(left,(0,0,1),-10)] if left is not None else [])),
            *(([(right,(0,0,1),8)] if right is not None else [])),
            *(([(jaw,(1,0,0),7)] if jaw is not None else [])),
        ],
        "Teacher_Listen": [(head,(0,0,1),4.0)],
        "Teacher_Think": [
            (head,(0,1,0),6.0),
            *(([(spine,(0,0,1),1.8)] if spine is not None else [])),
        ],
    }
    for name,bones in channels.items():
        duration=2.4 if name!="Teacher_Talk" else 1.6
        steps=12 if name=="Teacher_Talk" else 8
        times=[i*duration/steps for i in range(steps+1)]
        time_accessor=accessor(times,"SCALAR",1)
        anim={"name":name,"samplers":[],"channels":[]}
        for joint,axis,amp in bones:
            base=nodes[joint].get("rotation",[0.0,0.0,0.0,1.0])
            # First/last pose are the neutral bind-pose; loops are seamless.
            angles=[amp*math.sin(2*math.pi*i/steps) for i in range(steps+1)]
            values=[]
            for angle in angles:
                values.extend(mul(base,rotation(axis,angle)))
            output_accessor=accessor(values,"VEC4",4)
            sm=len(anim["samplers"])
            anim["samplers"].append({"input":time_accessor,"output":output_accessor,"interpolation":"LINEAR"})
            anim["channels"].append({"sampler":sm,"target":{"node":joint,"path":"rotation"}})
        clips.append(anim)
    gltf["buffers"][0]["byteLength"]=len(data)
    raw_json=pad4(json.dumps(gltf,separators=(",",":"),ensure_ascii=False).encode("utf-8"),b" ")
    raw_bin=pad4(bytes(data))
    length=12+8+len(raw_json)+8+len(raw_bin)
    new=struct.pack("<4sII",b"glTF",2,length)
    new+=struct.pack("<II",len(raw_json),JSON_CHUNK)+raw_json
    new+=struct.pack("<II",len(raw_bin),BIN_CHUNK)+raw_bin
    return new
