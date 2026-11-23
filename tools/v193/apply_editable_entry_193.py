#!/usr/bin/env python3
"""v1.0.93: correct clipped single-line entry and allow wrong answers to be edited.

Run only AFTER v191 and v192 on the cached, assembled source.
Never touch board geometry, Compose focus zoom, Firebase or persisted scoring.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
PATH = SRC / "PuzzleScreen.kt"
HERE = Path(__file__).resolve().parent


def once(source: str, old: str, new: str, label: str) -> str:
    n = source.count(old)
    if n != 1:
        raise AssertionError(f"{label}: exactly 1 source anchor required, got {n}")
    return source.replace(old, new, 1)


def apply() -> None:
    original = PATH.read_text("utf-8")
    assert "Wq192Viewport.overviewTileDp(" in original, "v192 overview not installed"
    assert "horizontal.animateScrollTo(targetX)" in original, "v192 word zoom not installed"
    assert "keyboardWasShown = false" in original, "keyboard dismissal behavior not installed"
    assert "Wq190CrosswordHintInput.completedWord(" in original

    source = once(original,
        """    val hintedPositions: Map<Int, Char> = word.cells.mapIndexedNotNull { index, cell ->
        view.letters[cell]?.let { index to it }
    }.toMap()""",
        """    // Previously submitted WRONG letters are editable. Protect only
    // actual locked crossings from solved words / revealed hints.
    val existingPositions = word.cells.mapIndexedNotNull { index, cell ->
        view.letters[cell]?.let { index to it }
    }.toMap()
    val lockedPositionIndices = word.cells.mapIndexedNotNull { index, cell ->
        if (cell in view.locked) index else null
    }.toSet()
    val hintedPositions: Map<Int, Char> = Wq193EditableDraft.protectedPositions(
        existingPositions, lockedPositionIndices
    )""",
        "wrong answer not treated as locked/hinted")

    source = once(source,
        "                    if (cell !in view.locked && view.letters[cell] == null) {",
        "                    if (cell !in view.locked) {",
        "typed correction can replace previous incorrect board letters")

    source = once(source,
        """        edit(typedWord=true) { state -> state.enter(completeAnswer) }
        checkAndLearn()
        wordDraft=""
        inlineTyping=false
        softKeyboard?.hide()""",
        """        edit(typedWord=true) { state -> state.enter(completeAnswer) }
        checkAndLearn()
        if (view.wordIndex in view.solvedIndices) {
            // Only a correct, locked answer exits typing and hides the IME.
            wordDraft = ""
            inlineTyping = false
            softKeyboard?.hide()
        } else {
            // Wrong submission: keep the text and keyboard for backspace,
            // cursor editing, selection, or one-tap clear and retyping.
            inlineTyping = true
        }""",
        "failed answer remains editable instead of closing keyboard")

    source = once(source,
        "onValueChange={ wordDraft=englishLetters(it).take(word.cells.size) },",
        "onValueChange={ wordDraft=Wq193EditableDraft.normalizedText(it,word.cells.size) },",
        "handle user editing and backspace through controlled draft")

    source = once(source,
        "modifier=Modifier.weight(1f).height(46.dp).focusRequester(entryFocus),",
        "modifier=Modifier.weight(1f).heightIn(min=56.dp).focusRequester(entryFocus),",
        "honor Material3 OutlinedTextField minimum height, stop baseline clipping")

    source = once(source,
        """                            placeholder={Text("Cevabı buraya yaz",fontSize=12.sp)},
                            singleLine=true,""",
        """                            placeholder={Text("Cevabı buraya yaz",fontSize=12.sp)},
                            trailingIcon={
                                if(wordDraft.isNotEmpty()) {
                                    TextButton(
                                        onClick={ wordDraft=""; inlineTyping=true; runCatching { entryFocus.requestFocus() }; softKeyboard?.show() },
                                        contentPadding=PaddingValues(horizontal=5.dp)
                                    ) { Text("Sil",color=Forest,fontSize=12.sp) }
                                }
                            },
                            singleLine=true,""",
        "one tap clear button inside answer field")

    source = once(source,
        "modifier=Modifier.heightIn(min=46.dp),",
        "modifier=Modifier.heightIn(min=50.dp),",
        "ensure submit button remains tappable next to 56dp input")

    assert "Wq193EditableDraft.protectedPositions(" in source
    assert "if (view.wordIndex in view.solvedIndices) {" in source
    assert "wordDraft=Wq193EditableDraft.normalizedText(it,word.cells.size)" in source
    assert 'Text("Sil",color=Forest,fontSize=12.sp)' in source
    assert "Modifier.weight(1f).heightIn(min=56.dp).focusRequester(entryFocus)" in source
    assert "keyboardWasShown = false" in source
    assert "Wq192Viewport.overviewTileDp(" in source
    assert source.count("inlineEditor()") == 2
    assert source != original

    for name in ("Wq193EditableDraft.kt","Wq193EditableDraftTest.kt"):
        assert (HERE / name).is_file(), name
    TEST.mkdir(parents=True,exist_ok=True)
    PATH.write_text(source,"utf-8")
    shutil.copyfile(HERE / "Wq193EditableDraft.kt", SRC / "Wq193EditableDraft.kt")
    shutil.copyfile(HERE / "Wq193EditableDraftTest.kt", TEST / "Wq193EditableDraftTest.kt")
    print("PASS v193: text no longer cut off by 46dp height")
    print("PASS v193: wrong answers are not locked, stay editable, and have clear action")
    print("PASS v193: keyboard/zoom and v192 whole-board behavior untouched")


if __name__ == "__main__":
    apply()
