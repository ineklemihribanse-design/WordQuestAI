#!/usr/bin/env python3
"""v189: fit the complete crossword on phones and preview typed letters live.

Apply after v188 to the assembled Android source, without modifying progress,
score, saved words or Firebase data. Fail closed if the upstream UI changed.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATH = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"


def once(text: str, before: str, after: str, name: str) -> str:
    count = text.count(before)
    if count != 1:
        raise AssertionError(f"{name}: expected one anchor, found {count}")
    return text.replace(before, after, 1)


def apply() -> None:
    source = PATH.read_text(encoding="utf-8")
    s = source

    # The former default and keyboard-time zoom clipped rows/columns.
    # Actual post-v175 sources retain the compact-board preference in the
    # remember key. Parse the Kotlin initializer as a balanced block: do not
    # depend on an obsolete single-line source string.
    expanded_start = "    var expanded by remember("
    assert s.count(expanded_start) == 1, "expected one zoom preference initializer"
    a = s.index(expanded_start)
    brace = s.index("{", a)
    depth = 0
    end = None
    for n in range(brace, len(s)):
        if s[n] == "{":
            depth += 1
        elif s[n] == "}":
            depth -= 1
            if depth == 0:
                end = n
                break
    assert end is not None
    prefix = s[a:brace + 1]
    assert "game.id" in prefix and "expanded" in prefix
    s = s[:a] + prefix + " mutableStateOf(false) }" + s[end + 1:]

    s = once(s,
        "val keyboardBoard = (maxHeight * .42f).coerceIn(116.dp, 330.dp)",
        "val keyboardBoard = (maxHeight * .52f).coerceIn(145.dp, 460.dp)",
        "allocate more available keyboard-time height")
    s = once(s,
        "val keyboardKeys = game.puzzle.words.size > 8\n",
        "",
        "remove automatic zoom shortcut")
    # Some v175 phone layouts already use tighter outer gutters.
    if "val available = (maxWidth - 36.dp).coerceAtLeast(220.dp)" in s:
        s = once(s,
            "val available = (maxWidth - 36.dp).coerceAtLeast(220.dp)",
            "val available = (maxWidth - 20.dp).coerceAtLeast(220.dp)",
            "give the board the full phone content width")

    # The assembled source has a multiline boardHeight policy. Leave it
    # intact and introduce an explicit full-grid fit size for BOTH call sites.
    s = once(s,
        "            val boardHeight = if (keyboardVisible) keyboardBoard",
        """            val fittedBoardHeight = if (keyboardVisible) keyboardBoard
                else viewWidth * (board.rows.toFloat() / board.columns.coerceAtLeast(1).toFloat())
            val boardHeight = if (keyboardVisible) keyboardBoard""",
        "insert responsive whole-grid board height")
    assert s.count("viewWidth, boardHeight,") == 2, "both grid call sites needed"
    s = s.replace("viewWidth, boardHeight,", "viewWidth, fittedBoardHeight,")

    if ".padding(horizontal = if (wide) 23.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp)" in s:
        s = once(s,
            ".padding(horizontal = if (wide) 23.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp)",
            ".padding(horizontal = if (wide) 23.dp else 10.dp, vertical = if (keyboardVisible) 6.dp else 9.dp)",
            "remove wasted surrounding phone space")

    if "Arrangement.spacedBy(if (keyboardVisible) 8.dp else 14.dp)" in s:
        s = once(s,
            "Arrangement.spacedBy(if (keyboardVisible) 8.dp else 14.dp)",
            "Arrangement.spacedBy(if (keyboardVisible) 7.dp else 10.dp)",
            "compact vertical sections")
    s = once(s,
        "expanded || (keyboardVisible && keyboardKeys)",
        "expanded",
        "never force clipped zoom when IME appears")

    # Draft-only preview: rendered letters follow each edit and backspace.
    # Do not call Crossword.enter until the user presses Gönder, maintaining
    # the existing answer checker, scoring and persistent progress behaviour.
    s = once(s,
        "    val softKeyboard = LocalSoftwareKeyboardController.current\n",
        """    val softKeyboard = LocalSoftwareKeyboardController.current
    val previewLetters: Map<Cell, Char?> =
        remember(view.wordIndex, wordDraft, inlineTyping, view.locked) {
            if (!inlineTyping && wordDraft.isEmpty()) emptyMap()
            else buildMap<Cell, Char?> {
                val typed = englishLetters(wordDraft).uppercase()
                word.cells.forEachIndexed { index, cell ->
                    // A solved crossing retains its authoritative letter.
                    if (cell !in view.locked) put(cell, typed.getOrNull(index))
                }
            }
        }
""",
        "derive all visible draft letters without editing saved progress")

    s = once(s,
        "BoardFrame(board, view, viewWidth, boardHeight, expanded, { expanded = !expanded }) {",
        "BoardFrame(board, view, viewWidth, boardHeight, expanded, { expanded = !expanded }, previewLetters) {",
        "show live letters in tablet grid")
    s = once(s,
        """                        if (keyboardVisible) null else ({ expanded = !expanded }),
                    ) {""",
        """                        if (keyboardVisible) null else ({ expanded = !expanded }),
                        previewLetters,
                    ) {""",
        "show live letters in phone grid")
    s = once(s,
        "toggleZoom: (() -> Unit)?, onCell: (Cell) -> Unit,",
        "toggleZoom: (() -> Unit)?, previewLetters: Map<Cell, Char?>, onCell: (Cell) -> Unit,",
        "pass live preview to grid cells")
    s = once(s,
        "                                    size, tile, puzzle.numbers[position], view.letters[position],",
        "                                    size, tile, puzzle.numbers[position],\n                                    if (position in previewLetters) previewLetters[position] else view.letters[position],",
        "paint draft over the grid, including erased positions")

    # Shorten the oversized A1/level header; retain all nav/progress actions.
    if "Text(game.path, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp)" in s:
        s = once(s,
            "Text(game.path, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp)",
            "Text(game.path, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp)",
            "compact level caption")
    if "Text(game.title, color = Ink, fontFamily = Serif, fontSize = 33.sp, lineHeight = 36.sp)" in s:
        s = once(s,
            "Text(game.title, color = Ink, fontFamily = Serif, fontSize = 33.sp, lineHeight = 36.sp)",
            "Text(game.title, color = Ink, fontFamily = Serif, fontSize = 26.sp, lineHeight = 29.sp)",
            "compact level title")
    if "Modifier.fillMaxWidth().padding(top = 4.dp)," in s:
        s = once(s,
            "Modifier.fillMaxWidth().padding(top = 4.dp),",
            "Modifier.fillMaxWidth().padding(top = 2.dp),",
            "compact heading progress gap")

    assert "inlineEditor()" in s and "submitInlineWord()" in s
    assert s.count("previewLetters,") == 1 and s.count(", previewLetters) {") == 1
    assert "expanded || (keyboardVisible && keyboardKeys)" not in s
    if s == source:
        raise AssertionError("No crossword changes")
    PATH.write_text(s, encoding="utf-8")
    print("PASS v189: full board, compact header and live unsaved typing preview")


if __name__ == "__main__":
    apply()
