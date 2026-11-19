#!/usr/bin/env python3
"""Apply keyboard-safe crossword entry to the staged Android project.

The source is distributed as versioned ZIP overlays; this patch deliberately
runs AFTER all overlays so the CI APK actually includes these changes.
The anchors are checked; upstream changes fail fast instead of silently shipping
an unchanged screen.
"""
from pathlib import Path
import shutil

ROOT = Path("source/app/src")
UI = ROOT / "main/java/com/kelimebulmacasi/a1"
TESTS = ROOT / "test/java/com/kelimebulmacasi/a1"


def once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


def online_screen():
    path = UI / "PremiumOnlineScreen.kt"
    text = path.read_text(encoding="utf-8")
    text = once(text,
        '    BackHandler(match != null) { client.leave() }',
        '    LaunchedEffect(match?.turnUid, match?.turnUntil) {\n'
        '        if (match?.isCrossword == true && match?.cooperative == false) answerText = ""\n'
        '    }\n'
        '    BackHandler(match != null) { client.leave() }',
        "clear old draft after turn change")
    text = once(text,
        '    val keyboardIsOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0\n'
        '    val keyboardWord = remember(match?.boardId, wordIndex) {\n'
        '        if (match?.isCrossword == true) packs.puzzleById(match.boardId)?.puzzle?.words?.getOrNull(wordIndex)\n'
        '        else null\n'
        '    }\n', '', "remove floating overlay and unused keyboard state")

    begin = text.index("    // Sticky clue stays above the Android keyboard")
    end_marker = "\n    }\n}\n\n@Composable\nprivate fun PremiumOnlineMatchScreen"
    end = text.index(end_marker, begin)
    # Preserve the enclosing Box and composable closings.
    text = text[:begin] + text[end:]

    # Detect keyboard within the actual match composable; no fragile function
    # signature rewrite is needed when the online screen evolves.
    start = text.index("private fun PremiumOnlineMatchScreen(")
    body_start = text.index(") {", start) + len(") {")
    text = (
        text[:body_start] +
        "\n    val keyboardIsOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0" +
        text[body_start:]
    )

    text = once(text,
        '                val word = puzzle.words[selected]\n',
        '                val word = puzzle.words[selected]\n'
        '                val fixedPattern = crosswordFixedPattern(puzzle, solved, selected)\n'
        "                val missingCount = fixedPattern.count { it == '.' }\n",
        "online selected word pattern")
    text = once(text,
        '                ArenaScoreStrip(solved.size, match.size, myScore, countdown)',
        '                if (keyboardIsOpen) Text("Çözülen ${solved.size}/${match.size} · Puan $myScore" + '
        '(countdown?.let { " · ${it} sn" } ?: ""), color = OnlineGold, fontSize = 12.sp, '
        'fontWeight = FontWeight.SemiBold) '
        'else ArenaScoreStrip(solved.size, match.size, myScore, countdown)',
        "small scoreboard visible while keyboard is open")

    text = once(text,
        '                ArenaPuzzleGrid(puzzle, solved, selected, answerText) { setWordIndex(it) }',
        '                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen, answerText) { setWordIndex(it) }',
        "keep scrollable board visible and focus selected word while typing")

    text = once(text,
        '                    Text(word.entry.clue, color = OnlineCream, fontWeight = FontWeight.Medium)\n',
        '                    Text(word.entry.clue, color = OnlineCream, fontWeight = FontWeight.Medium)\n'
        '                    ArenaCrosswordTypingStrip(puzzle, solved, selected, answerText)\n',
        "keep clue and editable letter strip together")

    text = once(text,
        '                            if (!canAnswer || englishLetters(answerText).isEmpty()) return\n'
        '                            if (englishLetters(answerText) == word.entry.letters) {',
        '                            if (!canAnswer || englishLetters(answerText).length != missingCount) return\n'
        '                            if (fillCrosswordGaps(fixedPattern, answerText) == word.entry.letters) {',
        "online answer validation skips locked crossing letters")

    text = once(text,
        'OutlinedTextField(answerText, { setAnswerText(it.take(word.entry.answer.length + 3)) },',
        'OutlinedTextField(answerText, { setAnswerText(englishLetters(it).take(missingCount)) },',
        "online input length limited to unlocked squares")
    text = once(text,
        'Modifier.fillMaxWidth(), label = { Text("İngilizce cevabını yaz") }, singleLine = true,',
        'Modifier.fillMaxWidth(), label = { Text("Eksik harfleri yaz") }, singleLine = true,',
        "online input field label")
    text = once(text,
        '                        Text("Yazdığın harfler seçili altın karelerde de görünecek.",',
        '                        Text("Yeşil harfleri tekrar yazma. Yalnızca boş kareleri doldur.",',
        "online keyboard help")
    text = once(text,
        'enabled = answerText.isNotBlank() && canAnswer)',
        'enabled = englishLetters(answerText).length == missingCount && canAnswer)',
        "prevent submitting partial online answers")
    path.write_text(text, encoding="utf-8")
    return text


