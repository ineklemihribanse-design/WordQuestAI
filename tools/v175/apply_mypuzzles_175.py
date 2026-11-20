#!/usr/bin/env python3
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
shutil.copyfile(ROOT/"tools/v175/WordQuest175MyPuzzles.kt",
    SRC/"WordQuest175MyPuzzles.kt")
p=SRC/"HomeScreen.kt"
s=p.read_text("utf-8")
a='''        Page.Custom -> ScreenPage("Bulmacalarım", "Kendi kelimelerinle oyna", ::back) {
            PrimaryAction("Bulmaca ekle", { edit(null) })'''
b='''        Page.Custom -> ScreenPage("Bulmacalarım", "Kendi kelimelerinle oyna", ::back) {
            var wq175CompletedOnly by rememberSaveable { mutableStateOf(false) }
            val wq175CompletedCount = session.customGames.count {
                session.record(it)?.completed == true
            }
            Wq175MyPuzzlesHero(session.customGames.size, wq175CompletedCount)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                FilterChip(selected=!wq175CompletedOnly,
                    onClick={wq175CompletedOnly=false},label={Text("Tüm Bulmacalar")})
                FilterChip(selected=wq175CompletedOnly,
                    onClick={wq175CompletedOnly=true},label={Text("Tamamlanan")})
            }
            PrimaryAction("Bulmaca ekle", { edit(null) })'''
if s.count(a)!=1:raise AssertionError("My Puzzles intro anchor: "+str(s.count(a)))
s=s.replace(a,b,1)
a='''            for (game in session.customGames) GameItem(game, session.record(game), ::open) { edit(it.destination, it) }'''
b='''            for (game in session.customGames.filter {
                !wq175CompletedOnly || session.record(it)?.completed == true
            }) GameItem(game, session.record(game), ::open) { edit(it.destination, it) }'''
if s.count(a)!=1:raise AssertionError("My Puzzles filter anchor: "+str(s.count(a)))
s=s.replace(a,b,1)
p.write_text(s,"utf-8")
print("PASS: My Puzzles shows real saved/completed progress and reusable existing editor/backup.")
