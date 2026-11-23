#!/usr/bin/env python3
"""v1.0.95: ONLY the 30dp A1 badge artwork and the A1 text size.

All other assembled PuzzleScreen Kotlin bytes must remain unchanged.
Apply after v191/v192/v193/v194 patches to the verified full Android source.
"""
from pathlib import Path

SCREEN = (Path(__file__).resolve().parents[2] /
          "source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt")


def apply() -> None:
    original = SCREEN.read_text(encoding="utf-8")
    marker = "private fun GameHeading("
    ending = "private fun BoardFrame("
    assert original.count(marker) == 1, "Header must be unique"
    start = original.index(marker)
    end = original.index(ending, start)
    before, heading, after = original[:start], original[start:end], original[end:]

    old_icon = '''            if (!Wq189UploadedArtwork("solo_puzzle", Modifier.size(30.dp)))
                Wq175GlossIcon("solo_puzzle", Modifier.size(30.dp))'''
    new_icon = '''            // Fixed 30dp level badge: replaces artwork without altering card layout.
            Box(
                Modifier.size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(
                        Color(0xFFF5F9DE), Color(0xFFD8EBDD))))
                    .border(1.dp, Color(0xFFB8D8C2), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(Color(0xFF245E50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("★", color = Color(0xFFF6FBE5), fontFamily = Sans,
                        fontSize = 14.sp, lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold)
                }
            }'''
    old_level = '''                Text(game.path, color = Forest, fontFamily = Sans,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)'''
    new_level = '''                Text(game.path, color = Forest, fontFamily = Sans,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)'''
    assert heading.count(old_icon) == 1, "Existing 30dp icon anchor changed"
    assert heading.count(old_level) == 1, "Existing A1 12sp text anchor changed"
    changed = heading.replace(old_icon, new_icon, 1).replace(old_level, new_level, 1)

    # Strict structural no-touch guarantee: reconstruct EXACT original.
    assert changed.replace(new_icon, old_icon, 1).replace(
        new_level, old_level, 1
    ) == heading, "Unexpected header changes"
    updated = before + changed + after
    assert updated[:start] == original[:start]
    assert updated[start + len(changed):] == original[end:]
    assert "Modifier.size(30.dp)" in new_icon
    assert "fontSize = 15.sp" in new_level
    for unchanged in (
        "Wq192Viewport.overviewTileDp(",
        "horizontal.animateScrollTo(targetX)",
        "keyboardWasShown = false",
        "Wq193EditableDraft.protectedPositions(",
        'Text(letter.toString(), Modifier.align(Alignment.Center)',
        "Wq190CrosswordHintInput.completedWord(",
        "state.enter(completeAnswer)",
    ):
        assert unchanged in updated, unchanged

    SCREEN.write_text(updated, encoding="utf-8")
    print("PASS v195: A1 text 12sp -> 15sp")
    print("PASS v195: new fixed-30dp mint/forest star level badge")
    print("PASS v195: only two source substitutions; no changes to card geometry or gameplay")


if __name__ == "__main__":
    apply()
