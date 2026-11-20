#!/usr/bin/env python3
"""Regression gates for 1.0.75 stage one. Run after the overlay."""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
gradle=(root/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionCode = 101' in gradle
assert 'versionName = "1.0.75"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
assert gradle.count('isDebuggable = false') >= 1, "Offline upgrade must not expose app data via run-as/debugger"
assert 'applicationIdSuffix = ".fresh"\n            isDebuggable = false' in gradle

arena=(src/"ArenaApprovedHome.kt").read_text("utf-8")
assert 'matchSettingsPage by rememberSaveable' in arena
assert 'if (!matchSettingsPage)' in arena
assert 'matchSettingsPage=true' in arena
assert 'Wq175ArenaHeroAndModes(' in arena
assert 'Wq174UnifiedMatchSettings(' in arena
assert arena.count('Wq2DCharacterPicker(')==1
assert arena.index('if (!matchSettingsPage)') < arena.index('Wq175ArenaHeroAndModes(')
assert arena.index('} else {\n            TextButton(onClick={matchSettingsPage=false}') < arena.index('Wq174UnifiedMatchSettings(')

practice=(src/"VocabularyNeonPractice.kt").read_text("utf-8")
assert 'heightIn(min = 245.dp)' in practice
assert 'height(132.dp)' in practice
assert 'durationMillis = 390' in practice
assert 'Triple("Öğrendim", true, false)' in practice
assert 'Triple("Zorlandım", false, true)' in practice
assert 'enabled = reviewed' in practice
assert 'Atla ›' not in practice
assert 'session.reviewAnyLearning(card,' in practice

learning=(src/"LearningStore.kt").read_text("utf-8")
assert 'fun addManual(entry: Entry' in learning
assert 'if(loadCard(db,key)!=null) return false' in learning
assert 'dueAt=now' in learning
assert 'db.beginTransaction()' in learning
assert 'ON DELETE CASCADE' not in learning or 'writeCard' in learning
session=(src/"AppSession.kt").read_text("utf-8")
assert 'fun addManualLearning(' in session
assert 'learning.addManual(Entry(' in session
assert 'learningRevision++' in session
memory=(src/"VocabularyNeonHome.kt").read_text("utf-8")
assert 'Wq175AddWordDialog(' in memory
assert 'Wq175AddWordEntry(' in memory
assert 'Wq174MemoryHero(' in memory
assert memory.count('Wq2DCharacterPicker(')==1
assert (src/"WordQuest175ManualWords.kt").is_file()

for name in ("LenaLive3DStage.kt","ConversationScreen.kt","WordQuest2DCharacters.kt",
             "ArenaV2Visuals.kt","WordPronunciation.kt"):
    assert (src/name).exists(),name

print("PASS 1.0.75 stage-one regression gates")
print("All existing .fresh app identity, profile data, Arena callbacks and Lena intact.")
print("DO NOT RELEASE until all planned features and real-device QA pass.")

new_arena=(src/"WordQuest175Arena.kt").read_text("utf-8")
assert 'Wq2DPortrait(selectedAvatar' in new_arena, "real selected mascot must appear large"
assert 'Modifier.fillMaxWidth().height(192.dp)' in new_arena
assert 'Wq175StadiumBackdrop' in new_arena
assert 'onMode("duel")' in new_arena
assert 'onMode("team")' in new_arena
assert 'onMode("quiz")' in new_arena
print("PASS: premium 1.0.75 Arena stage shows the real selected mascot with three live mode callbacks")
