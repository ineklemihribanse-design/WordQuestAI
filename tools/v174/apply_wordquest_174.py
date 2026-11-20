#!/usr/bin/env python3
"""Overlay the approved 2026-11-20 Arena and Word Memory premium layouts.

Runs AFTER tools/v173_2d/apply_wordquest_173_2d.py; reuses the original app
logic and user data. Bumps signed in-place update code99 -> code100.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
PKG=ROOT/"tools/v174"

def exactly_one(s,a,b,description):
    if s.count(a)!=1:
        raise AssertionError(f"{description}: expected one anchor; found {s.count(a)}")
    return s.replace(a,b,1)

def arena():
    p=SRC/"ArenaApprovedHome.kt"
    s=p.read_text("utf-8")
    # Remove the SECOND character selector and the old banner in one block.
    # Keep only the clickable profile/character pill already in the top row.
    start=s.index('        Text("Arena",Modifier.fillMaxWidth(),')
    end=s.index('        Arena173UnifiedMatchSettings(',start)
    removed=s[start:end]
    assert removed.count("Wq2DCompactSelector(")==1,removed
    assert removed.count("Arena173AdventurePicker(")==1,removed
    s=s[:start]+'''        Wq174ArenaHeroAndModes(
            selectedAvatar=mascot,mode=mode,solo=solo,onMode=onMode
        )
'''+s[end:]
    s=exactly_one(s,'        Arena173UnifiedMatchSettings(',
        '        Wq174UnifiedMatchSettings(',"premium match settings")
    s=exactly_one(s,'Text(playerName.ifBlank{"Profil"},color=ArenaApproved.ink,',
        'Text(Wq2DMascots.label(mascot),color=ArenaApproved.ink,',
        "single character name in top button")
    s=exactly_one(s,
        '''            green=true
        )''',
        '''            green=false
        )''',"purple match-start action")
    assert s.count("Wq174ArenaHeroAndModes(")==1
    assert s.count("Wq174UnifiedMatchSettings(")==1
    assert "Wq2DCompactSelector(" not in s
    # This picker is still present exactly ONCE at the end of the home.
    assert s.count("Wq2DCharacterPicker(")==1
    p.write_text(s,"utf-8")

def memory():
    p=SRC/"VocabularyNeonHome.kt"
    s=p.read_text("utf-8")
    start=s.index('                Row(Modifier.fillMaxWidth().heightIn(min = 164.dp)')
    end=s.index('                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))',start)
    old=s[start:end]
    assert old.count("Wq2DCompactSelector(")==1
    s=s[:start]+'''                Wq174MemoryHero(
                    selected=selectedAvatar,
                    onPick={robotPicker=true},
                    isCompact=isCompact
                )
'''+s[end:]
    start=s.index('                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))',start)
    end=s.index('                NeonPanel {',start)
    s=s[:start]+'''                Wq174MemoryStats(total=total,due=due,learned=learned)
'''+s[end:]
    btn='''                    Button(onClick = { start("card", "DUE") }, enabled = due > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryGold,
                            contentColor = MemoryNavy), modifier = Modifier.fillMaxWidth()) {
                        Text(if (due > 0) "Tekrara Başla" else "Bugünlük tekrar tamam",
                            fontWeight = FontWeight.Bold)
                    }'''
    assert s.count(btn)==1,"review button upstream changed"
    s=s.replace(btn,'''                    Wq174GoldButton(
                        label=if (due > 0) "Tekrara Başla" else "Bugünlük tekrar tamam",
                        enabled=due > 0
                    ) { start("card", "DUE") }''',1)
    tile='''                            MemoryModeTile(tile, Modifier.weight(1f), total > 0) {
                                start(tile.mode)
                            }'''
    assert s.count(tile)==1,"study tile anchor changed"
    s=s.replace(tile,'''                            Wq174MemoryModeCard(
                                title=tile.title,subtitle=tile.subtitle,
                                mode=tile.mode,enabled=total > 0,
                                modifier=Modifier.weight(1f)
                            ) { start(tile.mode) }''',1)
    assert s.count("Wq174MemoryHero(")==1
    assert s.count("Wq174MemoryStats(")==1
    assert s.count("Wq174MemoryModeCard(")==1
    assert s.count("Wq2DCharacterPicker(")==1
    p.write_text(s,"utf-8")

def main():
    gradle=APP/"build.gradle.kts"
    s=gradle.read_text("utf-8")
    assert s.count("versionCode = 99")==1
    assert s.count('versionName = "1.0.73-original-2d-complete"')==1
    assert 'applicationIdSuffix = ".fresh"' in s
    assert 'applicationIdSuffix = ".fresh.qa"' not in s
    for name in ("WordQuest174Arena.kt","WordQuest174Memory.kt"):
        shutil.copyfile(PKG/name,SRC/name)
    for required in ("LenaLive3DStage.kt","ConversationScreen.kt",
                     "VocabularyNeonPractice.kt","ArenaV2Visuals.kt",
                     "WordQuest2DCharacters.kt"):
        assert (SRC/required).exists(),required
    arena()
    memory()
    s=s.replace("versionCode = 99","versionCode = 100",1)
    s=s.replace('versionName = "1.0.73-original-2d-complete"',
        'versionName = "1.0.74-premium-arena-memory"',1)
    gradle.write_text(s,"utf-8")
    print("WordQuest v1.0.74/code100: approved Arena and Word Memory premium UI.")
    print("One avatar selector per screen, real callbacks, previous data preserved.")
    print("Package ID, signing config, Lena, US/UK voices and updater unchanged.")

if __name__=="__main__":
    main()
