#!/usr/bin/env python3
"""Apply Package 4 to an assembled 1.0.85 tree, never to production.

Fail-closed unified-diff application preserves all unrelated v1.0.85 changes.
Run only after the normal v185 assembly step. On ANY conflict no patch is applied.
"""
import difflib
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
P4 = ROOT / "tools/v186"
BASE = P4 / "base"
CHANGED = P4 / "staged"
MODIFIED = [
    "AppSession.kt", "CustomLayout.kt", "HomeScreen.kt",
    "NewGameScreen.kt", "PracticeHubScreen.kt", "PuzzleScreen.kt",
    "VocabularyBank.kt",
]
ADDED = [
    "PuzzleClockStore.kt", "Wq186GameCards.kt",
    "Wq186ParagraphRepository.kt", "Wq186ParagraphWorkspace.kt",
]

def main() -> None:
    if not (ROOT / "tools/v185/apply_profile_185.py").is_file():
        raise SystemExit("Missing v185 baseline; abort.")
    if not (SRC / "WordQuest175ParagraphDictation.kt").is_file():
        raise SystemExit("The full v175 paragraph classes were not assembled; abort.")
    if not (SRC / "Wq176DailyChallenge.kt").is_file():
        raise SystemExit("The daily YDS source is missing; abort.")
    changes = []
    for name in MODIFIED + ["Wq176DailyChallenge.kt"]:
        old = BASE / name
        revised = (P4 / "Wq176DailyChallenge_186.kt") if name.startswith("Wq176") else (CHANGED / name)
        current = SRC / name
        if not old.is_file() or not revised.is_file() or not current.is_file():
            raise SystemExit(f"Missing base/source/revised for {name}; abort.")
        a = old.read_text("utf-8").splitlines(keepends=True)
        z = revised.read_text("utf-8").splitlines(keepends=True)
        target = f"app/src/main/java/com/kelimebulmacasi/a1/{name}"
        changes.extend(difflib.unified_diff(a, z, fromfile="a/"+target, tofile="b/"+target, n=3))
    for name in ADDED:
        origin = (CHANGED / name) if (CHANGED / name).is_file() else (P4 / name)
        if not origin.is_file() or (SRC / name).exists():
            raise SystemExit(f"Missing new source or source already exists: {name}; abort.")
    if not changes:
        raise SystemExit("Empty overlay; abort.")
    with tempfile.TemporaryDirectory() as tmp:
        patch = Path(tmp) / "package4.patch"
        patch.write_text("".join(changes), encoding="utf-8")
        # With no --3way fallback, this refuses to overwrite any conflicting change.
        check = subprocess.run(["git", "apply", "--check", "--whitespace=nowarn",
                                str(patch)], cwd=ROOT / "source", capture_output=True, text=True)
        if check.returncode:
            raise SystemExit("SAFE STOP: v185 overlaps with Package 4; manual merge required.\n"
                             + check.stdout + check.stderr)
        subprocess.run(["git", "apply", "--whitespace=nowarn", str(patch)],
                       cwd=ROOT / "source", check=True)
    for name in ADDED:
        origin = (CHANGED / name) if (CHANGED / name).is_file() else (P4 / name)
        shutil.copyfile(origin, SRC / name)
    # Add the new paragraph tool without losing existing dictation entries and callbacks.
    hub = SRC / "PracticeHubScreen.kt"
    text = hub.read_text("utf-8")
    if text.count("Wq175ParagraphPracticeEntry(session.profileKey)") != 2:
        raise SystemExit("SAFE STOP: paragraph hooks differ; manual review required.")
    text = text.replace("Wq175ParagraphPracticeEntry(session.profileKey)",
                        "Wq186ParagraphPracticeEntry(session.profileKey, ai)")
    old_header = "    session: AppSession,\n    pronunciation: WordPronunciation,"
    new_header = "    session: AppSession,\n    ai: AiSession,\n    pronunciation: WordPronunciation,"
    if text.count(old_header) != 1:
        raise SystemExit("SAFE STOP: PracticeHubScreen signature differs.")
    hub.write_text(text.replace(old_header, new_header, 1), "utf-8")
    home = SRC / "HomeScreen.kt"
    text = home.read_text("utf-8")
    old_call = "PracticeHubScreen(session, pronunciation,"
    if text.count(old_call) != 1:
        raise SystemExit("SAFE STOP: daily-plan navigation signature differs.")
    home.write_text(text.replace(old_call, "PracticeHubScreen(session, ai, pronunciation,", 1), "utf-8")

    assert "MAX_BOARD_WORDS = 70" in (SRC / "CustomLayout.kt").read_text("utf-8")
    assert "newGameFiltered(" in (SRC / "AppSession.kt").read_text("utf-8")
    assert "Wq186ParagraphPracticeEntry(session.profileKey, ai)" in hub.read_text("utf-8")
    assert "Wq186ParagraphRepository.kt" in [p.name for p in SRC.iterdir()]
    print("PASS: staged Package 4 source merged only where v185 anchors match.")
    print("No signing, APK publishing, Firebase or account-data migration.")

if __name__ == "__main__":
    main()
