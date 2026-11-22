#!/usr/bin/env python3
"""Stage unchanged user-provided Arena WebP art when the original ZIP is available.

Missing art leaves the previous Arena image intact; it must not count as release-ready.
"""
from pathlib import Path
from zipfile import ZipFile
import shutil
import hashlib

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT/"assets/user_uploads/WordQuestAI_Arena_13inc_Tablet_ve_Telefon.zip"
DEST = ROOT/"source/app/src/main/assets/arena_v183"
NAMES = [
    "arena_tablet_13_16x10.webp",
    "arena_tablet_13_16x10_koyu.webp",
    "arena_telefon_9x19_5.webp",
    "arena_telefon_9x19_5_koyu.webp",
]

def stage() -> bool:
    if not SOURCE.is_file():
        print("INCOMPLETE: exact Arena ZIP absent; existing premium Arena art retained")
        return False
    with ZipFile(SOURCE) as z:
        entries = [e for e in z.infolist() if not e.is_dir()]
        assert hashlib.sha256(SOURCE.read_bytes()).hexdigest() == (
            "80da38acbccf202c68f2f3b84acad93bfbcd9091554c125a26ff4808004b1906"
        ), "Arena ZIP differs from user's original approved upload"
        assert len(entries) <= 12, "Arena ZIP unexpected file count"
        assert sum(e.file_size for e in entries) <= 90*1024*1024
        names = {e.filename for e in entries}
        assert len(names) == len(entries), "Repeated ZIP paths"
        assert all(".." not in n and not n.startswith("/") and "\\" not in n
                   for n in names), "Unsafe archive paths"
        by_name = {name: next((e for e in entries
            if e.filename == name), None) for name in NAMES}
        assert all(by_name.values()), "Missing original WebP images"
        assert z.testzip() is None, "Arena ZIP CRC failed"
        DEST.mkdir(parents=True, exist_ok=True)
        for name, entry in by_name.items():
            data = z.read(entry)
            assert 10000 < len(data) < 15*1024*1024
            assert data[:4] == b"RIFF" and data[8:12] == b"WEBP"
            assert data[12:16] == b"VP8 " and data[23:26] == b"\x9d\x01\x2a"
            width=int.from_bytes(data[26:28],"little")&0x3fff
            height=int.from_bytes(data[28:30],"little")&0x3fff
            assert (width,height) == ((2560,1600) if name.startswith("arena_tablet")
                                      else (1080,2340)), "Arena art dimensions differ"
            (DEST/name).write_bytes(data)
            print("PASS Arena original bytes:", name, len(data))
    return True

if __name__ == "__main__":
    stage()
