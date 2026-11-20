#!/usr/bin/env python3
from pathlib import Path
src=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"HomeScreen.kt").read_text("utf-8")
for text in (
    'Wq175MyPuzzlesHero(session.customGames.size, wq175CompletedCount)',
    'wq175CompletedOnly by rememberSaveable',
    'session.record(it)?.completed == true',
    'PrimaryAction("Bulmaca ekle", { edit(null) })',
    'go(Page.Backup)',
    'GameItem(game, session.record(game), ::open) { edit(it.destination, it) }'
):
    assert text in s,text
art=(src/"WordQuest175MyPuzzles.kt").read_text("utf-8")
assert 'Wq175GlossIcon("puzzle"' in art
print("PASS: real crosswords, edit, backups and completed-filter remain connected.")
