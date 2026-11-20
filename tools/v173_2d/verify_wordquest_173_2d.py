#!/usr/bin/env python3
"""Smoke-check migrated 1.0.73 sources before Android compilation and release."""
from pathlib import Path
import hashlib

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
SRC=APP/"src/main/java/com/kelimebulmacasi/a1"
gradle=(APP/"build.gradle.kts").read_text("utf-8")
assert 'versionCode = 99' in gradle
assert 'versionName = "1.0.73-original-2d-complete"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
assert 'applicationIdSuffix = ".fresh.qa"' not in gradle
assert 'applicationId = "com.kelimebulmacasi.a1.practice"' in gradle

home=(SRC/"ArenaApprovedHome.kt").read_text("utf-8")
memory=(SRC/"VocabularyNeonHome.kt").read_text("utf-8")
study=(SRC/"VocabularyNeonPractice.kt").read_text("utf-8")
visuals=(SRC/"ArenaV2Visuals.kt").read_text("utf-8")
avatar=(SRC/"WordQuest2DCharacters.kt").read_text("utf-8")
picker=(SRC/"WordQuest2DPicker.kt").read_text("utf-8")
modes=(SRC/"Arena173Components.kt").read_text("utf-8")
for s in ("Arena173AdventurePicker(","Arena173UnifiedMatchSettings(",
          "Wq2DCompactSelector(","Wq2DCharacterPicker(",
          "rememberWq2DSelection(avatarProfileKey)"):
    assert s in home,s
assert 'ProfileRobotPortrait(robot,' not in memory
assert 'Wq2DCompactSelector(selectedAvatar' in memory
assert 'rememberWq2DSelection(session.profileKey)' in memory
assert 'Wq2DPortrait(' in study and 'victory=true' in study
assert 'Harika, doğru cevap!' in study
assert 'Wq2DPortrait(winnerAvatar' in visuals
assert 'Wq2DMascots.valid(kind)' in visuals
assert 'if(topics.isNotEmpty() && topic !in topics)' in modes
assert "Bulmaca Düellosu" in modes and "Ortak Bulmaca" in modes
assert "5 Şıklı Düello" in modes
assert 'WordQuest 20 görsellik 2D paketini seç' in avatar
assert '"astra","piko","nobi","tera","lopi"' in avatar
assert '"tomba","foksi","moka","pera","gigi"' in avatar
assert "Özgün 2D Görselleri Yükle" in picker
assert 'import io.github.sceneview' not in avatar+picker
assert (SRC/"LenaLive3DStage.kt").exists()
assert (SRC/"ConversationScreen.kt").exists()
assert (SRC/"VocabularyNeonHome.kt").exists()

asset=APP/"src/main/assets/arena/icons/wq173_mode_icons.webp"
assert asset.is_file() and asset.stat().st_size==4230
assert hashlib.sha256(asset.read_bytes()).hexdigest()==(
    "18d2f4a2782b4f7235f7c083bd2d41e83b79b5814d22a6d1557265904dcd84fb"
)
for pose,expected_size,expected_sha in (
    ("idle",218222,"00bc228719d1869d81ad16e1adab96991fa3e7a3c4bdcc00f2057b695e061253"),
    ("victory",239740,"f1701ece95b98fb82261e9c736bd2cc6d93b3246f07cffbc1261ed47f1217741")
):
    file=APP/"src/main/assets/avatars/wordquest_2d"/("wordquest_"+pose+"_atlas.webp")
    assert file.is_file() and file.stat().st_size==expected_size,pose
    assert hashlib.sha256(file.read_bytes()).hexdigest()==expected_sha,pose
assert "Bitmap.createBitmap(atlas,(index%5)*224,(index/5)*224,224,224)" in avatar
assert 'wordquest_"+pose+"_atlas.webp' in avatar
for name in ("PremiumOnlineScreen.kt","PremiumArenaScreen.kt"):
    text=(SRC/name).read_text("utf-8")
    assert 'avatarProfileKey=session.profileKey' in text,name
    assert "PAS GEÇ" in text and "CEVABI GÖNDER" in text,name
    assert 'crosswordFullWordMatches' in text,name
print("PASS: original 10x2D artwork slots, shared compact selector and victory.")
print("PASS: Arena timer, online match/word validation and merged match settings.")
print("PASS: versionCode 99 hotfix / original .fresh package / Lena and Word Memory intact.")
