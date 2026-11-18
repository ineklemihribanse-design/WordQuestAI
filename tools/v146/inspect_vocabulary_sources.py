#!/usr/bin/env python3
"""Read-only audit of vocabulary files contained in versioned source overlays."""
from __future__ import annotations

import base64
import io
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FILES = [
    "v118-learning-studio-overlay.b64",
    "v119-arena-midnight-sounds-overlay.b64",
    "v120-multi-ai-arena-overlay.b64",
    "v121-live-3d-teacher-overlay.b64",
]
MARKERS = ("data class LearningCard", "enum class WordType", "partOfSpeech",
           "LearningFolder", "WordCategory", "LearningScreen", "WordType",
           "data class Word", "getWordType")

def main():
    for name in FILES:
        payload = base64.b64decode((ROOT / name).read_text().strip())
        with zipfile.ZipFile(io.BytesIO(payload)) as package:
            entries = [p for p in package.namelist() if p.endswith((".kt", ".kts"))]
            print(f"== {name} ({len(entries)} code entries) ==")
            print("FILES:", ", ".join(entries))
            for path in entries:
                data = package.read(path).decode("utf-8", "replace")
                if any(marker.casefold() in (path + " " + data).casefold() for marker in MARKERS):
                    print(f"== FOUND {path} {len(data)} chars ==")
                    for i, line in enumerate(data.splitlines(), 1):
                        if any(m.casefold() in line.casefold() for m in MARKERS):
                            lo, hi = max(i - 12, 1), min(i + 18, len(data.splitlines()))
                            for n in range(lo, hi + 1):
                                print(f"{n:4d}: {data.splitlines()[n - 1][:220]}")
                            print("----")
                    if len(data) <= 16000 and ("LearningCard" in path or "WordType" in path):
                        print(f"== FULL {path} ==\\n{data}")
                    else:
                        print(f"== HEAD {path} ==\\n{data[:2200]}")

if __name__ == "__main__":
    main()
