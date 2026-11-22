#!/usr/bin/env python3
"""Fail-closed v1.0.83 overlay. MUST run strictly after tools/v182/apply_ui_182.py.
Never resets SQLite, preferences, Firebase, installed package identity, art or mascots.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS = ROOT / "tools/v183"


def replace_one(text: str, old: str, new: str, description: str) -> str:
    found = text.count(old)
    if found != 1:
        raise AssertionError(f"{description}: expected one source anchor, found {found}")
    return text.replace(old, new, 1)


def protect_answers(text: str, needle: str, validity: str, description: str) -> str:
    """Wrap only the matching forEach block; preserve all original callbacks verbatim."""
    if text.count(needle) != 1:
        raise AssertionError(f"{description}: ambiguous option loop")
    start = text.index(needle)
    opening = text.index("{", start)
    depth = 0
    ending = None
    for n in range(opening, len(text)):
        if text[n] == "{":
            depth += 1
        elif text[n] == "}":
            depth -= 1
            if depth == 0:
                ending = n + 1
                break
    if ending is None:
        raise AssertionError(f"{description}: unterminated option loop")
    original = text[start:ending]
    return text[:start] + (
        f"if (Wq183QuizIsValid({validity})) {{\n"
        + original
        + "\n                        } else {\n"
        + "                            Wq183QuizUnavailableQuestion()\n"
        + "                        }"
    ) + text[ending:]


def update(path: str, action):
    dest = SRC / path
    old = dest.read_text(encoding="utf-8")
    new = action(old)
    if new == old:
        raise AssertionError(f"v183 patch made no change: {path}")
    dest.write_text(new, encoding="utf-8")
    print("PASS v183", path)


shutil.copyfile(TOOLS / "Wq183QuizQuality.kt", SRC / "Wq183QuizQuality.kt")

def online(source):
    source = replace_one(
        source,
        """Text(question.prompt, color = Color.White, fontSize = 21.sp,
                            lineHeight=28.sp, fontWeight = FontWeight.Bold)""",
        "Wq183QuizPrompt(question.prompt)",
        "visible high-contrast online question card",
    )
    source = protect_answers(
        source,
        "question.options.forEachIndexed { n, option ->",
        "question.prompt, question.options, question.correct",
        "online five-choice invalid-answer guard",
    )
    return source

update("PremiumOnlineScreen.kt", online)
update(
    "PremiumArenaScreen.kt",
    lambda source: protect_answers(
        source,
        "current.options.forEachIndexed { index, option ->",
        "current.prompt, current.options, current.correct",
        "offline five-choice invalid-answer guard",
    ),
)

update(
    "Wq182QuizAndInfo.kt",
    lambda source: replace_one(
        source,
        "maxLines=4,overflow=TextOverflow.Ellipsis)",
        "softWrap=true)",
        "do not silently truncate lengthy five-choice answers",
    ),
)

gradle = ROOT / "source/app/build.gradle.kts"
before = gradle.read_text("utf-8")
assert before.count("versionCode = 108") == 1
assert before.count('versionName = "1.0.82"') == 1
assert 'applicationIdSuffix = ".fresh"' in before
gradle.write_text(
    before.replace("versionCode = 108", "versionCode = 109", 1)
    .replace('versionName = "1.0.82"', 'versionName = "1.0.83"', 1),
    "utf-8",
)
print("PASS: 1.0.83 code109; existing application identity and saved-data paths unchanged.")
