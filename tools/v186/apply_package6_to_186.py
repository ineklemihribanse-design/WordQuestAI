#!/usr/bin/env python3
"""Package 6 overlay after fully assembled Package 4 + Package 5.

No signing, release, data removal, avatar overwrite, or automatic Firebase
deployment. Each source edit is checked before a single byte is changed.
"""
from pathlib import Path
import json
import shutil

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
P6 = ROOT / "tools/v186/package6"
DB = ROOT / "source/database.rules.json"
PENDING = {}

def load(name):
    path = APP / name
    if not path.exists():
        raise AssertionError("Missing fully assembled Android source: " + name)
    return PENDING.get(path, path.read_text("utf-8"))

def save(name, contents):
    PENDING[APP / name] = contents

def once(contents, old, new, label):
    found = contents.count(old)
    if found != 1:
        raise AssertionError(f"P6 {label}: expected exactly one anchor, got {found}")
    return contents.replace(old, new, 1)

def navigation():
    s=load("HomeScreen.kt")
    if "navigationTrail" not in s:
        s=once(s,
            '    var page by rememberSaveable { mutableStateOf(Page.Home.name) }',
            '    var page by rememberSaveable { mutableStateOf(Page.Home.name) }\n'
            '    var navigationTrail by rememberSaveable { mutableStateOf(listOf<String>()) }\n'
            '    var confirmLeaveWork by rememberSaveable { mutableStateOf(false) }',
            "save parent navigation trail")
        s=once(s, '    fun go(target: Page) { page = target.name }',
            '''    fun go(target: Page) {
        if (target.name != page) {
            navigationTrail = (navigationTrail + page).takeLast(32)
            page = target.name
        }
    }''',
            "navigate to actual previous route")
        start=s.index("    fun back() {")
        finish=s.index("    fun packages(",start)
        s=s[:start]+'''    fun back() {
        val current = Page.valueOf(page)
        val previous = navigationTrail.lastOrNull() ?: Page.Home.name
        if (current == Page.Game) {
            session.closeGame()
            gameId = null
        }
        val destination = when(current) {
            Page.Game -> origin
            Page.Packages -> packageOrigin
            Page.Topic -> Page.Packages.name
            Page.NewGame -> newGameOrigin
            Page.Editor -> editorOrigin
            Page.VerbGame -> Page.Verbs.name
            Page.Backup -> Page.Custom.name
            Page.Home -> Page.Home.name
            else -> previous
        }
        if (current != Page.Home) {
            navigationTrail = navigationTrail.dropLast(1)
            page = destination.takeIf { candidate ->
                Page.entries.any { it.name == candidate }
            } ?: Page.Home.name
        }
    }

    fun requestBack() {
        if (page in setOf(Page.Game.name, Page.Editor.name, Page.VerbGame.name)) {
            confirmLeaveWork = true
        } else back()
    }

'''+s[finish:]
        s=once(s,
            '    BackHandler(page != Page.Home.name) { back() }',
            '''    BackHandler(page != Page.Home.name) { requestBack() }
    if(confirmLeaveWork) AlertDialog(
        onDismissRequest={confirmLeaveWork=false},
        title={Text("Çalışmadan çıkılsın mı?")},
        text={Text("Çalışmayı bırakıp önceki ekrana dönmek istiyor musun?")},
        confirmButton={TextButton(onClick={confirmLeaveWork=false;back()}){Text("Çık")}},
        dismissButton={TextButton(onClick={confirmLeaveWork=false}){Text("Devam et")}}
    )''',
            "system Android back confirmation")
        s=once(s,"existing, ::back, ai.hasKey",
            "existing, ::requestBack, ai.hasKey","editor back")
        s=once(s,
            "PuzzleScreen(game, crossword, round.onBoard(saved), ::back,",
            "PuzzleScreen(game, crossword, round.onBoard(saved), ::requestBack,",
            "crossword back")
        s=once(s,
            "session.verbs, VerbPool.valueOf(verbPool), VerbExercise.valueOf(verbMode), verbSeed, ::back,",
            "session.verbs, VerbPool.valueOf(verbPool), VerbExercise.valueOf(verbMode), verbSeed, ::requestBack,",
            "verbs back")
    assert "confirmLeaveWork" in s and "navigationTrail" in s
    save("HomeScreen.kt",s)

