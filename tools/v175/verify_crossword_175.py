#!/usr/bin/env python3
from pathlib import Path
src=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"PuzzleScreen.kt").read_text("utf-8")
for anchor in (
    'Wq175GlossIcon("puzzle",Modifier.size(45.dp))',
    'Color(0xFFEAF7CE)',
    'heightIn(min=50.dp)',
    'Color(0xFF8DBD64)',
    'Wq175TimeRemainingBadge(remainingMs)',
    'if (shuffling || timedOut) return',
    'onLearning(LearningSignal(',
    'pronunciation.pronounce(',
    'save(after)'
):
    assert anchor in s,anchor
assert (src/"WordQuest175TimedPuzzle.kt").exists()
print("PASS: visual crossword improvements keep solving, timing, hints and persistence.")
