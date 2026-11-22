#!/usr/bin/env python3
"""Stage unchanged user-provided Arena WebP art when the original ZIP is available.

Missing art leaves the previous Arena image intact; it must not count as release-ready.
"""
from pathlib import Path
from zipfile import ZipFile
import shutil

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
            (DEST/name).write_bytes(data)
            print("PASS Arena original bytes:", name, len(data))
    return True

if __name__ == "__main__":
    stage()
