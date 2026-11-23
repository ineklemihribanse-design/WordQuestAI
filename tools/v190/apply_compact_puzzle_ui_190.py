#!/usr/bin/env python3
"""v190 compact crossword UI and consistent hint/draft overlays.

Installed only AFTER the seven packages and previous v190 UI overlays.
It must not change solved-word persistence, achievements, Firebase, or scoring.
The answer editor is relocated in the follow-up layout patch, after checking
the exact assembled source. All anchors fail closed.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
FILE=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"

def one(s,old,new,label):
    count=s.count(old)
    if count!=1:
        raise AssertionError(f"{label}: expected one match, found {count}")
    return s.replace(old,new,1)

def apply():
    src=FILE.read_text("utf-8")
    s=src

    # It was duplicating "YDS · Kolay · Karışık" above/below the heading.
    path='        Text(game.path, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp)\n'
    s=one(s,path,"","remove duplicated game level/category line")
    # Keep title, progress/score, Back and Shuffle controls intact.
    assert 'Text(game.title,' in s
    assert 'view.points.toString() + " puan"' in s

    # The answer clue remains readable but should not occupy a large card.
    s=one(s,
        "vertical = if (compact) 7.dp else 10.dp",
        "vertical = if (compact) 5.dp else 7.dp",
        "reduce question-card vertical padding")
    s=one(s,
        "fontSize = if (compact) 17.sp else 22.sp, lineHeight = if (compact) 20.sp else 25.sp,",
        "fontSize = if (compact) 15.sp else 18.sp, lineHeight = if (compact) 18.sp else 21.sp,\n                    maxLines = 2, overflow = TextOverflow.Ellipsis,",
        "smaller meaning and answer clue, two-line allowance")
    s=one(s,
        "if (compact) 27.dp else 34.dp",
        "if (compact) 25.dp else 30.dp",
        "reduce large clue number badge")
    s=one(s,
        'Text(word.entry.context, color = Forest, fontFamily = Sans, fontSize = if (compact) 12.sp else 14.sp, lineHeight = 17.sp)',
        'Text(word.entry.context, color = Forest, fontFamily = Sans, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)',
        "compact optional clue context")

    # Existing editor (Gönder) remains the only entry validation point.
    # The reveal command writes one authoritative character into view.letters.
    # Never overlay the revealed character with a blank unsent input preview.
    s=one(s,
        "remember(view.wordIndex, wordDraft, inlineTyping, view.locked) {",
        "remember(view.wordIndex, wordDraft, inlineTyping, view.letters, view.locked) {",
        "listen for newly revealed letter events")
    s=one(s,
        "if (cell !in view.locked) put(cell, typed.getOrNull(index))",
        """if (cell !in view.locked && view.letters[cell] == null)
                        put(cell, typed.getOrNull(index))""",
        "revealed hints and saved crossing letters must win over unsent draft")

    # Compact meaning card in landscape (clue on right) and portrait.
    original='ActiveClue(word, view.wordIndex in view.solvedIndices, false)'
    if s.count(original)!=2:
        raise AssertionError(f"expected two noncompact clue call sites, got {s.count(original)}")
    s=s.replace(original,'ActiveClue(word, view.wordIndex in view.solvedIndices, true)')

    assert "fun revealAndLearn()" in s
    assert '"Harf aç"' in s
    assert 'val previewLetters: Map<Cell, Char?>' in s
    assert 'inlineEditor()' in s
    assert 'submitInlineWord()' in s
    if src==s:raise AssertionError("no crossword UI change made")
    FILE.write_text(s,"utf-8")
    print("PASS v190: duplicate YDS label removed and clue card compact")
    print("PASS v190: opened hint letters and live answer draft render together")
    print("PASS v190: all scoring and validation handlers untouched")

if __name__=="__main__":
    apply()
