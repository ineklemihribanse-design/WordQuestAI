#!/usr/bin/env python3
"""v1.0.96: replace only the old A1 star icon with approved universal 3D art.

Runs after v195. Exact 30dp dimensions and the existing dynamic level text
remain unchanged. Stages only the verified user-provided icon bitmap and
its simple renderer, without changing the crossword engine or other views.
"""
from pathlib import Path
import ast
import base64
import hashlib
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
SCREEN = SRC / "PuzzleScreen.kt"
APP_ASSETS = ROOT / "source/app/src/main/assets"
HERE = Path(__file__).resolve().parent
B64 = HERE / "level_badge_approved_3d.webp.b64"
EXPECTED_SHA256 = "8abe9cae51a083a2450902302c71b5a9f2d5ccf0a16b14a8d51026353a87babf"


def old_approved_star() -> str:
    # Read v195's exact replacement expression instead of guessing its
    # indentation: the original screen must be unchanged outside the icon.
    module = ast.parse((ROOT / "tools/v195/apply_a1_level_badge_195.py").read_text("utf-8"))
    for node in ast.walk(module):
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id == "new_icon"
            for target in node.targets
        ):
            return ast.literal_eval(node.value)
    raise AssertionError("Expected v195 icon anchor not found")


def apply() -> None:
    raw = base64.b64decode(B64.read_text("ascii").strip(), validate=True)
    actual = hashlib.sha256(raw).hexdigest()
    assert actual == EXPECTED_SHA256, "User-approved 3D icon hash mismatch"
    assert raw.startswith(b"RIFF") and raw[8:12] == b"WEBP"
    assert 4000 < len(raw) < 18000
    renderer = (HERE / "Wq196LevelBadge.kt").read_text("utf-8")
    assert 'assets.open("wq196_level_badge.webp")' in renderer

    old = SCREEN.read_text("utf-8")
    marker = "private fun GameHeading("
    end_marker = "private fun BoardFrame("
    assert old.count(marker) == 1
    i = old.index(marker)
    j = old.index(end_marker, i)
    heading = old[i:j]
    old_icon = old_approved_star()
    new_icon = '            Wq196LevelBadge(Modifier.size(30.dp))'
    assert heading.count(old_icon) == 1
    assert heading.count('fontWeight = FontWeight.Bold, fontSize = 15.sp)') == 1
    updated = old[:i] + heading.replace(old_icon, new_icon, 1) + old[j:]
    assert updated.replace(new_icon, old_icon, 1) == old, (
        "Unexpected change outside previously approved 30dp icon"
    )
    assert "Wq192Viewport.overviewTileDp(" in updated
    assert "horizontal.animateScrollTo(targetX)" in updated
    assert "Wq193EditableDraft.protectedPositions(" in updated
    assert 'Text(letter.toString(), Modifier.align(Alignment.Center)' in updated

    APP_ASSETS.mkdir(parents=True, exist_ok=True)
    # All assertions precede writing; no partial game-state changes.
    SCREEN.write_text(updated, "utf-8")
    (APP_ASSETS / "wq196_level_badge.webp").write_bytes(raw)
    shutil.copyfile(HERE / "Wq196LevelBadge.kt", SRC / "Wq196LevelBadge.kt")
    print("PASS v196: exact approved universal 3D badge installed from uploaded art")
    print("PASS v196: original 30dp position, dynamic 15sp level and full board unchanged")


if __name__ == "__main__":
    apply()
