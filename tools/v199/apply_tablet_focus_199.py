#!/usr/bin/env python3
"""Tablet-only crossword focus patch after v194. No changes to phone UI,
overview, cells, editor, clue list, scoring, persistence or New Game."""
from pathlib import Path
import shutil

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
SCREEN = SRC / "PuzzleScreen.kt"


def one(s, old, new):
    assert s.count(old) == 1, (old[:160], s.count(old))
    return s.replace(old, new, 1)


def patch(original):
    s = original
    # Width of the tablet, not current orientation or available IME width.
    # In landscape on a phone, smallestScreenWidthDp remains below 600.
    start = '            val fittedBoardHeight = if (wide) {'
    end = '\n            val boardHeight = '
    i = s.index(start)
    j = s.index(end, i)
    old_height = s[i:j]
    assert 'Wq190BoardPolicy.fittedHeightDp(' in old_height
    assert 'Wq190BoardPolicy.phoneHeightDp(' in old_height
    new_height = old_height.replace(
        '                Wq190BoardPolicy.fittedHeightDp(',
        '                val originalTabletHeight = Wq190BoardPolicy.fittedHeightDp(', 1)
    anchor = '                ).dp\n            } else {'
    new_height = one(new_height, anchor, '''                ).dp
                if (keyboardVisible &&
                    androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600) {
                    Wq199TabletFocus.keyboardBoardHeightDp(
                        maxHeight.value, originalTabletHeight.value
                    ).dp
                } else originalTabletHeight
            } else {''')
    s = s[:i] + new_height + s[j:]

    i = s.index('private fun BoardFrame(')
    j = s.index('@Composable\nprivate fun WordFields(', i)
    before, board, after = s[:i], s[i:j], s[j:]
    old_board = board
    selected = '''    val selected = view.cell
    // Focus the active WORD, not its first cell. The two nested scroll states
    // move inside the board only; never scroll the entire page to find it.
    val focusCells = puzzle.words[view.wordIndex].cells.ifEmpty { listOf(selected) }
    val firstColumn = focusCells.minOf { it.column }
    val lastColumn = focusCells.maxOf { it.column }
    val firstRow = focusCells.minOf { it.row }
    val lastRow = focusCells.maxOf { it.row }
'''
    board = one(board, selected, '')
    old_tile = '    val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile'
    new_tile = selected + '''    val tabletFocus = Wq199TabletFocus.enabled(
        androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp, enlarged
    )
    var measuredBoardWidth by remember { mutableStateOf(0) }
    var measuredBoardHeight by remember { mutableStateOf(0) }
    // Keep the old zoom and all ordinary overview geometry on phones.
    val tile = if (tabletFocus) Wq199TabletFocus.tileDp(
        maxOf(regularTile, 30.dp).value, width.value, height.value,
        lastColumn - firstColumn + 1, lastRow - firstRow + 1, 2f
    ).dp else if (enlarged) maxOf(regularTile, 30.dp) else regularTile'''
    board = one(board, old_tile, new_tile)
    old_effect = '''    LaunchedEffect(view.wordIndex, enlarged, windowWidth, windowHeight, tile) {
        if (enlarged) {'''
    new_effect = '''    LaunchedEffect(view.wordIndex, enlarged, windowWidth, windowHeight, tile, tabletFocus) {
        if (tabletFocus) {
            // Wait for actual scroll extents and viewport sizes after IME
            // animation/rotation. Never center against stale pre-IME bounds.
            snapshotFlow {
                listOf(horizontal.maxValue, vertical.maxValue,
                    measuredBoardWidth, measuredBoardHeight)
            }.collectLatest { measured ->
                val (maxX, maxY, viewportX, viewportY) = measured
                if (maxX == Int.MAX_VALUE || maxY == Int.MAX_VALUE ||
                    viewportX <= 0 || viewportY <= 0) return@collectLatest
                val cellPx = with(density) { (tile - gap).roundToPx().coerceAtLeast(1).toFloat() }
                val tilePx = cellPx + with(density) { gap.roundToPx().toFloat() }
                val insetPx = with(density) { 6.dp.toPx() }
                val targetX = Wq199TabletFocus.scrollPx(
                    firstColumn, lastColumn, tilePx, cellPx,
                    viewportX, maxX, insetPx)
                val targetY = Wq199TabletFocus.scrollPx(
                    firstRow, lastRow, tilePx, cellPx,
                    viewportY, maxY, insetPx)
                kotlinx.coroutines.coroutineScope {
                    launch { horizontal.animateScrollTo(targetX) }
                    vertical.animateScrollTo(targetY)
                }
            }
        } else if (enlarged) {'''
    board = one(board, old_effect, new_effect)
    board = one(board,
        'val gap = if (tile < 25.dp) 1.dp else 2.dp',
        'val gap = if (tabletFocus && tile <= 2.dp) 0.dp else if (tile < 25.dp) 1.dp else 2.dp')
    board = one(board,
        'Modifier.width(windowWidth).height(windowHeight).clip(boardShape).background(Board)',
        '''Modifier.width(windowWidth).height(windowHeight)
                .then(if (tabletFocus) Modifier.onSizeChanged {
                    measuredBoardWidth = it.width
                    measuredBoardHeight = it.height
                } else Modifier).clip(boardShape).background(Board)''')
    # The original phone scroll and overview reset branch must remain identical.
    assert board[board.index('        } else if (enlarged) {') + len('        } else '):board.index('    Column(Modifier.fillMaxWidth()')] == old_board[old_board.index('        if (enlarged) {') + 8:old_board.index('    Column(Modifier.fillMaxWidth()')]
    s = before + board + after
    imports = []
    for item in ('androidx.compose.runtime.snapshotFlow', 'kotlinx.coroutines.flow.collectLatest', 'androidx.compose.ui.layout.onSizeChanged'):
        if 'import ' + item not in s:
            imports.append('import ' + item)
    if imports:
        pos = s.index('\nimport ')
        s = s[:pos] + '\n' + '\n'.join(imports) + s[pos:]
    for marker in ('state.enter(completeAnswer)', 'keyboardWasShown = false',
                   'Wq193EditableDraft.protectedPositions(', 'Wq192Viewport.overviewTileDp(',
                   'Wq190BoardPolicy.phoneHeightDp(', 'horizontal.animateScrollTo(targetX)'):
        assert marker in original and marker in s, marker
    assert s.count('inlineEditor()') == original.count('inlineEditor()') == 2
    # Reverse the narrow source edits to prove all other code is identical.
    restored = s
    for item in imports:
        restored = restored.replace(item + '\n', '', 1)
    restored = one(restored, new_height, old_height)
    restored = one(restored, board, old_board)
    assert restored == original
    return s


def main():
    source = patch(SCREEN.read_text("utf-8"))
    for name in ("Wq199TabletFocus.kt", "Wq199TabletFocusTest.kt"):
        assert (HERE / name).is_file(), name
    TEST.mkdir(parents=True, exist_ok=True)
    SCREEN.write_text(source, "utf-8")
    shutil.copyfile(HERE / "Wq199TabletFocus.kt", SRC / "Wq199TabletFocus.kt")
    shutil.copyfile(HERE / "Wq199TabletFocusTest.kt", TEST / "Wq199TabletFocusTest.kt")
    print("PASS tablet focus patch: selected word fits visible area; measured IME scroll limits")
    print("PASS phone sizing and focus branch unchanged; all other puzzle source byte-identical")


if __name__ == "__main__":
    main()
