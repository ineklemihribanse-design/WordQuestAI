#!/usr/bin/env python3
"""Validate exact published manifest+APK+delta; replay never creates release assets."""
import hashlib,json,os
from pathlib import Path
d=Path(os.environ["WQ_RELEASE_DIR"])
def sha(p):
    with p.open("rb") as f:return hashlib.file_digest(f,"sha256").hexdigest()
old=json.loads((d/"base/update.json").read_text())
new=json.loads((d/"new/update.json").read_text())
for m,p,version,code,digest in ((old,d/"old.apk","WQ_BASE_VERSION","WQ_BASE_CODE","WQ_BASE_SHA"),(new,d/"target.apk","WQ_VERSION","WQ_TARGET_CODE","WQ_TARGET_SHA")):
    assert m["schemaVersion"]==1 and m["packageName"]=="com.kelimebulmacasi.a1.practice.fresh"
    assert m["versionName"]==os.environ[version] and m["tag"]=="v"+os.environ[version]
    assert m["versionCode"]==int(os.environ[code]) and m["apkFile"]=="WordQuestAI.apk"
    assert m["sizeBytes"]==p.stat().st_size and m["sha256"]==sha(p)==os.environ[digest]
matches=[p for p in new["deltas"] if p["fromVersionCode"]==old["versionCode"] and p["toVersionCode"]==new["versionCode"]]
assert len(matches)==1
p=matches[0]
assert p["file"]==f'WordQuestAI-{old["versionCode"]}-to-{new["versionCode"]}.wqd.gz'
assert (p["baseSizeBytes"],p["baseSha256"])==(old["sizeBytes"],old["sha256"])
assert (p["targetSizeBytes"],p["targetSha256"])==(new["sizeBytes"],new["sha256"])
assert p["sizeBytes"]==(d/"patch.gz").stat().st_size and p["sha256"]==sha(d/"patch.gz")==os.environ["WQ_DELTA_SHA"]
(d/"report.json").write_text(json.dumps({"eligible":True,"delta":p}))
print("PASS: pinned published APK and delta bytes match both public manifests")
