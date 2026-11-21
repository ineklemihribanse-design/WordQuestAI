#!/usr/bin/env python3
"""WordQuest 1.0.80: social cards, readable Arena palette and mobile/tablet board.

Runs after the v179 overlay, never edits saved player data or Firebase account IDs.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS=ROOT/"tools/v180"

def once(s,old,new,label):
    count=s.count(old)
    if count!=1:
        raise AssertionError(f"{label}: expected one anchor, found {count}")
    return s.replace(old,new,1)

print("Applying 1.0.80 Arena + Account UI without changing saved player state")
shutil.copyfile(TOOLS/"Wq180ArenaBoard.kt",SRC/"ArenaBoardV2.kt")
shutil.copyfile(TOOLS/"Wq180ArenaStage.kt",SRC/"Wq180ArenaStage.kt")

typing=SRC/"ArenaCrosswordTyping.kt"
s=typing.read_text("utf-8")
s=once(s,
    "import androidx.compose.material3.Text",
    "import androidx.compose.material3.Text\nimport androidx.compose.material3.OutlinedButton",
    "external zoom button import")
s=once(s,
    "import androidx.compose.runtime.Composable",
    "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable",
    "save zoom state per puzzle")
s=once(s,
    '''    ArenaBoardV2(puzzle,solved,selected,placed,compact,onSelect)''',
    '''    var zoomed by rememberSaveable(puzzle.hashCode()) { mutableStateOf(false) }
    // Controls live OUTSIDE the grid container, preserving cell visibility.
    Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=3.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween){
        Text(if(compact)"SEÇİLİ KELİME" else "TÜM TAHTA",
            color=androidx.compose.ui.graphics.Color(0xFF6D57A8),
            fontSize=11.sp,fontWeight=FontWeight.ExtraBold)
        OutlinedButton(onClick={zoomed=!zoomed},
            contentPadding=PaddingValues(horizontal=14.dp,vertical=4.dp)){
            Text(if(zoomed)"Küçült" else "Büyüt ⤢",fontSize=12.sp)
        }
    }
    ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,onSelect)''',
    "large centered crossword and zoom outside grid")
typing.write_text(s,"utf-8")

# The all-clues action was dark green on a transparent stadium surface.
# Give it a crisp light button with dark text on every game background.
extras=SRC/"Wq179ArenaExtras.kt"
s=extras.read_text("utf-8")
s=once(s,
    '''    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(16.dp)) {
        Text("☷  TÜM KELİMELER  ·  $unsolved kaldı",fontWeight=FontWeight.Bold)
    }''',
    '''    Button(onClick={open=true},modifier=Modifier.fillMaxWidth(),
        colors=ButtonDefaults.buttonColors(
            containerColor=Color(0xFFF3E8FF),
            contentColor=Color(0xFF402759)),
        shape=RoundedCornerShape(16.dp)) {
        Text("☷  TÜM KELİMELER  ·  $unsolved kaldı",
            color=Color(0xFF402759),fontWeight=FontWeight.Bold)
    }''',
    "visible high-contrast all-clues button")
extras.write_text(s,"utf-8")

online=SRC/"PremiumOnlineScreen.kt"
s=online.read_text("utf-8")
s=once(s,"Wq179ArenaStageBackground(\n            Modifier.matchParentSize(), dark = match?.phase == \"playing\")",
    "Wq180ArenaStageBackground(\n            Modifier.matchParentSize(), dark = match?.phase == \"playing\")",
    "warm lavender stadium background")
online.write_text(s,"utf-8")

accounts=SRC/"Accounts.kt"
s=accounts.read_text("utf-8")
s=once(s,
    '                "/directory/$userId" to null,',
    '                "/directory/$userId" to null,\n                "/friendProfiles/$userId" to null,',
    "private shared summary deleted with account")
accounts.write_text(s,"utf-8")

gradle=ROOT/"source/app/build.gradle.kts"
s=gradle.read_text("utf-8")
assert s.count('versionCode = 105')==1 and s.count('versionName = "1.0.79"')==1
assert 'applicationIdSuffix = ".fresh"' in s
gradle.write_text(s.replace('versionCode = 105','versionCode = 106',1)
    .replace('versionName = "1.0.79"','versionName = "1.0.80"',1),"utf-8")
print("PASS: versionCode 106, same package ID and signing identity, new grid layout")
