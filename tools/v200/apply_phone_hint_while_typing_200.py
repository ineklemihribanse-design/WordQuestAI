#!/usr/bin/env python3
"""Only add a phone keyboard-mode Harf aç action beside Sonraki ipucu.
The board renderer, viewport geometry and normal game layout stay unchanged."""
import re
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
FILE = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"

def balanced(s, start, opening, closing):
    assert s[start] == opening
    depth, i = 1, start + 1
    while i < len(s):
        ch = s[i]
        if ch in ('"', "'"):
            q = ch
            i += 1
            while i < len(s):
                if s[i] == "\\":
                    i += 2
                    continue
                if s[i] == q:
                    break
                i += 1
        elif ch == opening:
            depth += 1
        elif ch == closing:
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise AssertionError("Unbalanced next clue control")

def patch(s):
    start = s.index("                } else {\n                    // Phone editor")
    stop = s.index("private fun GameHeading(", start)
    part = s[start:stop]
    target = '"Sonraki ipucu"'
    assert part.count(target) == 1, "Expected the actual phone next clue action"
    label = part.index(target)
    candidates = list(re.finditer(r"(?m)^([ \t]*)TextButton\s*\(", part[:label]))
    assert candidates, "Next clue TextButton not found"
    m = candidates[-1]
    call_start = m.start()
    paren = part.index("(", m.start())
    call_end = balanced(part, paren, "(", ")")
    k = call_end
    while k < len(part) and part[k].isspace():
        k += 1
    if k < len(part) and part[k] == "{":
        call_end = balanced(part, k, "{", "}")
    assert call_start < label < call_end, "Not the next clue button"
    original = part[call_start:call_end]
    indent = m.group(1)
    # A previous ColumnScope end-alignment is irrelevant within this row.
    next_in_row = original.strip().replace(".align(Alignment.End)", "")
    hint = """TextButton(
        onClick = {
            // Keep already typed letters in their original cells when a
            // newly opened hint removes one of the available positions.
            val selectedIndex = view.wordIndex
            val oldDraft = wordDraft
            val oldTyped = Wq190CrosswordHintInput.typedPositions(
                word.cells.size, hintedPositions, oldDraft
            )
            revealAndLearn()
            if (view.wordIndex == selectedIndex &&
                selectedIndex !in view.solvedIndices) {
                if (oldDraft.length != word.cells.size) {
                    wordDraft = word.cells.mapIndexedNotNull { index, cell ->
                        if (cell in view.locked) null else oldTyped[index]
                    }.joinToString("")
                }
                inlineTyping = true
                runCatching { entryFocus.requestFocus() }
                softKeyboard?.show()
            }
        },
        enabled = !outOfTime && !timedOut && !shuffling &&
            view.wordIndex !in view.solvedIndices,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text("Harf aç", color = Forest, fontWeight = FontWeight.SemiBold)
    }"""
    replacement = indent + """if (keyboardVisible &&
    androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp < 600) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
""" + "\n".join("        " + line for line in hint.splitlines()) + "\n        " + next_in_row + """
    }
} else {
    """ + original.strip() + "\n}"
    out = s[:start] + part[:call_start] + replacement + part[call_end:] + s[stop:]
    assert out[:start] == s[:start]
    assert out[out.index("private fun GameHeading("):] == s[stop:]
    assert out.count("revealAndLearn()") == s.count("revealAndLearn()") + 1
    assert out.replace(replacement, original, 1) == s
    return out

if __name__ == "__main__":
    original = FILE.read_text("utf-8")
    FILE.write_text(patch(original), "utf-8")
    print("PASS: keyboard-only phone Harf aç beside existing next clue")
    print("PASS: board geometry, tablet and all other source unchanged")
