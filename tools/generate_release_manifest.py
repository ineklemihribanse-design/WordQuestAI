#!/usr/bin/env python3
"""Read the Gradle-produced APK metadata and generate public update.json (no private info)."""
import hashlib
import json
from pathlib import Path
import re
import sys

meta_file, apk_file, output_file, notes_file = map(Path, sys.argv[1:5])
meta = json.loads(meta_file.read_text())
assert meta["applicationId"] == "com.kelimebulmacasi.a1.practice.offline101"
item = meta["elements"][0]
code = int(item["versionCode"])
name = str(item["versionName"])
assert re.fullmatch(r"\d+\.\d+\.\d+(?:\.\d+)?(?:-[a-z0-9-]+)?", name)
assert code > 38
apk_bytes = apk_file.read_bytes()
assert len(apk_bytes) > 10_000_000
notes = notes_file.read_text(encoding="utf-8")[:3500]
manifest = {
    "schemaVersion": 1,
    "tag": "v" + name.split("-", 1)[0],
    "versionCode": code,
    "versionName": name,
    "packageName": meta["applicationId"],
    "apkFile": "WordQuestAI.apk",
    "sizeBytes": len(apk_bytes),
    "sha256": hashlib.sha256(apk_bytes).hexdigest(),
    "notes": notes,
}
output_file.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")