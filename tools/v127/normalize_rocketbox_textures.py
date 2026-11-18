#!/usr/bin/env python3
"""Convert rigged Rocketbox GLB from required EXT_texture_webp to core PNG/JPEG.

Filament / older Android glTF loaders might not understand EXT_texture_webp.
Keep head materials high-resolution for face zoom, downscale other textures,
replace texture data of unused outfit parts with 1x1 PNG placeholders.
Repack bufferViews without changing any geometry, joint or animation indices.
"""
from pathlib import Path
from io import BytesIO
from PIL import Image
import struct,json,hashlib,sys

def convert(src,dest):
    src,dest=Path(src),Path(dest)
    raw=src.read_bytes()
    assert raw[:4]==b"glTF"
    version,total=struct.unpack_from("<II",raw,4)
    assert version==2 and total==len(raw)
    at=12
    chunks=[]
    while at<len(raw):
        length,kind=struct.unpack_from("<II",raw,at)
        at+=8
        chunks.append((kind,raw[at:at+length]))
        at+=length
    model=json.loads(next(data for kind,data in chunks if kind==0x4E4F534A).decode("utf-8"))
    bin_data=next(data for kind,data in chunks if kind==0x004E4942)
    if "EXT_texture_webp" not in model.get("extensionsUsed",[]):
        raise ValueError("Expected source GLB to require EXT_texture_webp")
    # Find textures actually referenced by character nodes with meshes.
    used_meshes={node["mesh"] for node in model["nodes"] if "mesh" in node}
    used_materials={p["material"] for m in used_meshes
                    for p in model["meshes"][m]["primitives"] if "material" in p}
    used_textures=set()
    def collect(value):
        if isinstance(value,dict):
            for name,part in value.items():
                if name.endswith("Texture") and isinstance(part,dict) and isinstance(part.get("index"),int):
                    used_textures.add(part["index"])
                collect(part)
        elif isinstance(value,list):
            for part in value:collect(part)
    for i in used_materials:collect(model["materials"][i])
    used_images={
        model["textures"][i].get("extensions",{}).get("EXT_texture_webp",{}).get(
            "source",model["textures"][i].get("source"))
        for i in used_textures
    }
    original_views=model["bufferViews"]
    image_by_view={im["bufferView"]:i for i,im in enumerate(model.get("images",[]))
                   if im.get("mimeType")=="image/webp"}
    new_bin=bytearray()
    count=0
    for view_index,view in enumerate(original_views):
        offset=view.get("byteOffset",0)
        content=bin_data[offset:offset+view["byteLength"]]
        assert len(content)==view["byteLength"]
        if view_index in image_by_view:
            img_index=image_by_view[view_index]
            item=model["images"][img_index]
            name=(item.get("name") or "").lower()
            if img_index not in used_images:
                image=Image.new("RGB",(1,1),(255,255,255))
            else:
                image=Image.open(BytesIO(content))
                image.load()
                max_px=2048 if "head_color" in name or "head_normal" in name else 1024
                if max(image.size)>max_px:
                    image.thumbnail((max_px,max_px),Image.Resampling.LANCZOS)
            result=BytesIO()
            if image.mode=="RGBA" or any(p in name for p in ("normal","rough","opacity")):
                image.save(result,format="PNG",optimize=True)
                item["mimeType"]="image/png"
            else:
                image.convert("RGB").save(result,format="JPEG",quality=88,subsampling=0,optimize=True)
                item["mimeType"]="image/jpeg"
            content=result.getvalue()
            count+=1
        new_bin.extend(b"\0"*((-len(new_bin))%4))
        view["byteOffset"]=len(new_bin)
        view["byteLength"]=len(content)
        new_bin.extend(content)
    for texture in model.get("textures",[]):
        ext=texture.get("extensions",{}).pop("EXT_texture_webp",None)
        if ext:texture["source"]=ext["source"]
        if not texture.get("extensions"):texture.pop("extensions",None)
    model["extensionsUsed"]=[x for x in model.get("extensionsUsed",[]) if x!="EXT_texture_webp"]
    model["extensionsRequired"]=[x for x in model.get("extensionsRequired",[]) if x!="EXT_texture_webp"]
    model["buffers"][0]["byteLength"]=len(new_bin)
    json_blob=json.dumps(model,ensure_ascii=False,separators=(",",":")).encode("utf-8")
    json_blob+=b" "*((-len(json_blob))%4)
    new_bin.extend(b"\0"*((-len(new_bin))%4))
    result=(
        struct.pack("<4sII",b"glTF",2,12+8+len(json_blob)+8+len(new_bin))+
        struct.pack("<II",len(json_blob),0x4E4F534A)+json_blob+
        struct.pack("<II",len(new_bin),0x004E4942)+new_bin
    )
    assert struct.unpack_from("<I",result,8)[0]==len(result)
    new_model=json.loads(result[20:20+len(json_blob)].decode("utf-8"))
    assert "EXT_texture_webp" not in new_model.get("extensionsUsed",[])
    assert len(new_model.get("skins",[]))==len(model.get("skins",[]))
    assert len(new_model.get("animations",[]))==len(model.get("animations",[]))
    dest.write_bytes(result)
    print("Core glTF image codecs:",count,"active image count:",len(used_images),
          "new size:",len(result),"sha256:",hashlib.sha256(result).hexdigest())

if __name__=="__main__":
    if len(sys.argv)!=3:raise SystemExit("Usage: normalize_rocketbox_textures.py input.glb output.glb")
    convert(sys.argv[1],sys.argv[2])
