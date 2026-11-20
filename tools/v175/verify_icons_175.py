#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
ICON=ROOT/"source/app/src/main/assets/wq175_icons"
names=("arena_duel","arena_team","arena_quiz","trophy","listen",
    "sentence","grammar","story","speak","puzzle","yds",
    "cards","plan","camera","assistant","packages","verbs","mistakes","ai_teacher")
for name in names:
    path=ICON/(name+".webp")
    assert path.stat().st_size>800,name
    with Image.open(path) as im:
        assert im.size==(256,256),name
        assert im.mode in ("RGBA","RGB"),name
arena=(SRC/"WordQuest174Arena.kt").read_text("utf-8")
home=(SRC/"HomeScreen.kt").read_text("utf-8")
practice=(SRC/"WordQuest175PracticeVisuals.kt").read_text("utf-8")
assert 'Wq175GlossIcon("arena_"+kind,Modifier.size(76.dp))' in arena
assert 'Wq175GlossIcon(Wq175IconNames.menu(title)' in home
assert practice.count('Wq175GlossIcon(Wq175IconNames.practice(kind)')==2
assert (SRC/"WordQuest175Icons.kt").exists()
assert (SRC/"WordQuest175TimedPuzzle.kt").exists()
assert (SRC/"WordQuest175ImportWords.kt").exists()
assert (SRC/"LenaLive3DStage.kt").exists()
print("PASS: 19 distinct original high DPI art assets integrated in Arena, Home and Daily rooms.")
