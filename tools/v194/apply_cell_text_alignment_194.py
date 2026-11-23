#!/usr/bin/env python3
"""v1.0.94: center crossword letters independently of top-left clue numbers.

Surgical visual-only Compose patch. No puzzle BoardFrame, board dimensions,
overview/focus zoom, input editing, score, or Firebase changes are permitted.
Run after v191, v192 and v193 patches against the complete cached source.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"

def apply() -> None:
    original = SCREEN.read_text(encoding="utf-8")
    start_marker = "private fun LetterCell("
    assert original.count(start_marker) == 1, "Expected exactly one LetterCell"
    start = original.index(start_marker)
    end = original.find("\n@Composable", start + len(start_marker))
    assert end != -1, "LetterCell section boundary missing"
    before, cell, after = original[:start], original[start:end], original[end:]

    number_label = 'Text(number.toString(), Modifier.align(Alignment.TopStart).padding(start = 1.dp),'
    old_letter = 'Text(letter.toString(), Modifier.padding(top = if (number == null) 0.dp else 3.dp),'
    new_letter = 'Text(letter.toString(), Modifier.align(Alignment.Center),'
    assert cell.count(number_label) == 1, "Keep clue number top-left unchanged"
    assert cell.count(old_letter) == 1, "Old conditional 3dp letter shift not found"
    assert cell.count('contentAlignment = Alignment.Center,') == 1
    assert cell.count('if (number != null) {') == 1
    assert cell.count('if (letter != null) {') == 1

    cell = cell.replace(old_letter, new_letter, 1)
    assert 'padding(top = if (number == null) 0.dp else 3.dp)' not in cell
    assert cell.count(number_label) == 1
    assert cell.count(new_letter) == 1

    updated = before + cell + after
    assert updated.replace(new_letter, old_letter, 1) == original, (
        "Only the letter modifier may change; board mechanics must remain byte-identical"
    )
    for unchanged in (
        "Wq192Viewport.overviewTileDp(",
        "horizontal.animateScrollTo(targetX)",
        "keyboardWasShown = false",
        "Wq193EditableDraft.protectedPositions(",
        "state.enter(completeAnswer)",
    ):
        assert unchanged in updated, unchanged

    SCREEN.write_text(updated, encoding="utf-8")
    print("PASS v194: 11 / 6 / 7 and all other numbered squares center letter exactly")
    print("PASS v194: small number remains upper-left, no change to font sizes or board geometry")
    print("PASS v194: only LetterCell letter modifier changed, all gameplay untouched")

if __name__ == "__main__":
    apply()
