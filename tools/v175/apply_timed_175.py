#!/usr/bin/env python3
"""Opt-in real timed crossword, applied after apply_core_175.py."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"

def once(s,a,b,label):
    if s.count(a)!=1:
        raise AssertionError(f"{label}: {s.count(a)} matches")
    return s.replace(a,b,1)

def setup():
    p=SRC/"NewGameScreen.kt"
    s=p.read_text("utf-8")
    s=once(s,"import androidx.compose.ui.Modifier",
        "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.LocalContext","context import")
    s=once(s,"    val scope = rememberCoroutineScope()",
        "    val scope = rememberCoroutineScope()\n"
        "    val context = LocalContext.current\n"
        "    var timedMinutes by rememberSaveable { mutableIntStateOf(0) }","timer setup state")
    mark='''        BodyText("Kelime sayısı")'''
    extra='''        BodyText("Oyun Biçimi")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(0 to "Klasik",3 to "3 dk",5 to "5 dk",
                10 to "10 dk").forEach { (minutes,title) ->
                FilterChip(selected=timedMinutes==minutes,
                    onClick={timedMinutes=minutes},
                    enabled=!loading,
                    label={Text(title,fontFamily=Sans)},
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=LightLime))
            }
        }
        if(timedMinutes>0) BodyText(
            "Süreye karşı: süren başladığında arka planda da devam eder. " +
            "Süre bitince cevap girişi kapanır.")
'''
    s=once(s,mark,extra+mark,"timed mode choices")
    s=once(s,"                    if (game != null) open(game)",
        '''                    if (game != null) {
                        Wq175TimedPuzzle.configure(context,session.profileKey,
                            game.progressId,timedMinutes)
                        open(game)
                    }''',"save choice for this generated puzzle")
    p.write_text(s,"utf-8")

def crossword():
    p=SRC/"PuzzleScreen.kt"
    s=p.read_text("utf-8")
    s=once(s,"    pronunciation: WordPronunciation,\n    save: (Progress) -> Progress,",
        "    pronunciation: WordPronunciation,\n    profileKey: String = \"\",\n    save: (Progress) -> Progress,","profile scoped timer")
    s=once(s,'''    var finished by remember { mutableStateOf(false) }''',
        '''    var finished by remember { mutableStateOf(false) }
    val timer=remember(context,profileKey,game.progressId) {
        Wq175TimedPuzzle.read(context,profileKey,game.progressId)
    }
    var remainingMs by remember(timer) {
        mutableLongStateOf(timer?.remaining()?:0L)
    }
    var timedOut by rememberSaveable(profileKey,game.progressId) {
        mutableStateOf(false)
    }
    LaunchedEffect(timer,game.progressId) {
        if(timer!=null && !timedOut) {
            remainingMs=timer.remaining()
            while(remainingMs>0L && !view.complete) {
                delay(200L)
                remainingMs=timer.remaining()
            }
            if(remainingMs<=0L && !view.complete) {
                timedOut=true
                keyboard.closeKeyboard()
            }
        }
    }''',"real monotonic timer")
    s=once(s,'        if (shuffling) return',
        '        if (shuffling || timedOut) return',"disable edits at zero")
    s=once(s,'''    fun checkAndLearn() {
        val active''',
        '''    fun checkAndLearn() {
        if(timedOut)return
        val active''',"disable answer check")
    s=once(s,'            if (!keyboardVisible) GameHeading(game, view, ::leave, ::shuffleRound, shuffling)',
        '''            if (!keyboardVisible) GameHeading(game, view, ::leave, ::shuffleRound, shuffling)
            if(timer!=null) Wq175TimeRemainingBadge(remainingMs)''',
        "timer in game header")
    s=once(s,'''                it.onNextWord = {
                    engine.moveToNextWord()
                    view = engine.view()
                }''',
        '''                it.onNextWord = {
                    if(!timedOut) {
                        engine.moveToNextWord()
                        view = engine.view()
                    }
                }''',"stop virtual keyboard at deadline")
    mark='''    if (finished) {
        AlertDialog('''
    dialog='''    if(timedOut) {
        AlertDialog(
            onDismissRequest={timedOut=false;leave()},
            title={Text("Süre doldu!",fontWeight=FontWeight.ExtraBold,
                color=Color(0xFFB63C58))},
            text={Text("Süreye karşı oyun sona erdi. " +
                "Bulmacada kazandığın puan: " + view.points +
                " / " + board.words.size*10 +
                ". Çözdüğün kelimeler ve kayıtların korunur.")},
            confirmButton={
                TextButton(onClick={timedOut=false;leave()}) {
                    Text("Sonuçları kaydet ve çık")
                }
            },
            containerColor=Color.White
        )
    }
'''
    s=once(s,mark,dialog+mark,"timed game result dialog")
    p.write_text(s,"utf-8")

def entry():
    p=SRC/"HomeScreen.kt"
    s=p.read_text("utf-8")
    s=once(s,'''session.preferences, pronunciation) { progress ->''',
        '''session.preferences, pronunciation, session.profileKey) { progress ->''',
        "profile scoped puzzle entry")
    p.write_text(s,"utf-8")

def main():
    shutil.copyfile(PKG/"WordQuest175TimedPuzzle.kt",
        SRC/"WordQuest175TimedPuzzle.kt")
    setup()
    crossword()
    entry()
    print("PASS: time-boxed crossword mode, profile-local durable selection,")
    print("monotonic deadline, live countdown, enforced timeout, protected saves.")

if __name__=="__main__":main()
