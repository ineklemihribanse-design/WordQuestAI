#!/usr/bin/env python3
"""Give clue starts priority over crossing words; no UI or board changes."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
p=PKG/"Crossword.kt"
s=p.read_text("utf-8")
old='''        val choices = puzzle.wordsAt[target] ?: return
        val selected = if (lastGridTap == target && choices.size == 2 && wordIndex in choices) {'''
new='''        val touchingWords = puzzle.wordsAt[target] ?: return
        // A numbered square is an explicit clue selector. A crossing word
        // passing through that square must not override its printed number.
        val numberedStarts = touchingWords.filter { puzzle.words[it].start == target }
        val choices = numberedStarts.ifEmpty { touchingWords }
        val selected = if (lastGridTap == target && choices.size == 2 && wordIndex in choices) {'''
assert s.count(old)==1,("selectCell anchor",s.count(old))
before=s
s=s.replace(old,new,1)
assert s.replace(new,old,1)==before
p.write_text(s,"utf-8")
TEST.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/"tools/v212/Wq212NumberedCellSelectionTest.kt",TEST/"Wq212NumberedCellSelectionTest.kt")
print("PASS: only selectCell candidate priority changed; same layout, cursor placement, typing, scoring and existing unnumbered crossing logic")
