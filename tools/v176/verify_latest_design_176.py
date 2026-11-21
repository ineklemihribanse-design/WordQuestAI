#!/usr/bin/env python3
"""Protect the user's last approved mobile/tablet design in the 1.0.76 content build.

This runs *after* the 1.0.75 scene/layout overlay and the 1.0.76 question
overlay. It checks the actual composed Android source, not just design scripts.
It does not replace UI assets, change stored profiles, or waive emulator QA.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

required={
    "PuzzleScreen.kt":{
        "phone board takes substantially more actual space": "(maxHeight * .47f).coerceIn(330.dp, 440.dp)",
        "phones use their own responsive geometry": "val phoneLayout = maxWidth < 600.dp",
        "normal phone cells are large, not zoom-only": "else if(phoneGrid) maxOf(regularTile, 28.dp)",
        "selection remains scrollable": "if (enlarged || phoneGrid)",
    },
    "WordQuest175Arena.kt":{
        "distinct phone/tablet stage aspect": "if(compact)310.dp else if(wideTablet)378.dp else 342.dp",
        "mascot centered over stage podium": "Modifier.align(Alignment.BottomCenter)",
        "mascot feet anchored at podium": ".offset(y=(-43).dp)",
        "original arena scene": 'Wq175SceneBackdrop("arena"',
    },
    "WordQuest175Matching.kt":{
        "tablet and mobile have separate banner ratios": "height(if(wide)255.dp else 224.dp)",
        "original matching art remains": 'Wq175ThemeBanner("eslestirmelerim"',
    },
    "HomeScreen.kt":{
        "native bottom bar remains in design": "Wq175HomeBottomNav(",
        "distinct package catalogue route": "Page.PackageCatalog -> Wq175PackageCatalog(",
        "profile-only mascot picker": "Wq2DCompactSelector(chosenMaskot.value",
        "home scene is present": 'Wq175SceneBackdrop("ana_sayfa"',
    },
    "ConversationScreen.kt":{
        "mobile speak button never wraps": 'Text(if(recording) "Kayıt" else "Konuş",maxLines=1,softWrap=false',
        "mobile listen button never wraps": 'Text(if(speaking==null)"Dinle" else "Dur",maxLines=1,softWrap=false',
    },
    "WordQuestSettings.kt":{
        "voice selector is inside a collapsing card": 'accent.label+" cihaz sesleri"',
    },
    "WordQuest175ThemeArt.kt":{
        "theme backdrop implementation": "Wq175SceneBackdrop(slug,Modifier.matchParentSize())",
    },
}
for filename,checks in required.items():
    content=(SRC/filename).read_text("utf-8")
    for description,needle in checks.items():
        assert needle in content,(filename,description)
    print("DESIGN PASS:",filename,len(checks),"preservation checks")

home=(SRC/"HomeScreen.kt").read_text("utf-8")
assert "Modifier.fillMaxWidth().fillMaxHeight(.96f)" not in home,\
    "Home portrait returned over the original illustrated banner"
assert "Wq175PremiumHomeGrid(" in home, "Landing page menu missing"
assert "Wq175HomeStartButton(newGame)" in home, "Continue/New Game order changed"

puzzle=(SRC/"PuzzleScreen.kt").read_text("utf-8")
assert 'var expanded by remember(game.id) { mutableStateOf(board.words.size > 20) }' in puzzle
assert 'mutableStateOf(board.words.size > 20 || compactBoard)' not in puzzle
assert "BoardFrame(board, view, viewWidth, boardHeight" in puzzle
assert "Actions(view, ::checkAndLearn, ::revealAndLearn" in puzzle
print("DESIGN PASS: last approved artwork/layout remains active on phone and tablet")
print("LIMITATION: screenshots still need manual inspection; code checks are not perceptual QA")
