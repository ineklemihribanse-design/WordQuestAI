#!/usr/bin/env python3
"""v1.0.31: make additional female low-memory GLB variants without degrading rig/face.

The renderer's final CPU/GPU allocations are driver-specific, so these files
are a conservative fallback, not proof that an Android native crash is fixed.
Original GLBs and 1.0.30 -mobile.glb remain untouched.

Run AFTER tools/v130/build_android_mobile_glb.py in the reconstructed source.
"""
from __future__ import annotations
import json
import struct
from io import BytesIO
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
DIR = ROOT / "source/app/src/main/assets/avatars"
JSON = 0x4E4F534A
BIN = 0x004E4942

def parse(raw: bytes):
    assert raw[:4] == b"glTF", "GLB signature"
    version, total = struct.unpack_from("<II", raw, 4)
    assert version == 2 and total == len(raw), "GLB header"
    jslen, jstag = struct.unpack_from("<II", raw, 12)
    assert jstag == JSON
    document = json.loads(raw[20:20 + jslen])
    offset = 20 + jslen
    n, kind = struct.unpack_from("<II", raw, offset)
    assert kind == BIN and offset + 8 + n <= len(raw), "GLB binary chunk"
    return document, raw[offset + 8:offset + 8 + n]

def compact(actor: str):
    source = DIR / (actor + "-mobile.glb")
    dest = DIR / (actor + "-mobile-lite.glb")
    original = source.read_bytes()
    doc, old_binary = parse(original)
    before = json.loads(json.dumps(doc))
    views = doc["bufferViews"]
    image_views = {image["bufferView"]: i for i, image in enumerate(doc["images"])}
    assert len(image_views) == len(doc["images"]), "Unsupported shared/external image view"
    accessor_views = {a["bufferView"] for a in doc["accessors"] if "bufferView" in a}
    assert not (accessor_views & image_views.keys()), "Geometry/image views must not alias"

    out = bytearray()
    original_texel_count = 0
    lite_texel_count = 0
    for i, view in enumerate(views):
        offset = view.get("byteOffset", 0)
        data = old_binary[offset:offset + view["byteLength"]]
        assert len(data) == view["byteLength"]
        if i in image_views:
            img = doc["images"][image_views[i]]
            assert img.get("mimeType") in ("image/png", "image/jpeg")
            pic = Image.open(BytesIO(data))
            pic.load()
            original_texel_count += pic.width * pic.height
            name = (img.get("name") or "").lower()
            is_face = any(token in name for token in ("head", "face", "eye"))
            cap = 1024 if is_face else 384
            if max(pic.size) > cap:
                pic.thumbnail((cap, cap), Image.Resampling.LANCZOS)
            lite_texel_count += pic.width * pic.height
            output = BytesIO()
            if img["mimeType"] == "image/png":
                pic.save(output, format="PNG", optimize=True)
            else:
                pic.convert("RGB").save(output, format="JPEG", quality=76,
                                       optimize=True, subsampling=0)
            data = output.getvalue()
        out.extend(b"\0" * ((-len(out)) % 4))
        view["byteOffset"] = len(out)
        view["byteLength"] = len(data)
        out.extend(data)

    doc["buffers"][0]["byteLength"] = len(out)
    encoded = json.dumps(doc, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    encoded += b" " * ((-len(encoded)) % 4)
    out.extend(b"\0" * ((-len(out)) % 4))
    packed = (struct.pack("<4sII", b"glTF", 2, 12 + 8 + len(encoded) + 8 + len(out)) +
              struct.pack("<II", len(encoded), JSON) + encoded +
              struct.pack("<II", len(out), BIN) + out)
    assert len(packed) < len(original), "Lite GLB must reduce encoded asset size"

    after, _ = parse(packed)
    # Critical: morphs, facial targets, bones, animation sampler indices
    # and materials must remain *byte-for-byte identical in glTF JSON*.
    for key in ("nodes", "meshes", "skins", "animations", "accessors",
                "materials", "textures", "scenes"):
        assert after.get(key) == before.get(key), (actor, "changed", key)
    assert len(after.get("skins", [])) >= 1
    assert len(after.get("animations", [])) == 28
    target_counts = [len(p.get("targets", [])) for m in after["meshes"] for p in m.get("primitives", [])]
    assert 52 in target_counts, (actor, "ARKit face morphs missing")
    dest.write_bytes(packed)
    print(f"{actor}: mobile {len(original)/1048576:.2f} MiB -> lite {len(packed)/1048576:.2f} MiB; "
          f"texture texels {original_texel_count:,} -> {lite_texel_count:,}; 80-joint rig + 28 animations kept")

if __name__ == "__main__":
    for name in ("emma", "sophia"):
        compact(name)
