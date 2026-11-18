#!/usr/bin/env python3
"""Offline QA polish for Ethan's EXISTING real-human Android GLB.

Only modifies FLOAT32 animation output values in-place (same byte counts).
Talk arm joint rotations are blended toward the actual Idle joint pose to avoid
oversized repetitive arm swings. ARKit mouth target weights are gently lowered;
eyes/blinking, joints, geometry, skinning, license and clip names are preserved.
No native Filament APIs are changed; device visual QA remains mandatory.
"""
from pathlib import Path
import hashlib
import json
import math
import struct

ROOT = Path(__file__).resolve().parents[2]
GLB = ROOT / "source/app/src/main/assets/avatars/ethan-mobile.glb"
REPORT = ROOT / "source/ethan_natural_motion_qa.json"
assert GLB.is_file()
original = GLB.read_bytes()
assert original[:4] == b"glTF"
assert struct.unpack_from("<II", original, 4) == (2, len(original))
j_len, j_kind = struct.unpack_from("<II", original, 12)
assert j_kind == 0x4E4F534A
doc = json.loads(original[20:20 + j_len])
bin_offset = 20 + j_len
bin_size, bin_kind = struct.unpack_from("<II", original, bin_offset)
assert bin_kind == 0x004E4942
start = bin_offset + 8
assert start + bin_size <= len(original)
raw = bytearray(original)
animations = {a.get("name"): a for a in doc.get("animations", [])}
assert len(animations) >= 28 and {"Idle", "Talk", "Think", "Wave"} <= animations.keys()
nodes = doc["nodes"]
accessors = doc["accessors"]
views = doc["bufferViews"]
meshes = doc["meshes"]

def rows(accessor_id, width):
    a = accessors[accessor_id]
    assert a["componentType"] == 5126
    assert a["type"] == ("SCALAR" if width == 1 else "VEC4")
    assert "sparse" not in a and "bufferView" in a
    v = views[a["bufferView"]]
    assert v.get("buffer", 0) == 0
    step = v.get("byteStride", width * 4)
    assert step >= width * 4
    offset = start + v.get("byteOffset", 0) + a.get("byteOffset", 0)
    assert offset + (a["count"] - 1) * step + width * 4 <= start + bin_size
    return [list(struct.unpack_from("<" + "f" * width, raw, offset + i * step))
            for i in range(a["count"])]

def write_rows(accessor_id, values):
    a = accessors[accessor_id]
    width = 1 if a["type"] == "SCALAR" else 4
    assert len(values) == a["count"]
    view = views[a["bufferView"]]
    stride = view.get("byteStride", width * 4)
    at = start + view.get("byteOffset", 0) + a.get("byteOffset", 0)
    for i, row in enumerate(values):
        assert len(row) == width and all(math.isfinite(f) for f in row)
        struct.pack_into("<" + "f" * width, raw, at + i * stride, *row)

def unit(q):
    length = math.sqrt(sum(x*x for x in q))
    assert length > 0.000001 and all(math.isfinite(x) for x in q)
    return [x / length for x in q]

def slerp(a, b, blend):
    a, b = unit(a), unit(b)
    dot = sum(x*y for x,y in zip(a,b))
    if dot < 0:
        b = [-x for x in b]
        dot = -dot
    if dot > 0.9995:
        return unit([x*(1-blend)+y*blend for x,y in zip(a,b)])
    angle = math.acos(max(-1., min(1.,dot)))
    scale = math.sin(angle)
    return unit([x*(math.sin((1-blend)*angle)/scale)+y*(math.sin(blend*angle)/scale)
                 for x,y in zip(a,b)])

def sample(times, values, at, interpolation):
    assert len(times) == len(values) and len(times) > 0
    if at <= times[0]: return values[0]
    if at >= times[-1]: return values[-1]
    for i in range(len(times)-1):
        if times[i] <= at <= times[i+1]:
            if interpolation == "STEP" or times[i+1] == times[i]: return values[i]
            return slerp(values[i], values[i+1], (at-times[i])/(times[i+1]-times[i]))
    return values[-1]

def channels_by_node(clip):
    mapping = {}
    for channel in clip["channels"]:
        dest = channel["target"]
        if dest.get("path") != "rotation": continue
        sampler = clip["samplers"][channel["sampler"]]
        if sampler.get("interpolation","LINEAR") not in ("LINEAR", "STEP"): continue
        times = [x[0] for x in rows(sampler["input"],1)]
        rotations = rows(sampler["output"],4)
        assert len(times) == len(rotations), "No unsupported cubic animation writes"
        mapping[dest["node"]] = (times,rotations,sampler.get("interpolation","LINEAR"),sampler["output"])
    return mapping