def direct_arena():
    home=load("Wq175PremiumHome.kt")
    home=once(home,
        '{ activeSection="ARENA" }',
        'arena',
        "single-tap Arena category, no intermediate home page")
    save("Wq175PremiumHome.kt",home)

    arena=load("ArenaApprovedHome.kt")
    old='''        if (!matchSettingsPage) {
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
            Wq174UnifiedMatchSettings('''
    new='''        // Mode choice and match settings belong to a single Arena destination.
        Wq175ArenaHeroAndModes(
            selectedAvatar=mascot,mode=mode,solo=solo,onMode=onMode
        )
        Wq174UnifiedMatchSettings('''
    arena=once(arena,old,new,"Arena no intermediate mode/settings page")
    arena=once(arena,
        '        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)\\n        }\\n    }'.replace('\\n','\n'),
        '        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)\\n    }'.replace('\\n','\n'),
        "remove old two-page UI closing brace")
    save("ArenaApprovedHome.kt",arena)

def compact_lobby():
    s = load("WordQuest175Arena.kt")
    s = once(s, ".heightIn(min=98.dp)", ".heightIn(min=76.dp)", "native mode-card height")
    s = once(s, "Box(Modifier.size(67.dp)", "Box(Modifier.size(54.dp)", "mode illustration")
    s = once(s, ".padding(horizontal=15.dp,vertical=12.dp)",
             ".padding(horizontal=13.dp,vertical=8.dp)", "mode card padding")
    s = once(s, "val heroHeight = if (customMascot) {\n"
                "                if (compact) 382.dp else if (wideTablet) 438.dp else 405.dp\n"
                "            } else if(compact)310.dp else if(wideTablet)378.dp else 342.dp",
             "val heroHeight = if (customMascot) {\n"
             "                if (compact) 273.dp else if (wideTablet) 348.dp else 316.dp\n"
             "            } else if(compact)250.dp else if(wideTablet)326.dp else 287.dp",
             "preserve Arena artwork while fitting mobile viewports")
    s = once(s,
             "if (compact) 212.dp else if (wideTablet) 288.dp else 262.dp",
             "if (compact) 171.dp else if (wideTablet) 236.dp else 212.dp",
             "full-body mascot widths remain proportional")
    s = once(s,
             "if (compact) 348.dp else if (wideTablet) 410.dp else 380.dp",
             "if (compact) 249.dp else if (wideTablet) 320.dp else 288.dp",
             "full-body mascot feet fit on podium")
    s = once(s,
             "Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {",
             "Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(7.dp)) {",
             "compress Arena selector without removing labels")
    s = once(s,'Text("Maceranı seç",fontSize=19.sp',
             'Text("Maceranı seç",fontSize=16.sp',"selector heading")
    save("WordQuest175Arena.kt",s)

