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
assert 'Wq175PremiumPracticeSpecialTile("Yanlışlarımdan bulmaca"' in hub
assert 'Wq175PremiumPracticeSpecialTile("Kişisel AI öğretmenim"' in hub
assert hub.count("Wq175PremiumPracticeSpecialTile(")==2
assert hub.count("MenuItem@ {")>=1
assert 'internal fun Wq175PremiumPracticeSpecialTile(' in grid
assert 'Wq175SpecialTileArt(teacher,Modifier.size(76.dp))' in grid
assert 'Wq175SpecialTileArt(teacher,Modifier.size(70.dp))' in grid
assert 'R.drawable.wq167_lena_portrait' in grid
assert 'Wq175GlossIcon("mistakes",modifier)' in grid
assert 'PracticeKind.entries.chunked(2)' in grid
icons=(src/"WordQuest175Icons.kt").read_text("utf-8")
assert '"yanlışlarımdan" in s -> "mistakes"' in icons
assert '"kişisel ai öğretmen" in s -> "ai_teacher"' in icons
assert 'Wq175DailyPlanBanner(' in hub
print("PASS: phone cards stack artwork above text; tablet cards remain horizontal")
from patch_practice_extra_calls_175 import replace_special_card_call
sample='''MenuItem("Yanlışlarımdan bulmaca", "a", "✦") {
    if (true) return@MenuItem
}
MenuItem("Kişisel AI öğretmenim", "b", "✦") {
    if (true) return@MenuItem
}'''
for title in ("Yanlışlarımdan bulmaca","Kişisel AI öğretmenim"):
    sample=replace_special_card_call(sample,title)
assert sample.count("MenuItem@ {")==2
assert sample.count("return@MenuItem")==2
direct='MenuItem("Kişisel AI öğretmenim", "Kişisel asistan", "✦", assistant)'
direct=replace_special_card_call(direct,"Kişisel AI öğretmenim")
assert direct == 'Wq175PremiumPracticeSpecialTile("Kişisel AI öğretmenim", "Kişisel asistan", "✦", assistant)'
inline='MenuItem("Kişisel AI öğretmenim", "AI", "✦", onClick={ return@MenuItem })'
inline=replace_special_card_call(inline,"Kişisel AI öğretmenim")
assert 'return@Wq175PremiumPracticeSpecialTile' in inline
print("PASS: premium Daily Plan labels work for trailing lambdas, direct callbacks and inline lambdas")
