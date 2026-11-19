#!/usr/bin/env python3
"""Check that Arena beta staging is complete and preserves the existing app."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TESTS=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
rules=json.loads((ROOT/"source/database.rules.json").read_text("utf-8"))["rules"]
room=rules["premiumRooms"]["$code"]
assert all(k in room for k in ("rulesVersion","replay","hintUsed","ready","hostAvatar","guestAvatar"))
assert "auth.uid === data.parent().child('guestUid').val()" in room["guestAvatar"][".write"]
assert "replay" in room["solved"]["$index"][".write"]
assert "rulesVersion" in room["solved"]["$index"][".write"]
assert "auth.uid === $uid" in room["ready"]["$uid"][".write"]
assert "configRevision" in room["ready"]["$uid"][".validate"]
assert "configRevision" in room["phase"][".write"]
assert "auth.uid === $uid" in room["hintUsed"]["$uid"]["$index"][".write"]
assert "auth.uid === $uid" in room["replay"]["$index"]["$uid"][".write"]
screen=(SRC/"PremiumOnlineScreen.kt").read_text("utf-8")
client=(SRC/"PremiumOnlineRoom.kt").read_text("utf-8")
assert all(k in screen for k in (
    "ArenaApprovedHome(", "ArenaApprovedRoomLobby(", "ArenaLiveScoreV2(",
    "ArenaVictoryV2(", "client.useHint(selected)",
    "PremiumOnlineLimits.canAttempt(match,selected,myUid)"))
assert "match.hostAvatar,match.guestAvatar" in screen
lobby=(SRC/"ArenaApprovedLobby.kt").read_text("utf-8")
assert "client::toggleReady" in lobby
assert "match.guestUid in match.readyBy" in lobby
assert "client.updateLobbySettings(" in lobby
assert "ArenaApprovedHome(" in (SRC/"PremiumArenaScreen.kt").read_text("utf-8")
assert "ArenaRulesCardV3(" in lobby
assert (SRC/"ArenaV3Rules.kt").is_file()
assert "ArenaRulesCardV3(" in (SRC/"ArenaApprovedHome.kt").read_text("utf-8")
theme=(SRC/"ArenaApprovedTheme.kt").read_text("utf-8")
home=(SRC/"ArenaApprovedHome.kt").read_text("utf-8")
assert all(k in theme for k in ("ArenaApprovedHeroArt", "ArenaApprovedPrimary",
    "ArenaApprovedOption", "purpleGradient", "greenGradient"))
assert all(k in home for k in ("Her maç yeni bir keşif", "İngilizce Seviyesi",
    "Konu / Paket", "Oyun Biçimi", "Maç Ayarları", "Tek Başına Oyna"))
assert all(k in lobby for k in ("Oda Lobisi", "Oda Kodu", "Yarışmayı Başlat",
    "Arkadaşlarını Davet Et", "ArenaApprovedPlayerCard", "client::toggleReady"))

assert all(k in client for k in (
    "fun useHint(index: Int)", "fun toggleReady()", "fun replayRound(",
    "fun canAttempt(", "rulesVersion", "hintUsed", "replayBy",
    "fun updateLobbySettings(", "configRevision"))
assert (TESTS/"ArenaDuelEngineTest.kt").is_file()
assert (TESTS/"ArenaFirebaseReplayTest.kt").is_file()
assert (TESTS/"ArenaFullWordInputTest.kt").is_file()
assert (TESTS/"ArenaMascotRigTest.kt").is_file()
rig=(SRC/"ArenaMascotRig.kt").read_text("utf-8")
renderer=(SRC/"ArenaRiggedMascot.kt").read_text("utf-8")
assert all(key in rig for key in (
    '"robot", "Bip"', '"fox", "Pufi"', '"owl", "Mimo"',
    '"rabbit", "Lila"', '"bear", "Tiko"',
    'TROPHY_PIVOT', 'LEFT_HAND', 'LEFT_KNEE', 'RIGHT_ELBOW',
    'fun isHierarchical()', 'fun pose('
))
assert all(key in renderer for key in (
    'ArenaRiggedMascot(', 'holdingTrophy:Boolean=false',
    'mascotArm(', 'mascotLeg(', 'mascotHead(', 'mascotTrophy(',
    'rememberInfiniteTransition(', 'withTransform('
))
assert "ArenaRiggedMascot(kind,modifier)" in (SRC/"ArenaV2Visuals.kt").read_text("utf-8")
assert "holdingTrophy=!isDraw" in (SRC/"ArenaV2Visuals.kt").read_text("utf-8")
assert "ArenaApprovedHeroArt(kind=mascot" in home
assert "ArenaMascotRig.choices" in home
assert "ArenaMascotRig.isValid(it)" in client
assert "newData.val() === 'rabbit'" in room["guestAvatar"][".validate"]
assert "newData.val() === 'bear'" in room["guestAvatar"][".validate"]

assert (SRC/"ArenaFullWordInput.kt").is_file()
solo=(SRC/"PremiumArenaScreen.kt").read_text("utf-8")
typing=(SRC/"ArenaCrosswordTyping.kt").read_text("utf-8")
assert 'label = { Text("Kelimeyi yaz") }' in screen
assert 'label = { Text("Kelimeyi yaz") }' in solo
assert 'ArenaOutlineButton("PAS", {' in screen
assert 'ArenaOutlineButton("PAS", {' in solo
assert 'crosswordFullWordMatches(fixedPattern' in screen
assert 'crosswordFullWordMatches(fixedPattern' in solo
assert 'crosswordFullWordPreview(' in typing
assert 'ArenaCrosswordTypingStrip(' not in screen
assert 'ArenaCrosswordTypingStrip(' not in solo
assert 'Eksik harfleri yaz' not in screen
assert 'Eksik harfleri yaz' not in solo
assert 'Spacer(Modifier.height(22.dp))' in screen
assert 'Spacer(Modifier.height(22.dp))' in solo

gradle=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'applicationIdSuffix = ".fresh"' in gradle
assert 'versionCode = 73' in gradle
assert 'versionName = "1.0.48-neon-word-memory"' in gradle
assert (SRC/"Teacher25DPortrait.kt").is_file()
print("Arena beta: staged UI, backend, rules and tests verified; Firebase rules NOT deployed.")
