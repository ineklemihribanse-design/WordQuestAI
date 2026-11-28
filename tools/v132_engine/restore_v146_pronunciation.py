#!/usr/bin/env python3
"""Restore unchanged pronunciation assets from the exact published v1.0.146 APK.
The separate PR cache may regenerate Ogg serial numbers, inflating delta updates.
Fail closed if the base APK or the expected asset inventory differs.
"""
import hashlib,sys,zipfile
from pathlib import Path,PurePosixPath
APK_SHA="cedc490c4b735eaa45fd33b7a762059979a9f9a03cb3d7d52ed4175ea4d6637f"
apk=Path(sys.argv[1]); assets=Path(sys.argv[2])
with apk.open("rb") as f:
    assert hashlib.file_digest(f,"sha256").hexdigest()==APK_SHA,"Wrong published base APK"
prefix="assets/pronunciation/"
root=assets/"pronunciation"
with zipfile.ZipFile(apk) as z:
    entries=[i.filename for i in z.infolist() if not i.is_dir() and i.filename.startswith(prefix)]
    assert len(entries)==16814 and len(set(entries))==len(entries),len(entries)
    relative={n.removeprefix(prefix) for n in entries}
    assert all(not PurePosixPath(n).is_absolute() and ".." not in PurePosixPath(n).parts for n in relative)
    current={f.relative_to(root).as_posix() for f in root.rglob("*") if f.is_file()}
    assert current==relative,"Pronunciation inventory mismatch"
    changed=0
    for name in entries:
        data=z.read(name)
        dest=root/name.removeprefix(prefix)
        if dest.read_bytes()!=data:
            dest.write_bytes(data);changed+=1
    assert all((root/n.removeprefix(prefix)).read_bytes()==z.read(n) for n in entries)
print(f"PASS: all {len(entries)} pronunciation assets match verified v1.0.146; restored {changed} regenerated entries")
