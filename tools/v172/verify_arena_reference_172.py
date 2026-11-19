#!/usr/bin/env python3
"""Fast gate for the requested Arena reference widgets after installation."""
from pathlib import Path
import re

root=Path(__file__).resolve().parents[2]
p=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
a=(p/"ArenaApprovedHome.kt").read_text("utf-8")
v=(p/"ArenaV2Visuals.kt").read_text("utf-8")
ui=(p/"ArenaReferenceComponents.kt").read_text("utf-8")
assert a.count("Arena172AdventurePicker(")==1
assert a.count("Arena172MatchSettings(")==1
assert a.count('ArenaApprovedPanel("Maç Kurulumu"')==1
assert 'onTopic(value)' in a and 'onLevel(value)' in a
assert 'Arena172LiveScoreboard(' in v
assert all(s in ui for s in ("Bulmaca Düellosu","Ortak Bulmaca",
    "5 Şıklı Düello","Maceranı Seç!","Kalan Süre","Maç Ayarları","İpucu"))
for name in ("PremiumArenaScreen.kt","PremiumOnlineScreen.kt"):
    code=(p/name).read_text("utf-8")
    assert 'ArenaOutlineButton("»  PAS GEÇ"' in code,name
    assert 'ArenaGoldButton("✓  CEVABI GÖNDER"' in code,name
    assert "crosswordFullWordMatches" in code,name
gradle=(root/"source/app/build.gradle.kts").read_text("utf-8")
assert "versionCode = 97" in gradle
assert 'versionName = "1.0.72-arena-reference-controls"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
assert (p/"LenaLive3DStage.kt").is_file()
assert (p/"VocabularyNeonHome.kt").is_file()
print("PASS: v172 Arena mode, timer, settings, answer labels and existing core sources.")
