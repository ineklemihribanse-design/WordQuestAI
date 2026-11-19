#!/usr/bin/env python3
"""Prepare a signed Android upgrade PREVIEW, never mutate app id or keystore.

1.0.50 versionCode 75 is installed in the current new-account app.
The Arena V3 preview becomes versionCode 76. Only the version is changed,
so an Android update retains the app's local data.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
gradle=root/"source/app/build.gradle.kts"
source=gradle.read_text(encoding="utf-8")
edits={
    'versionCode = 73': 'versionCode = 76',
    'versionName = "1.0.48-neon-word-memory"': 'versionName = "1.0.51-arena-approved-ui"'
}
for old,new in edits.items():
    if source.count(old)!=1: raise RuntimeError(f"Unexpected release config for {old}")
    source=source.replace(old,new,1)
if 'applicationIdSuffix = ".fresh"' not in source:
    raise RuntimeError("App identity changed, refusing to prepare an upgrade.")
gradle.write_text(source,encoding="utf-8")
print("Arena 1.0.51 design preview code76: same .fresh app identity, new version.")
