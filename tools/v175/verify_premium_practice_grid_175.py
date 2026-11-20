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
assert 'Wq175PremiumPracticeSpecialTile("Yanlışlarımdan bulmaca"' in hub
assert 'Wq175PremiumPracticeSpecialTile("Kişisel AI öğretmenim"' in hub
assert hub.count('Wq175PremiumPracticeSpecialTile(')==2
assert 'if(teacher)"ai_teacher" else "mistakes"' in grid
assert 'onClick=onClick' in grid
assert 'PracticeKind.entries.chunked(2)' in grid
assert 'Wq175DailyPlanBanner(' in hub
print("PASS: seven premium practice cards preserve original navigation callbacks")
