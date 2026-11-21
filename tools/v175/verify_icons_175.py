#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
ICON=ROOT/"source/app/src/main/assets/wq175_icons"
names=("arena_duel","arena_team","arena_quiz","trophy","listen",
    "sentence","grammar","story","speak","puzzle","yds",
    "cards","plan","camera","assistant","packages","verbs","mistakes","ai_teacher",
    "pdf","scan","translate","paragraph","synonym","completion",
    "exam","review","timer","success","notebook",
       "analytics","insights","friends","account","settings","backup",
       "history","library","streak","messages",
       "level_a1","level_a2","level_b1","level_b2","level_c1","level_c2")
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
assert practice.count('Wq175GlossIcon(Wq175IconNames.practice(kind)')==3
# Daily Plan tile, exercise header icon and interior background must all exist.
assert (SRC/"WordQuest175Icons.kt").exists()
assert (ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175PremiumIconRoutingTest.kt").is_file()
for token in ('"analytics"','"insights"','"friends"','"account"',
              '"settings"','"backup"','"history"','"library"','"streak"','"messages"',
              '"pdf"','"camera"','"scan"','"paragraph"','"translate"',
              '"synonym"','"completion"','"exam"','"review"',
              '"notebook"','"timer"','"success"'):
    assert "-> "+token in (SRC/"WordQuest175Icons.kt").read_text("utf-8")
icon_router=(SRC/"WordQuest175Icons.kt").read_text("utf-8")
assert '"yanlışlarımdan" in s -> "mistakes"' in icon_router
assert '"kişisel ai öğretmen" in s -> "ai_teacher"' in icon_router

assert (SRC/"WordQuest175TimedPuzzle.kt").exists()
assert (SRC/"WordQuest175ImportWords.kt").exists()
assert (SRC/"LenaLive3DStage.kt").exists()
print("PASS: 46 original 256px WebP icons with six A1–C2 illustrations.")
