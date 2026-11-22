#!/usr/bin/env python3
"""1.0.83 fail-closed release gate. Never emits or publishes an APK.

External evidence is explicit: a source-only pass cannot mean device or release-ready.
"""
import json
import sys
from pathlib import Path

REQUIRED = (
    "android_gradle_build_passed",
    "android_jvm_tests_passed",
    "question_visibility_5_10_12_15_20_passed",
    "two_real_accounts_arena_passed",
    "mascot_zip_device_import_passed",
    "arena_original_art_verified",
    "home_routes_and_icons_device_passed",
    "profile_and_progress_migration_passed",
    "signed_apk_verified",
    "in_place_upgrade_108_to_109_passed",
    "all_pages_device_smoke_passed",
)

def run(path: Path) -> int:
    if not path.is_file():
        print("BLOCKED: test evidence file does not exist")
        return 2
    evidence = json.loads(path.read_text("utf-8"))
    missing = [key for key in REQUIRED if evidence.get(key) is not True]
    if missing:
        print("BLOCKED: 1.0.83 is NOT release ready. Missing verifications:")
        for key in missing: print(" -",key)
        return 2
    print("PASS: supplied evidence declares every required gate; review actual logs before publishing")
    return 0

if __name__=="__main__":
    try:
        sys.exit(run(Path(sys.argv[1]) if len(sys.argv)>1 else Path(
            "tools/v183/release-evidence-183.json")))
    except (ValueError, OSError, KeyError) as exc:
        print("BLOCKED: invalid QA evidence:",exc,file=sys.stderr)
        sys.exit(2)
