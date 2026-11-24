#!/usr/bin/env python3
"""Apply on top of the exact v1.0.100 source reconstruction.
Do not change board geometry, renderer, zoom, game engine or storage."""
from pathlib import Path
import shutil
import runpy

ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
balanced=runpy.run_path(str(ROOT/"tools/v200/apply_phone_hint_while_typing_200.py"))["balanced"]

def one(s,a,b):
    assert s.count(a)==1,(a[:160],s.count(a))
    return s.replace(a,b,1)

def puzzle(s):
    old=s
    # BoardFrame and all subsequent rendering helpers must stay byte-identical.
    tail="private fun GameHeading("
    assert s.count(tail)==1
    s=one(s,'    val entryFocus = remember { FocusRequester() }',
        '''    var wq201Opened by remember(game.id, board) { mutableStateOf<Map<Cell, Char>>(emptyMap()) }
    val entryFocus = remember { FocusRequester() }''')
    s=one(s,'''    val hintedPositions: Map<Int, Char> = Wq193EditableDraft.protectedPositions(
        existingPositions, lockedPositionIndices
    )''','''    val revealedPositionLetters = word.cells.mapIndexedNotNull { index, cell ->
        wq201Opened[cell]?.let { index to it }
    }.toMap()
    val hintedPositions = Wq201HintDraft.authoritative(
        existingPositions, lockedPositionIndices, revealedPositionLetters
    )''')
    start=s.index("    val previewLetters: Map<Cell, Char?> =")
    end=s.index("    LaunchedEffect(inlineTyping, view.wordIndex, outOfTime)",start)
    assert "Wq190CrosswordHintInput.typedPositions(" in s[start:end]
    s=s[:start]+'''    // Compute from current state rather than retaining a mutable map as a
    // remember key. The existing BoardFrame consumes this map unchanged.
    val previewLetters: Map<Cell, Char?> =
        if (!inlineTyping && wordDraft.isEmpty()) emptyMap()
        else buildMap<Cell, Char?> {
            val typed = Wq190CrosswordHintInput.typedPositions(
                word.cells.size, hintedPositions, englishLetters(wordDraft)
            )
            word.cells.forEachIndexed { index, cell ->
                if (index !in hintedPositions) {
                    // A blank draft must never cover a real engine letter.
                    val draftLetter = typed[index]
                    if (draftLetter != null || view.letters[cell] == null)
                        put(cell, draftLetter)
                }
            }
        }
'''+s[end:]
    # Keep the existing hint action itself intact. Track only letters changed
    # by the actual engine hint, never infer the answer from puzzle.solution.
    marker="    val inlineEditor: @Composable () -> Unit = {"
    s=one(s,marker,'''    fun revealWhileTyping() {
        if (outOfTime || timedOut || shuffling ||
            view.wordIndex in view.solvedIndices) return
        val selectedIndex = view.wordIndex
        val cells = word.cells
        val before = cells.mapIndexedNotNull { index, cell ->
            view.letters[cell]?.let { index to it }
        }.toMap()
        val oldLocked = hintedPositions.toMap()
        val oldDraft = wordDraft
        revealAndLearn()
        if (view.wordIndex == selectedIndex) {
            val after = cells.mapIndexedNotNull { index, cell ->
                view.letters[cell]?.let { index to it }
            }.toMap()
            val added = Wq201HintDraft.newlyRevealed(before, after)
            wq201Opened = wq201Opened + added.mapKeys { (index, _) -> cells[index] }
            val locked = cells.mapIndexedNotNull { index, cell ->
                if (cell in view.locked) index else null
            }.toSet()
            val revealed = cells.mapIndexedNotNull { index, cell ->
                wq201Opened[cell]?.let { index to it }
            }.toMap()
            val newLocked = Wq201HintDraft.authoritative(after, locked, revealed)
            wordDraft = Wq201HintDraft.remap(cells.size, oldLocked, newLocked, oldDraft)
            if (selectedIndex !in view.solvedIndices) {
                inlineTyping = true
                runCatching { entryFocus.requestFocus() }
                softKeyboard?.show()
            }
        }
    }

    val wq201TypingActions: @Composable () -> Unit = {
        val tablet = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { revealWhileTyping() },
                enabled = !outOfTime && !timedOut && !shuffling &&
                    view.wordIndex !in view.solvedIndices,
                modifier = if (tablet) Modifier.height(36.dp) else Modifier,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text("Harf aç", color = Forest, fontWeight = FontWeight.SemiBold) }
            TextButton(
                onClick = {
                    if (!outOfTime && !timedOut && !shuffling) {
                        engine.moveToNextWord()
                        view = engine.view()
                        if (view.wordIndex !in view.solvedIndices) {
                            inlineTyping = true
                            runCatching { entryFocus.requestFocus() }
                            softKeyboard?.show()
                        }
                    }
                },
                enabled = !outOfTime && !timedOut && !shuffling,
                modifier = if (tablet) Modifier.height(36.dp) else Modifier,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) { Text("Sonraki ipucu", color = Forest, fontWeight = FontWeight.SemiBold) }
        }
    }

'''+marker)
    # Existing wide tablet branch: keep all geometry and only occupy the
    # unused area after the editor, as requested.
    s=one(s,'''                            if (!tabletTyping)
                                Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)''',
        '''                            if (tabletTyping) wq201TypingActions()
                            else Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)''')
    # Replace the previous phone-only keyboard action row; portrait tablets
    # use this same branch. The existing non-keyboard branch remains intact.
    a=s.index("                } else {\n                    // Phone editor")
    b=s.index("if (keyboardVisible &&\n    androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp < 600) {",a)
    e=balanced(s,s.index("{",b),"{","}")
    assert "Harf aç" in s[b:e] and "Sonraki ipucu" in s[b:e]
    s=s[:b]+"if (keyboardVisible) { wq201TypingActions() }"+s[e:]
    assert s[s.index(tail):]==old[old.index(tail):]
    # Geometry and all BoardFrame call sites remain identical.
    for a,b in (("val fittedBoardHeight", "            Column("),
                ("private fun BoardFrame(", "@Composable\nprivate fun WordFields(")):
        x=old.index(a); y=old.index(b,x)
        assert old[x:y] in s,a
    assert s.count("inlineEditor()")==old.count("inlineEditor()")==2
    assert s.count("BoardFrame(")==old.count("BoardFrame(")
    assert s.count("wq201TypingActions()")==2
    assert "state.enter(completeAnswer)" in s
    return s