def fair_online():
    s = load("PremiumOnlineScreen.kt")
    for _probe in ("Wq179ArenaWordChooser(", "setWordIndex(it)",
                   "Wq179ArenaSkip.consume(", "freeSkipUsed",
                   "Wq179ArenaOrder.next(", "Wq181ArenaHintBar("):
        print("P6 online source probe:",_probe,s.count(_probe))
    # The next clue is automatic and deferred incorrect/unknown clues return last.
    s=once(s,
        "wordIndex=Wq179ArenaOrder.next(m.code,myUid,m.size,m.solvedBy.keys,-1)",
        "wordIndex=Wq186ArenaNext.choose(m,myUid,-1)",
        "random opening word")
    # Package 1 already installed the stricter server-authorized next-word
    # selector. Keep it when combining P1 and P6; older P6-only builds
    # continue to use the original branch below.
    original_next="wordIndex = Wq179ArenaOrder.next(match.code,myUid,match.size,match.solvedBy.keys,wordIndex)"
    if original_next in s:
        s=once(s,original_next,
            "wordIndex = Wq186ArenaNext.choose(match,myUid,wordIndex)",
            "automatic next word with final-round deferred failures")
    else:
        assert "wordIndex = Wq186ArenaNextWord.next(" in s, (
            "P6 unexpected word selector after Package 1")

    s = once(s,
             "    Wq183OnlinePortraitStrip(\n",
             "    if (match.phase == \"finished\") Wq183OnlinePortraitStrip(\n",
             "no duplicate green player banner during live game")
    s = once(s,
             "    BackHandler(match != null) { client.leave() }",
             """    var confirmRoomExit by rememberSaveable { mutableStateOf(false) }
    BackHandler(match != null) { confirmRoomExit = true }
    if(confirmRoomExit) AlertDialog(
        onDismissRequest={confirmRoomExit=false},
        title={Text("Arena maçından ayrıl?")},
        text={Text("Odadan çıkarsan oyunun devam eder; yeniden katılmak için oda kodunu kullanabilirsin.")},
        confirmButton={TextButton(onClick={
            confirmRoomExit=false
            client.leave()
        }) {Text("Odadan çık")}},
        dismissButton={TextButton(onClick={confirmRoomExit=false}) {Text("Oyuna dön")}}
    )""",
             "hardware back never exits Arena instantly")
    s = once(s,
             "if (match != null) client.leave() else back()",
             "if (match != null) confirmRoomExit = true else back()",
             "top bar uses same leave confirmation")
    s = once(s,
             "optionsOpen = false; client.leave()",
             "optionsOpen = false; confirmMatchLeave = true",
             "room menu uses local leave confirmation")
    s = once(s,
             '    var optionsOpen by remember(match.code) { mutableStateOf(false) }',
             '''    var optionsOpen by remember(match.code) { mutableStateOf(false) }
    var confirmMatchLeave by remember(match.code) { mutableStateOf(false) }
    if (confirmMatchLeave) AlertDialog(
        onDismissRequest={confirmMatchLeave=false},
        title={Text("Odadan çıkmak istiyor musun?")},
        text={Text("Yarışman odada devam eder. İnternetin geldiğinde kodla tekrar katılabilirsin.")},
        confirmButton={TextButton(onClick={confirmMatchLeave=false;client.leave()}) {
            Text("Odadan çık")
        }},
        dismissButton={TextButton(onClick={confirmMatchLeave=false}) {
            Text("Oyuna dön")
        }}
    )''',
             "match menu leave confirmation is self-contained")

    # UI scores are recomputed on identical deterministic board/quiz content
    # on BOTH devices; RTDB owns solvedBy/answers and mistake penalties.
    marker = '    val hostName = ArenaNickname.safeDisplay(match.hostName, "Kurucu")'
    score = """    fun arenaScore(player: String): Int {
        if (match.mode == "quiz") {
            return match.answers[player].orEmpty().entries.sumOf { (index, choice) ->
                val q=questions.getOrNull(index)
                when {
                    q == null || choice == -1 -> 0
                    choice == q.correct -> Wq186ArenaFairPlay.wordPoints(
                        q.options.getOrNull(q.correct)?.count(Char::isLetter) ?: 4,
                        match.level)
                    else -> -3
                }
            }
        }
        return Wq186ArenaFairPlay.totalScore(
            match.solvedBy,player,
            { index -> board?.puzzle?.words?.getOrNull(index)?.cells?.size ?: 4 },
            { _ -> match.level },match.mistakesBy
        )
    }
"""
    s = once(s,marker,score+marker,"score by level and letter count")
    # Limit rewrites to this match composable. The rest of the file uses the
    # pre-existing scorer for lobby-only information.
    s = once(s,"val myScore = match.score(myUid, questions)",
             "val myScore = arenaScore(myUid)","current player weighted score")
    s = once(s,
             "val otherScore = match.score(PremiumOnlineLimits.otherPlayer(match, myUid), questions)",
             "val otherScore = arenaScore(PremiumOnlineLimits.otherPlayer(match, myUid))",
             "opponent weighted score")
    for player in ("match.hostUid","match.guestUid"):
        s = s.replace("match.score("+player+", questions)",
                      "arenaScore("+player+")")
    # The above scorer cannot be used before board/questions were loaded;
    # all matching score calls are inside the live match composable.
    if "arenaScore(match.hostUid)" not in s or "arenaScore(match.guestUid)" not in s:
        raise AssertionError("P6 host/guest scorer was not wired")
    s = once(s,
             "    // Gameplay stays compact. The room code, sharing and full settings are under ⋮.",
             """    Wq186ArenaSocialHud(
        session=session,match=match,myUid=myUid,isHost=isHost,
        hostName=hostName,guestName=guestName,
        hostScore=arenaScore(match.hostUid),guestScore=arenaScore(match.guestUid)
    )
    // Gameplay stays compact. The room code, sharing and full settings are under ⋮.""",
             "emojis profiles connection and consensus live for both players")
    # Prevent 'All Words' dialog from bypassing the designed skip budget.

    # Disable the existing chooser only; never remove the selected-clue header.
    needle="Wq179ArenaWordChooser("
    if s.count(needle)!=1:
        raise AssertionError("P6 word chooser: expected exactly one call")
    start_call=s.index(needle)
    check=s[start_call:start_call+130]
    if not all(key in check for key in ("puzzle","solved","selected")):
        raise AssertionError("P6 word chooser: unknown signature")
    begin=s.rfind("\n",0,start_call)+1
    indent=s[begin:start_call]
    if indent.strip():
        raise AssertionError("P6 chooser: unexpected line prefix")
    end_call=s.find("\n"+indent+"}",start_call)
    if end_call<0:
        raise AssertionError("P6 word chooser: no terminating lambda")
    s=s[:begin]+indent+"// Clue selection is automatic in live competition.\n"+s[end_call+len(indent)+2:]
    s = once(s,
             """                    setWordIndex(it)
                    setAnswerText("")
                }""",
             """                    // Live puzzle tiles are read-only; only the active clue accepts text.
                }""",
             "disable selecting an arbitrary puzzle cell")
    import re
    grid_skip=r"Wq179ArenaOrder\.next\(\s*match\.code,myUid,puzzle\.words\.size,solved,selected\)"
    s,n=re.subn(grid_skip,"Wq186ArenaNext.choose(match,myUid,selected)",s)
    if n!=1:
        assert n==0 and "setWordIndex(nextPlayable)" in s, (
            f"P6 automatic skip word selector: {n} matches")
    s = once(s,
             """    var freeSkipUsed by rememberSaveable(match.code,myUid) {
        mutableStateOf(Wq179ArenaSkip.used(context,match.code,myUid))
    }""",
             """    val freeSkips = Wq186ArenaSkipNetwork.count(session,match.code,myUid)""",
             "two retained skip rights")
    s = once(s,"if(!freeSkipUsed && canAnswer &&","if(freeSkips < 2 && canAnswer &&",
             "allow two automatic word changes")
    s = once(s,'"1 ÜCRETSİZ KELİME GEÇ"',
             '"KELİMEYİ GEÇ · ${2-freeSkips} HAK"',"dynamic skip label")
    s = once(s,
             """                                    Wq179ArenaSkip.consume(context,match.code,myUid)
                                    freeSkipUsed=true""",
             """                                    Wq186ArenaSkipNetwork.consume(session,match.code,myUid){ ok ->
                                        if(ok) {
                                            setWordIndex(Wq186ArenaNext.choose(match,myUid,selected))
                                            setAnswerText("")
                                        } else client.note("Kelime geçişi kaydedilemedi.")
                                    }""",
             "consume a skip atomically")
    # The actual word change happens only after Firebase transaction commits.
    # Package 1 computed nextPlayable from the authoritative match snapshot.
    # Preserve that server-authorized selection after the skip transaction:
    # the Package 6 generic chooser alone can return a deferred clue.
    if "setWordIndex(nextPlayable)" in s:
        s=once(s,
            "setWordIndex(Wq186ArenaNext.choose(match,myUid,selected))",
            "setWordIndex(nextPlayable)",
            "P1 validated skip target survives P6 transaction")
    stale='''                                    setWordIndex(Wq186ArenaNext.choose(match,myUid,selected))
                                    setAnswerText("")
                                },Modifier.fillMaxWidth())'''
    if stale not in s:
        # P1 reuses a validated nextPlayable instead of the legacy selector.
        # Do not duplicate that client-side action after Firebase confirms it.
        p1_stale='''                                    setWordIndex(nextPlayable)
                                    setAnswerText("")
                                },Modifier.fillMaxWidth())'''
        if p1_stale not in s:
            raise AssertionError("P6 skip callback source differs")
        stale=p1_stale
    s=once(s,stale,
        '''                                },Modifier.fillMaxWidth())''',
        "remove old unguarded client-side skip")
    save("PremiumOnlineScreen.kt",s)