idle = channels_by_node(animations["Idle"])
talk = channels_by_node(animations["Talk"])
arm_written = []
head_torso_written = []
for node_id, (talk_times, rotations, _, output_id) in talk.items():
    joint = nodes[node_id].get("name","").lower().replace(" ", "")
    if "armature" in joint: continue
    is_head = any(term in joint for term in ("head", "neck"))
    is_torso = any(term in joint for term in ("spine", "chest", "torso", "abdomen"))
    is_arm = any(term in joint for term in
                 ("shoulder","clavicle","upperarm","lowerarm","forearm","leftarm","rightarm",
                  "arm_l","arm_r","arm.","wrist","hand"))
    if not (is_head or is_torso or is_arm): continue
    if node_id not in idle: continue  # Use real Idle, never invent bone poses.
    idle_times, idle_rotations, idle_interpolation, _ = idle[node_id]
    # Device screen recording: Ethan head leaned too far while his arms were
    # pinned straight downward. Preserve more of his original arm movement,
    # while strongly limiting neck/head roll relative to his natural Idle.
    if is_head:
        talk_strength = 0.25 if "head" in joint else 0.34
    elif is_torso:
        talk_strength = 0.46
    elif "hand" in joint or "wrist" in joint:
        talk_strength = 0.72
    elif any(term in joint for term in ("forearm","lowerarm","elbow")):
        talk_strength = 0.66
    else:
        talk_strength = 0.56
    duration = max(talk_times[-1],0.0001)
    idle_duration = max(idle_times[-1],0.0001)
    updated = []
    for t, position in zip(talk_times, rotations):
        idle_at = min(idle_times[-1],max(idle_times[0],(t/duration)*idle_duration))
        base = sample(idle_times,idle_rotations,idle_at,idle_interpolation)
        updated.append(slerp(base,position,talk_strength))
    write_rows(output_id,updated)
    record = {"bone":joint,"talk_strength":talk_strength,"keyframes":len(updated)}
    if is_head or is_torso:
        head_torso_written.append(record)
    else:
        arm_written.append(record)
assert len(arm_written) >= 2, "No valid named arm joints; inspect GLB before changing its pose"
assert len(head_torso_written) >= 1, "No named head/torso rotations; inspect why Ethan tilts"

# Reduce full-open vowels seen in the user's screenshot while retaining real
# ARKit morph targets. The visible effect is bounded and not phoneme-accurate.
factor = {"A":0.78,"E":0.86,"I":0.85,"O":0.83,"U":0.85,"MBP":0.90}
face_written = []
for clip_name, clip in animations.items():
    if not clip_name.startswith("Teacher_Viseme_"): continue
    kind = clip_name.removeprefix("Teacher_Viseme_").split("_")[0]
    if kind not in factor: continue
    for channel in clip["channels"]:
        target = channel["target"]
        assert target["path"] == "weights"
        node = nodes[target["node"]]
        mesh = meshes[node["mesh"]]
        names = mesh.get("extras", {}).get("targetNames") or []
        count = len(names)
        assert count >= 52
        sampler = clip["samplers"][channel["sampler"]]
        output = sampler["output"]
        weights = rows(output,1)
        assert len(weights) % count == 0
        changes = 0
        for i, weight in enumerate(weights):
            name = names[i % count].lower()
            if name.startswith("jaw") or name.startswith("mouth"):
                before = weight[0]
                weight[0] = before * factor[kind]
                if before != 0.0: changes += 1
        if changes:
            write_rows(output,weights)
            face_written.append(clip_name)
assert len(set(face_written)) >= 12, "Ethan ARKit visemes are missing"

updated = bytes(raw)
assert len(updated) == len(original) and updated[:start] == original[:start]
assert updated != original
GLB.write_bytes(updated)
record = {
    "source":"licensed unchanged Ethan real-human GLB",
    "method":"animation accessor values only; retain original glTF structure",
    "input_sha256":hashlib.sha256(original).hexdigest(),
    "output_sha256":hashlib.sha256(updated).hexdigest(),
    "animated_arm_bones":arm_written,
    "controlled_head_torso_bones":head_torso_written,
    "viseme_clips_softened":sorted(set(face_written)),
    "face_quality":"TTS word-range approximation, NOT phoneme alignment",
    "device_visual_approved":False,
}
REPORT.write_text(json.dumps(record,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("Ethan video QA: balanced",len(arm_written),"arm bones;",len(head_torso_written),"head/torso bones;",len(set(face_written)),"face clips")
print("Unchanged: glTF JSON, skeleton/skin, vertices, animation list, API, app startup")
print("PENDING: real Android device naturalness and TTS synchronized visual QA")
