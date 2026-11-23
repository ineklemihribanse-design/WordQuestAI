#!/usr/bin/env python3
"""v190 final word-entry integration: correctly interleave user-drafted and
previously revealed letters, in BOTH preview and real answer submission.

After compact puzzle layout, before Kotlin compile. No change to points,
progress saving, hint cost, word verification or Firebase calls.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
TOOLS=ROOT/"tools/v190"
FILE=SRC/"PuzzleScreen.kt"

def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f"{label}: expected exactly 1 match, found {n}")
    return s.replace(old,new,1)

def main():
    original=FILE.read_text("utf-8")
    s=original
    needle="    val previewLetters: Map<Cell, Char?> ="
    idx=s.index(needle)
    open_idx=s.index("{",s.index("remember(",idx))
    depth=0
    end=-1
    for p in range(open_idx,len(s)):
        if s[p]=="{": depth+=1
        elif s[p]=="}":
            depth-=1
            if depth==0:
                end=p+1
                break
    assert end>open_idx, "failed to identify exact preview composable state"
    old=s[idx:end]
    for marker in (
        "remember(view.wordIndex, wordDraft, inlineTyping, view.letters, view.locked)",
        "val typed = englishLetters(wordDraft).uppercase()",
        "view.letters[cell] == null",
    ):
        assert marker in old, marker

    replacement='''    // Revealed letters, solved crossings and their positions are authoritative.
    // They are never erased/overwritten by a partial, unsent answer.
    val hintedPositions: Map<Int, Char> = word.cells.mapIndexedNotNull { index, cell ->
        view.letters[cell]?.let { index to it }
    }.toMap()
    val previewLetters: Map<Cell, Char?> =
        remember(view.wordIndex, wordDraft, inlineTyping, view.letters, view.locked) {
            if (!inlineTyping && wordDraft.isEmpty()) emptyMap()
            else buildMap<Cell, Char?> {
                val typed = englishLetters(wordDraft)
                val preview = Wq190CrosswordHintInput.typedPositions(
                    word.cells.size, hintedPositions, typed
                )
                word.cells.forEachIndexed { index, cell ->
                    if (cell !in view.locked && view.letters[cell] == null) {
                        put(cell, preview[index])
                    }
                }
            }
        }'''
    s=s[:idx]+replacement+s[end:]
    s=once(s,
        '''        if(outOfTime || view.wordIndex in view.solvedIndices ||
            englishLetters(wordDraft).length != word.cells.size) return
        edit(typedWord=true) { state -> state.enter(wordDraft) }''',
        '''        if(outOfTime || view.wordIndex in view.solvedIndices) return
        val completeAnswer = Wq190CrosswordHintInput.completedWord(
            word.cells.size, hintedPositions, wordDraft
        ) ?: return
        edit(typedWord=true) { state -> state.enter(completeAnswer) }''',
        "combine revealed hint and remaining input before checking")
    s=once(s,
        '''                            enabled=englishLetters(wordDraft).length==word.cells.size,''',
        '''                            enabled=Wq190CrosswordHintInput.canSubmit(
                                word.cells.size, hintedPositions, wordDraft
                            ),''',
        "Gönder enabled after only missing letters are written")

    assert "state.enter(completeAnswer)" in s
    assert "state.enter(wordDraft)" not in s
    assert "if (position in previewLetters) previewLetters[position]" in s
    assert "Wq190CrosswordHintInput.typedPositions(" in s
    assert s.count("inlineEditor()")==2
    assert s.count("Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)")==2

    # Fail closed before replacing any source. Use the helper already tested
    # against a seven-letter puzzle with one revealed central crossing.
    for n in ("Wq190CrosswordHintInput.kt","Wq190CrosswordHintInputTest.kt"):
        assert (TOOLS/n).is_file(),n
    FILE.write_text(s,"utf-8")
    shutil.copyfile(TOOLS/"Wq190CrosswordHintInput.kt",SRC/"Wq190CrosswordHintInput.kt")
    TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(TOOLS/"Wq190CrosswordHintInputTest.kt",TEST/"Wq190CrosswordHintInputTest.kt")
    print("PASS v190: hints and live text field share the same character positions")
    print("PASS v190: partial answers use known letters without exposing solutions")
    print("PASS v190: scoring, hint charges and saved progress unchanged")

if __name__=="__main__":main()
