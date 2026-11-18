#!/usr/bin/env python3
"""Fail-closed glTF2 binary bounds audit for licensed Rocketbox adult humans.

Filament JNI does not always survive malformed bufferView, accessor or texture
inputs on Android. Check the actual model bytes staged into the APK, not the
downloaded source files. This is NOT a device/GPU rendering certification.
"""
from pathlib import Path
from collections import Counter
import json, struct

folder = Path(__file__).resolve().parents[2] / "source/app/src/main/assets/avatars"
COMPONENT_SIZE = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}
COMPONENT_COUNT = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}

for actor in ("emma", "sophia", "ethan"):
    b = (folder / f"{actor}.glb").read_bytes()
    assert b[:4] == b"glTF"
    version, claimed = struct.unpack_from("<II", b, 4)
    assert version == 2 and claimed == len(b), actor
    json_length, json_type = struct.unpack_from("<II", b, 12)
    assert json_type == 0x4E4F534A and 20 + json_length + 8 <= len(b), actor
    gltf = json.loads(b[20:20 + json_length])
    bin_header = 20 + json_length
    bin_length, bin_type = struct.unpack_from("<II", b, bin_header)
    assert bin_type == 0x004E4942, actor
    bin_start = bin_header + 8
    assert bin_start + bin_length <= len(b), actor
    assert not gltf.get("extensionsRequired", []) or (
        "EXT_texture_webp" not in gltf["extensionsRequired"]), actor

    views = gltf.get("bufferViews", [])
    for i, view in enumerate(views):
        assert view.get("buffer", 0) == 0, (actor, "external buffer", i)
        start = view.get("byteOffset", 0)
        size = view.get("byteLength", 0)
        assert 0 <= start and 0 <= size and start + size <= bin_length, (
            actor, "bufferView outside BIN", i, start, size, bin_length)

    accessors = gltf.get("accessors", [])
    for i, accessor in enumerate(accessors):
        assert accessor.get("componentType") in COMPONENT_SIZE, (actor, "component", i)
        assert accessor.get("type") in COMPONENT_COUNT, (actor, "type", i)
        count = accessor.get("count", -1)
        assert 0 <= count <= 100_000_000, (actor, "accessor count", i, count)
        if "bufferView" not in accessor:
            # Sparse/zero-initialized accessors are valid glTF; a subsequent
            # Filament device load test remains required for their behavior.
            continue
        view = views[accessor["bufferView"]]
        element = COMPONENT_SIZE[accessor["componentType"]] * COMPONENT_COUNT[accessor["type"]]
        stride = view.get("byteStride", element)
        assert stride >= element, (actor, "stride", i, stride, element)
        byte_offset = accessor.get("byteOffset", 0)
        required = byte_offset + ((count - 1) * stride + element if count else 0)
        assert byte_offset >= 0 and required <= view["byteLength"], (
            actor, "accessor overread", i, required, view["byteLength"])
        if "sparse" in accessor:
            sparse = accessor["sparse"]
            assert sparse["count"] <= count, (actor, "invalid sparse", i)

    for node_index, node in enumerate(gltf.get("nodes", [])):
        for child in node.get("children", []):
            assert 0 <= child < len(gltf["nodes"]), (actor, "bad node child", node_index)
        if "mesh" in node:
            assert 0 <= node["mesh"] < len(gltf.get("meshes", [])), (actor, "bad mesh", node_index)
    skins = gltf.get("skins", [])
    assert skins and max(len(s["joints"]) for s in skins) <= 256, (actor, "Filament joint cap")
    primitives = [p for m in gltf.get("meshes", []) for p in m.get("primitives", [])]
    assert primitives, (actor, "no renderable mesh")
    for prim in primitives:
        for idx in list(prim.get("attributes", {}).values()) + ([prim["indices"]] if "indices" in prim else []):
            assert 0 <= idx < len(accessors), (actor, "bad primitive accessor", idx)
        assert len(prim.get("targets", [])) <= 256, (actor, "morph target cap")

    images = gltf.get("images", [])
    for i, im in enumerate(images):
        assert im.get("mimeType") in ("image/jpeg", "image/png"), (actor, "texture MIME", i)
        if "bufferView" in im:
            assert im["bufferView"] < len(views), (actor, "image bufferView", i)
    animations = gltf.get("animations", [])
    for animation in animations:
        for sam in animation.get("samplers", []):
            assert 0 <= sam["input"] < len(accessors)
            assert 0 <= sam["output"] < len(accessors)
        for channel in animation.get("channels", []):
            assert 0 <= channel["sampler"] < len(animation["samplers"])
            assert 0 <= channel["target"].get("node", -1) < len(gltf["nodes"])

    print(actor,
          "GLB MiB", round(len(b) / 1048576, 2),
          "meshes", len(gltf.get("meshes", [])),
          "primitives", len(primitives),
          "materials", len(gltf.get("materials", [])),
          "images", len(images),
          "skins", [len(x["joints"]) for x in skins],
          "animations", len(animations),
          "morph max", max(len(p.get("targets", [])) for p in primitives),
          "texture bytes", sum(views[i["bufferView"]]["byteLength"] for i in images if "bufferView" in i),
          "extensions required", gltf.get("extensionsRequired", []))
print("PASS: no out-of-bounds GLB accessors/bufferViews, supported texture payloads")
print("NOTE: Valid glTF bytes alone do NOT certify native renderer stability on Android")
