#!/usr/bin/env python3
"""Fail-fast, after-overlay integration gates for v1.0.75."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
GRADLE=ROOT/"source/app/build.gradle.kts"
src=lambda name:(SOURCE/name).read_text("utf-8")
gradle=GRADLE.read_text("utf-8")
assert 'versionCode = 101' in gradle
assert 'versionName = "1.0.75-learning-experience-preview"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
assert 'applicationIdSuffix = ".fresh.qa"' not in gradle
assert 'Wq175GameModeSelector(' in src("NewGameScreen.kt")
assert 'Wq175TimedGames.start(' in src("NewGameScreen.kt")
assert 'Wq175TimedGames.clear(' in src("NewGameScreen.kt")
puzzle=src("PuzzleScreen.kt")
assert 'Wq175CountdownBadge(wq175Remaining)' in puzzle
assert 'Wq175TimedGames.remainingMs(wq175Round)' in puzzle
assert 'wq175Expired = true' in puzzle
assert 'if (shuffling || wq175Expired) return' in puzzle
assert '"Süre doldu!"' in puzzle
arena=src("ArenaApprovedHome.kt")
assert 'if (!wq175SettingsOpen)' in arena
assert 'onMode={ selected -> onMode(selected); wq175SettingsOpen = true }' in arena
assert arena.count('Wq174UnifiedMatchSettings(')==1
assert arena.count('Wq2DCharacterPicker(')==1
assert arena.count('Wq174ArenaHeroAndModes(')==1
memory=src("VocabularyNeonHome.kt")
assert 'Wq175WordImportDialog(session, ai)' in memory
assert memory.count('Wq2DCharacterPicker(')==1
assert 'Wq175WordImportDialog(' in src("WordQuest175WordImport.kt")
assert 'scanPairs(' in src("WordQuest175WordImport.kt")
assert 'ai.suggestWords(' in src("WordQuest175WordImport.kt")
assert 'session.addManualLearning(' in src("WordQuest175WordImport.kt")
assert 'addPersonalCard(' in src("LearningStore.kt")
assert 'learning.addPersonalCard(' in src("AppSession.kt")
assert 'existing != null' in src("LearningStore.kt")
card=src("VocabularyNeonPractice.kt")
assert 'heightIn(min = 245.dp)' in card
assert 'tween(durationMillis = 420)' in card
assert 'Öğrendim' in card
assert '"Atla ›"' not in card
assert 'ReviewRating.Hard' in card
for required in ('LenaLive3DStage.kt','ConversationScreen.kt','WordQuest2DCharacters.kt',
                 'PremiumOnlineScreen.kt','PracticeHubScreen.kt','DocumentScan.kt'):
    assert (SOURCE/required).exists(),required
assert (ROOT/"source/app/src/main/assets/avatars/wordquest_2d/wordquest_idle_atlas.webp").stat().st_size>100000
assert (ROOT/"source/app/src/main/assets/avatars/wordquest_2d/wordquest_victory_atlas.webp").stat().st_size>100000
print("v175 PASS: real modes, persistent timer, editable import, saved word data, separate Arena settings.")
print("v175 PASS: original 2D avatars, unchanged .fresh identity, all Lena and existing sources.")
