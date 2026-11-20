#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
screen=(root/"source/app/src/main/java/com/kelimebulmacasi/a1/YdsCenterScreen.kt").read_text("utf-8")
for marker in (
    'full: Boolean = false',
    'if (full) YdsContent.pool(null, null)',
    'selection.take(if (full) 80',
    'if (full) 180 * 60',
    'if (full) 180L else 15L',
    '80 soruluk tam deneme · 180 dakika',
    '180L * 60L * 1000L) / 1000L',
    'Wq175YdsResult(',
):
    assert marker in screen, marker
assert screen.count("store.record(current, correct)")==1
assert screen.count("session.recordYdsAttempt(current, correct, text)")==1
assert (root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175FullYdsTest.kt").exists()
print("PASS: Full 80-question YDS mock on original profile-scoped progress and deadline")
