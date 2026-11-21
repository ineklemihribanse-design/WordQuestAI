#!/usr/bin/env python3
"""Smoke test academy destination continuity, no invented exam data."""
from pathlib import Path
src=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
home=(src/"HomeScreen.kt").read_text("utf-8")
academy=(src/"WordQuest175Academy.kt").read_text("utf-8")
assert home.count('MenuItem("YDS Akademi"') == 1
assert home.count('Wq175YdsAcademy(') == 1
assert "prep = yds" in home and "exams = ydsCenter" in home
assert 'Wq175PackageIntro(mode, level)' in home
assert 'Wq175GlossIcon(Wq175IconNames.menu(title)' in home
assert 'BackHandler(onBack = back)' in academy
assert 'A1–C2 · Altı seviyede kelime koleksiyonları' in academy
assert 'selected:Boolean=false,onSelect:()->Unit' in academy
assert 'YDS Kelime Paketleri' in academy
assert 'Wq175ThemeBanner("yds_akademi"' in academy
for name in ("Kelime","Dilbilgisi","Paragraf","Çeviri","Yakın Anlam",
             "Cümle Tamamlama","Mini Deneme","Tam Deneme","Yanlış Analizi"):
    assert 'Wq175AcademyModule("'+name+'"' in academy,name
for icon in ("paragraph","translate","synonym","completion","timer","exam","review"):
    assert '"'+icon+'"' in academy
assert "modules.chunked(cols)" in academy
selector=(Path(__file__).resolve().parents[2]/"tools/v175/apply_academy_175.py").read_text("utf-8")
assert 'Wq175CefrLevelCard(' in selector
assert 'onSelect={chooseLevel(value)}' in selector
assert 'levels.chunked(columns)' in selector
assert 'if(mode == GameMode.Yds)' in selector
assert 'for ((group, entries) in games)' in selector
assert 'internal fun Wq175CefrLevelCard(' in academy
assert '"level_"+tier.lowercase(java.util.Locale.ROOT)' in academy
assert 'Surface(onClick=onSelect' in academy
assert 'maxWidth<320.dp || LocalDensity.current.fontScale>1.45f -> 1' in academy
assert 'maxWidth<600.dp || LocalDensity.current.fontScale>1.25f -> 2' in academy
assert 'val horizontal=maxWidth>=250.dp' in academy
assert "module.action" in academy
assert 'Wq175AcademyModule("Kelime","YDS kelimeleri","cards"' in academy
assert (src/"YdsCenterScreen.kt").exists()
assert (src/"NewGameScreen.kt").exists()
print("PASS: one YDS Academy, existing prep/exam destinations and honest 2k-tier target.")
