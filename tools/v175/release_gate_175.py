#!/usr/bin/env python3
"""Explicit release-readiness gate for WordQuest AI 1.0.75.

No production release is authorized by a successful build alone. This gate
requires auditable manual evidence for hardware and two-account workflows.
It never uploads APKs or accesses private account data.
"""
import argparse
import json
from pathlib import Path
import sys

REQUIRED = {
    "physical_phone_camera_ocr": "Camera capture -> OCR -> preview -> save -> relaunch",
    "physical_phone_pdf_ocr": "Real PDF -> OCR -> preview -> edit -> save",
    "existing_174_user_data": "Backed-up 1.0.74 profile -> in-place 1.0.75 -> compare counts and saved progress",
    "arena_two_accounts": "Two distinct accounts -> invite/match -> score and reconnect",
    "responsive_manual_visual": "Review actual room screens on phone, large-font and tablet",
    "cefr_content_review": "Document A1/A2/B1/B2/C1/C2 counts, quality and remaining target gaps",
}


def evaluate(document):
    if document.get("versionName") != "1.0.75" or document.get("versionCode") != 101:
        raise ValueError("Release evidence must target v1.0.75 code101")
    results = document.get("checks")
    if not isinstance(results, dict):
        raise ValueError("Missing checks mapping")
    failures = []
    for key, description in REQUIRED.items():
        item = results.get(key)
        if not isinstance(item, dict):
            failures.append(f"{key}: missing evidence ({description})")
            continue
        if item.get("status") != "passed":
            failures.append(f"{key}: not passed")
            continue
        if not isinstance(item.get("evidence_url"), str) or not item["evidence_url"].startswith("https://"):
            failures.append(f"{key}: missing HTTPS evidence link")
        if not isinstance(item.get("tested_at"), str) or len(item["tested_at"]) < 10:
            failures.append(f"{key}: missing test date")
        if not isinstance(item.get("tester"), str) or not item["tester"].strip():
            failures.append(f"{key}: missing tester")
    return failures


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("evidence", type=Path)
    args = parser.parse_args()
    try:
        issues = evaluate(json.loads(args.evidence.read_text(encoding="utf-8")))
    except (ValueError, OSError, json.JSONDecodeError) as error:
        print(f"RELEASE BLOCKED: {error}", file=sys.stderr)
        return 1
    if issues:
        for issue in issues:
            print("RELEASE BLOCKED:", issue, file=sys.stderr)
        return 1
    print("RELEASE GATE PASS: all six documented physical/online/manual checks have evidence.")
    print("This report does not sign, upload or publish the APK.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