def skip_storage():
    s=load("Wq179ArenaExtras.kt")
    start=s.index("internal object Wq179ArenaSkip {")
    end=s.index("\n}\n\n@Composable",start)+2
    old=s[start:end]
    if "getBoolean(key(room,uid),false)" not in old:
        raise AssertionError("P6 skip baseline drift")
    new="""internal object Wq179ArenaSkip {
    private fun key(room:String,uid:String)="p6-skip-count-"+room+"-"+uid
    private fun store(ctx:Context)=ctx.getSharedPreferences(
        "wordquest_arena_skips",Context.MODE_PRIVATE)
    fun count(ctx:Context,room:String,uid:String):Int =
        store(ctx).getInt(key(room,uid),0).coerceIn(0,2)
    fun used(ctx:Context,room:String,uid:String)=count(ctx,room,uid)>=2
    fun consume(ctx:Context,room:String,uid:String):Int {
        val next=(count(ctx,room,uid)+1).coerceAtMost(2)
        store(ctx).edit().putInt(key(room,uid),next).commit()
        return next
    }
}"""
    s=s[:start]+new+s[end:]
    save("Wq179ArenaExtras.kt",s)

def hint_area():
    s=load("Wq181ArenaHintBar.kt")
    s=once(s,"modifier=Modifier.fillMaxWidth()){",
            "modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){",
            "compact native hint action")
    s=once(s,".padding(horizontal=12.dp,vertical=9.dp)",
            ".padding(horizontal=9.dp,vertical=3.dp)",
            "hint card reduced paddings")
    s=once(s,'Text("İPUCU ($left)"','Text("💡 $left"',
            "compact hint button")
    save("Wq181ArenaHintBar.kt",s)

