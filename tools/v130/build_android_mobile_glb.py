#!/usr/bin/env python3
"""Create compact, fully rigged Android GLB variants without modifying originals.

Only embedded PNG/JPEG image bufferViews are recompressed/downscaled. Geometry,
ARKit blendshape accessors, skeletons, materials, 28 animation clips, and
licensing metadata are unchanged. Used by the isolated real-human QA viewer.

This may improve GPU memory pressure, but it is not a proof against native
Filament or device driver faults. Never replace the original .glb assets.
"""
import json
import struct
from io import BytesIO
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "source/app/src/main/assets/avatars"
JSON = 0x4E4F534A
BIN = 0x004E4942


def make_mobile(actor: str) -> None:
    source = ASSETS / (actor + ".glb")
    target = ASSETS / (actor + "-mobile.glb")
    raw = source.read_bytes()
    assert raw[:4] == b"glTF"
    version, length = struct.unpack_from("<II", raw, 4)
    assert version == 2 and length == len(raw)
    json_len, json_kind = struct.unpack_from("<II", raw, 12)
    assert json_kind == JSON
    doc = json.loads(raw[20:20 + json_len])
    pos = 20 + json_len
    bin_len, bin_kind = struct.unpack_from("<II", raw, pos)
    assert bin_kind == BIN
    old_bin = raw[pos+8:pos+8+bin_len]
    views = doc["bufferViews"]
    images = doc.get("images", [])
    image_by_view = {i["bufferView"]: idx for idx,i in enumerate(images) if "bufferView" in i}
    assert len(image_by_view) == len(images), "Unexpected external or shared image buffers"
    accessor_views = {a["bufferView"] for a in doc.get("accessors", []) if "bufferView" in a}
    assert not accessor_views.intersection(image_by_view), "Image buffer also used as geometry"
    new_bin = bytearray()
    converted = 0
    saved = 0
    for index, view in enumerate(views):
        original = old_bin[view.get("byteOffset", 0):view.get("byteOffset", 0)+view["byteLength"]]
        assert len(original) == view["byteLength"]
        data = original
        if index in image_by_view:
            im = images[image_by_view[index]]
            assert im.get("mimeType") in ("image/png", "image/jpeg"), (actor, im)
            name = (im.get("name") or "").lower()
            image = Image.open(BytesIO(original))
            image.load()
            # Keep face close-ups readable, reduce other textures first.
            cap = 1024 if "head" in name or "face" in name else 512
            if max(image.size) > cap:
                image.thumbnail((cap, cap), Image.Resampling.LANCZOS)
            output = BytesIO()
            if im["mimeType"] == "image/png":
                # Preserve alpha/normal map channels; do not convert to RGB JPEG.
                image.save(output, format="PNG", optimize=True)
            else:
                image.convert("RGB").save(output, format="JPEG", quality=80,
                                          optimize=True, subsampling=0)
            data = output.getvalue()
            converted += 1
            saved += len(original) - len(data)
        new_bin.extend(b"\x00"*((-len(new_bin))%4))
        view["byteOffset"] = len(new_bin)
        view["byteLength"] = len(data)
        new_bin.extend(data)
    assert converted >= 10, (actor, converted)
    assert saved > 0, (actor, saved)
    doc["buffers"][0]["byteLength"] = len(new_bin)
    json_bytes = json.dumps(doc, separators=(",",":"), ensure_ascii=False).encode("utf-8")
    json_bytes += b" " * ((-len(json_bytes))%4)
    new_bin.extend(b"\x00"*((-len(new_bin))%4))
    packed = (
        struct.pack("<4sII", b"glTF", 2, 12 + 8 + len(json_bytes) + 8 + len(new_bin))
        + struct.pack("<II", len(json_bytes), JSON) + json_bytes
        + struct.pack("<II", len(new_bin), BIN) + new_bin
    )
    assert len(packed) < len(raw), (actor, len(packed), len(raw))
    after = json.loads(packed[20:20+len(json_bytes)])
    original_doc = json.loads(raw[20:20+json_len])
    # Preserve GLTF structure and animation indices exactly. Only image bytes
    # and view byte ranges may change, not the underlying skeleton/face data.
    for key in ("nodes", "meshes", "skins", "animations", "materials",
                "accessors", "textures", "scenes"):
        assert after.get(key) == original_doc.get(key), (actor, key)
    assert len(after["images"]) == len(original_doc["images"])
    target.write_bytes(packed)
    print(actor, "Android real-human GLB MiB",
          round(len(raw)/1048576,2), "->", round(len(packed)/1048576,2),
          "images", converted)


if __name__ == "__main__":
    for teacher in ("emma", "sophia", "ethan"):
        make_mobile(teacher)
