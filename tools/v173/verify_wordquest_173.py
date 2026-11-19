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
assert 'WordQuestAvatarPortrait(selectedAvatar' in memory
assert 'WordQuestSharedAvatarPicker(' in home
assert 'WordQuestAvatarPortrait(' in practice
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
assert (PKG/"LenaLive3DStage.kt").is_file()
assert (PKG/"ConversationScreen.kt").is_file()
assert (PKG/"VocabularyNeonPractice.kt").is_file()
print("PASS: v173 unified match setup, real-source preview gating, shared avatar choice.")
print("PENDING: full GLB rigs, device UI QA and six model redistribution licenses.")
