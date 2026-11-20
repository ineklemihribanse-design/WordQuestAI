#!/usr/bin/env python3
"""Fail-fast integration checks for WordQuest 1.0.74 approved designs."""
from pathlib import Path
import hashlib
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
gradle=(APP/"build.gradle.kts").read_text("utf-8")
assert gradle.count("versionCode = 100")==1
assert 'versionName = "1.0.74-premium-arena-memory"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
assert 'applicationIdSuffix = ".fresh.qa"' not in gradle

arena=(SRC/"ArenaApprovedHome.kt").read_text("utf-8")
memory=(SRC/"VocabularyNeonHome.kt").read_text("utf-8")
arena174=(SRC/"WordQuest174Arena.kt").read_text("utf-8")
memory174=(SRC/"WordQuest174Memory.kt").read_text("utf-8")
for required in (
    "Wq174ArenaHeroAndModes(","Wq174UnifiedMatchSettings(",
    "Wq2DCharacterPicker(","rememberWq2DSelection(avatarProfileKey)",
):
    assert required in arena,required
assert arena.count("Wq2DCharacterPicker(")==1
assert arena.count("Wq2DCompactSelector(")==0
assert arena.count("Wq174ArenaHeroAndModes(")==1
assert "Arena173AdventurePicker(" not in arena
assert "Arena173UnifiedMatchSettings(" not in arena
assert 'green=false' in arena

assert memory.count("Wq174MemoryHero(")==1
assert memory.count("Wq2DCharacterPicker(")==1
assert memory.count("Wq174MemoryStats(")==1
assert memory.count("Wq174GoldButton(")==1
assert memory.count("Wq174MemoryModeCard(")==1
assert "Wq2DCompactSelector(" not in memory
assert 'session.learningDueCount(System.currentTimeMillis())' in memory
assert 'start("card", "DUE")' in memory
assert 'start(tile.mode)' in memory

for name in ("Bulmaca Düellosu","Ortak Bulmaca","5 Şıklı Düello"):
    assert name in arena174,name
assert "Wq174ArenaColosseum(" in arena174
assert 'PremiumArenaRules.allowedLevels' in arena174
assert 'onTopic(topics.first())' in arena174
assert "onSeconds(" in arena174
assert "onSize(" in arena174
assert "onSolo(" in arena174
assert 'onMode("team")' in arena174
assert "Wq2DPortrait(selected" in memory174
assert 'Wq2DCompactSelector(selected' in memory174
for mode in ('"card"','"write"','"type"'):
    assert mode in memory174

for filename in ("LenaLive3DStage.kt","ConversationScreen.kt",
                 "PremiumArenaScreen.kt","PremiumOnlineScreen.kt",
                 "VocabularyNeonPractice.kt","ArenaV2Visuals.kt",
                 "WordQuest2DCharacters.kt"):
    assert (SRC/filename).exists(),filename
for filename in ("PremiumArenaScreen.kt","PremiumOnlineScreen.kt"):
    contents=(SRC/filename).read_text("utf-8")
    assert 'avatarProfileKey=session.profileKey' in contents
    assert "PAS GEÇ" in contents and "CEVABI GÖNDER" in contents
    assert 'crosswordFullWordMatches' in contents

for pose,size,sha in (
    ("idle",218222,"00bc228719d1869d81ad16e1adab96991fa3e7a3c4bdcc00f2057b695e061253"),
    ("victory",239740,"f1701ece95b98fb82261e9c736bd2cc6d93b3246f07cffbc1261ed47f1217741")
):
    file=APP/"src/main/assets/avatars/wordquest_2d"/("wordquest_"+pose+"_atlas.webp")
    assert file.stat().st_size==size
    assert hashlib.sha256(file.read_bytes()).hexdigest()==sha
print("PASS 1.0.74: 2D original art, single selector, premium Arena and Memory.")
print("PASS: callbacks and word data intact, same signed .fresh app ID code100.")
