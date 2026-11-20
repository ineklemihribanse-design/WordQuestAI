#!/usr/bin/env python3
"""Structural checks on the cumulative v173 candidate; no shipping claims."""
from pathlib import Path
import re
import hashlib

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
gradle=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert gradle.count("versionCode = 98")==1
assert 'versionName = "1.0.73-arena-avatar-candidate"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle

home=(PKG/"ArenaApprovedHome.kt").read_text("utf-8")
shared=(PKG/"WordQuestSharedAvatars.kt").read_text("utf-8")
arena=(PKG/"Arena173Components.kt").read_text("utf-8")
memory=(PKG/"VocabularyNeonHome.kt").read_text("utf-8")
practice=(PKG/"VocabularyNeonPractice.kt").read_text("utf-8")
visuals=(PKG/"ArenaV2Visuals.kt").read_text("utf-8")
assert home.count("Arena173UnifiedMatchSettings(")==1
assert home.count("Arena173AdventurePicker(")==1
assert 'Arena172MatchSettings(' not in home
assert 'Arena172AdventurePicker(' not in home
assert 'ArenaApprovedPanel("İngilizce Seviyesi"' not in home
assert 'ArenaApprovedPanel("Konu / Paket"' not in home
assert 'ArenaApprovedPanel("Oyun Biçimi"' not in home
assert "Seviye Seçiniz" in arena and "Konu Seçiniz" in arena
assert "PremiumArenaRules.allowedLevels" in arena
installer=(PKG/"WordQuestPrivateAvatarAssets.kt").read_text("utf-8")
scene=(PKG/"WordQuestAvatarStage3D.kt").read_text("utf-8")
assert "ZipInputStream" in installer and "MessageDigest.getInstance(\"SHA-256\")" in installer
assert installer.count('Model("')==10
assert "createModelInstance(modelFile)" in scene and "model.playAnimation(" in scene
assert "WordQuestPrivateAvatarAssets.installed(context,avatar.id)" in shared
assert "3D Avatar Paketini Telefonumdan Ekle" in shared
assert 'wq173_source_models_preview.webp' in installer
assert '5309a66b51463da64d852861a90c1535e57528f01b97a425bff0fd69a5b54a7f' in installer
assert 'fun previewFile(context:Context):File?' in installer
assert 'WordQuestPrivateAvatarAssets.previewFile(context)' in shared
assert 'Bitmap.createBitmap(preview,(index%5)*80,(index/5)*80,80,80)' in shared
assert 'WordQuestAvatarPortrait(avatarId,Modifier.fillMaxSize())' in scene
assert "ActivityResultContracts.OpenDocument()" in shared
assert "WordQuestAvatarStage3D(mascot" in home
assert "WordQuestAvatarStage3D(selectedAvatar" in memory
assert "WordQuestAvatarStage3D(winnerAvatar" in visuals
assert "WordQuestAvatarStage3D(" in practice
assert "if(topics.isNotEmpty() && topic !in topics)" in arena
assert 'mode=="duel" || (solo && mode!="quiz")' not in arena
assert 'enabled=topics.isNotEmpty()' in arena
assert 'if (reviewed && reviews[position] == true && studyMode != "card")' in practice
assert 'Harika, doğru cevap!' in practice
assert "onSolo(true)" in arena and "onSolo(false)" in arena
assert all(x in arena for x in ("Bulmaca Düellosu","Ortak Bulmaca",
    "5 Şıklı Düello","İpucu","Süre","Bulmaca"))
assert 'WordQuestSharedAvatarPicker(' in memory
assert 'rememberWordQuestAvatar(session.profileKey)' in memory
assert 'rememberWordQuestAvatar(avatarProfileKey)' in home
assert 'synchronizeOnlineForActiveProfile(context,avatarProfileKey)' in home
assert 'fun hasSelection(context:Context,profile:String)' in shared
assert 'DisposableEffect(context,profile)' in shared
assert 'registerOnSharedPreferenceChangeListener(listener)' in shared
assert 'WordQuestAvatarStage3D(selectedAvatar' in memory
assert 'WordQuestSharedAvatarPicker(' in home
assert 'WordQuestAvatarStage3D(' in practice
assert 'WordQuestAvatarPortrait(' in visuals
assert 'WordQuestAvatarStore.save(context,avatarProfileKey,chosen)' in home
assert 'WordQuestAvatarStore.save(context,session.profileKey,chosen)' in memory
for name in ("PremiumOnlineScreen.kt","PremiumArenaScreen.kt"):
    contents=(PKG/name).read_text("utf-8")
    assert "avatarProfileKey=session.profileKey" in contents,name
    assert "CEVABI GÖNDER" in contents and "PAS GEÇ" in contents
    assert "crosswordFullWordMatches" in contents
for token in ("astra","piko","nobi","tera","lopi","tomba","foksi","moka","pera","gigi"):
    assert f'WordQuestSharedAvatar("{token}"' in shared,token
assert shared.count("licensedForBuild:Boolean")>=1
assert shared.count("null,false)")==6
assert shared.count(",true)")==4
previews=ROOT/"source/app/src/main/assets/avatars/shared/wq173_verified_previews.webp"
assert previews.is_file() and previews.stat().st_size==1748
assert hashlib.sha256(previews.read_bytes()).hexdigest()==(
 "792da177fafecddf20200e66e5f09c3f608cd73e9a453e0ce022096f9950c075")
icons=ROOT/"source/app/src/main/assets/arena/icons/wq173_mode_icons.webp"
assert icons.is_file() and icons.stat().st_size==4230
assert hashlib.sha256(icons.read_bytes()).hexdigest()==(
 "18d2f4a2782b4f7235f7c083bd2d41e83b79b5814d22a6d1557265904dcd84fb")
assert 'BitmapFactory.decodeStream' in arena
assert 'Çapraz kılıçlar' in arena
assert (PKG/"LenaLive3DStage.kt").is_file()
assert (PKG/"ConversationScreen.kt").is_file()
assert (PKG/"VocabularyNeonPractice.kt").is_file()
print("PASS: v173 unified match setup, real-source preview gating, shared avatar choice.")
print("PENDING: full GLB rigs, device UI QA and six model redistribution licenses.")
