#!/usr/bin/env python3
"""Stage the user's EXACT chairs_and_window_set_-_game_ready.glb in WordQuest.

No substitute/generated chair, no remote download. CI refuses publication if
the user's original 19MB model has not been placed in the repository.
"""
from pathlib import Path
import hashlib, json, shutil, struct, sys

ROOT = Path(__file__).resolve().parents[2]
ORIGINAL = ROOT / "assets/studio/chairs_and_window_set_-_game_ready.glb"
DEST = ROOT / "source/app/src/main/assets/studio/chairs_and_window_set_-_game_ready.glb"
EXPECTED_SIZE = 19765636
EXPECTED_MESHES = ("Asset_Asset_0", "Plane_Plane_0")
SOURCE = "https://sketchfab.com/3d-models/chairs-and-window-set-game-ready-2dac4adc767c41bbacdd26bff649d739"

def verify(data: bytes) -> dict:
    assert len(data) == EXPECTED_SIZE, ("Not the uploaded original GLB", len(data))
    magic, version, length = struct.unpack_from("<4sII", data)
    assert magic == b"glTF" and version == 2 and length == len(data)
    size, chunk = struct.unpack_from("<I4s", data, 12)
    assert chunk == b"JSON"
    gltf = json.loads(data[20:20 + size])
    assert tuple(x.get("name") for x in gltf.get("meshes", [])) == EXPECTED_MESHES
    assert len(gltf.get("images", [])) == 4
    assert len(gltf.get("nodes", [])) == 7
    extras = gltf["asset"]["extras"]
    assert extras["license"].startswith("CC-BY-4.0")
    assert extras["source"] == SOURCE
    assert extras["title"] == "Chairs and Window Set - Game Ready"
    return {"title": extras["title"], "author": extras["author"],
            "license": extras["license"], "source": extras["source"],
            "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
            "meshNames": list(EXPECTED_MESHES),
            "note": "The EXACT provided glTF binary is copied unchanged."}

def main():
    if not ORIGINAL.is_file():
        raise SystemExit("BLOCKED: upload the ORIGINAL user-provided GLB to " +
                         str(ORIGINAL.relative_to(ROOT)) +
                         ". Do not replace it with a generated chair or publish v1.0.70.")
    data = ORIGINAL.read_bytes()
    metadata = verify(data)
    DEST.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ORIGINAL, DEST)
    assert DEST.read_bytes() == data
    (DEST.parent / "chairs_and_window_set_provenance.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Verified and staged original supplied chair/window GLB:",
          metadata["sha256"], "bytes", len(data))

if __name__ == "__main__":
    main()
