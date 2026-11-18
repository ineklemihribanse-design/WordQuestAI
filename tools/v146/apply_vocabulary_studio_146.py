#!/usr/bin/env python3
"""Install v146 vocabulary learning UI onto the fully assembled WordQuest source.

Run AFTER v118 learning-studio and later teacher overlays. Never touch database
schema, signed app identity, cloud credentials, or existing learning records.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source"
PACKAGE = Path("app/src/main/java/com/kelimebulmacasi/a1")
TEST_PACKAGE = Path("app/src/test/java/com/kelimebulmacasi/a1")

def replace_once(text: str, before: str, after: str, path: Path) -> str:
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f"Expected exactly one source anchor in {path}, found {count}: {before[:72]!r}")
    return text.replace(before, after, 1)

def main() -> None:
    if not (SRC / "settings.gradle.kts").is_file():
        raise RuntimeError("Build the complete archived Android source before applying v146.")
    for name in ("VocabularyStudyModel.kt", "VocabularySetupScreen.kt",
                 "VocabularyPracticeScreen.kt"):
        source = ROOT / "tools/v146" / name
        target = SRC / PACKAGE / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

    for name in ("VocabularyAnswerCheckerTest.kt",):
        source = ROOT / "tools/v146" / name
        target = SRC / TEST_PACKAGE / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

    file = SRC / PACKAGE / "LearningStudioScreen.kt"
    text = file.read_text(encoding="utf-8")
    text = replace_once(text,
        '    var deckFilter by rememberSaveable(session.profileKey) { mutableStateOf("") }\n',
        '    var deckFilter by rememberSaveable(session.profileKey) { mutableStateOf("") }\n'
        '    var practice by remember(session.profileKey) { mutableStateOf<VocabularyStudyConfig?>(null) }\n',
        file)
    text = replace_once(text,
        '    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }\n',
        '    val activePractice = practice\n'
        '    if (activePractice != null) {\n'
        '        BackHandler { practice = null }\n'
        '        VocabularyPracticeScreen(session, pronunciation, activePractice) { practice = null }\n'
        '        return\n'
        '    }\n'
        '    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }\n',
        file)
    text = replace_once(text,
        '            "Kartlar" -> LearningFlashcardStudio(session, pronunciation, folders,\n'
        '                initialFolder = deckFolder, initialFilter = deckFilter)\n',
        '            "Kartlar" -> VocabularySetupScreen(session, folders,\n'
        '                initialFolder = deckFolder, initialFilter = deckFilter,\n'
        '                onStart = { practice = it })\n',
        file)
    text = replace_once(text,
        '                                        BodyText(card.clue)\n'
        '                                    }\n'
        '                                    TextButton(onClick = { detailKey = card.key })',
        '                                        BodyText(card.clue)\n'
        '                                        VocabularyWordTypes.label(card.answer)?.let { BodyText("Tür: " + it) }\n'
        '                                    }\n'
        '                                    TextButton(onClick = { detailKey = card.key })',
        file)
    file.write_text(text, encoding="utf-8")

    notebook = SRC / PACKAGE / "LearningScreen.kt"
    text = notebook.read_text(encoding="utf-8")
    text = replace_once(text,
        '                        WordIpa(card.answer, session.preferences)\n'
        '                        BodyText(card.clue)',
        '                        WordIpa(card.answer, session.preferences)\n'
        '                        BodyText(card.clue)\n'
        '                        VocabularyWordTypes.label(card.answer)?.let { BodyText("Tür: " + it) }',
        notebook)
    notebook.write_text(text, encoding="utf-8")
    gradle = SRC / "app/build.gradle.kts"
    build = gradle.read_text(encoding="utf-8")
    if build.count("versionCode = 70") != 1 or build.count('versionName = "1.0.45-face-alignment-blink-qa"') != 1:
        raise RuntimeError("Unexpected version; refusing to change app identity or apply a stale patch.")
    if 'applicationIdSuffix = ".offline101"' not in build:
        raise RuntimeError("Upgrade-compatible package must be retained.")
    build = build.replace("versionCode = 70", "versionCode = 71", 1).replace(
        'versionName = "1.0.45-face-alignment-blink-qa"',
        'versionName = "1.0.46-vocabulary-learning-modes"', 1)
    gradle.write_text(build, encoding="utf-8")
    print("v146 vocabulary studio installed: setup/navigation, distinct practice screen, grading, word types.")
    print("Learning history, folders and database remain unchanged.")

if __name__ == "__main__":
    main()
