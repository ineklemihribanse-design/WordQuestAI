#!/usr/bin/env python3
from pathlib import Path
src=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"HomeScreen.kt").read_text("utf-8")
for text in (
    'Wq175MyPuzzlesHero(session.customGames.size, wq175CompletedCount)',
    'wq175CompletedOnly by rememberSaveable',
    'session.record(it)?.completed == true',
    'PrimaryAction("Bulmaca ekle", { edit(null) })',
    'Wq175MatchingEntry(onClick={wq175MatchingOpen=true})',
    'Wq175MatchingDialog(session)',
    'wq175MatchingOpen by rememberSaveable',
    'go(Page.Backup)',
    'GameItem(game, session.record(game), ::open) { edit(it.destination, it) }'
):
    assert text in s,text
art=(src/"WordQuest175MyPuzzles.kt").read_text("utf-8")
assert 'Wq175GlossIcon("puzzle"' in art
print("PASS: real crosswords, edit, backups and completed-filter remain connected.")

matching=(src/"WordQuest175Matching.kt").read_text("utf-8")
for token in ('"vocabulary-extra.tsv"', '"vocabulary-addendum-v175.tsv"',
              'session.searchLearning("",0,5000)', 'store.mistake(',
              'store.finish(', 'finishGame(Wq175MatchEnd.Completed)',
              'wq175MatchResultTitle(endReason)', 'wrongPairs=(wrongPairs+mistaken).distinctBy',
              'Wq175MatchingEntry',
              'Wq175MatchingDialog'):
    assert token in matching,token
assert (src.parents[5]/"src/test/java/com/kelimebulmacasi/a1/Wq175MatchingTest.kt").exists()
for name in ('"timer"','"review"','"notebook"','"success"'):
    assert name in matching, name
assert 'Wq175GlossIcon(modeArt,Modifier.size(57.dp))' in matching
print("PASS: matching cards, game, local profile stats and Kotlin test installed")
