#!/usr/bin/env python3
"""Synchronize keyboard exit with Android IME transition, preserving all artwork and geometry."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
p=PKG/"PuzzleScreen.kt"
original=p.read_text("utf-8")
s=original
def once(a,b):
    global s
    assert s.count(a)==1,(a[:180],s.count(a))
    s=s.replace(a,b,1)
once("@Composable\ninternal fun PuzzleScreen(",
"@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)\n@Composable\ninternal fun PuzzleScreen(")
once("    val imeVisible = WindowInsets.ime.getBottom(density) > 0",
'''    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val imeSourcePx = WindowInsets.imeAnimationSource.getBottom(density)
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)
    val imeClosing = Wq215ImeExit.isClosing(
        android.os.Build.VERSION.SDK_INT >= 30, imeSourcePx, imeTargetPx
    )''')
once("    val keyboardVisible = Wq213TypingContinuity.layoutActive(imeVisible, inlineTyping, keyboardWasShown)",
'''    // Follow the platform's dismissal transition, not the previous frame's
    // latched typing layout. A real closing animation never needs focus zoom.
    val keyboardVisible = Wq215ImeExit.typingLayout(
        imeVisible, imeClosing, imeTargetPx, inlineTyping, keyboardWasShown
    )''')
once('''            withFrameNanos { }
            runCatching {
                entryFocus.requestFocus()
                if (!imeVisible) softKeyboard?.show()
            }''',
'''            withFrameNanos { }
            runCatching {
                entryFocus.requestFocus()
                if (!imeVisible) softKeyboard?.show()
            }''') # existing guarded focus contract
once('''    LaunchedEffect(imeVisible) {
        if (imeVisible && !view.complete && !outOfTime && !timedOut) {
            keyboardWasShown = true
            inlineTyping = true
        } else if (!imeVisible && keyboardWasShown) {''',
'''    LaunchedEffect(imeVisible, imeClosing) {
        if (imeClosing && keyboardWasShown) {
            endTypingMode(requestImeHide = false)
        } else if (imeVisible && !imeClosing && !view.complete && !outOfTime && !timedOut) {
            keyboardWasShown = true
            inlineTyping = true
        } else if (!imeVisible && keyboardWasShown) {''')
# Keep the board calls and dimensions identical; only eliminate redundant
# overview scroll resets on every intermediate viewport size.
once('''        } else {
            // IME dismissed: no stale manual offsets or half-visible board.
            horizontal.scrollTo(0)
            vertical.scrollTo(0)
        }
    }''',
'''        }
    }
    LaunchedEffect(enlarged) {
        if (!enlarged) {
            // Reset once upon entering overview, not on each IME resize.
            kotlinx.coroutines.coroutineScope {
                if (horizontal.value != 0) launch { horizontal.scrollTo(0) }
                if (vertical.value != 0) launch { vertical.scrollTo(0) }
            }
        }
    }''')
# Do not change the renderer, tile geometry, or other composables.
def between(src,a,b):
    i=src.index(a); return src[i:src.index(b,i)]
assert between(s,"val fittedBoardHeight","            Column(")==between(original,"val fittedBoardHeight","            Column(")
assert s[s.index("private fun WordFields("):]==original[original.index("private fun WordFields("):]
assert between(s,"private fun GameHeading(","private fun BoardFrame(")==between(original,"private fun GameHeading(","private fun BoardFrame(")
assert "Wq213TypingContinuity.advanceSolved(engine, submittedIndex)" in s
assert s.count("BoardFrame(")==original.count("BoardFrame(")
assert "delay(180L)" not in between(s,"fun endTypingMode(","    Box(")
p.write_text(s,"utf-8")
TEST.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/"tools/v215/Wq215ImeExit.kt",PKG/"Wq215ImeExit.kt")
shutil.copyfile(ROOT/"tools/v215/Wq215ImeExitTest.kt",TEST/"Wq215ImeExitTest.kt")
print("PASS: native closing target synchronizes overview; reset scroll once; board geometry and existing typing continuity retained")
