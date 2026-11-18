#!/usr/bin/env python3
"""Make a NEW separately installable Android application; never change the original APK."""
from pathlib import Path

root = Path("source")
gradle = root / "app/build.gradle.kts"
src = gradle.read_text(encoding="utf-8")
old = 'applicationIdSuffix = ".offline101"'
new = 'applicationIdSuffix = ".fresh"'
assert src.count(old) == 1, "Unexpected Android application identity; abort instead of risking the old app"
assert 'versionCode = 73' in src
gradle.write_text(src.replace(old, new), encoding="utf-8")

# The new application must NEVER fetch updates signed with the old certificate.
old_repo = "mihribansezik5-lgtm/WordQuestAI-Updates"
new_repo = "ineklemihribanse-design/WordQuestAI"
files_changed = []
for f in (root / "app/src").rglob("*"):
    if f.suffix not in {".kt", ".java", ".xml", ".json"}:
        continue
    text = f.read_text(encoding="utf-8")
    if old_repo in text:
        f.write_text(text.replace(old_repo, new_repo), encoding="utf-8")
        files_changed.append(str(f))
print("New applicationId: com.kelimebulmacasi.a1.practice.fresh")
print("Updater repository substitutions:", len(files_changed))
