#!/usr/bin/env python3
"""Temporarily put Emma/Sophia outside Android APK; NEVER delete their sources.

This is intentionally the LAST staging step, after previous v127..v134
validation. Archived human GLBs are preserved in a separately uploaded GitHub
Actions artifact AND source candidates can be fully reproduced using retained
repository scripts. Ethan is the only human GLB packaged for active rendering.

Run this AFTER all prior QA scripts that still audit female models and BEFORE
Gradle assembles the APK. Do not modify stored chats, notes or teacher data.
"""
from pathlib import Path
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "source/app/src/main/assets/avatars"
ARCH = ROOT / "source/archived-teachers"
ARCH.mkdir(parents=True, exist_ok=True)
assert (ASSETS/"ethan-mobile.glb").is_file()
assert (ASSETS/"ethan.glb").is_file()

provenance = ASSETS/"AVATAR_PROVENANCE.json"
lipmeta = ASSETS/"AVATAR_LIPSYNC_METADATA.json"
for original in (provenance,lipmeta):
    assert original.is_file(),original
    shutil.copy2(original,ARCH/original.name)

archived = []
for slug in ("emma","sophia"):
    dest = ARCH/slug
    dest.mkdir(parents=True, exist_ok=True)
    candidate = ROOT/"candidate-human-teachers"/slug
    if candidate.is_dir():
        shutil.copytree(candidate, dest/"source", dirs_exist_ok=True)
    files = sorted(ASSETS.glob(slug+"*"))
    assert files, "Do not discard an avatar that was not backed up: "+slug
    assert any(p.suffix==".glb" for p in files)
    for file in files:
        if not file.is_file():continue
        b=file.read_bytes()
        digest=hashlib.sha256(b).hexdigest()
        shutil.move(str(file),str(dest/file.name))
        assert (dest/file.name).is_file()
        assert hashlib.sha256((dest/file.name).read_bytes()).hexdigest()==digest
        archived.append({"actor":slug,"name":file.name,"sha256":digest,"bytes":len(b)})
    assert not list(ASSETS.glob(slug+"*.glb")),"Female GLBs remain in active APK"

# Only Ethan appears in the APK's active model metadata. The complete originals
# remain in archived-teachers and can be restored without recollecting data.
for meta in (provenance,lipmeta):
    value=json.loads(meta.read_text("utf-8"))
    field="models" if meta==provenance else "actors"
    assert {x["actor"] for x in value[field]}=={"emma","sophia","ethan"}
    value[field]=[x for x in value[field] if x["actor"]=="ethan"]
    value["inactive_teachers_archived"]=True
    value["archive_restore_recipe"]="tools/v134/archive_inactive_teachers_134.py"
    meta.write_text(json.dumps(value,ensure_ascii=False,indent=2)+"\n","utf-8")

assert (ASSETS/"ethan-mobile.glb").read_bytes()[:4]==b"glTF"
assert not list(ASSETS.glob("emma*.glb")) and not list(ASSETS.glob("sophia*.glb"))
assert archived and len({row["actor"] for row in archived})==2
(ARCH/"ARCHIVE_MANIFEST.json").write_text(json.dumps({
    "status":"INACTIVE_NOT_DELETED",
    "active":"ethan",
    "available_for_restore":["emma","sophia"],
    "warning":"Keep this artifact and repository source-generation scripts; not inside Android APK",
    "archived_files":archived
},ensure_ascii=False,indent=2)+"\n","utf-8")
print("ACTIVE 3D APP: Ethan only. Emma and Sophia restored outside the APK under source/archived-teachers")
print("ARCHIVED",len(archived),"files, bytes",sum(a["bytes"] for a in archived))
