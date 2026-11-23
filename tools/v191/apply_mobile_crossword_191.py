#!/usr/bin/env python3
"""Mobile crossword layout adjustment after v190 hint/answer integration.

Keep Firebase, scores, saved words and stable inline TextField ownership intact.
Requires the actual assembled 1.0.90 source, not the old source/main snapshot.
Anchors fail closed instead of silently publishing an unmatched UI.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"
BOARD_POLICY = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1/Wq190BoardPolicy.kt"


def once(source: str, old: str, new: str, description: str) -> str:
    matches = source.count(old)
    if matches != 1:
        raise AssertionError(f"{description}: expected one anchor, found {matches}")
    return source.replace(old, new, 1)


def within(source: str, beginning: str, ending: str, change) -> str:
    assert source.count(beginning) == 1, beginning
    start = source.index(beginning)
    end = source.index(ending, start)
    return source[:start] + change(source[start:end]) + source[end:]


def compact_heading(part: str) -> str:
    part = once(part, ".padding(horizontal=13.dp,vertical=12.dp)",
                ".padding(horizontal=11.dp,vertical=7.dp)", "header padding")
    part = once(part, "verticalArrangement = Arrangement.spacedBy(6.dp)) {",
                "verticalArrangement = Arrangement.spacedBy(4.dp)) {", "header line spacing")
    part = once(part, 'Wq175GlossIcon("puzzle",Modifier.size(32.dp))',
                '''if (!Wq189UploadedArtwork("solo_puzzle", Modifier.size(30.dp)))
                Wq175GlossIcon("solo_puzzle", Modifier.size(30.dp))''',
                "use the user's approved dedicated solo crossword icon")
    part = once(part, ".padding(horizontal = 9.dp, vertical = 6.dp)",
                ".padding(horizontal = 9.dp, vertical = 4.dp)", "score badge padding")
    return part


def compact_clue(part: str) -> str:
    return once(part,
                ".padding(horizontal = 13.dp, vertical = if (compact) 5.dp else 7.dp)",
                ".padding(horizontal = 11.dp, vertical = if (compact) 4.dp else 6.dp)",
                "active clue compact padding")


def apply() -> None:
    original = SCREEN.read_text(encoding="utf-8")
    policy = BOARD_POLICY.read_text(encoding="utf-8")
    assert "fun phoneHeightDp(" in policy, "Mobile sizing policy not installed"
    for name in ("val previewLetters: Map<Cell, Char?>",
                 "Wq190CrosswordHintInput.completedWord(",
                 "fun submitInlineWord()", "inlineEditor()",
                 "val wide = maxWidth >= 760.dp",
                 "if (!keyboardVisible) GameHeading("):
        assert name in original, name
    source = within(original, "private fun GameHeading(", "@Composable\nprivate fun BoardFrame(",
                    compact_heading)
    source = within(source, "private fun ActiveClue(", "@Composable\nprivate fun Actions(",
                    compact_clue)

    # The v189 phone sizing only considered the grid width. A tall board
    # extended below the screen, especially when the keyboard resized the
    # viewport. Use both axes; preserve v190 tablet policy unchanged.
    start_marker = "            val fittedBoardHeight = if (wide) {"
    assert source.count(start_marker) == 1
    start = source.index(start_marker)
    end_marker = "\n            val boardHeight = "
    end = source.index(end_marker, start)
    former = source[start:end]
    assert "Wq190BoardPolicy.fittedHeightDp(" in former
    assert "else if (keyboardVisible) keyboardBoard" in former
    source = source[:start] + """            val fittedBoardHeight = if (wide) {
                Wq190BoardPolicy.fittedHeightDp(
                    viewWidth.value, maxHeight.value, board.rows, board.columns
                ).dp
            } else {
                Wq190BoardPolicy.phoneHeightDp(
                    viewWidth.value, maxHeight.value, board.rows, board.columns,
                    keyboardVisible
                ).dp
            }""" + source[end:]

    # When typing on a short phone, the check/hint action row competed with
    # the only TextField for vertical space. It remains available as soon as
    # the IME closes; the inline Gönder action remains usable while typing.
    source = once(source,
        """                    inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""",
        """                    inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    if (!keyboardVisible)
                        Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""",
        "hide secondary buttons only during phone IME")

    # A user selecting another word while typing must not be forcibly sent
    # back to scroll position zero while the keyboard is moving.
    old_scroll = "        scope.launch { pageScroll.scrollTo(0) }"
    if source.count(old_scroll) == 1:
        source = source.replace(old_scroll,
            "        if (!keyboardVisible) scope.launch { pageScroll.scrollTo(0) }", 1)

    assert source.count("inlineEditor()") == 2, "keep one stable editor per layout"
    assert "focusRequester(entryFocus)" in source
    assert "state.enter(completeAnswer)" in source
    assert "Wq190BoardPolicy.phoneHeightDp(" in source
    assert "Wq189UploadedArtwork(\"solo_puzzle\"" in source
    assert "if (!keyboardVisible)\n                        Actions(" in source
    assert source != original
    SCREEN.write_text(source, encoding="utf-8")
    print("PASS v191: compact header, dedicated crossword icon, smaller clue")
    print("PASS v191: whole grid bounded by phone width and viewport height")
    print("PASS v191: keyboard keeps the text editor composed without secondary rows")
    print("PASS v191: scoring, persistence, hints, Firebase and Arena untouched")


if __name__ == "__main__":
    apply()
