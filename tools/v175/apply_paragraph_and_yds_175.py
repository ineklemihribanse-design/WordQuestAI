#!/usr/bin/env python3
"""Install additive, offline paragraph dictation and expand authentic YDS exercises.

Run after apply_practice_visuals_175.py and apply_yds_exam_ui_175.py.
Do not modify existing voice files, registration, profiles, Arena, or signer.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
PKG = ROOT / "tools/v175"

def once(text: str, old: str, new: str, label: str) -> str:
    matches = text.count(old)
    assert matches == 1, f"{label}: expected one anchor, got {matches}"
    return text.replace(old, new, 1)

def main():
    shutil.copyfile(PKG / "WordQuest175ParagraphDictation.kt",
                    SRC / "WordQuest175ParagraphDictation.kt")
    shutil.copyfile(PKG / "Wq175YdsExpansion.kt", SRC / "Wq175YdsExpansion.kt")
    TEST.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(PKG / "Wq175ParagraphTest.kt", TEST / "Wq175ParagraphTest.kt")

    screen = SRC / "PracticeHubScreen.kt"
    content = screen.read_text("utf-8")
    content = once(content,
        "        for (kind in PracticeKind.entries) {",
        "        Wq175ParagraphPracticeEntry(session.profileKey)\n"
        "        for (kind in PracticeKind.entries) {",
        "dedicated paragraph lesson from the Daily Plan")
    content = once(content,
        "        Wq175PracticeQuestionHeader(kind,number)\n",
        "        Wq175PracticeQuestionHeader(kind,number)\n"
        "        if (kind == PracticeKind.Dictation) {\n"
        "            Wq175ParagraphPracticeEntry(session.profileKey)\n"
        "        }\n",
        "paragraph lesson inside existing dictation room")
    screen.write_text(content, "utf-8")

    bank = SRC / "YdsContent.kt"
    content = bank.read_text("utf-8")
    content = once(content,
        ") + YdsExtraContent.questions + Wq175YdsGrammar.questions",
        ") + YdsExtraContent.questions + Wq175YdsGrammar.questions + Wq175YdsExpansion.questions",
        "append expanded YDS question bank")
    bank.write_text(content, "utf-8")

    tests = TEST / "YdsCenterTest.kt"
    content = tests.read_text("utf-8")
    content = once(content,
        "assertEquals(120, YdsContent.questions.size)",
        "assertEquals(150, YdsContent.questions.size)", "actual YDS count")
    content = once(content,
        "assertEquals(120, YdsContent.questions.map { it.id }.toSet().size)",
        "assertEquals(150, YdsContent.questions.map { it.id }.toSet().size)",
        "unique YDS ids")
    tests.write_text(content, "utf-8")
    print("PASS: 12 A1-C2 paragraphs, US/UK playback, ordered word-error analysis, profile records.")
    print("PASS: YDS question bank grows from 120 to 150, with original Turkish explanations.")

if __name__ == "__main__":
    main()
