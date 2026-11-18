#!/usr/bin/env python3
"""Download and validate pinned CC0 skinned GLB avatars at APK build time.

GitHub's repository contains no copyrighted avatar bundles or paid APIs.
The asset URLs are pinned to an immutable public commit. Model files are
bundled inside the APK, so there is no runtime network connection.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import struct
from urllib.request import Request, urlopen

from bake_skeletal_animations import bake_clips

REPO = Path(__file__).resolve().parents[2]
CONFIG = json.loads((REPO / "tools/v126/avatar_sources.json").read_text(encoding="utf-8"))
OUT = REPO / "source/app/src/main/assets/avatars"
OUT.mkdir(parents=True, exist_ok=True)

def inspect_glb(blob: bytes, label: str) -> dict:
    if len(blob) < 28 or blob[:4] != b"glTF":
        raise RuntimeError(f"{label}: not GLB 2.0")
    version, total = struct.unpack_from("<II", blob, 4)
    if version != 2 or total != len(blob):
        raise RuntimeError(f"{label}: malformed GLB length/version")
    chunk_size, chunk_type = struct.unpack_from("<I4s", blob, 12)
    if chunk_type != b"JSON":
        raise RuntimeError(f"{label}: no JSON chunk")
    data = json.loads(blob[20:20+chunk_size].decode("utf-8"))
    meshes = data.get("meshes", [])
    nodes = data.get("nodes", [])
    skins = data.get("skins", [])
    clips = data.get("animations", [])
    if not meshes or not nodes:
        raise RuntimeError(f"{label}: missing human mesh")
    # Require a skeletal rig, not an imported static/procedural mannequin.
    if not skins or not any(node.get("skin") is not None for node in nodes):
        raise RuntimeError(f"{label}: missing skeletal skin")
    if not clips:
        raise RuntimeError(f"{label}: missing animation clips")
    if not data.get("materials"):
        raise RuntimeError(f"{label}: missing textured/material data")
    names = [clip.get("name", f"Clip {i}") for i,clip in enumerate(clips)]
    return {"meshes":len(meshes),"skins":len(skins),"animations":len(clips),
            "animationNames":names, "materials":len(data["materials"])}

report = {"source_commit":CONFIG["source_commit"], "models":[]}
for spec in CONFIG["models"]:
    url = ("https://raw.githubusercontent.com/trebeljahr/quaternius-showcase/"
           + CONFIG["source_commit"] + "/public/glb/" + spec["source"])
    last = None
    for attempt in range(3):
        try:
            request = Request(url,headers={"User-Agent":"WordQuestAI-AssetVerifier/1.0"})
            with urlopen(request, timeout=75) as handle:
                blob = handle.read(12*1024*1024)
            break
        except Exception as exc:
            last = exc
    else:
        raise RuntimeError(f"Download failed for {spec['target']}: {last}")
    if len(blob) != spec["size"]:
        raise RuntimeError(f"Unexpected model size for {spec['target']}: {len(blob)}")
    # Some publicly redistributed GLB copies retain skin weights but omit the
    # original FBX motion clips. Bake 4 deterministic joint animations into
    # this existing professional humanoid rig; no primitives are generated.
    blob = bake_clips(blob,spec["label"])
    info = inspect_glb(blob,spec["label"])
    (OUT/spec["target"]).write_bytes(blob)
    digest=hashlib.sha256(blob).hexdigest()
    report["models"].append({"label":spec["label"],"file":spec["target"],
                             "sha256":digest,"bytes":len(blob),**info})
    print(f"{spec['label']}: {info['meshes']} meshes, "
          f"{info['skins']} rigs, {info['animations']} animation clips, SHA256 {digest}")
(OUT/"AVATAR_PROVENANCE.json").write_text(
    json.dumps({"license":"CC0-1.0","author":"Quaternius",**report},
               ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("All 3 CC0 skinned humanoid assets verified and packed offline")
