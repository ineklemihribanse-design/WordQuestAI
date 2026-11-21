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
assert 'val stacked=maxWidth < 225.dp || LocalDensity.current.fontScale>1.20f' in grid
assert 'Wq175PremiumPracticeArt(kind,Modifier.size(70.dp))' in grid
assert 'Wq175PremiumPracticeArt(kind,Modifier.size(75.dp))' in grid
assert 'Surface(onClick=onClick' in grid
assert 'onSelect(kind)' in grid
assert 'Wq175PremiumPracticeGrid { kind ->' in hub
assert 'selected = kind' in hub and 'questionIndex = 0' in hub
assert 'MenuItem("Yanlışlarımdan bulmaca"' in hub
assert 'MenuItem("Kişisel AI öğretmenim"' in hub
assert 'Wq175PremiumPracticeSpecialTile(' not in hub
assert 'PracticeKind.entries.chunked(2)' in grid
icons=(src/"WordQuest175Icons.kt").read_text("utf-8")
assert '"yanlışlarımdan" in s -> "mistakes"' in icons
assert '"kişisel ai öğretmen" in s -> "ai_teacher"' in icons
assert 'Wq175DailyPlanBanner(' in hub
print("PASS: phone cards stack artwork above text; tablet cards remain horizontal")
print("PASS: seven Daily Plan routes, original callbacks and two dedicated icons")
