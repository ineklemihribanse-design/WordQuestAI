#!/usr/bin/env python3
"""Inspect updater contract and test data safely, without building/installing."""
from pathlib import Path
import base64,zipfile,io,re
archive=Path("v116-updater-overlay.b64")
with zipfile.ZipFile(io.BytesIO(base64.b64decode(archive.read_bytes()))) as z:
    paths=[x for x in z.namelist() if x.endswith(("UpdateProtocol.kt","UpdateProtocolTest.kt"))]
    print("UPDATE_FILES",paths)
    for p in paths:
        rows=z.read(p).decode("utf-8").splitlines()
        print("===== START",p,len(rows),"LINES =====")
        for i,row in enumerate(rows):
            print(f"{i+1:04d}: {row}")
        print("===== END",p,"=====")
print("===== v116 kotlin patch =====")
print(Path("v116-kotlin-fix.patch").read_text("utf-8")[:15000])
