#!/usr/bin/env python3
"""Source invariants for WordQuestAI 1.0.86 Package 4's single-board 70-word option.

This lightweight check is not a replacement for Android/Compose compile or emulator testing.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "android-current/app/src/main/java/com/kelimebulmacasi/a1"


def source(name: str) -> str:
    path = SRC / name
    assert path.is_file(), f"Missing {path}"
    return path.read_text(encoding="utf-8")


def main() -> None:
    layout = source("CustomLayout.kt")
    generator = source("VocabularyBank.kt")
    new_game = source("NewGameScreen.kt")
    session = source("AppSession.kt")
    ui = source("PuzzleScreen.kt")
    home = source("HomeScreen.kt")
    saved = source("PlayerStore.kt")

    assert "internal const val MAX_BOARD_WORDS = 70" in layout
    assert "internal const val MAX_BOARD_SIDE = 78" in layout
    assert "suspend fun flexibleLargePuzzle(" in layout
    assert "placed.size == requested" in layout
    assert "puzzle.words.size == requested" in layout
    assert "distinctBy(Entry::letters)" in layout
    assert "2 * (height - width) * (height - width)" in layout

    assert "flexibleLargePuzzle(candidates, size," in generator
    assert "puzzle == null || puzzle.words.size != size" in generator
    assert "size in MIN_NEW_GAME_WORDS..MAX_BOARD_WORDS" in generator

    assert "MAX_NEW_GAME_WORDS = 70" in new_game
    assert "gameSizes = listOf(10, 20, 30, 50, 70)" in new_game
    assert "NewGameSection(\"Seviye\"" in new_game
    assert "NewGameSection(\"Konu\"" in new_game
    assert "NewGameSection(\"Kelime Türü\"" in new_game
    assert "NewGameSection(\"Oyun Biçimi\"" in new_game
    assert "PuzzleClockStore(context, session.profileKey).save(" in new_game
    assert "val game = session.newGameFiltered(levels, topics, kinds, count!!)" in new_game
    assert "session.newGameSeries(" not in new_game
    assert "bölümlere ayrılır" not in new_game

    assert "newGameSeries(" not in session
    assert "nextGeneratedPart(" not in session
    assert "generateGame(catalog.vocabulary.pool(level, topic, kind)" in session
    assert "internal suspend fun newGameFiltered(" in session
    assert "nextRound" not in ui
    assert "clock: PuzzleClock? = null" in ui
    assert "outOfTime" in ui
    assert "nextGeneratedPart(" not in home
    assert "if (generated) MAX_BOARD_WORDS else 20" in saved

    for path in (
        ROOT / "tools/v175/content/wordquest_en_tr_A1.tsv",
        ROOT / "tools/v175/content/wordquest_en_tr_A2.tsv",
        ROOT / "tools/v175/content/wordquest_en_tr_B1.tsv",
        ROOT / "tools/v175/content/wordquest_en_tr_B2.tsv",
        ROOT / "tools/v175/content/wordquest_en_tr_C1.tsv",
    ):
        if not path.exists():
            # source/main snapshot does not include newer v175 content: those are
            # layered in CI after the original app's complete overlay sequence.
            print(f"SKIP v175-only library (not in this source snapshot): {path.name}")
            continue
        words = set()
        for line in path.read_text(encoding="utf-8").splitlines():
            columns = line.split("\t")
            if len(columns) >= 5 and 2 <= len(columns[3]) <= 22:
                words.add(columns[3].strip().upper())
        assert len(words) >= 70, f"Not enough unique words in {path}"
        print(f"PASS library {path.name} unique={len(words)}")

    print("PASS: single-board 8–70 words, multi-select cards, countdown/stopwatch, no split")
    print("NOTE: full Android APK compilation and on-device performance not proven by this check")


if __name__ == "__main__":
    main()
