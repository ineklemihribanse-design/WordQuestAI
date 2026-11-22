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
if ARENA.is_file():
    assert hashlib.sha256(ARENA.read_bytes()).hexdigest() == ARENA_SHA
    with zipfile.ZipFile(ARENA) as z:
        assert z.testzip() is None
        originals = {name: z.read(name) for name in EXPECTED}
else:
    base = ROOT / "assets/user_uploads/arena_v184"
    originals = {name:(base/name).read_bytes() for name in EXPECTED}
sha = {
    "arena_telefon_9x19_5.webp": "cb9cfce13f461d57d459acc7ea5b025ef338b89c4d89a986dafea9adc46ad486",
    "arena_telefon_9x19_5_koyu.webp": "42749638c831d9aa917e4e985b7859b00331113163ac4eebd88301ac9baf943c",
    "arena_tablet_13_16x10.webp": "fe3022d97bcedcc1eb9694cbf4e03eba662304b686faf9fe0eccf25d5bb58218",
    "arena_tablet_13_16x10_koyu.webp": "306c21ecc362d356ffb3493cd730ffe2341f3e82a0c6b25ff6fb805974d8efb7",
}
for name, size in EXPECTED.items():
    data = originals[name]
    assert hashlib.sha256(data).hexdigest() == sha[name]
    assert data[:4] == b"RIFF" and data[8:12] == b"WEBP"
    assert Image.open(io.BytesIO(data)).size == size
    print("PASS Arena",name,size)

for gender in ("Kiz","Erkek"):
    file = ROOT / f"mascot_packs/WordQuestAI_{gender}_Idle_Kupa_v184.zip"
    if not file.is_file():
        # Personal photos and mascot art are intentionally NOT committed to GitHub.
        # Run this complete test with the private integration bundle locally.
        print("INFO: private",gender,"maskot ZIP absent from CI checkout; no private artwork uploaded")
        continue
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
