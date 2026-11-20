#!/usr/bin/env python3
"""WordQuest 1.0.75 overlay applied after the FULL v1.0.74 pipeline.

Stage one: split Arena mode selection/settings; retain match callbacks,
correct Word Memory card rating and improve flip; add profile-local words
without altering the existing schema or records. No premature release.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
PKG=ROOT/"tools/v175"

def once(s,needle,replacement,label):
    found=s.count(needle)
    if found!=1:
        raise AssertionError(f"{label}: expected one match, got {found}")
    return s.replace(needle,replacement,1)

def arena():
    shutil.copyfile(PKG/"WordQuest175Arena.kt",SRC/"WordQuest175Arena.kt")
    file=SRC/"ArenaApprovedHome.kt"
    s=file.read_text("utf-8")
    s=once(s,'    var joinOpen by rememberSaveable { mutableStateOf(false) }',
        '    var matchSettingsPage by rememberSaveable { mutableStateOf(false) }\n'
        '    var joinOpen by rememberSaveable { mutableStateOf(false) }',
        "arena navigation state")
    s=once(s,'''        Wq174ArenaHeroAndModes(
            selectedAvatar=mascot,mode=mode,solo=solo,onMode=onMode
        )
        Wq174UnifiedMatchSettings(''',
        '''        if (!matchSettingsPage) {
            Wq175ArenaHeroAndModes(
                selectedAvatar=mascot,mode=mode,solo=solo,
                onMode={ nextMode ->
                    onMode(nextMode)
                    matchSettingsPage=true
                }
            )
        } else {
            TextButton(onClick={matchSettingsPage=false},
                modifier=Modifier.align(Alignment.Start)) {
                Text("‹  Maceranı Seç ekranına dön",
                    color=Color(0xFF6534A9),
                    fontWeight=FontWeight.ExtraBold)
            }
            Text("Seçilen mod: " + when(mode) {
                "team" -> "Ortak Bulmaca"
                "quiz" -> "5 Şıklı Düello"
                else -> "Bulmaca Düellosu"
            },color=ArenaApproved.ink,fontWeight=FontWeight.SemiBold)
            Wq174UnifiedMatchSettings(''',"Arena distinct page boundary")
    s=once(s,
        '        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)\n    }',
        '        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)\n'
        '        }\n    }',"close Arena settings page")
    assert s.count('Wq174UnifiedMatchSettings(')==1
    assert s.count('Wq175ArenaHeroAndModes(')==1
    assert s.count('Wq2DCharacterPicker(')==1
    file.write_text(s,"utf-8")

def lesson():
    file=SRC/"VocabularyNeonPractice.kt"
    s=file.read_text("utf-8")
    s=once(s,'import androidx.compose.animation.core.tween',
        'import androidx.compose.animation.core.tween\n'
        'import androidx.compose.animation.core.FastOutSlowInEasing',"easing import")
    s=once(s,'animationSpec = tween(durationMillis = 520), label = "card-turn"',
        'animationSpec = tween(durationMillis = 390, easing = FastOutSlowInEasing), label = "card-turn"',
        "smooth flip")
    s=once(s,'.heightIn(min = 355.dp)',
        '.heightIn(min = 245.dp)',"card minimum height")
    s=once(s,'    Box(Modifier.fillMaxWidth().height(190.dp)',
        '    Box(Modifier.fillMaxWidth().height(132.dp)',
        "AI concept illustration height")
    s=once(s,r'''                    listOf(
                        Triple("✕\nBilmiyorum", false, true),
                        Triple("◔\nZor", false, true),
                        Triple("◉\nOrta", true, true),
                        Triple("✓\nBiliyorum", true, false)
                    )''','''                    listOf(
                        Triple("Bilmiyorum", false, true),
                        Triple("Zorlandım", false, true),
                        Triple("Tekrar", false, true),
                        Triple("Öğrendim", true, false)
                    )''',"clear learning rating buttons")
    s=once(s,'''            Button(onClick = { next() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MemoryCyan,
                    contentColor = MemoryNavy)) {
                Text(if (position == deck.lastIndex) "Bitir" else
                    if (reviewed) "Sonraki ›" else "Atla ›")
            }''',
        '''            Button(onClick = { next() }, modifier = Modifier.weight(1f),
                enabled = reviewed,
                colors = ButtonDefaults.buttonColors(containerColor = MemoryCyan,
                    contentColor = MemoryNavy)) {
                Text(if (position == deck.lastIndex) "Bitir" else "Sonraki ›")
            }''',"remove accidental skip grading")
    assert '"Atla' not in s
    file.write_text(s,"utf-8")

def add_words():
    shutil.copyfile(PKG/"WordQuest175ManualWords.kt",SRC/"WordQuest175ManualWords.kt")
    store=SRC/"LearningStore.kt"
    s=store.read_text("utf-8")
    at='    /** Atomically updates the card only if the unique event was inserted. */'
    method='''    /** Persist a user's own word directly in the EXISTING per-profile FSRS notebook.
     * No migration, no reset of review events, no replacement of existing cards.
     */
    @Synchronized
    fun addManual(entry: Entry, now: Long = System.currentTimeMillis()): Boolean {
        val key=learningWordKey(entry)
        val db=writableDatabase
        db.beginTransaction()
        try {
            if(loadCard(db,key)!=null) return false
            writeCard(db,LearningCard(
                key=key, answer=entry.answer, clue=entry.clue,
                context=entry.context, dueAt=now))
            db.setTransactionSuccessful()
            return true
        }finally { db.endTransaction() }
    }

'''
    s=once(s,at,method+at,"persist manual words")
    store.write_text(s,"utf-8")
    session=SRC/"AppSession.kt"
    s=session.read_text("utf-8")
    at='    /** Short, bounded evidence summary; derived only from the currently active player'
    insert='''    /** Add personal vocabulary to the same learning database as Arena/crosswords.
     * Reject duplicates and malformed words without losing any old study history.
     */
    internal fun addManualLearning(answer:String,clue:String,example:String):Boolean {
        val english=answer.trim().replace(Regex("\\\\s+")," ")
        val turkish=clue.trim().replace(Regex("\\\\s+")," ")
        if(english.length !in 2..75 || turkish.length !in 2..140 ||
            !isEnglishAnswer(english)) {
            learningError="Geçerli İngilizce kelime ve Türkçe anlam yaz."
            return false
        }
        return runCatching {
            learning.addManual(Entry(english,turkish,example.trim().take(300)))
        }.onSuccess {
            if(it){learningRevision++;learningError=null}
            else learningError="Bu kelime zaten defterinde bulunuyor."
        }.onFailure {
            learningError="Kelime kaydedilemedi: depolama alanını kontrol et."
        }.getOrDefault(false)
    }

'''
    s=once(s,at,insert+at,"save to real FSRS notebook")
    session.write_text(s,"utf-8")
    home=SRC/"VocabularyNeonHome.kt"
    s=home.read_text("utf-8")
    s=once(s,'''    var robotPicker by remember { mutableStateOf(false) }''',
        '''    var robotPicker by remember { mutableStateOf(false) }
    var addWordOpen by rememberSaveable(session.profileKey) { mutableStateOf(false) }''',
        "word editor state")
    marker='    Box(Modifier.fillMaxSize().background('
    s=once(s,marker,'''    if(addWordOpen) Wq175AddWordDialog(
        onDismiss={addWordOpen=false},
        add={answer,clue,example->session.addManualLearning(answer,clue,example)},
        error={session.learningError}
    )
'''+marker,"manual editor dialog")
    s=once(s,'''                Wq174MemoryStats(total=total,due=due,learned=learned)''',
        '''                Wq175AddWordEntry(onClick={addWordOpen=true})
                Wq174MemoryStats(total=total,due=due,learned=learned)''',
        "show personal add word button")
    home.write_text(s,"utf-8")

def main():
    gradle=APP/"build.gradle.kts"
    s=gradle.read_text("utf-8")
    assert s.count('versionCode = 100')==1
    assert s.count('versionName = "1.0.74-premium-arena-memory"')==1
    assert 'applicationIdSuffix = ".fresh"' in s
    assert 'applicationIdSuffix = ".fresh.qa"' not in s
    arena()
    lesson()
    add_words()
    # 'offline' inherits the debug build type in the legacy pipeline.
    # A user-upgrade APK must NOT be debuggable, even though it is APK-signed.
    s=once(s,'applicationIdSuffix = ".fresh"',
        'applicationIdSuffix = ".fresh"\n            isDebuggable = false',
        "production-safe offline build type")
    s=once(s,'versionCode = 100','versionCode = 101',"version code")
    s=once(s,'versionName = "1.0.74-premium-arena-memory"',
        'versionName = "1.0.75-learning-upgrade-candidate"',"version name")
    gradle.write_text(s,"utf-8")
    print("WordQuest 1.0.75 code 101: separate Arena settings and 2D study improvements.")
    print("Manual Word Memory entries in existing FSRS database; 1.0.74 user data intact.")
    print("Unsigned candidate; do not publish production until remaining scope is tested.")

if __name__=="__main__": main()
