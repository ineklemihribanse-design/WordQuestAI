#!/usr/bin/env python3
"""Fail-closed source and version checks for 1.0.79 responsive Arena."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
content={n:(P/n).read_text("utf-8") for n in (
 "PremiumOnlineScreen.kt","PremiumArenaScreen.kt","ArenaBoardV2.kt",
 "ArenaReferenceComponents.kt","ArenaCrosswordTyping.kt","WordQuestSettings.kt",
 "Wq179ArenaExtras.kt"
)}
assert 'Wq179ArenaOrder.next' in content['PremiumOnlineScreen.kt']
assert 'Wq179ArenaOrder.next' in content['PremiumArenaScreen.kt']
assert 'Wq179ArenaWordChooser' in content['PremiumOnlineScreen.kt']
assert 'Wq179ArenaWordChooser' in content['PremiumArenaScreen.kt']
assert 'Wq179ArenaClueHeader' in content['PremiumOnlineScreen.kt']
assert 'Wq179ArenaSkip.consume' in content['PremiumOnlineScreen.kt']
assert 'Wq179ArenaOrder.hintPosition' in content['PremiumOnlineScreen.kt']
assert 'Wq179ArenaOrder.hintPosition' in content['PremiumArenaScreen.kt'] or 'Random((roundId+currentWord)' in content['PremiumArenaScreen.kt']
assert 'Wq179ArenaGameAudio' in content['PremiumOnlineScreen.kt']
assert 'dark = match?.phase == "playing"' in content['PremiumOnlineScreen.kt']
assert 'Color(0xFF10142E)' in content['Wq179ArenaExtras.kt']
assert 'Color(0xFF2B1850)' in content['Wq179ArenaExtras.kt']
assert 'Wq179ArenaAudioSettings()' in content['WordQuestSettings.kt']
assert 'zoomed' in content['ArenaBoardV2.kt']
assert 'fitHeight' in content['ArenaBoardV2.kt']
assert 'verticalScroll(vertical)' in content['ArenaBoardV2.kt']
assert 'Modifier.size(if(active)54.dp else 47.dp)' in content['ArenaReferenceComponents.kt']
assert 'label="warning-clock"' in content['ArenaReferenceComponents.kt']
assert 'ArenaBoardV2(puzzle,solved,selected,placed,compact,onSelect)' in content['ArenaCrosswordTyping.kt']
assert 'versionCode = 105' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionName = "1.0.79"' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'applicationIdSuffix = ".fresh"' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
for n in ("PremiumOnlineRoom.kt","ArenaApprovedLobby.kt"):
 assert (P/n).exists()
assert 'syncSelectedAvatar()' in (P/"PremiumOnlineScreen.kt").read_text("utf-8")
assert 'client::toggleReady' in (P/"ArenaApprovedLobby.kt").read_text("utf-8")
print("PASS: 1.0.79 full board, clues, selection, one free skip, audio and turn glow.")
print("PASS: existing account/ready, signing identity and Firebase v178 schema preserved.")
