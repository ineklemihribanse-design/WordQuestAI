#!/usr/bin/env python3
"""Stage only the 33 exact user-approved 512px icons when the original ZIP is in repo.
Never overwrite older premium assets; missing ZIP keeps v182 atlas fully functional.
"""
import hashlib
import re
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/"assets/user_uploads/WordQuestAI_33_Ikon_Android_Guncel.zip"
TARGET=ROOT/"source/app/src/main/assets/approved_icons_v183"
CATALOG=ROOT/"tools/v182/Wq182Icons.kt"
PREFIX="WordQuestAI_Ikon_Paketi_Uygulamaya_Hazir/android/app/src/main/res/drawable-nodpi/"
EXPECTED="6a584b4732ec434d24d202f8457f8835f81d60d978400edabd87000985378627"

def stage() -> bool:
    if not SOURCE.is_file():
        print("INCOMPLETE: original 512px icon pack absent; approved v182 atlas remains")
        return False
    original=SOURCE.read_bytes()
    assert hashlib.sha256(original).hexdigest()==EXPECTED, "Unexpected icon ZIP; do not replace approved set"
    src=CATALOG.read_text("utf-8")
    catalog=src.split("private val names = listOf(",1)[1].split(")",1)[0]
    names=re.findall(r'"(wq_[a-z0-9_]+)"',catalog)
    assert len(names)==33 and len(set(names))==33
    with ZipFile(SOURCE) as z:
        assert z.testzip() is None, "ZIP CRC error"
        available=set(z.namelist())
        for name in names:
            path=PREFIX+name+".webp"
            assert path in available, "Missing approved original: "+name
            raw=z.read(path)
            assert 10000<len(raw)<1_000_000 and raw[:4]==b"RIFF" and raw[8:12]==b"WEBP"
            assert raw[12:16]==b"VP8L" and raw[20]==0x2F, "Expected transparent lossless 512px WebP"
            width=1+(raw[21]|((raw[22]&0x3f)<<8))
            height=1+((raw[22]>>6)|(raw[23]<<2)|((raw[24]&15)<<10))
            assert (width,height)==(512,512), "Unexpected WebP dimensions"
            TARGET.mkdir(parents=True,exist_ok=True)
            (TARGET/(name+".webp")).write_bytes(raw)
    print("PASS: 33 exact original 512px user-approved icons staged without modifying old icons")
    return True

if __name__=="__main__":
    stage()
