#!/usr/bin/env python3
"""v1.0.102: fix clipped tablet typing actions; refine ONLY the New Game page.
Requires the exact published v1.0.101 reconstruction. No board or keyboard edits."""
from pathlib import Path
import runpy
import re

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
balanced = runpy.run_path(str(ROOT / "tools/v200/apply_phone_hint_while_typing_200.py"))["balanced"]

def one(s, a, b):
    assert s.count(a) == 1, (a[:140], s.count(a))
    return s.replace(a, b, 1)

def puzzle(s):
    original = s
    # The old action row follows the board/editor and is clipped by the IME.
    # Do not change the board height, editor, keyboard or left column geometry.
    old = """                            if (tabletTyping) wq201TypingActions()
                            else Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)"""
    new = """                            if (!tabletTyping)
                                Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)"""
    s = one(s, old, new)
    # The entire existing clue body is reused in both branches. On a wide
    # tablet with the IME showing, actions live outside the scrolling list.
    right = """Column(Modifier.weight(1f).then(
                            if (tabletTyping) Modifier.verticalScroll(rememberScrollState()) else Modifier
                        ), verticalArrangement = Arrangement.spacedBy(10.dp)) {"""
    assert s.count(right) == 1
    a = s.index(right)
    b = balanced(s, a + len(right) - 1, "{", "}")
    body = s[a + len(right):b - 1]
    assert "ActiveClue(" in body, "Unexpected right panel"
    replacement = """Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(if (tabletTyping) 4.dp else 0.dp)) {
                            if (tabletTyping) {
                                // Fixed above the independently scrolling clues, never under the IME.
                                wq201TypingActions()
                            }
                            Column(
                                Modifier.fillMaxWidth().then(
                                    if (tabletTyping) Modifier.weight(1f)
                                        .verticalScroll(rememberScrollState()) else Modifier
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {""" + body + """
                            }
                        }"""
    s = s[:a] + replacement + s[b:]
    assert s.replace(replacement, right + body + "}", 1).replace(new, old, 1) == original
    # Phone branch and every rendering helper are byte-for-byte unchanged.
    for anchor in ("                } else {\n                    // Phone editor",
                   "private fun BoardFrame(", "private fun GameHeading("):
        assert original[original.index(anchor):] == s[s.index(anchor):], anchor
    # All board dimension logic, board call sites, and the existing answer
    # editor are identical. No custom keyboard or gameplay restyling.
    def calls(src):
        return [src[m.start():balanced(src, m.end()-1, "(", ")")]
                for m in re.finditer(r"\bBoardFrame\(", src)]
    assert calls(original) == calls(s)
    assert original[original.index("val fittedBoardHeight"):original.index("            Column(",original.index("val fittedBoardHeight"))] == s[s.index("val fittedBoardHeight"):s.index("            Column(",s.index("val fittedBoardHeight"))]
    assert s.count("wq201TypingActions()") == 2
    return s

def ui(s):
    # The old header cropped only the top few pixels of the background and
    # overlaid a gray fade, hiding the gold/lilac wave. Render full-page art.
    start=s.index("        val landscapeArtwork = wide && maxWidth > maxHeight")
    end=s.index("        CompositionLocalProvider",start)
    s=s[:start]+'''        val landscapeArtwork = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        Wq198Artwork(if (landscapeArtwork) "tablet_202" else "phone_202",
            Modifier.matchParentSize(), scale = ContentScale.Crop,
            alignment = Alignment.TopCenter)
'''+s[end:]
    a=s.index("                Row(\n                    Modifier.fillMaxWidth()")
    b=s.index("                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth())",a)
    header=s[a:b]
    assert 'Text("Yeni Oyun"' in header and 'Wq198Artwork("level"' in header
    s=s[:a]+'''                Row(
                    Modifier.fillMaxWidth().heightIn(min = if (wide) 78.dp else 70.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val side = if (wide) 62.dp else 44.dp
                    TextButton(onClick = back, modifier = Modifier.size(side),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) { Text("‹", fontSize = 32.sp, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("WordQuest ", color = Color.White, fontFamily = Serif,
                                fontSize = if (wide) 16.sp else 13.sp, fontWeight = FontWeight.Bold)
                            Text("AI", color = Color(0xFFD9F389), fontFamily = Serif,
                                fontSize = if (wide) 16.sp else 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Yeni Oyun", color = Color.White, fontFamily = Serif,
                            fontSize = if (wide) 30.sp else 24.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(levelText + " · " + available + " kelime",
                            color = Color(0xFFF4FFF0), fontSize = if (wide) 14.sp else 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Wq198Artwork("level", Modifier.size(side))
                }
'''+s[b:]
    # A single outer silhouette supplies the gradient/shadow. The original
    # Material outlined button also draws an inner border at its touch inset,
    # producing the unwanted double lines on phones. Remove only that stroke.
    s=one(s,'border = BorderStroke(1.dp, if (active) Color(0xFFC2D88D) else Wq198Edge),',
          'border = null,')
    s=one(s,'return Modifier.shadow(2.dp, shape).background(Brush.verticalGradient(colors), shape)',
          'return Modifier.shadow(2.dp, shape).background(Brush.verticalGradient(colors), shape)')
    s=one(s,'Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)',
          'Text(text, fontSize = 18.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)')
    # Remove opaque footer card: only the unchanged Start control is elevated.
    s=one(s,'color = Color(0xFFF9F7FC), border = BorderStroke(1.dp, Wq198Edge),',
          'color = Color.Transparent, border = null,')
    assert "windowInsetsPadding(WindowInsets.safeDrawing).imePadding()" in s
    return s

def grid(s):
    return one(s,'''border = BorderStroke(1.dp,
                                if (active) Color(0xFFC3D89C) else Color(0xFFD7DED5)),''',
        'border = null,')

if __name__ == "__main__":
    patches={"PuzzleScreen.kt":puzzle, "Wq198NewGameUi.kt":ui,
             "Wq197CompactNewGameUi.kt":grid}
    changed = {SRC/name:fn((SRC/name).read_text("utf-8")) for name,fn in patches.items()}
    for p,data in changed.items(): p.write_text(data,"utf-8")
    print("PASS: tablet writing actions fixed above independent clue scroll; phone board, keyboard and all board geometry unchanged")
    print("PASS: centered New Game hero, supplied full-page backgrounds, single-edge 3D buttons and existing form layout")