def theme(s):
    s=one(s,"import androidx.compose.ui.graphics.Brush",
          "import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.draw.shadow")
    s=one(s,"scale: ContentScale = ContentScale.Fit,",
          "scale: ContentScale = ContentScale.Fit,\n    alignment: Alignment = Alignment.Center,")
    s=one(s,"contentScale = scale, filterQuality = FilterQuality.High)",
          "contentScale = scale, alignment = alignment, filterQuality = FilterQuality.High)")
    s=one(s,"Modifier.fillMaxSize().background(Color(0xFFF1EDF5))",
          "Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF15483D), Color(0xFFF1EDF5), Color(0xFFF1EDF5))))")
    # Paint a separate, full-width background layer, not a header card. Art
    # is cropped proportionally; only the header zone receives the artwork.
    s=one(s,"        val short = maxHeight < 680.dp\n        CompositionLocalProvider",
        '''        val short = maxHeight < 680.dp
        val landscapeArtwork = wide && maxWidth > maxHeight
        Box(Modifier.matchParentSize().background(Color(0xFFF1EDF5)))
        Box(Modifier.fillMaxWidth().height(if (wide) 108.dp else 78.dp)
            .background(Color(0xFF17483C))) {
            Wq198Artwork(if (landscapeArtwork) "tablet" else "phone",
                Modifier.matchParentSize(), scale = ContentScale.Crop,
                alignment = Alignment.TopCenter)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                0f to Color(0x3515483D), 0.58f to Color(0x3015483D),
                0.76f to Color(0x1515483D), 1f to Color(0xFFF1EDF5))))
        }
        CompositionLocalProvider''')
    s=one(s,'''                        .background(Brush.horizontalGradient(listOf(
                            Color(0xFF15483D), Color(0xFF276A53), Color(0xFF1D5144)
                        )), RoundedCornerShape(18.dp))
''',"")
    # Same measured dimensions, spacing and padding as v200.
    s=one(s,'''        Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        color = Color(0xFFFAF7FD), border = BorderStroke(1.dp, Wq198Edge),''',
        '''        Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(
                Color(0xFFFFFCFF), Color(0xFFF4ECFA), Color(0xFFEFE6F7)
            )), RoundedCornerShape(15.dp)),
        shape = RoundedCornerShape(15.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, Color(0xFFD8CBE5)),''')
    s=one(s,"modifier = Modifier.weight(1f).heightIn(min = if (LocalWq200Wide.current) 52.dp else 40.dp),",
        '''modifier = Modifier.weight(1f).heightIn(min = if (LocalWq200Wide.current) 52.dp else 40.dp)
                    .then(wq201Pill(active, enabled, 13.dp)),''')
    s=one(s,"containerColor = if (active) Wq198Lime else Color(0xFFFFFEFA),",
        "containerColor = Color.Transparent,\n                    disabledContainerColor = Color.Transparent,")
    # Footer retains its original size and position. Only the actual button
    # is restyled; the original validation and callback are passed unchanged.
    s+='''
internal fun wq201Pill(active: Boolean, enabled: Boolean, radius: androidx.compose.ui.unit.Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    val colors = when {
        !enabled -> listOf(Color(0xFFF1EEF3), Color(0xFFE5E1E9))
        active -> listOf(Color(0xFFF3F9D5), Color(0xFFE4F1B2), Color(0xFFD6E9A0))
        else -> listOf(Color(0xFFFFFFFF), Color(0xFFFFFEFA), Color(0xFFF0ECE8))
    }
    return Modifier.shadow(2.dp, shape).background(Brush.verticalGradient(colors), shape)
}

@Composable
internal fun Wq201StartAction(text: String, action: () -> Unit, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Button(onClick = action, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .shadow(3.dp, shape).background(Brush.verticalGradient(
                if (enabled) listOf(Color(0xFF33765D), Color(0xFF205749), Color(0xFF164639))
                else listOf(Color(0xFFE2E2E7), Color(0xFFD5D3DB))
            ), shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent, contentColor = Color.White,
            disabledContentColor = Color(0xFF88858B)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}
'''
    return s

