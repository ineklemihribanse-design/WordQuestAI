#!/usr/bin/env python3
"""Paket 4: Set British English as the default paragraph listening accent.

Apply after the v1.0.85 paragraph component has been assembled. Existing users'
explicit accent selection is not overwritten. This is a pure source patch; it
never accesses device preferences, changes Firebase, or publishes an APK.
"""
from __future__ import annotations

import argparse
from pathlib import Path

OLD = 'var accent by rememberSaveable(profileKey) { mutableStateOf("US") }'
NEW = 'var accent by rememberSaveable(profileKey) { mutableStateOf("UK") }'
PARAGRAPH_PATHS = (
    "tools/v175/WordQuest175ParagraphDictation.kt",
    "source/app/src/main/java/com/kelimebulmacasi/a1/WordQuest175ParagraphDictation.kt",
    "android-current/app/src/main/java/com/kelimebulmacasi/a1/WordQuest175ParagraphDictation.kt",
)

def patched(source: str) -> str:
    if "Wq175ParagraphPracticeDialog" not in source or 'val locale = if (accent == "UK") Locale.UK else Locale.US' not in source:
        raise ValueError("Unsupported paragraph implementation; refusing to patch")
    if NEW in source and OLD not in source:
        return source
    if source.count(OLD) != 1 or NEW in source:
        raise ValueError("Unexpected paragraph accent default; manual review required")
    return source.replace(OLD, NEW, 1)

def apply(root: Path) -> int:
    changed = 0
    found = 0
    for relative in PARAGRAPH_PATHS:
        path = root / relative
        if not path.exists():
            continue
        found += 1
        before = path.read_text(encoding="utf-8")
        after = patched(before)
        if before != after:
            path.write_text(after, encoding="utf-8")
            changed += 1
        print(f"PASS British default: {relative}")
    if found == 0:
        raise FileNotFoundError("Paragraph component not present: assemble v1.0.85 first")
    return changed

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", nargs="?", type=Path, default=Path("."))
    args = parser.parse_args()
    count = apply(args.root.resolve())
    print(f"British en-GB default set; updated {count} source file(s); American choice retained.")

if __name__ == "__main__":
    main()
