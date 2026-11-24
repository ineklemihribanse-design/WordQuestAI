#!/usr/bin/env python3
"""Refine New Game and pin ONLY the landscape tablet crossword while typing.
Apply after v198 and v199. No phone crossword geometry or game logic changes."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"

def one(s, a, b):
    assert s.count(a) == 1, (a[:140], s.count(a))
    return s.replace(a, b, 1)

def page(s):
    s = one(s, "import androidx.compose.ui.graphics.Color",
               "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush")
    s = one(s, "internal val LocalWq198Short = staticCompositionLocalOf { false }",
        """internal val LocalWq198Short = staticCompositionLocalOf { false }
internal val LocalWq200Wide = staticCompositionLocalOf { false }""")
    a = s.index("    BoxWithConstraints(Modifier.fillMaxSize().background(Wq198CardColor))")
    b = s.index("\n@Composable\ninternal fun Wq198Card", a)
    s = s[:a] + """    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color(0xFFF1EDF5))
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val wide = maxWidth >= 600.dp
        val short = maxHeight < 680.dp
        CompositionLocalProvider(LocalWq198Short provides short, LocalWq200Wide provides wide) {
            Column(
                Modifier.widthIn(max = 1000.dp).fillMaxSize()
                    .padding(horizontal = if (wide) 16.dp else 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(if (wide) 10.dp else 5.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(
                            Color(0xFF15483D), Color(0xFF276A53), Color(0xFF1D5144)
                        )), RoundedCornerShape(18.dp))
                        .padding(horizontal = 8.dp, vertical = if (wide) 8.dp else 2.dp)
                        .heightIn(min = if (wide) 60.dp else 46.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = back, modifier = Modifier.size(46.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) { Text("‹", fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f)) {
                        Text("Yeni Oyun", color = Color.White, fontFamily = Serif,
                            fontSize = if (wide) 26.sp else 20.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(levelText + " · " + available + " kelime",
                            color = Color(0xFFE7F5DB), fontSize = if (wide) 13.sp else 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Wq198Artwork("level", Modifier.size(if (wide) 54.dp else 36.dp))
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val viewport = maxHeight
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = if (wide) viewport else 0.dp),
                        verticalArrangement = if (wide) Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                            else Arrangement.spacedBy(5.dp),
                        content = body,
                    )
                }
                Surface(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF9F7FC), border = BorderStroke(1.dp, Wq198Edge),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp), content = footer)
                }
            }
        }
    }
}
""" + s[b:]
    s = one(s, "color = Wq198CardColor, border = BorderStroke(1.dp, Wq198Edge),",
               "color = Color(0xFFFAF7FD), border = BorderStroke(1.dp, Wq198Edge),")
    s = one(s, "Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),\n            verticalArrangement = Arrangement.spacedBy(4.dp), content = body,",
        """Modifier.fillMaxWidth().padding(horizontal = if (LocalWq200Wide.current) 16.dp else 7.dp,
                vertical = if (LocalWq200Wide.current) 12.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(if (LocalWq200Wide.current) 7.dp else 2.dp), content = body,""")
    s = one(s, "if (LocalWq198Short.current) 34.dp else 41.dp",
               "if (LocalWq200Wide.current) 52.dp else 32.dp")
    s = one(s, "fontSize = 17.sp, fontWeight = FontWeight.Bold)",
               "fontSize = if (LocalWq200Wide.current) 21.sp else 16.sp, fontWeight = FontWeight.Bold)")
    s = one(s, "modifier = Modifier.weight(1f).heightIn(min = 44.dp),",
               "modifier = Modifier.weight(1f).heightIn(min = if (LocalWq200Wide.current) 52.dp else 40.dp),")
    s = one(s, 'Wq198Artwork(icon, Modifier.size(30.dp))',
               'Wq198Artwork(icon, Modifier.size(if (LocalWq200Wide.current) 42.dp else 27.dp))')
    s = one(s, 'fontSize = 12.sp, fontWeight = if (active)', 'fontSize = if (LocalWq200Wide.current) 15.sp else 12.sp, fontWeight = if (active)')
    return s

def grid(s):
    s = one(s, "options == englishLevels -> if (maxWidth >= 480.dp) 6 else 3",
               "options == englishLevels -> if (maxWidth >= 306.dp) 6 else 3")
    s = one(s, "modifier = Modifier.weight(1f).heightIn(min = 40.dp),",
               "modifier = Modifier.weight(1f).heightIn(min = if (LocalWq200Wide.current) 48.dp else 36.dp),")
    s = one(s, "fontSize = if (columns >= 5) 11.sp else 12.sp,",
               "fontSize = if (LocalWq200Wide.current) 14.sp else if (columns >= 5) 11.sp else 12.sp,")
    s = one(s, "Column(Modifier.width(155.dp)) { heading() }",
               "Column(Modifier.width(220.dp)) { heading() }")
    return s

def form(s):
    s = one(s, """val limit = if (LocalWq198Short.current && maxWidth < 500.dp) 2
                        else if (maxWidth >= 600.dp) 5
                        else 4""",
        """val limit = if (LocalWq200Wide.current) 10
                        else if (LocalWq198Short.current) 2
                        else 3""")
    s = one(s, 'if (choices.size > 2) {', 'if (choices.size > (if (LocalWq200Wide.current) 10 else if (LocalWq198Short.current) 2 else 3)) {')
    s = one(s, 'if (maxWidth >= 620.dp) {', 'if (maxWidth >= 620.dp) {')
    return s

def puzzle(s):
    original = s
    # Only the tablet's wide typing layout loses its outer vertical scroll.
    s = one(s, "            val wide = maxWidth >= 760.dp",
        """            val wide = maxWidth >= 760.dp
            val tabletTyping = wide && keyboardVisible &&
                androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600""")
    s = one(s, "Modifier.fillMaxWidth().verticalScroll(pageScroll)\n                    .padding(horizontal = if (wide)",
        """Modifier.fillMaxWidth()
                    .then(if (tabletTyping) Modifier.fillMaxHeight() else Modifier.verticalScroll(pageScroll))
                    .padding(horizontal = if (wide)""")
    # A retained page scroll offset is ignored while typing; only the clue
    # column can scroll, independently of the anchored crossword and input.
    s = one(s, "Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {",
        """Column(Modifier.weight(1f).then(
                            if (tabletTyping) Modifier.verticalScroll(rememberScrollState()) else Modifier
                        ), verticalArrangement = Arrangement.spacedBy(10.dp)) {""")
    s = one(s, """                            inlineEditor()
                            Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""",
        """                            inlineEditor()
                            if (!tabletTyping)
                                Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)""")
    # The IME-safe height is allocated before the board; leave space for
    # the existing text field, container spacing and (if active) timer.
    s = one(s, "maxHeight.value, originalTabletHeight.value",
        "(maxHeight.value - if (timer != null) 40f else 0f), originalTabletHeight.value")
    # Restore top on entry, including when a clue was selected far down
    # the previous outer list. No reset or modifier changes on phones.
    anchor = "            val phoneLayout = maxWidth < 600.dp"
    s = one(s, anchor, """            LaunchedEffect(tabletTyping) {
                if (tabletTyping) pageScroll.scrollTo(0)
            }
""" + anchor)
    # Verify the entire phone layout and BoardFrame implementation are untouched.
    marker = "                } else {\n                    // Phone editor"
    assert marker in original and marker in s
    assert original[original.index(marker):] == s[s.index(marker):]
    if "import androidx.compose.foundation.layout.fillMaxHeight\n" not in s and "import androidx.compose.foundation.layout.*\n" not in s:
        s = one(s, "import androidx.compose.foundation.layout.Arrangement",
                   "import androidx.compose.foundation.layout.Arrangement\nimport androidx.compose.foundation.layout.fillMaxHeight")
    return s

def main():
    changes = {
        "Wq198NewGameUi.kt": page,
        "Wq197CompactNewGameUi.kt": grid,
        "NewGameScreen.kt": form,
        "PuzzleScreen.kt": puzzle,
    }
    pending = {}
    for name, fn in changes.items():
        p = SRC / name
        pending[p] = fn(p.read_text("utf-8"))
    for p, data in pending.items():
        p.write_text(data, "utf-8")
    print("PASS: New Game safe insets, compact phone cards, balanced tablet layout")
    print("PASS: tablet IME pins board and input; independent clue scrolling; phone and BoardFrame untouched")

if __name__ == "__main__":
    main()
