#!/usr/bin/env python3
"""Apply approved 1.0.75 incremental screens AFTER the tested 1.0.74 overlay.

This script deliberately does not modify Lena, its saved keys, audio/US-UK,
accounts, the app ID, Arena match engines, backups or update signing.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
ASSETS=ROOT/"tools/v175"

def once(data, before, after, label):
    n=data.count(before)
    if n!=1: raise AssertionError(f"{label}: expected one anchor but got {n}")
    return data.replace(before,after,1)

def patch(path,func):
    target=SRC/path
    s=target.read_text("utf-8")
    target.write_text(func(s),"utf-8")

def learn_store(s):
    anchor="    @Synchronized\n    fun exportArchive(): LearningArchive {"
    block='''    /** Insert a user-approved card without pretending it was a wrong answer.
     * Existing cards, review events and schedules remain untouched.
     */
    fun addPersonalCard(englishRaw: String, meaningRaw: String, noteRaw: String): Boolean {
        val english = cleanAnswer(englishRaw).uppercase(java.util.Locale.ROOT)
        val meaning = meaningRaw.trim()
        if (!isEnglishAnswer(english) || meaning.length !in 2..125 ||
            !meaning.any(Char::isLetter)) return false
        val entry = Entry(english, meaning, noteRaw.trim().take(160))
        val key = learningWordKey(entry)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val existing = loadCard(db, key)
            if (existing != null) {
                db.setTransactionSuccessful()
                false
            } else {
                writeCard(db, LearningCard(key, english, meaning, entry.context,
                    dueAt = System.currentTimeMillis()))
                db.setTransactionSuccessful()
                true
            }
        } finally { db.endTransaction() }
    }

'''
    return once(s,anchor,block+anchor,"personal word storage")

def app_session(s):
    anchor="    internal fun learningCard(key: String) = learningRevision.let { learning.card(key) }"
    block='''    internal fun addManualLearning(english: String, meaning: String, note: String): Boolean {
        return runCatching { learning.addPersonalCard(english, meaning, note) }
            .onSuccess { if (it) learningRevision++; learningError = null }
            .onFailure { learningError = "Kelime eklenemedi. Depolama alanını kontrol et." }
            .getOrDefault(false)
    }
'''
    return once(s,anchor,anchor+"\n"+block,"session personal add")

def memory_home(s):
    anchor='    var robotPicker by remember { mutableStateOf(false) }'
    s=once(s,anchor,anchor+'\n    var wq175AddingWords by remember { mutableStateOf(false) }',
        "memory add dialog state")
    anchor="    if (robotPicker) {"
    s=once(s,anchor,"    if (wq175AddingWords) {\n        Wq175WordImportDialog(session, ai) { wq175AddingWords = false }\n    }\n"+anchor,
        "memory add dialog")
    anchor='''                Wq174MemoryHero(
                    selected=selectedAvatar,
                    onPick={robotPicker=true},
                    isCompact=isCompact
                )
'''
    btn='''                OutlinedButton(
                    onClick = { wq175AddingWords = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF2EDFF),
                        contentColor = Color(0xFF56339A))
                ) {
                    Text("＋  Kelime Ekle · Elle / PDF / Görsel / Kamera / AI",
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
'''
    return once(s,anchor,anchor+btn,"memory add button after hero")

def memory_cards(s):
    s=once(s,'animationSpec = tween(durationMillis = 520)',
        'animationSpec = tween(durationMillis = 420)',
        "quicker smooth card rotation")
    s=once(s,'Modifier.fillMaxWidth().heightIn(min = 355.dp)',
        'Modifier.fillMaxWidth().heightIn(min = 245.dp)',
        "responsive card height")
    s=once(s,'Box(Modifier.fillMaxWidth().height(190.dp)',
        'Box(Modifier.fillMaxWidth().height(132.dp)',
        "reduce unused image height")
    s=once(s,'if (correct && !difficult) ReviewRating.Good else ReviewRating.Again',
        'if (correct && !difficult) ReviewRating.Good else if (correct) ReviewRating.Hard else ReviewRating.Again',
        "hard card rating")
    s=once(s,'Triple("◉\\\\nOrta", true, true)',
        'Triple("◉\\\\nTekrar", false, true)',
        "repeat choice")
    s=once(s,'Triple("✓\\\\nBiliyorum", true, false)',
        'Triple("✓\\\\nÖğrendim", true, false)',
        "learned choice")
    s=once(s,'Button(onClick = { next() }, modifier = Modifier.weight(1f),',
        '''Button(onClick = {
                if (studyMode == "card" && !reviewed) record(true)
                next()
            }, modifier = Modifier.weight(1f),''',
        "new meaningful next action")
    s=once(s,'if (reviewed) "Sonraki ›" else "Atla ›"',
        'if (reviewed) "Sonraki ›" else if (studyMode == "card") "Öğrendim ✓" else "Tekrar Çalış"',
        "remove Skip from card navigation")
    assert '"Atla ›"' not in s
    return s

def new_game(s):
    anchor='    var loading by remember { mutableStateOf(false) }'
    s=once(s,anchor,
        '''    var wq175Timed by rememberSaveable { mutableStateOf(false) }
    var wq175Minutes by rememberSaveable { mutableIntStateOf(5) }
    val wq175Context = androidx.compose.ui.platform.LocalContext.current
'''+anchor,"timer choices state")
    anchor='        BodyText("Kelime sayısı")'
    s=once(s,anchor,
        '''        Wq175GameModeSelector(wq175Timed, { wq175Timed = it },
            wq175Minutes, { wq175Minutes = it })
'''+anchor,"mode selector")
    anchor='''                    if (game != null) open(game)
'''
    s=once(s,anchor,'''                    if (game != null) {
                        if (wq175Timed) Wq175TimedGames.start(
                            wq175Context, game.progressId, wq175Minutes)
                        else Wq175TimedGames.clear(wq175Context, game.progressId)
                        open(game)
                    }
''',"persist game timer before play")
    return s

def puzzle(s):
    anchor='''    var finished by remember { mutableStateOf(false) }'''
    s=once(s,anchor,anchor+'''
    val wq175Round = remember(game.progressId) {
        Wq175TimedGames.read(context, game.progressId)
    }
    var wq175Remaining by remember(game.progressId) {
        mutableLongStateOf(wq175Round?.let { Wq175TimedGames.remainingMs(it) } ?: 0L)
    }
    var wq175Expired by remember(game.progressId) {
        mutableStateOf(wq175Round != null && wq175Remaining <= 0L)
    }
    LaunchedEffect(game.progressId, wq175Round) {
        if (wq175Round != null) {
            while (!wq175Expired && !finished) {
                wq175Remaining = Wq175TimedGames.remainingMs(wq175Round)
                if (wq175Remaining <= 0L) {
                    wq175Expired = true
                    finished = true
                    keyboard.closeKeyboard()
                    break
                }
                delay(250L)
            }
        }
    }
''',"puzzle real-time tick and expiry")
    s=once(s,'        if (shuffling) return','        if (shuffling || wq175Expired) return',
        "prevent input on expiry")
    s=once(s,'    fun checkAndLearn() {\n',
        '    fun checkAndLearn() {\n        if (wq175Expired) return\n',
        "no check after timer expiry")
    s=once(s,'    fun selectWord(index: Int) {\n',
        '    fun selectWord(index: Int) {\n        if (wq175Expired) return\n',
        "no selection after timer expiry")
    s=once(s,'    fun shuffleRound() {\n',
        '    fun shuffleRound() {\n        if (wq175Expired) return\n',
        "no reset during expiry")
    s=once(s,
        '                if (!keyboardVisible) GameHeading(game, view, ::leave, ::shuffleRound, shuffling)',
        '''                if (!keyboardVisible) GameHeading(game, view, ::leave, ::shuffleRound, shuffling)
                if (wq175Round != null) Wq175CountdownBadge(wq175Remaining)
''',"live timer banner")
    s=once(s,
        '''            title = { Text("Bulmacayı tamamladın!", fontFamily = Serif, color = Ink) },''',
        '''            title = { Text(if (wq175Expired) "Süre doldu!" else "Bulmacayı tamamladın!",
                fontFamily = Serif, color = Ink) },''',
        "result screen timed label")
    s=once(s,
        '''            confirmButton = { TextButton(onClick = { finished = false }) { Text("Tamam", fontFamily = Sans) } },''',
        '''            confirmButton = { TextButton(onClick = {
                finished = false
                if (wq175Expired) leave()
            }) { Text(if (wq175Expired) "Sonucu Kapat" else "Tamam", fontFamily = Sans) } },''',
        "timed result close and return")
    return s

def arena_home(s):
    anchor='    var solo by rememberSaveable { mutableStateOf(soloInitially) }'
    s=once(s,anchor,anchor+'\n    var wq175SettingsOpen by rememberSaveable { mutableStateOf(false) }',
        "two-step arena state")
    anchor='''        Wq174ArenaHeroAndModes(
            selectedAvatar=mascot,mode=mode,solo=solo,onMode=onMode
        )
'''
    replacement='''        if (!wq175SettingsOpen) {
            Wq174ArenaHeroAndModes(
                selectedAvatar=mascot,mode=mode,solo=solo,
                onMode={ selected -> onMode(selected); wq175SettingsOpen = true }
            )
        } else {
            TextButton(onClick = { wq175SettingsOpen = false }) {
                Text("‹  Maceranı Seç", color = Color(0xFF5E2EB0),
                    fontWeight = FontWeight.ExtraBold)
            }
'''
    s=once(s,anchor,replacement,"only show arena match settings after selecting mode")
    anchor='''        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)
    }
    if(avatarPicker){'''
    s=once(s,anchor,'''        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)
        }
    }
    if(avatarPicker){''',"close settings step")
    assert s.count("Wq174UnifiedMatchSettings(")==1
    return s

def main():
    gradle=APP/"build.gradle.kts"
    settings=gradle.read_text("utf-8")
    assert settings.count("versionCode = 100")==1
    assert settings.count('versionName = "1.0.74-premium-arena-memory"')==1
    assert 'applicationIdSuffix = ".fresh"' in settings
    for name in ("WordQuest175Timer.kt","WordQuest175WordImport.kt"):
        shutil.copyfile(ASSETS/name,SRC/name)
    patch("LearningStore.kt",learn_store)
    patch("AppSession.kt",app_session)
    patch("VocabularyNeonHome.kt",memory_home)
    patch("VocabularyNeonPractice.kt",memory_cards)
    patch("NewGameScreen.kt",new_game)
    patch("PuzzleScreen.kt",puzzle)
    patch("ArenaApprovedHome.kt",arena_home)
    settings=settings.replace("versionCode = 100","versionCode = 101",1)
    settings=settings.replace('versionName = "1.0.74-premium-arena-memory"',
        'versionName = "1.0.75-learning-experience-preview"',1)
    gradle.write_text(settings,"utf-8")
    print("1.0.75 code101: two-step Arena, real timer, user-approved import, smaller flip cards.")
    print("1.0.74 already shipped app package, signer, user DB and Lena code unchanged.")

if __name__=="__main__":
    main()
