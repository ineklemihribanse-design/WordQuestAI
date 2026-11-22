#!/usr/bin/env python3
"""P6 static security/data-preservation acceptance after full source assembly."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
rules=json.loads((root/"source/database.rules.json").read_text("utf-8"))["rules"]
room=rules["premiumRooms"]["$code"]
extra=room["package6"]

def contains(source,name,*anchors):
    body=(src/name).read_text("utf-8")
    for anchor in anchors:
        assert anchor in body,(name,anchor)
    return body

assert "auth != null" in room[".read"]
assert "hostUid" in room[".read"] and "guestUid" in room[".read"]
assert "auth.uid" in room[".read"]
assert "quotaSlot" in room[".write"]
assert "duel" in room["mode"][".validate"]
assert "team" in room["mode"][".validate"]
assert "quiz" in room["mode"][".validate"]
assert "5" in room["size"][".validate"]
assert "20" in room["size"][".validate"]
assert "guestUid" in room["guestUid"][".write"]
assert "auth.uid === $uid" in room["answers"]["$uid"]["$index"][".write"]
assert "mode" in room["answers"]["$uid"]["$index"][".write"]
assert "team" in room["solved"]["$index"][".write"]
assert set(extra)=={"presence","emoji","finishVotes","skips","settings",
    "hintClaims","retrySpent"}
for category in ("emoji","finishVotes","presence","skips",
                 "hintClaims","retrySpent"):
    assert "$uid" in extra[category],category
assert "auth.uid" in extra["settings"][".write"]
assert "lobby" in extra["settings"][".write"]
assert "settings" in extra["hintClaims"]["$uid"]["$slot"][".write"]
assert "settings" in extra["retrySpent"]["$uid"]["$index"][".write"]
assert "2" in extra["skips"]["$uid"][".write"]
assert "newData" in extra["finishVotes"]["$uid"][".write"]

home=contains(src,"HomeScreen.kt","confirmLeaveWork","navigationTrail",
              "Çalışmadan çıkılsın mı?")
arena=contains(src,"ArenaApprovedHome.kt","Wq175ArenaHeroAndModes(",
               "Wq186ArenaPresetControls(", "Wq174UnifiedMatchSettings(")
assert "if (!matchSettingsPage)" not in arena
landing=contains(src,"Wq175PremiumHome.kt",
                 'Wq175HomeTile("ARENA"')
assert '{ activeSection="ARENA" }' not in landing
live=contains(src,"PremiumOnlineScreen.kt",
              "Wq186ArenaSocialHud(", "Wq186ArenaHintAction(",
              "submitWithRetry(selected,true)", "submitWithRetry(selected,false)",
              "Wq186ArenaNext.choose")
assert "Wq179ArenaWordChooser(puzzle,solved,selected)" not in live
assert "if (match.phase == \"finished\") Wq183OnlinePortraitStrip(" in live
contains(src,"PremiumOnlineRoom.kt","package6Rules","package6Retries",
         "submitWithRetry", "hintClaims", "premiumRooms")
contains(src,"PremiumArenaContent.kt","levelForWord",
         "levelPool: List<String>","target.level")
contains(src,"PuzzleScreen.kt","typedDialog","wordDraft","Kelimeyi yaz")
contains(src,"ArenaBoardV2.kt","hintLetters:Map<Int,Char>",
         "val hinted=active")
for file in ("Wq186ArenaFairPlay.kt","Wq186ArenaHintNetwork.kt",
             "Wq186ArenaSkipNetwork.kt","Wq186ArenaSocialHud.kt",
             "Wq186ArenaPresetControls.kt"):
    assert (src/file).is_file(),file

gradle=(root/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionCode = 112' in gradle and 'versionName = "1.0.86"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
print("PASS P6 secure member-only Firebase, quiz/team, two skips, random hints, "+
      "retry budget, back confirmation, direct Arena and untouched app identity.")
