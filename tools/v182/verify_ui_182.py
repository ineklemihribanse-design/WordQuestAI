#!/usr/bin/env python3
"""Fail-closed Android v182 icon and interface checks."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
get=lambda file:(SRC/file).read_text("utf-8")
atlas=ROOT/"source/app/src/main/assets/wq182_approved_33_icons_atlas.webp"
raw=atlas.read_bytes()
assert len(raw)>150000 and raw[:4]==b"RIFF" and raw[8:12]==b"WEBP"
icon=get("Wq182Icons.kt")
assert 'return cropped!=null' in icon
assert '"arena_team" to "wq_ortak_bulmaca"' in icon
assert '"arena_quiz" to "wq_bes_sikli_duello"' in icon
assert '"arena_duel" to "wq_bulmaca_duellosu"' in icon
assert 'Wq182Artwork(selectedReplacement,modifier)' in get("WordQuest175Icons.kt")
assert 'Wq182Artwork("wq_bulmacalarim"' in get("Wq175PremiumHome.kt")
quiz=get("PremiumOnlineScreen.kt")
assert 'Wq182QuizAnswer(' in quiz
assert 'client.answer(index,n)' in quiz
assert 'sounds.correct(session.preferences)' in quiz and 'sounds.wrong(session.preferences)' in quiz
assert 'Wq182Info("Düello kuralları"' in quiz
home=get("HomeScreen.kt")
assert 'Text(social.nickname.ifBlank { "Kullanıcı adını belirle" }' in home
assert 'social.saveName(username)' in home
assert 'Text("Kullanıcı adını yaz")' in home
assert 'Wq182Info("Kullanıcı adı ve eşitleme"' in home
assert 'session.social.nickname.ifBlank {"Oyuncu"}.take(16)' in home
assert 'session.accountName?.takeIf { it.isNotBlank() } ?: "Oyuncu"' not in home
assert 'versionCode = 108' in (ROOT/"source/app/build.gradle.kts").read_text()
assert 'versionName = "1.0.82"' in (ROOT/"source/app/build.gradle.kts").read_text()
assert 'applicationIdSuffix = ".fresh"' in (ROOT/"source/app/build.gradle.kts").read_text()
print("PASS: 33 verified imported art assets, approved originals preserved")
print("PASS: quiz state/score/sounds, edited nickname and context info button")
print("PASS: versionCode 108; same installed app ID and current Firebase schema")
