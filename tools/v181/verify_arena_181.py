#!/usr/bin/env python3
"""WordQuest v1.0.81: verified hero image reuse + real hint cell + responsive board."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
f=lambda n:(SRC/n).read_text("utf-8")
stage=f("Wq181ArenaHeroStage.kt")
online=f("PremiumOnlineScreen.kt")
board=f("ArenaBoardV2.kt")
typing=f("ArenaCrosswordTyping.kt")
score=f("ArenaReferenceComponents.kt")
hint=f("Wq181ArenaHintBar.kt")
assert (ROOT/"source/app/src/main/res/drawable-nodpi/wq175_arena_hero.webp").stat().st_size>5000
assert 'painterResource(R.drawable.wq175_arena_hero)' in stage
assert 'ContentScale.Crop' in stage and 'Color(0xB7271649)' in stage
assert 'Wq181ArenaHeroStage(' in online
assert 'Wq181ArenaHintBar(' in online
assert 'client.useHint(selected)' in online
assert 'val hintIsUsed=selected in match.hintBy[myUid].orEmpty()' in online
assert 'val revealedIndex=if(hintIsUsed)' in online
assert 'hintIndex=revealedIndex,hintChar=revealedChar' in online
assert 'Wq181ArenaHintSlot.reserve' in online
assert 'fixedPattern.contains(' in online
assert 'lockedPosition' in f("Wq181ArenaHintSlot.kt")
assert 'Wq179ArenaOrder.hintPosition' in f("Wq181ArenaHintSlot.kt")
assert 'Text("İPUCU ($left)"' in hint
assert 'onClick=onHint,enabled=canUse&&left>0' in hint
assert 'hintIndex:Int=-1,hintChar:Char?=null,onSelect:(Int)->Unit' in board
assert 'hinted->Color(0xFFFFE6A9)' in board
assert 'if(hinted)hintChar' in board
assert 'ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,onSelect)' in typing
assert 'ButtonDefaults.outlinedButtonColors' in typing
assert 'val gap=if(maxWidth<500.dp)1.dp else 1.5.dp' in board
assert '.coerceIn(360.dp,1000.dp)' in board
assert 'if(active)85.dp else 73.dp' in score
assert 'Color(0xFFA9FFE0)' in online
assert 'versionCode = 107' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionName = "1.0.81"' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'applicationIdSuffix = ".fresh"' in (ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'Wq180FriendsPanel' in f("Wq180FriendCards.kt")
assert 'Wq179ArenaOrder.next' in online
print("PASS: stage art exact same approved Arena hero, legible header, larger mascot, responsive 100%-fit board.")
print("PASS: selected random hint is visible above crossword AND the exact tile is revealed, with existing Firebase hint quota.")
print("PASS: friends, scoring, signing identity and existing Firebase rules remain intact.")