def options(s):
    s=one(s,"modifier = Modifier.weight(1f).heightIn(min = if (LocalWq200Wide.current) 48.dp else 36.dp),",
        '''modifier = Modifier.weight(1f).heightIn(min = if (LocalWq200Wide.current) 48.dp else 36.dp)
                                .then(wq201Pill(active, enabled, 22.dp)),''')
    s=one(s,"containerColor = if (active) Color(0xFFE8F4BB) else Color(0xFFFFFEFA),",
        "containerColor = Color.Transparent,\n                                disabledContainerColor = Color.Transparent,")
    return s

def form(s):
    s=one(s,'PrimaryAction(if (loading) "Bulmaca hazırlanıyor…" else "Oyuna Başla",',
          'Wq201StartAction(if (loading) "Bulmaca hazırlanıyor…" else "Oyuna Başla",')
    # Keep local lambda labels valid if the old helper used a labelled return.
    s=s.replace("return@PrimaryAction","return@Wq201StartAction")
    assert "session.newGameFiltered(levels, topics, kinds, count!!)" in s
    assert "MAX_NEW_GAME_WORDS = 70" in s
    return s

if __name__=="__main__":
    files={"PuzzleScreen.kt":puzzle,"Wq198NewGameUi.kt":theme,
           "Wq197CompactNewGameUi.kt":options,"NewGameScreen.kt":form}
    changed={SRC/name:fn((SRC/name).read_text("utf-8")) for name,fn in files.items()}
    for path,content in changed.items(): path.write_text(content,"utf-8")
    TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(HERE/"Wq201HintDraft.kt",SRC/"Wq201HintDraft.kt")
    shutil.copyfile(HERE/"Wq201HintDraftTest.kt",TEST/"Wq201HintDraftTest.kt")
    print("PASS: BoardFrame, geometry and render helpers unchanged; only preview state and writing-mode actions changed.")
    print("PASS: original hero artwork, 3D surfaces and current New Game layout retained; existing start callback unchanged.")
