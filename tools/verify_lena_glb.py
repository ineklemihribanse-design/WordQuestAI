#!/usr/bin/env python3
"""Validate Lena GLB structure and required animation clips without third-party packages.

Usage: python3 tools/verify_lena_glb.py path/to/Lena_Prism3D_AnimationPack_v6.glb
"""
import json
import struct
import sys
from pathlib import Path

REQUIRED = {
    "01_Idle_Natural_Breathing",
    "02_Greeting_Gentle_Wave",
    "03_Talking_Hand_Gestures",
    "04_Listening_Nod",
    "06_Face_Soft_Blink",
    "07_Face_Speech_Cycle",
    "13_Thinking_Softly",
}
def validate(path: Path):
    with path.open("rb") as f:
        header = f.read(12)
        if len(header) != 12:
            raise ValueError("Truncated GLB header")
        magic, version, length = struct.unpack("<4sII", header)
        if magic != b"glTF" or version != 2 or length != path.stat().st_size:
            raise ValueError("Invalid GLB header or length")
        chunk = f.read(8)
        if len(chunk) != 8:
            raise ValueError("Missing JSON chunk")
        json_length, chunk_type = struct.unpack("<I4s", chunk)
        if chunk_type != b"JSON":
            raise ValueError("First chunk is not JSON")
        data = json.loads(f.read(json_length))
    clips = [a.get("name", "") for a in data.get("animations", [])]
    missing = REQUIRED - set(clips)
    if not data.get("skins") or not data.get("meshes"):
        raise ValueError("GLB needs a mesh and skin")
    if missing:
        raise ValueError("Missing clips: " + ", ".join(sorted(missing)))
    for anim in data["animations"]:
        if not anim.get("channels") or not anim.get("samplers"):
            raise ValueError("Empty animation: " + anim.get("name", "<unnamed>"))
    print(f"PASS: {len(clips)} clips, {len(data['skins'])} skins, {len(data['meshes'])} meshes")
    print("NOTE: This does not validate Prisma3D/Android playback or visual deformation.")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: verify_lena_glb.py <model.glb>")
    try:
        validate(Path(sys.argv[1]))
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        sys.exit(f"FAIL: {exc}")
