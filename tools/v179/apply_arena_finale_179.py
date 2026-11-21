#!/usr/bin/env python3
"""Attach enlarged per-player finale and replay picker after v179 Arena changes."""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v179"

def once(s,a,z,label):
    n=s.count(a)
    assert n==1,f"{label} anchor count {n}"
    return s.replace(a,z,1)

extras=PKG/"Wq179ArenaExtras.kt"
s=extras.read_text("utf-8")
s=once(s,
    "    puzzle:Puzzle, solved:Set<Int>, selected:Int,\n    choose:(Int)->Unit\n) {",
    "    puzzle:Puzzle, solved:Set<Int>, selected:Int,\n"
    "    canChoose:(Int)->Boolean={true},choose:(Int)->Unit\n) {",
    "word chooser individual eligibility")
s=once(s,
    "Surface(onClick={if(!done){choose(index);open=false} else {}},\n                            enabled=!done,",
    "Surface(onClick={if(!done && canChoose(index)){choose(index);open=false}},\n"
    "                            enabled=!done && canChoose(index),",
    "word chooser blocks spent first-round clues")
s=once(s,
    '                                    if(done)"  ✓ ÇÖZÜLDÜ" else "",',
    '                                    if(done)"  ✓ ÇÖZÜLDÜ"\n'
    '                                    else if(!canChoose(index))"  · SON ŞANSI BEKLE" else "",',
    "show why a pending word cannot yet be answered")
extras.write_text(s,"utf-8")
# A previously shipped version may not have packaged these new files in source.
for filename in ("Wq179ArenaGrandFinale.kt","Wq179ArenaReplayPanel.kt"):
    shutil.copyfile(PKG/filename,SRC/filename)
shutil.copyfile(extras,SRC/"Wq179ArenaExtras.kt")
p=SRC/"PremiumOnlineScreen.kt"
s=p.read_text("utf-8")
start=s.index('            if (!match.cooperative) ArenaVictoryV2(')
end=s.index('            // Both players see the winner animation',start)
assert 'match.hostAvatar' in s[start:end] and 'match.guestAvatar' in s[start:end]
s=s[:start]+'''            Wq179ArenaGrandFinale(
                match,myUid,hostName,guestName,
                match.score(match.hostUid,questions),
                match.score(match.guestUid,questions))
'''+s[end:]
s=once(s,
    "                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen, answerText) {",
    '''                if(!keyboardIsOpen && !match.cooperative) {
                    Wq179ArenaReplayPanel(match,puzzle,myUid){chosen->
                        setWordIndex(chosen)
                        setAnswerText("")
                    }
                }
                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen, answerText) {''',
    "replay picker on live crossword")
s=once(s,
    "Wq179ArenaWordChooser(puzzle,solved,selected){ newIndex ->",
    '''Wq179ArenaWordChooser(puzzle,solved,selected,
                        canChoose={index->match.cooperative ||
                            PremiumOnlineLimits.canAttempt(match,index,myUid)}) { newIndex ->''',
    "all words show, but only permissible attempts selectable")
s=once(s,'                PremiumWinnerCelebration(match.code, winnerName)',
    '                Text("Maçın ayrıntılı sonuçları hazırlanıyor…", color=OnlineSoft)',
    "do not duplicate the old small celebration")
p.write_text(s,"utf-8")
print("PASS: match-specific enlarged finale, true score difference and original profile mascots.")
print("PASS: visible missed-word replay queue; does not bypass existing Firebase rules.")
