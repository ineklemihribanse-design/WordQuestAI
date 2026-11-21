#!/usr/bin/env python3
"""Check an in-place 1.0.80 -> 1.0.81 package upgrade from Android dumpsys output.

This verifies installation continuity and relevant runtime permission state,
NOT the content of private app databases. A clean emulator cannot certify
real profiles, study history or cloud records.
"""
from pathlib import Path
import re
import sys


def package_snapshot(dumpsys: str, expected_package: str) -> dict:
    if f"Package [{expected_package}]" not in dumpsys:
        raise ValueError(f"Package identity missing: {expected_package}")

    def read(pattern: str, label: str) -> str:
        match = re.search(pattern, dumpsys, re.MULTILINE)
        if match is None:
            raise ValueError(f"Missing Android package metadata: {label}")
        return match.group(1).strip()

    camera = re.search(r"^\s*android\.permission\.CAMERA:\s+granted=(true|false)\b",
                       dumpsys, re.MULTILINE)
    return {
        "package": expected_package,
        "version": int(read(r"\bversionCode=(\d+)\b", "versionCode")),
        "uid": read(r"^\s*userId=(\d+)\b", "userId"),
        "first_installed": read(r"^\s*firstInstallTime=([^\r\n]+)", "firstInstallTime"),
        "data_dir": (re.search(r"^\s*dataDir=(\S+)", dumpsys,
                               re.MULTILINE) or [None, None])[1],
        "camera_permission": camera.group(1) if camera else None,
    }


def verify_upgrade(before: str, after: str, package: str) -> dict:
    old = package_snapshot(before, package)
    new = package_snapshot(after, package)
    if old["version"] != 106 or new["version"] != 107:
        raise ValueError(
            f"Expected in-place code 106 -> 107; got {old['version']} -> {new['version']}"
        )
    if old["uid"] != new["uid"]:
        raise ValueError("Android app UID changed after upgrade (possible uninstall/reinstall)")
    if old["first_installed"] != new["first_installed"]:
        raise ValueError("firstInstallTime changed: old app installation was not preserved")
    if old["data_dir"] and new["data_dir"] and old["data_dir"] != new["data_dir"]:
        raise ValueError("Android package data directory changed")
    # Losing a previously granted permission is a real regression, including
    # the case where the new manifest silently drops CAMERA entirely.
    if old["camera_permission"] == "true" and new["camera_permission"] != "true":
        raise ValueError("Android camera grant state changed during upgrade")
    return {
        "from": old["version"], "to": new["version"],
        "same_uid": True, "same_initial_install": True,
        "same_data_dir": old["data_dir"] == new["data_dir"] if (
            old["data_dir"] and new["data_dir"]) else None,
        "camera_grant_unchanged": old["camera_permission"] == new["camera_permission"]
            if old["camera_permission"] is not None and
               new["camera_permission"] is not None else None,
    }


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit("Usage: verify_upgrade_metadata_177.py old-dumpsys new-dumpsys package")
    try:
        result = verify_upgrade(Path(sys.argv[1]).read_text("utf-8"),
                                Path(sys.argv[2]).read_text("utf-8"), sys.argv[3])
    except (ValueError, OSError) as exc:
        print(f"FAIL: Android in-place continuity: {exc}", file=sys.stderr)
        raise SystemExit(1)
    print("PASS: v1.0.80 to v1.0.81 Android install continuity", result)
    print("LIMITATION: private app database contents still need real user-data QA.")
