#!/usr/bin/env python3
"""Source checks: both player avatars + lobby ready, no accidental regressions."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
room=(PKG/"PremiumOnlineRoom.kt").read_text("utf-8")
screen=(PKG/"PremiumOnlineScreen.kt").read_text("utf-8")
visuals=(PKG/"ArenaV2Visuals.kt").read_text("utf-8")
profiles=(PKG/"WordQuest2DCharacters.kt").read_text("utf-8")
rules=json.loads((ROOT/"source/database.rules.json").read_text("utf-8"))
premium=rules["rules"]["premiumRooms"]["$code"]
assert "Wq2DMascots.selected(context, session.profileKey)" in room
assert 'getString("kind","robot")' not in room
assert 'val key = if (isHost) "hostAvatar" else "guestAvatar"' in room
assert 'client.syncSelectedAvatar()' in screen
assert "if(Wq2DMascots.valid(kind))" in visuals
assert "Wq2DPortrait(kind,modifier)" in visuals
assert 'fun toggleReady()' in room and '"ready"' in room
assert 'ArenaApprovedRoomLobby(' in screen
lobby=(PKG/"ArenaApprovedLobby.kt").read_text("utf-8")
assert 'val allReady=guestPresent && hostReady && guestReady' in lobby
assert 'onReady=client::toggleReady' in lobby
assert "premiumRooms" in rules["rules"] and "roomQuota" in rules["rules"]
assert "ready" in premium and "$uid" in premium["ready"]
assert "guestAvatar" in premium and "hostAvatar" in premium
assert "configRevision" in premium["phase"][".write"]
assert all("newData.val() === '"+x+"'" in premium["guestAvatar"][".validate"]
           for x in ("piko","astra","foksi","robot","fox"))
assert 'versionCode = 104' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionName = "1.0.78"' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
print("PASS: both avatars, ready permissions, authorized start gate and v1.0.78 identity.")
