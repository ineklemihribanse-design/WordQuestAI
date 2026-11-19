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
assert "auth.uid === $uid" in room["hintUsed"]["$uid"]["$index"][".write"]
assert "auth.uid === $uid" in room["replay"]["$index"]["$uid"][".write"]
screen=(SRC/"PremiumOnlineScreen.kt").read_text("utf-8")
client=(SRC/"PremiumOnlineRoom.kt").read_text("utf-8")
assert all(k in screen for k in (
    "ArenaHomeV2(", "ArenaRoomPlayersV2(", "ArenaLiveScoreV2(",
    "ArenaVictoryV2(", "client.useHint(selected)", "client::toggleReady",
    "PremiumOnlineLimits.canAttempt(match,selected,myUid)"))
assert "match.hostAvatar,match.guestAvatar" in screen
assert "match.guestUid in match.readyBy" in screen
assert all(k in client for k in (
    "fun useHint(index: Int)", "fun toggleReady()", "fun replayRound(",
    "fun canAttempt(", "rulesVersion", "hintUsed", "replayBy"))
assert (TESTS/"ArenaDuelEngineTest.kt").is_file()
assert (TESTS/"ArenaFirebaseReplayTest.kt").is_file()
gradle=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'applicationIdSuffix = ".offline101"' in gradle
assert 'versionCode = 72' in gradle
assert 'versionName = "1.0.47-vocabulary-2p5d-compat"' in gradle
assert (SRC/"Teacher25DPortrait.kt").is_file()
print("Arena beta: staged UI, backend, rules and tests verified; Firebase rules NOT deployed.")
