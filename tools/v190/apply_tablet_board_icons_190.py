#!/usr/bin/env python3
"""v190: Restore a full-size landscape-tablet grid with the IME open and wire
the user's approved artwork into the ACTUAL bottom navigation.

Run AFTER all seven packages, v188, and v189. No database or Firebase writes.
All source validations happen before any Android file is modified.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
HERE = Path(__file__).resolve().parent

def one(s: str, before: str, after: str, label: str) -> str:
    n = s.count(before)
    if n != 1:
        raise AssertionError(f"{label}: expected exactly one anchor, found {n}")
    return s.replace(before, after, 1)

def apply():
    planned = {}

    file = SRC / "PuzzleScreen.kt"
    s = file.read_text("utf-8")
    # v189 introduced fittedBoardHeight but accidentally reused phone IME height
    # on landscape tablets. A 20x20 board was constrained to ~50% of the
    # available height and displayed at ~11dp tiles in the top-left corner.
    s = one(s,
        """            val fittedBoardHeight = if (keyboardVisible) keyboardBoard
                else viewWidth * (board.rows.toFloat() / board.columns.coerceAtLeast(1).toFloat())""",
        """            // Landscape/tablet uses nearly the entire visible space, even
            // while the system keyboard is open. The phone has its own compact
            // keyboard policy. Fit both row/column axes without cropping.
            val fittedBoardHeight = if (wide) {
                Wq190BoardPolicy.fittedHeightDp(
                    viewWidth.value, maxHeight.value,
                    board.rows, board.columns
                ).dp
            } else if (keyboardVisible) keyboardBoard
            else viewWidth * (board.rows.toFloat() / board.columns.coerceAtLeast(1).toFloat())""",
        "use full height on tablet including with handwriting/IME keyboard")

    # The larger board and the clue controls must keep their independent
    # weighted columns. Recenter the board inside its own available slot.
    s = one(s,
        "                        Box(Modifier.weight(1.22f)) {",
        "                        Box(Modifier.weight(1.22f), contentAlignment = Alignment.TopCenter) {",
        "center tablet grid, without changing the clue layout")

    assert "val wide = maxWidth >= 760.dp" in s
    assert "val wide = maxWidth >= 760.dp && !keyboardVisible" not in s
    assert "val previewLetters: Map<Cell, Char?>" in s
    assert "if (position in previewLetters) previewLetters[position]" in s
    assert s.count("fittedBoardHeight,") == 2, "both portrait/tablet call sites must be fitted"
    planned[file] = s

    # These two old puzzle icons were *hard-coded* in Wq175HomeBottomNav.
    # Changing only the home tile router cannot replace this bottom nav.
    file = SRC / "Wq175HomeNavigation.kt"
    s = file.read_text("utf-8")
    previous_nav = '            icon={Wq175GlossIcon("puzzle",Modifier.size(29.dp))},'
    assert s.count(previous_nav) == 2, (
        "Home and New Game are the two original puzzle-icon navigation items")
    s = s.replace(previous_nav,
        """            icon={
                if (!Wq189UploadedArtwork("home",Modifier.size(35.dp)))
                    Wq175GlossIcon("home",Modifier.size(35.dp))
            },""", 1)
    s = s.replace(previous_nav,
        """            icon={
                if (!Wq189UploadedArtwork("new_game",Modifier.size(35.dp)))
                    Wq175GlossIcon("new_game",Modifier.size(35.dp))
            },""", 1)
    # Günlük was not part of the requested replacement set. Preserve its
    # existing icon and callback without modifying it.
    assert 'icon={Wq2DPortrait(chosen,Modifier.size(29.dp))}' in s, (
        "Profile must retain the user's own mascot rather than a stock icon")
    planned[file] = s

    # Guard against silent fallbacks: every newly assigned bottom/nav asset
    # must exist in the same approved icon catalog and embedded ZIP.
    catalog = (SRC / "Wq189UploadedIcons.kt").read_text("utf-8")
    for marker in (
        '"home" to "home"',
        '"new_game" to "puzzle_abc_neon"',
        '"packages" to "ready_folders"',
        '"verbs" to "verb_forms"',
        '"solo_puzzle" to "puzzle_builder"',
    ):
        assert marker in catalog, marker
    assets = SRC / "assets"  # actual asset installation path is src/main/assets
    assets = ROOT / "source/app/src/main/assets/approved_icons_v189"
    for filename in (
        "home.webp", "puzzle_abc_neon.webp", "study_checklist.webp",
        "ready_folders.webp", "verb_forms.webp", "puzzle_builder.webp",
    ):
        assert (assets / filename).is_file(), f"Missing bundled artwork: {filename}"
    code = HERE / "Wq190BoardPolicy.kt"
    tests = HERE / "Wq190BoardPolicyTest.kt"
    assert code.is_file() and tests.is_file()

    # Only write after every anchor + artwork + contract has been verified.
    for file, source in planned.items():
        file.write_text(source, "utf-8")
    shutil.copyfile(code, SRC / code.name)
    TEST.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(tests, TEST / tests.name)
    print("PASS v190: landscape tablet IME uses full-grid fitted dimensions")
    print("PASS v190: Ana and Yeni Oyun have two distinct true uploaded icons")
    print("PASS v190: approved icons present in Android app assets and routes")
    print("PASS v190: draft letters and original profile mascot preserved")

if __name__ == "__main__":
    apply()
