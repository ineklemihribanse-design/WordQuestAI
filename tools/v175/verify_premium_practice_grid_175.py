#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
grid=(src/"Wq175PremiumPracticeGrid.kt").read_text("utf-8")
hub=(src/"PracticeHubScreen.kt").read_text("utf-8")
assert 'BoxWithConstraints' in grid and 'LocalDensity.current.fontScale>1.35f' in grid
assert 'PracticeKind.entries.chunked(2)' in grid
for name in ("Dictation","Sentence","Grammar","Story","Speak"):
    assert 'PracticeKind.'+name in grid
assert 'Wq175GlossIcon(Wq175IconNames.practice(kind)' in grid
assert 'Wq175PremiumPracticeGrid { kind ->' in hub
assert 'selected = kind' in hub and 'questionIndex = 0' in hub
assert 'Yanlışlarımdan bulmaca' in hub and 'Kişisel AI öğretmenim' in hub
assert 'Wq175DailyPlanBanner(' in hub
print("PASS: all five actual practice modes render separate icons and preserve callbacks")
