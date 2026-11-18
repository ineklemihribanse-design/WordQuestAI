#!/usr/bin/env python3
"""One-time visible test update (authorized by user), no final art approval.

Increment Android version ABOVE previously installed v1.0.27.1 beta 1 (51).
Keep applicationIdSuffix, signing, Room DB and saved API preferences intact.
The app checks GitHub Releases/latest and will not see GitHub prereleases;
therefore this is a deliberately labeled public PREVIEW update with stable
release routing, not a claim of finished avatar visual quality.
"""
from pathlib import Path
src=Path(__file__).resolve().parents[2]/"source/app/build.gradle.kts"
data=src.read_text(encoding="utf-8")
assert data.count("versionCode = 51")==1
assert data.count('versionName = "1.0.27.1-animated-lipsync-avatar"')==1
assert 'applicationIdSuffix = ".offline101"' in data
data=data.replace("versionCode = 51","versionCode = 53",1)
data=data.replace('versionName = "1.0.27.1-animated-lipsync-avatar"',
                  'versionName = "1.0.28-human-teachers-preview"',1)
src.write_text(data,encoding="utf-8")
assert "versionCode = 53" in data
assert "1.0.28-human-teachers-preview" in data
# Installed v1.0.27.1 clients accept only ^vN.N.N$ tags.
# Prevent a future four-part release from reintroducing Failed requirement.
import re
name = "1.0.28-human-teachers-preview"
tag = "v" + name.split("-", 1)[0]
assert re.fullmatch(r"v[0-9]+[.][0-9]+[.][0-9]+", tag), tag

print("User-requested human teacher preview v1.0.28 code53; package/signing untouched")
