#!/usr/bin/env python3
"""Build a real, safely importable WordQuest Mascot Pack v1 ZIP template.
No network, paid service, external image libraries or user data.
"""
import json
import struct
import sys
import zlib
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

PNG_SIG=b"\x89PNG\r\n\x1a\n"

def chunk(kind, data):
    return (struct.pack(">I",len(data))+kind+data+
        struct.pack(">I", zlib.crc32(kind+data)&0xffffffff))

def transparent_png(width=1024,height=1024):
    # An empty, transparent 1024px canvas to REPLACE with personal art.
    raw=b"".join(b"\x00"+b"\x00"*(width*4) for _ in range(height))
    return (PNG_SIG+chunk(b"IHDR",struct.pack(">IIBBBBB",
        width,height,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+
        chunk(b"IEND",b""))

def make(output:Path):
    manifest={
        "formatVersion":1,
        "id":"ornek-maskot",
        "name":"Benim Maskotum",
        "packageVersion":1,
        "animation":"static",
        "images":{"idle":"images/idle.png"}
    }
    info=("WORDQUEST MASCOT PACK v1 — TÜRKÇE ŞABLON\n"
          "1. images/idle.png dosyasını kendi 2D maskot görselinle değiştir.\n"
          "2. PNG veya WebP kullan. En büyük kenar 2048 pikseli aşmasın.\n"
          "3. Şeffaf zemin önerilir; her ifade aynı karakteri göstermeli.\n"
          "4. manifest.json içindeki name ve id alanlarını düzenle.\n"
          "5. İstersen images/happy.png, sad.png, thinking.png, win.png ekle;\n"
          "   her eklediğini manifest.json images alanında belirt.\n"
          "6. İsteğe bağlı kareler v1 deposunda doğrulanır, animasyon oynatımı\n"
          "   ilk uygulama sürümünde tüm ekranlarda garanti edilmez.\n"
          "7. ZIP kökünde manifest.json ve images klasörü bulunmalı.\n"
          "8. Bu örnek boş, şeffaf görsel içerir; kullanmadan önce değiştir.\n"
          "9. Paket cihazda yerel saklanır, kendiliğinden buluta yüklenmez.\n")
    output.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(output,"w",ZIP_DEFLATED,compresslevel=9) as archive:
        archive.writestr("manifest.json",json.dumps(
            manifest,ensure_ascii=False,indent=2)+"\n")
        archive.writestr("images/idle.png",transparent_png())
        archive.writestr("README_TR.txt",info)
    with ZipFile(output) as archive:
        assert sorted(archive.namelist()) == [
            "README_TR.txt","images/idle.png","manifest.json"]
        assert json.loads(archive.read("manifest.json"))["formatVersion"]==1
    print("PASS: template",output,output.stat().st_size,"bytes")

if __name__=="__main__":
    output=Path(sys.argv[1]) if len(sys.argv)>1 else Path(
        "WordQuest_Mascot_Template_v1.zip")
    make(output)
