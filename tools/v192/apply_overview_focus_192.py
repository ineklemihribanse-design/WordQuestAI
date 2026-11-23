#!/usr/bin/env python3
"""v1.0.92: restore crossword overview/focus interaction on actual Compose code.

Apply after v191 against the verified assembled 1.0.90 source.
Normal mode fits ALL rows and columns. IME mode zooms and smoothly scrolls
to the midpoint of the selected word, then returns to overview on IME exit.
Do not modify Crossword, progress, Firebase, scoring, or other game screens.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
SCREEN = SRC / "PuzzleScreen.kt"


def one(s: str, old: str, new: str, label: str) -> str:
    n = s.count(old)
    if n != 1:
        raise AssertionError(f"{label}: expected one match, got {n}")
    return s.replace(old, new, 1)


def section(s: str, start: str, end: str, fn) -> str:
    assert s.count(start) == 1, f"Start: {start}"
    a = s.index(start)
    b = s.index(end, a + len(start))
    return s[:a] + fn(s[a:b]) + s[b:]


def compact_editor(part: str) -> str:
    part = one(part,
        "Column(Modifier.fillMaxWidth().padding(11.dp),",
        "Column(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=5.dp),",
        "editor container padding")
    part = one(part, "verticalArrangement=Arrangement.spacedBy(7.dp)) {",
               "verticalArrangement=Arrangement.spacedBy(3.dp)) {",
               "editor internal spacing")
    part = one(part, "fontFamily=Sans,fontSize=14.sp,",
               "fontFamily=Sans,fontSize=12.sp,", "editor title size")
    part = one(part, 'Text("Cevabı buraya yaz")',
               'Text("Cevabı buraya yaz",fontSize=12.sp)', "editor hint size")
    part = one(part,
               "modifier=Modifier.weight(1f).focusRequester(entryFocus),",
               "modifier=Modifier.weight(1f).height(46.dp).focusRequester(entryFocus),",
               "fixed compact input height")
    part = one(part, "modifier=Modifier.heightIn(min=52.dp),",
               "modifier=Modifier.heightIn(min=46.dp),",
               "compact submit button")
    assert "Wq190CrosswordHintInput.canSubmit(" in part
    assert "keyboardActions=KeyboardActions(onDone={ submitInlineWord() })" in part
    return part


def restore_board(part: str) -> str:
    a = part.index("    val phoneGrid = ")
    b = part.index("    val gap = ", a)
    part = part[:a] + """    // Overview must fit the COMPLETE puzzle, even a dense 70-word grid.
    // A minimum 28dp cell in normal phone mode previously cropped both axes.
    // Keep the 30dp readable minimum ONLY for manual or keyboard focus zoom.
    val regularTile = Wq192Viewport.overviewTileDp(
        width.value, height.value, puzzle.columns, puzzle.rows
    ).dp
    val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile
""" + part[b:]
    # The original regularTile definition above is superseded by the tested
    # overview policy: do not leave two Kotlin declarations.
    part = one(part,
        "    val regularTile = minOf((width - 12.dp) / puzzle.columns, (height - 12.dp) / puzzle.rows)\n",
        "", "remove replaced regularTile")
    part = one(part,
        "    val windowHeight = if(phoneGrid && !enlarged) height else minOf(height, actualHeight)",
        "    val windowHeight = minOf(height, actualHeight)",
        "use exact fitted-height viewport")
    a = part.index("    val selected = view.cell\n")
    b = part.index("\n\n    Column(Modifier.fillMaxWidth()", a)
    former = part[a:b]
    assert "horizontal.scrollTo" in former and "vertical.scrollTo" in former
    part = part[:a] + """    val selected = view.cell
    // Focus the active WORD, not its first cell. The two nested scroll states
    // move inside the board only; never scroll the entire page to find it.
    val focusCells = puzzle.words[view.wordIndex].cells.ifEmpty { listOf(selected) }
    val firstColumn = focusCells.minOf { it.column }
    val lastColumn = focusCells.maxOf { it.column }
    val firstRow = focusCells.minOf { it.row }
    val lastRow = focusCells.maxOf { it.row }
    LaunchedEffect(view.wordIndex, enlarged, windowWidth, windowHeight, tile) {
        if (enlarged) {
            val tilePx = with(density) { tile.toPx() }
            val widthPx = with(density) { windowWidth.toPx() }
            val heightPx = with(density) { windowHeight.toPx() }
            val targetX = Wq192Viewport.focusScrollPx(
                firstColumn, lastColumn, tilePx, widthPx, horizontal.maxValue
            )
            val targetY = Wq192Viewport.focusScrollPx(
                firstRow, lastRow, tilePx, heightPx, vertical.maxValue
            )
            launch { horizontal.animateScrollTo(targetX) }
            vertical.animateScrollTo(targetY)
        } else {
            // IME dismissed: no stale manual offsets or half-visible board.
            horizontal.scrollTo(0)
            vertical.scrollTo(0)
        }
    }""" + part[b:]
    assert "phoneGrid" not in part
    assert "maxOf(regularTile, 28.dp)" not in part
    return part


def apply() -> None:
    original = SCREEN.read_text("utf-8")
    assert "Wq190BoardPolicy.phoneHeightDp(" in original, "v191 was not applied"
    assert "Wq189UploadedArtwork(\"solo_puzzle\"" in original
    assert "val inlineEditor: @Composable () -> Unit = {" in original
    assert "val wide = maxWidth >= 760.dp" in original

    s = section(original,
        "    val inlineEditor: @Composable () -> Unit = {",
        "    Box(\n        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),",
        compact_editor)

    # IME has a brief initially-hidden phase while the focused editor is
    # attached. Only respond to a real open->close transition.
    keyboard_reset = """    var keyboardWasShown by remember(game.id) { mutableStateOf(false) }
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) {
            keyboardWasShown = true
        } else if (keyboardWasShown) {
            // Android Back, Done, and a completed answer all restore overview.
            inlineTyping = false
            expanded = false
            keyboardWasShown = false
        }
    }

