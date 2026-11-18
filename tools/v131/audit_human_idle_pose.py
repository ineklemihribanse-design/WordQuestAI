#!/usr/bin/env python3
"""Audits whether the real glTF Idle clip rotates upper arm/shoulder joints.

A screenshot of bind-pose arms is NOT proof of broken skeleton proportions.
This analysis distinguishes a static arm pose from an animated one; animation
coverage can then be checked on a physical Android device before editing rigs.
"""
from pathlib import Path
import struct,json

ASSETS=Path(__file__).resolve().parents[2]/"source/app/src/main/assets/avatars"
for actor, suffix in (("emma","-mobile-lite.glb"),("sophia","-mobile-lite.glb"),("ethan","-mobile.glb")):
    b=(ASSETS/(actor+suffix)).read_bytes()
    n=struct.unpack_from("<I",b,12)[0]
    doc=json.loads(b[20:20+n])
    nodes=doc.get("nodes",[])
    clips={a.get("name"):a for a in doc.get("animations",[])}
    assert "Idle" in clips and "Talk" in clips and "Wave" in clips
    idle=clips["Idle"]
    targets=[(nodes[c["target"]["node"]].get("name",""),c["target"]["path"])
             for c in idle["channels"] if "node" in c["target"]]
    arm=[(name,path) for name,path in targets
         if path=="rotation" and any(x in name.lower()
             for x in ("shoulder","upperarm","upper_arm","forearm","lowerarm","arm_","arm.","armleft","armright"))]
    print(f"{actor}: Idle channels={len(targets)}, animated shoulder/arm rotations={len(arm)}")
    print("  arm bone names:",[name for name,_ in arm[:16]])
    if not arm:
        print("  WARNING: no named arm rotations in Idle; do NOT claim natural arm pose is visually fixed")
print("QA only: inspection of Idle channels does not replace Android screenshots or rig quality sign-off")