def crossword_typing():
    s=load("PuzzleScreen.kt")
    needle='    val word = board.words[view.wordIndex]'
    s=once(s,needle,
        needle+''' 
    // P6 + UX patch: never interrupt the game with an answer dialog.
    // The editable word is always in the visible clue panel.
    var inlineTyping by remember { mutableStateOf(false) }
    var wordDraft by remember(view.wordIndex) { mutableStateOf("") }
    val entryFocus = remember { FocusRequester() }
    val softKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(inlineTyping, view.wordIndex, outOfTime) {
        if(inlineTyping && !outOfTime &&
            view.wordIndex !in view.solvedIndices) {
            entryFocus.requestFocus()
            softKeyboard?.show()
        }
    }''', "inline word-entry state and focus")
    s=once(s,"import androidx.compose.ui.draw.clip",
        '''import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions''',
        "inline field keyboard/focus imports")
    # Preserve the native AndroidView's other callbacks and the existing
    # answer-check/persistence pipeline. Only replace openings of its IME.
    assert s.count("keyboard.openKeyboard()") >= 3
    s=s.replace("keyboard.openKeyboard()","inlineTyping = true")
    marker='    Box(\n        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),'
    inline='''    fun submitInlineWord() {
        if(outOfTime || view.wordIndex in view.solvedIndices ||
            englishLetters(wordDraft).length != word.cells.size) return
        edit(typedWord=true) { state -> state.enter(wordDraft) }
        checkAndLearn()
        wordDraft=""
        inlineTyping=false
        softKeyboard?.hide()
    }

    val inlineEditor: @Composable () -> Unit = {
        if (!outOfTime && view.wordIndex !in view.solvedIndices) {
            Surface(
                modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(16.dp),
                color=Color(0xFFF7F1FD),
                border=BorderStroke(1.dp, Color(0xFFDCCFEB))
            ) {
                Column(Modifier.fillMaxWidth().padding(11.dp),
                    verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        verticalAlignment=Alignment.CenterVertically) {
                        Text("İngilizce cevabı yaz", color=Ink,
                            fontFamily=Sans,fontSize=14.sp,
                            fontWeight=FontWeight.SemiBold,
                            modifier=Modifier.weight(1f))
                        Text(word.cells.size.toString()+" harf",
                            color=Forest,fontSize=12.sp)
                    }
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        OutlinedTextField(
                            value=wordDraft,
                            onValueChange={ wordDraft=englishLetters(it).take(word.cells.size) },
                            modifier=Modifier.weight(1f).focusRequester(entryFocus),
                            placeholder={Text("Cevabı buraya yaz")},
                            singleLine=true,
                            shape=RoundedCornerShape(12.dp),
                            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),
                            keyboardActions=KeyboardActions(onDone={ submitInlineWord() })
                        )
                        Button(onClick={submitInlineWord},
                            enabled=englishLetters(wordDraft).length==word.cells.size,
                            modifier=Modifier.heightIn(min=52.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Forest),
                            shape=RoundedCornerShape(12.dp)) {
                            Text("Gönder",color=Color.White)
                        }
                    }
                }
            }
        }
    }

'''
    s=once(s,marker,inline+marker,"inline answer panel inside puzzle screen")
    s=once(s,
       '''                            if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)''',
       '''                            inlineEditor()
                            if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)''',
       "tablet word input beneath active clue")
    s=once(s,
       '''                        WordFields(word, view) { engine.selectLetter(it); view = engine.view(); inlineTyping = true }''',
       '''                        inlineEditor()''',
       "phone keyboard-visible answer stays in clue instead of modal")
    s=once(s,
       '''                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)''',
       '''                    if (!keyboardVisible) inlineEditor()
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)''',
       "phone normal answer input underneath board")
    assert 'if(typedDialog' not in s and 'title={Text("Kelimeyi yaz")}' not in s
    assert s.count("inlineEditor()")==3
    assert s.count("focusRequester(entryFocus)")==1
    save("PuzzleScreen.kt",s)

