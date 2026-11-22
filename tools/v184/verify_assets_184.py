#!/usr/bin/env python3
"""No-Android-device verification for v184 game-ready local assets."""
from pathlib import Path
import hashlib, json, zipfile, io, sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ARENA = ROOT / "assets/user_uploads/WordQuestAI_Arena_13inc_Tablet_ve_Telefon.zip"
ARENA_SHA = "80da38acbccf202c68f2f3b84acad93bfbcd9091554c125a26ff4808004b1906"
EXPECTED = {
    "arena_telefon_9x19_5.webp": (1080,2340),
    "arena_telefon_9x19_5_koyu.webp": (1080,2340),
    "arena_tablet_13_16x10.webp": (2560,1600),
    "arena_tablet_13_16x10_koyu.webp": (2560,1600),
}
assert hashlib.sha256(ARENA.read_bytes()).hexdigest() == ARENA_SHA
with zipfile.ZipFile(ARENA) as z:
    assert z.testzip() is None
    for name, size in EXPECTED.items():
        data = z.read(name)
        assert data[:4] == b"RIFF" and data[8:12] == b"WEBP"
        assert Image.open(io.BytesIO(data)).size == size
        print("PASS Arena",name,size)

for gender in ("Kiz","Erkek"):
    file = ROOT / f"mascot_packs/WordQuestAI_{gender}_Idle_Kupa_v184.zip"
    assert file.stat().st_size <= 25 * 1024 * 1024
    with zipfile.ZipFile(file) as z:
        assert z.testzip() is None
        assert set(z.namelist()) == {"manifest.json","images/idle.png","images/win.png"}
        j = json.loads(z.read("manifest.json"))
        assert j["formatVersion"] == 1 and j["packageVersion"] in range(1,1_000_001)
        assert j["animation"] == "static"
        assert j["images"] == {"idle":"images/idle.png","win":"images/win.png"}
        for pose in ("idle","win"):
            b = z.read(f"images/{pose}.png")
            assert b.startswith(b"\x89PNG\r\n\x1a\n")
            im = Image.open(io.BytesIO(b)).convert("RGBA")
            assert im.size == (1024,1536)
            a = im.getchannel("A")
            assert a.getextrema() == (0,255),"Must contain true transparency"
            assert all(a.getpixel(c)==0 for c in [(0,0),(1023,0),(0,1535),(1023,1535)])
            assert a.point(lambda x: 255 if x>10 else 0).getbbox() is not None
            print("PASS",gender,pose,"transparency, crop, dimensions",len(b))

source=(ROOT/"tools/v184/apply_visuals_184.py").read_text("utf-8")
assert "versionCode = 110" in source and 'versionName = "1.0.84"' in source
assert 'win' in source and 'victory=true' in source
assert (ROOT/"tools/v184/Wq184BitmapTrim.kt").is_file()
print("PASS v184 static integration contracts")
