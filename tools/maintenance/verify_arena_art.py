#!/usr/bin/env python3
"""Validate the exact four shipped Arena background inputs before packaging."""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]/"assets/user_uploads/arena_v184"
for stem,size in (("arena_tablet_13_16x10",(2560,1600)),("arena_telefon_9x19_5",(1080,2340))):
    for suffix in ("","_koyu"):
        path=ROOT/(stem+suffix+".webp")
        assert path.is_file() and 0<path.stat().st_size<=15*1024*1024,path
        with Image.open(path) as im:
            assert im.format=="WEBP" and im.size==size,(path,im.format,im.size)
            im.verify()
        with Image.open(path) as im:
            im.load()
        print("PASS",path.name,size,path.stat().st_size)
