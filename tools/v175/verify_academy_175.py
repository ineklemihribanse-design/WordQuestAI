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
assert 'İçerik hedefi: Her seviyede 2.000 kelime / ifade' in academy
assert 'YDS Kelime Paketleri' in academy
assert (src/"YdsCenterScreen.kt").exists()
assert (src/"NewGameScreen.kt").exists()
print("PASS: one YDS Academy, existing prep/exam destinations and honest 2k-tier target.")
