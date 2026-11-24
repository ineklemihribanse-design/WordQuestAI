#!/usr/bin/env python3
"""Keep the existing inline answer editor and IME active between answers."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
p=PKG/"PuzzleScreen.kt"
original=p.read_text("utf-8")
s=original
def once(old,new,label):
    global s
    assert s.count(old)==1,(label,s.count(old))
    s=s.replace(old,new,1)
# Separate intentional typing from the transient Android IME inset.
once("    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0",
'''    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var inlineTyping by remember(game.id, crossword) { mutableStateOf(false) }
    var keyboardWasShown by remember(game.id, crossword) { mutableStateOf(false) }
    val keyboardVisible = Wq213TypingContinuity.layoutActive(imeVisible, inlineTyping, keyboardWasShown)''',"stable typing state")
once("    var inlineTyping by remember { mutableStateOf(false) }\n","", "remove old duplicate typing state")
once("    val softKeyboard = LocalSoftwareKeyboardController.current",
'''    val softKeyboard = LocalSoftwareKeyboardController.current
    val typingFocusManager = androidx.compose.ui.platform.LocalFocusManager.current''',"explicit dismissal focus")
once('''    LaunchedEffect(inlineTyping, view.wordIndex, outOfTime) {
        if(inlineTyping && !outOfTime &&
            view.wordIndex !in view.solvedIndices) {
            // Wait for the visible editor to attach before requesting focus.
            // Never crash the game if focus is temporarily unavailable.
            withFrameNanos { }
            runCatching {
                entryFocus.requestFocus()
                softKeyboard?.show()
            }
        }
    }''',
'''    LaunchedEffect(inlineTyping, view.wordIndex, outOfTime, timedOut, view.complete) {
        if (inlineTyping && !outOfTime && !timedOut && !view.complete) {
            // Same TextField remains mounted; request focus after composition.
            // Do not hide and re-show an IME that is already visible.
            withFrameNanos { }
            runCatching {
                entryFocus.requestFocus()
                if (!imeVisible) softKeyboard?.show()
            }
        }
    }
    LaunchedEffect(outOfTime, timedOut, view.complete) {
        if (outOfTime || timedOut || view.complete) {
            inlineTyping = false
            keyboardWasShown = false
            typingFocusManager.clearFocus(force = true)
            softKeyboard?.hide()
        }
    }''',"retain editor focus and close only at terminal state")
once('''        edit(typedWord=true) { state -> state.enter(completeAnswer) }
        checkAndLearn()
        if (view.wordIndex in view.solvedIndices) {
            // Only a correct, locked answer exits typing and hides the IME.
            wordDraft = ""
            inlineTyping = false
            softKeyboard?.hide()
        } else {
            // Wrong submission: keep the text and keyboard for backspace,
            // cursor editing, selection, or one-tap clear and retyping.
            inlineTyping = true
        }''',
'''        if (timedOut || shuffling) return
        val submittedIndex = view.wordIndex
        inlineTyping = true
        edit(typedWord=true) { state -> state.enter(completeAnswer) }
        checkAndLearn()
        if (submittedIndex in view.solvedIndices) {
            wordDraft = ""
            if (Wq213TypingContinuity.advanceSolved(engine, submittedIndex)) {
                // Avoid selectWord UI callback: it cancels correct-answer audio.
                view = engine.view()
            }
        }
        // Wrong answers retain the draft; correct answers retain typing.
        // The end-of-game effect handles actual completion.''',"submit and advance without closing IME")
# A solved clue selected by the player or completed by a hint must not
# dispose the focused input. Keep it mounted during typing; ignore edits
# until the player selects another unsolved clue.
a=s.index("    val inlineEditor: @Composable () -> Unit = {")
b=s.index("\n    }",a)+len("\n    }")
old=s[a:b]
assert old.count("if (!outOfTime && view.wordIndex !in view.solvedIndices) {")==1
new=old.replace("if (!outOfTime && view.wordIndex !in view.solvedIndices) {",
"if (!outOfTime && !timedOut && !view.complete && (inlineTyping || view.wordIndex !in view.solvedIndices)) {",1)
assert new.count("onValueChange={ wordDraft=Wq193EditableDraft.normalizedText(it,word.cells.size) },")==1
new=new.replace("onValueChange={ wordDraft=Wq193EditableDraft.normalizedText(it,word.cells.size) },",
"onValueChange={ if (view.wordIndex !in view.solvedIndices) wordDraft=Wq193EditableDraft.normalizedText(it,word.cells.size) },",1)
assert new.count("enabled=Wq190CrosswordHintInput.canSubmit(")==1
new=new.replace("enabled=Wq190CrosswordHintInput.canSubmit(",
"enabled=view.wordIndex !in view.solvedIndices && Wq190CrosswordHintInput.canSubmit(",1)
s=s[:a]+new+s[b:]
once('''    var keyboardWasShown by remember(game.id) { mutableStateOf(false) }
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) {
            keyboardWasShown = true
        } else if (keyboardWasShown) {
            // Android Back, Done, and a completed answer all restore overview.
            inlineTyping = false
            expanded = false
            keyboardWasShown = false
        }
    }''',
'''    fun endTypingMode() {
        inlineTyping = false
        expanded = false
        keyboardWasShown = false
        typingFocusManager.clearFocus(force = true)
        softKeyboard?.hide()
    }
    LaunchedEffect(imeVisible) {
        if (imeVisible && !view.complete && !outOfTime && !timedOut) {
            keyboardWasShown = true
            inlineTyping = true
        } else if (!imeVisible && keyboardWasShown) {
            // Native IME Back/hide ends the session. A short transient inset
            // change during composition must not reset the board to overview.
            delay(180L)
            endTypingMode()
        }
    }
    androidx.activity.compose.BackHandler(enabled = inlineTyping || imeVisible) {
        endTypingMode()
    }''',"native dismissal returns overview")
# Existing rendering and dimensions are explicitly compared to the exact
# assembled v112 before writing any change.
assert s[s.index("private fun GameHeading("):]==original[original.index("private fun GameHeading("):]
def between(src,a,b):
    x=src.index(a); return src[x:src.index(b,x)]
assert between(s,"val fittedBoardHeight","            Column(")==between(original,"val fittedBoardHeight","            Column(")
assert s.count("inlineEditor()")==original.count("inlineEditor()")==2
assert s.count("BoardFrame(")==original.count("BoardFrame(")
assert "keyboardActions=KeyboardActions(onDone={ submitInlineWord() })" in s
assert "Button(onClick={ submitInlineWord() }" in s
p.write_text(s,"utf-8")
TEST.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/"tools/v213/Wq213TypingContinuity.kt",PKG/"Wq213TypingContinuity.kt")
shutil.copyfile(ROOT/"tools/v213/Wq213TypingContinuityTest.kt",TEST/"Wq213TypingContinuityTest.kt")
print("PASS: submit/IME continuity only; complete rendering helpers and board dimensions byte-identical")
