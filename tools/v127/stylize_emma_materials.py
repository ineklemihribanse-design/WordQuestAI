#!/usr/bin/env python3
"""Soft, editable adult-human material pass for Emma's *real* GLB, not a 2D render.

Preserve skeletal animations, blendshapes, vertex weights and geometry.
Soften skin pore textures slightly, retain hair/eyes, and use natural PBR values.
Run after 'normalize_rocketbox_textures.py' so images use PNG/JPEG only.
Compare visually; this never declares a model premium or approves release.
"""
from pathlib import Path
from io import BytesIO
import sys, struct, json, hashlib
from PIL import Image, ImageEnhance, ImageFilter

def chunks(payload):
    assert payload[:4]==b"glTF"
    ver,size=struct.unpack_from("<II",payload,4)
    assert ver==2 and size==len(payload)
    n=len(payload);off=12;parts=[]
    while off<n:
        size,tag=struct.unpack_from("<II",payload,off);off+=8
        assert off+size<=n and size%4==0
        parts.append((tag,payload[off:off+size]));off+=size
    return parts

def transform(path,dest):
    payload=Path(path).read_bytes()
    parts=chunks(payload)
    scene=json.loads(next(data for tag,data in parts if tag==0x4E4F534A).decode("utf-8"))
    binary=next(data for tag,data in parts if tag==0x004E4942)
    assert "EXT_texture_webp" not in scene.get("extensionsUsed",[])
    base_skins=len(scene["skins"])
    base_animation_names=[a.get("name") for a in scene["animations"]]
    base_morphs=sum(len(p.get("targets",[])) for m in scene["meshes"] for p in m["primitives"])
    # Materials actually used by visible meshes only.
    mesh_ids={n["mesh"] for n in scene["nodes"] if "mesh" in n}
    visible_materials={
        p.get("material") for m in mesh_ids
        for p in scene["meshes"][m]["primitives"] if "material" in p
    }
    # Identify texture index of face and hair from material names, not image filename:
    face_images=set();hair_images=set()
    def image_id(texture_index):
        return scene["textures"][texture_index].get("source")
    for i in visible_materials:
        material=scene["materials"][i]
        name=material.get("name","").lower()
        pbr=material.setdefault("pbrMetallicRoughness",{})
        pbr["metallicFactor"]=0.0
        if any(term in name for term in ("skin","face","body","head")):
            pbr["roughnessFactor"]=0.86
            index=pbr.get("baseColorTexture",{}).get("index")
            if isinstance(index,int):
                face_images.add(image_id(index))
        elif "hair" in name:
            pbr["roughnessFactor"]=0.78
            index=pbr.get("baseColorTexture",{}).get("index")
            if isinstance(index,int):
                hair_images.add(image_id(index))
        elif any(term in name for term in ("eye","iris","cornea")):
            pbr["roughnessFactor"]=0.3
        else:
            pbr["roughnessFactor"]=0.89
        # Prevent an exaggerated plastic sheen on skin and clothing.
        if material.get("alphaMode")=="MASK":
            material["alphaCutoff"]=0.45

    if not face_images:
        # Some source conversions have neutral material names; retain source
        # textures unmodified rather than guess and damage eye or hair.
        print("NOTE: no uniquely-named skin diffuse texture; adjusting PBR only")
    views=scene["bufferViews"]
    images={im["bufferView"]:i for i,im in enumerate(scene.get("images",[]))
            if "bufferView" in im}
    output=bytearray()
    softened=[]
    for idx,view in enumerate(views):
        chunk=binary[view.get("byteOffset",0):view.get("byteOffset",0)+view["byteLength"]]
        if idx in images and images[idx] in face_images-hair_images:
            im_id=images[idx]
            image=scene["images"][im_id]
            raw=Image.open(BytesIO(chunk))
            raw.load()
            if raw.width>200 and raw.height>200:
                # Keep pores but reduce overly photo-realistic noise. Eyes/mouth
                # materials stay sharp; hair retains alpha and strand detail.
                if raw.mode=="RGBA":
                    rgb=raw.convert("RGB")
                    alpha=raw.getchannel("A")
                else:
                    rgb=raw.convert("RGB")
                    alpha=None
                filtered=rgb.filter(ImageFilter.GaussianBlur(radius=1.65))
                rgb=Image.blend(rgb,filtered,0.55)
                rgb=ImageEnhance.Color(rgb).enhance(1.04)
                rgb=ImageEnhance.Contrast(rgb).enhance(0.96)
                rgb=ImageEnhance.Brightness(rgb).enhance(1.02)
                buff=BytesIO()
                if alpha is not None:
                    rgb.putalpha(alpha)
                    rgb.save(buff,format="PNG",optimize=True)
                    image["mimeType"]="image/png"
                else:
                    rgb.save(buff,format="JPEG",quality=92,subsampling=0,optimize=True)
                    image["mimeType"]="image/jpeg"
                chunk=buff.getvalue()
                softened.append(im_id)
        output.extend(b"\0"*((-len(output))%4))
        view["byteOffset"]=len(output)
        view["byteLength"]=len(chunk)
        output.extend(chunk)
    output.extend(b"\0"*((-len(output))%4))
    scene["buffers"][0]["byteLength"]=len(output)
    text=json.dumps(scene,separators=(",",":"),ensure_ascii=False).encode("utf-8")
    text+=b" "*((-len(text))%4)
    dest_payload=(struct.pack("<4sII",b"glTF",2,12+8+len(text)+8+len(output))
                +struct.pack("<II",len(text),0x4E4F534A)+text
                +struct.pack("<II",len(output),0x004E4942)+output)
    reread=chunks(dest_payload)
    new=json.loads(next(data for tag,data in reread if tag==0x4E4F534A).decode("utf-8"))
    assert len(new["skins"])==base_skins
    assert [a.get("name") for a in new["animations"]]==base_animation_names
    assert sum(len(p.get("targets",[])) for m in new["meshes"] for p in m["primitives"])==base_morphs
    Path(dest).write_bytes(dest_payload)
    print(json.dumps({"source":str(path),"output":str(dest),"bytes":len(dest_payload),
        "sha256":hashlib.sha256(dest_payload).hexdigest(),"skinImagesSoftened":softened,
        "visibleMaterials":len(visible_materials),"animations":len(base_animation_names),
        "morphTargets":base_morphs},indent=2))

if __name__=="__main__":
    if len(sys.argv)!=3:
        raise SystemExit("Usage: stylize_emma_materials.py input-mobile.glb output-stylized.glb")
    transform(sys.argv[1],sys.argv[2])