def offline_screen():
    path = UI / "PremiumArenaScreen.kt"
    text = path.read_text(encoding="utf-8")
    text = once(text,
        'import androidx.compose.runtime.*',
        'import androidx.compose.runtime.*\nimport androidx.compose.ui.platform.LocalDensity',
        "offline IME density import")
    text = once(text,
        '    Column(\n        Modifier.fillMaxSize().background(ArenaNight).windowInsetsPadding(WindowInsets.safeDrawing)',
        '    val keyboardIsOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0\n'
        '    Column(\n        Modifier.fillMaxSize().background(ArenaNight).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()',
        "offline screen resizes with Android keyboard")
    text = once(text,
        '                    val word = puzzle.words[currentWord.coerceIn(puzzle.words.indices)]\n',
        '                    val word = puzzle.words[currentWord.coerceIn(puzzle.words.indices)]\n'
        '                    val fixedPattern = crosswordFixedPattern(puzzle, solved.toSet(), currentWord)\n'
        "                    val missingCount = fixedPattern.count { it == '.' }\n",
        "offline selected word fixed pattern")
    text = once(text,
        '                    ArenaPuzzleGrid(puzzle, solved.toSet(), currentWord) { index ->',
        '                    ArenaCompactCrosswordBoard(puzzle, solved.toSet(), currentWord, keyboardIsOpen, wordAnswer) { index ->',
        "offline compact board stays scrollable while typing")
    text = once(text,
        '                        Text(word.entry.clue, color = ArenaCream, fontSize = 17.sp, fontWeight = FontWeight.Medium)\n',
        '                        Text(word.entry.clue, color = ArenaCream, fontSize = 17.sp, fontWeight = FontWeight.Medium)\n'
        '                        ArenaCrosswordTypingStrip(puzzle, solved.toSet(), currentWord, wordAnswer)\n',
        "offline selected letters remain visible")
    text = once(text,
        'onValueChange = { wordAnswer = it.take(word.entry.answer.length + 4) },',
        'onValueChange = { wordAnswer = englishLetters(it).take(missingCount) },',
        "offline unlocked letters only")
    text = once(text,
        '                            if (englishLetters(wordAnswer) == word.entry.letters) {',
        '                            if (englishLetters(wordAnswer).length == missingCount &&\n'
        '                                fillCrosswordGaps(fixedPattern, wordAnswer) == word.entry.letters) {',
        "offline crossing-aware validation")
    text = once(text,
        '}, enabled = wordAnswer.isNotBlank())',
        '}, enabled = englishLetters(wordAnswer).length == missingCount)',
        "offline prevent premature answer submission")
    # A repeated tap on the same cell must preserve the typed draft.
    # Tapping at an unsolved intersection toggles its across/down direction.
    text = once(text,
        'val next = indexes?.firstOrNull { it !in solved } ?: return@clickable',
        'val next = nextCrosswordSelection(indexes, solved, selected) ?: return@clickable',
        "choose next unsolved word at an intersection")
    text = once(text,
        'onSelect(next)',
        'if (next != selected) onSelect(next)',
        "do not clear a draft on the same selected word")

    # Board cells must preserve each letter's original slot, including crossing
    # positions, so a letter after a locked middle cell never shifts left.
    text = once(text,
        '    typedWord: String = "", onSelect: (Int) -> Unit) {',
        '    typedWord: String = "", autoFocusSelected: Boolean = false, onSelect: (Int) -> Unit) {',
        "allow automatic horizontal focus on the selected crossword word")
    text = once(text,
        '        val fontSize = if (cellSize < 25.dp) 11.sp else 14.sp',
        '''        val fontSize = if (cellSize < 25.dp) 11.sp else 14.sp
        val selectedFirstColumn = (0 until puzzle.columns).firstOrNull { column ->
            chosen?.cells?.any { target ->
                (0 until puzzle.rows).any { row -> Cell(row, column) == target }
            } == true
        } ?: 0
        val cellStepPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            (cellSize + 1.dp).toPx()
        }
        LaunchedEffect(autoFocusSelected, selected, boardScroll.maxValue, cellStepPx) {
            if (autoFocusSelected && boardScroll.maxValue > 0) {
                val targetOffset = ((selectedFirstColumn - 1).coerceAtLeast(0) * cellStepPx).toInt()
                boardScroll.animateScrollTo(targetOffset.coerceIn(0, boardScroll.maxValue))
            }
        }''',
        "scroll horizontally to selected word when the keyboard is open")
    text = once(text,
        '    val draftLetters = remember(typedWord) { englishLetters(typedWord) }',
        '    val draftLetters = remember(typedWord) { typedWord }',
        "position-aware board draft")
    text = once(text,
        'if (i < draftLetters.length && cell !in visibleLetters) put(cell, draftLetters[i])',
        "if (i < draftLetters.length && draftLetters[i] != '.' && cell !in visibleLetters) " +
        "put(cell, draftLetters[i])",
        "skip unfilled board cells in the live draft")
    path.write_text(text, encoding="utf-8")
    return text


def main():
    if not UI.is_dir():
        raise RuntimeError("Run after all WordQuest source overlays are extracted into source/")
    online = online_screen()
    offline = offline_screen()
    shutil.copyfile(Path("tools/v122/ArenaCrosswordTyping.kt"), UI / "ArenaCrosswordTyping.kt")
    shutil.copyfile(Path("tools/v122/ArenaCrosswordInputTest.kt"), TESTS / "ArenaCrosswordInputTest.kt")
    assert "Sticky clue stays" not in online
    assert "ArenaCompactCrosswordBoard" in online and "ArenaCompactCrosswordBoard" in offline
    assert "fillCrosswordGaps(fixedPattern" in online and "fillCrosswordGaps(fixedPattern" in offline
    print("Applied scrollable compact crossword board + crossing-aware input (online/offline)")


if __name__ == "__main__":
    main()