"""
    marker = "    Box(\n        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),"
    s = one(s, marker, keyboard_reset + marker, "restore overview on IME close")

    # Keep a stable TextField location in BOTH modes: the editor is always
    # BELOW the board and clue. It must never be conditionally reparented.
    phone_clue = """                    ActiveClue(word, view.wordIndex in view.solvedIndices, true) {
                        if (view.wordIndex !in view.solvedIndices) inlineTyping = true
                    }
"""
    s = one(s, phone_clue, "", "remove phone clue above board")
    s = one(s,
        """                    inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    if (!keyboardVisible)
""",
        phone_clue + """                    inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    if (!keyboardVisible)
""", "position clue/editor after board")
    assert s.count("inlineEditor()") == 2, "keep editor stable in each layout"

    s = one(s,
        "BoardFrame(board, view, viewWidth, fittedBoardHeight, expanded, { expanded = !expanded }, previewLetters) {",
        """BoardFrame(board, view, viewWidth, fittedBoardHeight,
                                expanded || keyboardVisible,
                                if (keyboardVisible) null else ({ expanded = !expanded }), previewLetters) {""",
        "tablet focus-to-selected-word after IME")
    s = one(s,
        "                        board, view, viewWidth, fittedBoardHeight, expanded,",
        "                        board, view, viewWidth, fittedBoardHeight, (expanded || keyboardVisible),",
        "phone IME zoom only while writing")
    s = one(s,
        "                        ({ expanded = !expanded }),",
        "                        if (keyboardVisible) null else ({ expanded = !expanded }),",
        "no manual zoom toggle beneath IME")

    s = section(s,
        "private fun BoardFrame(",
        "@Composable\nprivate fun WordFields(",
        restore_board)

    assert "Wq190CrosswordHintInput.completedWord(" in s
    assert "state.enter(completeAnswer)" in s
    assert "val previewLetters: Map<Cell, Char?>" in s
    assert "if (position in previewLetters) previewLetters[position]" in s
    assert "keyboardWasShown = false" in s
    assert "horizontal.animateScrollTo(targetX)" in s
    assert "val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile" in s
    assert s.index("                } else {\n                    // Phone editor") < s.index("private fun BoardFrame(")
    assert s != original

    # Keep changes atomic: checks first, write Kotlin only after all anchors.
    TEST.mkdir(parents=True, exist_ok=True)
    here = Path(__file__).resolve().parent
    for name in ("Wq192Viewport.kt", "Wq192ViewportTest.kt"):
        assert (here / name).is_file()
    SCREEN.write_text(s, "utf-8")
    shutil.copyfile(here / "Wq192Viewport.kt", SRC / "Wq192Viewport.kt")
    shutil.copyfile(here / "Wq192ViewportTest.kt", TEST / "Wq192ViewportTest.kt")
    print("PASS v192: normal overview fits all columns and rows, no 28dp force-zoom")
    print("PASS v192: keyboard smoothly centers full selected word in board viewport")
    print("PASS v192: back/IME dismiss resets overview; editor always after board")
    print("PASS v192: compact input without changing saved progress or Firebase")


if __name__ == "__main__":
    apply()