def reconnect():
    s=load("PremiumOnlineRoom.kt")
    s=once(s,
             """                status = "Oda bağlantısı reddedildi: ${error.message}"
                match = null""",
             """                status = "Bağlantı kesildi: ${error.message}. Oda saklandı; internet geldiğinde yeniden katılabilirsin."
                // Do not discard a live match or award a loss for a temporary network outage.""",
             "retain last game after Firebase listener interruption")
    save("PremiumOnlineRoom.kt",s)

def firebase_rules():
    current=json.loads(DB.read_text("utf-8"))
    approved=json.loads((ROOT/"firebase/database.rules.json").read_text("utf-8"))
    base=current["rules"]["premiumRooms"]["$code"]
    incoming=approved["rules"]["premiumRooms"]["$code"]["package6"]
    if "package6" in base:
        raise AssertionError("P6 already installed or unexpected Firebase rule drift")
    core={"emoji","finishVotes","presence","skips"}
    assert core.issubset(incoming) and "settings" in incoming
    # The rest of the P6 rules are installed after the source overlay.
    base["package6"]={key:incoming[key] for key in sorted(core)}
    PENDING[DB]=json.dumps(current,ensure_ascii=False,indent=2)+"\n"

def main():
    assert (APP/"Wq186MatchingImporter.kt").exists(),"Package 5 must be assembled first"
    assert all((P6 / x).exists() for x in (
        "Wq186ArenaFairPlay.kt","Wq186ArenaSocialHud.kt",
        "Wq186ArenaSkipNetwork.kt","Wq186ArenaFairPlayTest.kt"))
    navigation()
    direct_arena()
    compact_lobby()
    fair_online()
    skip_storage()
    hint_area()
    crossword_typing()
    reconnect()
    firebase_rules()
    home=load("HomeScreen.kt")
    assert "confirmLeaveWork" in home and "navigationTrail" in home, (
        "Package 6 Android back staging did not merge into the full Android source")
    gradle=ROOT/"source/app/build.gradle.kts"
    build=gradle.read_text("utf-8")
    if 'versionName = "1.0.85"' not in build or "versionCode = 111" not in build:
        raise AssertionError("P6 must follow 1.0.85 + assembled Package 4 and Package 5")
    assert 'applicationIdSuffix = ".fresh"' in build
    build=once(build,'versionName = "1.0.85"',
               'versionName = "1.0.86"',"P6 working version name")
    PENDING[gradle]=once(build,'versionCode = 111',
                      'versionCode = 112',"P6 versionCode and installed app identity")

    # All anchors and additive Firebase rules verified before mutating sources.
    for path,value in PENDING.items():
        path.write_text(value,"utf-8")
        print("PASS Package 6",path.relative_to(ROOT))
    for name in ("Wq186ArenaFairPlay.kt","Wq186ArenaSocialHud.kt",
                 "Wq186ArenaSkipNetwork.kt"):
        shutil.copyfile(P6/name,APP/name)
    tests=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
    tests.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(P6/"Wq186ArenaFairPlayTest.kt",tests/"Wq186ArenaFairPlayTest.kt")
    print("Source staged only; APK/Firebase deployment are separate, guarded operations.")

if __name__=="__main__":
    main()
