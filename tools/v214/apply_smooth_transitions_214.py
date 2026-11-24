#!/usr/bin/env python3
"""Two transition fixes. Do not change puzzle geometry or New Game artwork."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
def once(s,a,b):
    assert s.count(a)==1,(a[:160],s.count(a))
    return s.replace(a,b,1)
p=PKG/"PuzzleScreen.kt"
old=p.read_text("utf-8")
s=once(old,"    fun endTypingMode() {","    fun endTypingMode(requestImeHide: Boolean = true) {")
s=once(s,'''        typingFocusManager.clearFocus(force = true)
        softKeyboard?.hide()
    }
    LaunchedEffect(imeVisible)''','''        typingFocusManager.clearFocus(force = true)
        if (requestImeHide) softKeyboard?.hide()
    }
    LaunchedEffect(imeVisible)''')
s=once(s,'''            // Native IME Back/hide ends the session. A short transient inset
            // change during composition must not reset the board to overview.
            delay(180L)
            endTypingMode()''','''            // The native IME has already closed. Restore overview immediately
            // instead of holding the typing layout over an empty viewport.
            endTypingMode(requestImeHide = false)''')
assert old[old.index("private fun GameHeading("):]==s[s.index("private fun GameHeading("):]
assert "Wq213TypingContinuity.advanceSolved(engine, submittedIndex)" in s
assert "val keyboardVisible = Wq213TypingContinuity.layoutActive" in s

home=PKG/"HomeScreen.kt"
h=home.read_text("utf-8")
h=once(h,'''    val appContext = LocalContext.current
    LaunchedEffect(session.profileKey)''','''    val appContext = LocalContext.current
    // Warm the immutable local vocabulary while the user is on the home page.
    val newGameWarmScope = rememberCoroutineScope()
    var newGameWarmBank by remember(session) { mutableStateOf<VocabularyBank?>(null) }
    var newGameWarmJob by remember(session) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var newGameOpenJob by remember(session) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(session) {
        newGameWarmJob = newGameWarmScope.launch {
            try {
                newGameWarmBank = session.vocabulary()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The explicit open action can retry a failed warmup.
            }
        }
    }
    LaunchedEffect(session.profileKey)''')
h=once(h,'''        topicState.removeState("new-game")
        go(Page.NewGame)''','''        topicState.removeState("new-game")
        newGameOpenJob?.cancel()
        if (newGameWarmBank != null) {
            go(Page.NewGame)
        } else {
            val fromPage = page
            val fromProfile = session.profileKey
            newGameOpenJob = newGameWarmScope.launch {
                newGameWarmJob?.join()
                if (page != fromPage || session.profileKey != fromProfile) return@launch
                if (newGameWarmBank == null) {
                    try {
                        newGameWarmBank = session.vocabulary()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Preserve the existing New Game retry/error path.
                    }
                }
                if (page == fromPage && session.profileKey == fromProfile) go(Page.NewGame)
            }
        }''')
h=once(h,"NewGameScreen(session, newGameLevel, newGameTopic, newGameKind, ::back, ::open)",
"NewGameScreen(session, newGameLevel, newGameTopic, newGameKind, ::back, ::open, initialBank = newGameWarmBank)")
n=PKG/"NewGameScreen.kt"
ns=n.read_text("utf-8")
ns=once(ns,'''    initialKind: String?, back: () -> Unit, open: (Game) -> Unit,
)''','''    initialKind: String?, back: () -> Unit, open: (Game) -> Unit,
    initialBank: VocabularyBank? = null,
)''')
ns=once(ns,"    var bank by remember(session) { mutableStateOf<VocabularyBank?>(null) }",
"    var bank by remember(session, initialBank) { mutableStateOf(initialBank) }")
ns=once(ns,'''    LaunchedEffect(session) {
        try {
            bank = session.vocabulary()''','''    LaunchedEffect(session, initialBank) {
        if (bank != null) return@LaunchedEffect
        try {
            bank = session.vocabulary()''')
assert "Wq198NewGamePage(" in ns
assert ns[ns.index("    val yds ="):]==n.read_text("utf-8")[n.read_text("utf-8").index("    val yds ="):]
for path,content in ((p,s),(home,h),(n,ns)):
    path.write_text(content,"utf-8")
print("PASS: no artificial IME dismissal delay; warm vocabulary before New Game navigation; board and New Game form unchanged")
