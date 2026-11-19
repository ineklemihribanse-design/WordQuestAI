#!/usr/bin/env python3
"""Stage Lena's real GLB into the extracted Android project's assets.

This only stages the asset. Android renderer/character selector wiring is separate.
"""
from pathlib import Path
import json
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
MAP = ROOT / "assets/avatars/lena/animation-map.json"
ACTIVE = ROOT / "assets/avatars/active-teacher.json"
DEST = ROOT / "app-project/app/src/main/assets/avatars/lena"

def main():
    if not (ROOT / "app-project/settings.gradle.kts").is_file():
        sys.exit("ERROR: run scripts/prepare-codex-project.sh first")
    if not SOURCE.is_file() or not MAP.is_file() or not ACTIVE.is_file():
        sys.exit("ERROR: Lena GLB or animation-map.json is missing")
    subprocess.run([sys.executable, str(ROOT / "tools/verify_lena_glb.py"), str(SOURCE)], check=True)
    mapping = json.loads(MAP.read_text(encoding="utf-8"))
    if mapping.get("characterId") != "lena":
        sys.exit("ERROR: unexpected character ID")
    DEST.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SOURCE, DEST / SOURCE.name)
    shutil.copy2(MAP, DEST / MAP.name)
    selection = json.loads(ACTIVE.read_text(encoding="utf-8"))
    if selection.get("activeTeacher") != "lena":
        sys.exit("ERROR: active teacher configuration is not Lena")
    shutil.copy2(ACTIVE, DEST.parent.parent / "active-teacher.json")
    print("Lena assets staged:", DEST)
    print("NOTE: Android renderer integration and APK build are NOT completed.")

if __name__ == "__main__":
    main()
