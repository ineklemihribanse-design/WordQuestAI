#!/usr/bin/env python3
"""v190 compact crossword UI and consistent hint/draft overlays.

Installed only AFTER the seven packages and previous v190 UI overlays.
It must not change solved-word persistence, achievements, Firebase, or scoring.
The answer editor is relocated in the follow-up layout patch, after checking
the exact assembled source. All anchors fail closed.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
FILE=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"

def one(s,old,new,label):
    count=s.count(old)
    if count!=1:
        raise AssertionError(f"{label}: expected one match, found {count}")
    return s.replace(old,new,1)

def apply():
    src=FILE.read_text("utf-8")
    s=src
    if "import androidx.compose.ui.text.style.TextOverflow" not in s:
        s=one(s,
            "import androidx.compose.ui.text.font.FontWeight",
            "import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.style.TextOverflow",
            "import ellipsis rendering for compact clue")

    # Keep the informative "YDS · Kolay · Karışık" subtitle and remove
    # the huge repeated "Kolay · Karışık" title directly below it.
    # v175 placed the title on TWO lines, so older single-line matchers missed it.
    s=one(s,
        '''                Text(game.title, color = Ink, fontFamily = Serif,
                    fontSize = 28.sp, lineHeight = 31.sp)
''',
        '',"remove repeated level/category heading; keep path, score and progress")
    s=one(s,
        'Wq175GlossIcon("puzzle",Modifier.size(45.dp))',
        'Wq175GlossIcon("puzzle",Modifier.size(32.dp))',
        "compact game header icon")
    assert 'Text(game.path,' in s
    assert 'Text(game.title,' not in s
    assert 'view.points.toString() + " puan"' in s

    # The answer clue remains readable but should not occupy a large card.
    s=one(s,
        "vertical = if (compact) 10.dp else 14.dp",
        "vertical = if (compact) 5.dp else 7.dp",
        "reduce v175 inflated question-card vertical padding")
    s=one(s,
        "fontSize = if (compact) 17.sp else 22.sp, lineHeight = if (compact) 20.sp else 25.sp,",
        "fontSize = if (compact) 15.sp else 18.sp, lineHeight = if (compact) 18.sp else 21.sp,\n                    maxLines = 2, overflow = TextOverflow.Ellipsis,",
        "smaller meaning and answer clue, two-line allowance")
    s=one(s,
        "if (compact) 27.dp else 34.dp",
        "if (compact) 25.dp else 30.dp",
        "reduce large clue number badge")
    s=one(s,
        'Text(word.entry.context, color = Forest, fontFamily = Sans, fontSize = if (compact) 12.sp else 14.sp, lineHeight = 17.sp)',
        'Text(word.entry.context, color = Forest, fontFamily = Sans, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)',
        "compact optional clue context")

    # Existing editor (Gönder) remains the only entry validation point.
    # The reveal command writes one authoritative character into view.letters.
    # Never overlay the revealed character with a blank unsent input preview.
    s=one(s,
        "remember(view.wordIndex, wordDraft, inlineTyping, view.locked) {",
        "remember(view.wordIndex, wordDraft, inlineTyping, view.letters, view.locked) {",
        "listen for newly revealed letter events")
    s=one(s,
        "if (cell !in view.locked) put(cell, typed.getOrNull(index))",
        """if (cell !in view.locked && view.letters[cell] == null)
                        put(cell, typed.getOrNull(index))""",
        "revealed hints and saved crossing letters must win over unsent draft")

    # Compact meaning card in landscape (clue on right) and portrait.
    original='ActiveClue(word, view.wordIndex in view.solvedIndices, false)'
    if s.count(original)!=2:
        raise AssertionError(f"expected two noncompact clue call sites, got {s.count(original)}")
    s=s.replace(original,'ActiveClue(word, view.wordIndex in view.solvedIndices, true)')

    # Put the only editor *after* the grid. On tablets the board and answer
    # actions form a single left column. On phones the editor stays in the same
    # unconditional composition branch when the IME appears (focus is stable).
    s=one(s,
        """                        Box(Modifier.weight(1.22f), contentAlignment = Alignment.TopCenter) {""",
        """                        Column(Modifier.weight(1.42f), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {""",
        "full-width landscape crossword plus its answer controls")

    s=one(s,
        """                            inlineEditor()
                            if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                            Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""",
        """                            if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)""",
        "remove duplicate tablet input/actions from right side")

    s=one(s,
        """                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {""",
        """                            }
                            inlineEditor()
                            Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {""",
        "move tablet answer entry and Harf aç directly below the grid")

    s=one(s,
        """                    inlineEditor()
                    BoardFrame(""",
        """                    BoardFrame(""",
        "phone grid must appear above the answer textbox with IME open")
    s=one(s,
        """                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""",
        """                    inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""",
        "place phone answer textbox and Harf aç below the crossword")

    s=one(s,
        "(maxWidth * .55f - 22.dp).coerceAtMost(570.dp)",
        "(maxWidth * .56f - 16.dp).coerceAtMost(600.dp)",
        "slightly enlarge actual tablet grid with reclaimed clue space")

    assert s.count("inlineEditor()")==2
    assert s.count("Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)")==2

    assert "fun revealAndLearn()" in s
    assert '"Harf aç"' in s
    assert 'val previewLetters: Map<Cell, Char?>' in s
    assert 'inlineEditor()' in s
    assert 'submitInlineWord()' in s
    if src==s:raise AssertionError("no crossword UI change made")
    FILE.write_text(s,"utf-8")
    print("PASS v190: duplicate YDS label removed and clue card compact")
    print("PASS v190: opened hint letters and live answer draft render together")
    print("PASS v190: all scoring and validation handlers untouched")

if __name__=="__main__":
    apply()
