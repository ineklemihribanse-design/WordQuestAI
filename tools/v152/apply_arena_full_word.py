#!/usr/bin/env python3
"""Change ONLY Arena crossword answer input to full word and separate the Pass action.

Runs after v122 mobile keyboard, v148 competition rules, and v150 approved Arena
layouts. Does not patch the home or the ready/waiting lobby.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TESTS=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"

def once(s: str, old: str, new: str, label: str)->str:
    count=s.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected one occurrence, found {count}")
    return s.replace(old,new,1)

def online():
    path=SRC/"PremiumOnlineScreen.kt"
    s=path.read_text("utf-8")
    # Older assembled sources may already omit this unused variable.
    # Its absence is safe: subsequent full-word replacements are still verified.
    old_missing = "                val missingCount = fixedPattern.count { it == '.' }\n"
    if s.count(old_missing) > 1:
        raise AssertionError("ambiguous missing-letters online variable")
    s=s.replace(old_missing,"",1)
    s=once(s,
        '                    ArenaCrosswordTypingStrip(puzzle, solved, selected, answerText)\n',
        '',"remove misleading selected missing-letter strip")
    s=once(s,
        '                            if (!canAnswer || englishLetters(answerText).length != missingCount) return',
        '                            if (!canAnswer || !crosswordFullWordReady(word.entry.letters.length, answerText)) return',
        "require a full online word before attempting")
    s=once(s,
        '                            if (fillCrosswordGaps(fixedPattern, answerText) == word.entry.letters) {',
        '                            if (crosswordFullWordMatches(fixedPattern, answerText, word.entry.letters)) {',
        "validate full online answer and all crossings")
    s=once(s,
        'OutlinedTextField(answerText, { setAnswerText(englishLetters(it).take(missingCount)) },',
        'OutlinedTextField(answerText, { setAnswerText(englishLetters(it).take(word.entry.letters.length)) },',
        "type an entire word in online keyboard")
    s=once(s,
        'Modifier.fillMaxWidth(), label = { Text("Eksik harfleri yaz") }, singleLine = true,',
        'Modifier.fillMaxWidth(), label = { Text("Kelimeyi yaz") }, singleLine = true,',
        "online field title")
    s=once(s,
        'Text("Yeşil harfleri tekrar yazma. Yalnızca boş kareleri doldur.",',
        'Text("Kelimenin tamamını yaz. Açık harfler bulmacada yerinde kalır.",',
        "online helper text")
    s=once(s,
        'enabled = englishLetters(answerText).length == missingCount && canAnswer)',
        'enabled = crosswordFullWordReady(word.entry.letters.length, answerText) && canAnswer)',
        "online button should not submit partial answers")
    s=once(s,
        '                        if (match.cooperative || (match.rulesVersion >= 2 && canAnswer) || ',
        '                        Spacer(Modifier.height(22.dp))\n'
        '                        if (match.cooperative || (match.rulesVersion >= 2 && canAnswer) || ',
        "clear gap separating check from Pas")
    s=once(s,
        'ArenaOutlineButton(if(match.rulesVersion >= 2) "PAS GEÇ" else "BİLMİYORUM · CEZASIZ", {',
        'ArenaOutlineButton("PAS", {',
        "use a single pass label in all online crossword rooms")
    s=s.replace("Bilmiyorum kaydedildi.","Pas kaydedildi.")
    s=s.replace("Bilmiyorum derse","Pas geçerse")
    s=s.replace("Bilmiyorum dersen","Pas geçersen")
    assert 'Eksik harfleri yaz' not in s
    assert 'ArenaCrosswordTypingStrip(' not in s
    assert 'BİLMİYORUM · CEZASIZ' not in s
    path.write_text(s,"utf-8")

def offline():
    path=SRC/"PremiumArenaScreen.kt"
    s=path.read_text("utf-8")
    s=once(s,
        "                    val missingCount = fixedPattern.count { it == '.' }\n",
        '',"remove missing-letters offline variable")
    s=once(s,
        '                        ArenaCrosswordTypingStrip(puzzle, solved.toSet(), currentWord, wordAnswer)\n',
        '',"remove confusing offline missing-letter strip")
    s=once(s,
        'onValueChange = { wordAnswer = englishLetters(it).take(missingCount) },',
        'onValueChange = { wordAnswer = englishLetters(it).take(word.entry.letters.length) },',
        "offline full word field")
    s=once(s,
        'label = { Text("İngilizce cevabını yaz") },',
        'label = { Text("Kelimeyi yaz") },',
        "offline input label")
    s=once(s,
        '                            if (englishLetters(wordAnswer).length == missingCount &&\n'
        '                                fillCrosswordGaps(fixedPattern, wordAnswer) == word.entry.letters) {',
        '                            if (crosswordFullWordMatches(fixedPattern, wordAnswer, word.entry.letters)) {',
        "offline full word validation")
    s=once(s,
        '}, enabled = englishLetters(wordAnswer).length == missingCount)',
        '}, enabled = crosswordFullWordReady(word.entry.letters.length, wordAnswer))',
        "do not check incomplete offline words")
    s=once(s,
        '                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {\n'
        '                            ArenaOutlineButton("Bilmiyorum", {',
        '                        Spacer(Modifier.height(22.dp))\n'
        '                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {\n'
        '                            ArenaOutlineButton("PAS", {',
        "separate offline control from Pas and hint")
    assert 'ArenaCrosswordTypingStrip(' not in s
    assert 'Eksik harfleri yaz' not in s
    path.write_text(s,"utf-8")

def main():
    assert SRC.is_dir(),"Stage current Android source first"
    for name in ("ArenaFullWordInput.kt",):
        shutil.copyfile(ROOT/"tools/v152"/name,SRC/name)
    TESTS.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(ROOT/"tools/v152/ArenaFullWordInputTest.kt",
                    TESTS/"ArenaFullWordInputTest.kt")
    typing=SRC/"ArenaCrosswordTyping.kt"
    s=typing.read_text("utf-8")
    s=once(s,
        '    val positionedDraft = fillCrosswordGaps(crosswordFixedPattern(puzzle, solved, selected), typed)',
        '    val positionedDraft = crosswordFullWordPreview(crosswordFixedPattern(puzzle, solved, selected), typed)',
        "crossword board shows actual full-word character positions")
    typing.write_text(s,"utf-8")
    online()
    offline()
    print("Arena crossword: full-word typing and 22dp-separated PAS, both modes.")
    print("Arena home, ready waiting room, scores, matchmaking and rules unchanged.")

if __name__=="__main__":
    main()
