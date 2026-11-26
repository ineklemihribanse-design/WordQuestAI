#!/usr/bin/env python3
"""Fix recorded solo Arena regressions without altering ordinary puzzle or network logic."""
from pathlib import Path
import hashlib,shutil,re
main=Path("source/app/src/main")
p=main/"java/com/kelimebulmacasi/a1"
pack=Path(__file__).resolve().parent
names={"PremiumArenaScreen.kt","ArenaBoardV2.kt"}
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in names}
def once(s,a,b):
    assert s.count(a)==1,(a[:150],s.count(a))
    return s.replace(a,b,1)
f=p/"PremiumArenaScreen.kt";original=f.read_text()
start=original.index("private fun PremiumArenaOfflineScreen(")
end=original.index("@Composable\ninternal fun ArenaBanner(",start)
s=original[start:end]
s=once(s,'    var quizCount by rememberSaveable { mutableIntStateOf(10) }',
'''    var quizCount by rememberSaveable { mutableIntStateOf(10) }
    var activeRoundLevels by remember { mutableStateOf(listOf("A1")) }
    var activeRoundTopic by remember { mutableStateOf(PremiumArenaRules.allTopics) }
    val arenaConfig=androidx.compose.ui.platform.LocalConfiguration.current
    val arenaWide=arenaConfig.screenWidthDp>=650 && arenaConfig.screenWidthDp>arenaConfig.screenHeightDp''')
s=once(s,'        val requestId=roundId','        val requestId=roundId\n        val requestStage=stage\n        val requestTopic=topic')
s=once(s,'context,requestMode,chosenLevels,topic,requestSize','context,requestMode,chosenLevels,requestTopic,requestSize')
s=once(s,'if(roundId!=requestId||stage!=PremiumStage.Lobby)return@launch',
'''if(roundId!=requestId||stage!=requestStage)return@launch
                activeRoundLevels=chosenLevels.toList()
                activeRoundTopic=requestTopic''')
s=once(s,'                    quiz=result.questions','                    quiz=result.questions\n                    quizCount=quiz.size')
s=once(s,'    fun returnToLobby() { stage = PremiumStage.Lobby; feedback = "" }',
'''    fun returnToLobby() {
        roundId=java.util.UUID.randomUUID().toString()
        stage=PremiumStage.Lobby
        feedback=""
    }''')
a=s.index('                if (selectedMode == "quiz") ArenaPanelBlock {',s.index('PremiumStage.Lobby -> {'))
b=s.index('                ArenaApprovedHome(',a)
assert 'Text("SORU SAYISI"' in s[a:b] and 'quizCount=n' in s[a:b]
s=s[:a]+s[b:]
s=once(s,'                    sizes=packs.availableSizes(level,topic),size=size,onSize={size=it},',
'''                    sizes=if(selectedMode=="quiz")listOf(5,10,15,20) else PremiumArenaRules.allowedSizes,
                    size=if(selectedMode=="quiz")quizCount else size,
                    onSize={if(selectedMode=="quiz")quizCount=it else size=it},''')
s=once(s,'                        size=packs.availableSizes(it).firstOrNull()?:20',
'                        if(size !in PremiumArenaRules.allowedSizes)size=20')
s=once(s,'onTopic={topic=it;size=packs.availableSizes(level,it).firstOrNull()?:20}','onTopic={topic=it}')
# Replace only the exact banner prefixes confirmed in the assembled-source audit.
s=once(s,'ArenaBanner("${currentBoard.level} · ${currentBoard.category}", "Kelime Bulmacası",',
'ArenaBanner("${activeRoundLevels.joinToString(" + ")} · $activeRoundTopic", if(soloArenaMode=="team")"Ortak Bulmaca · Tek Kişilik" else "Kelime Bulmacası",')
s=once(s,'ArenaBanner("${level} · ${topic}", "5 Şıklı Kelime Düellosu", "$quizCount soruluk alıştırma · A / B / C / D / E")',
'ArenaBanner("${activeRoundLevels.joinToString(" + ")} · $activeRoundTopic", "5 Şıklı Kelime Düellosu", "${quiz.size} soruluk alıştırma · A / B / C / D / E")')
a=s.index('                    ArenaBanner(',s.index('PremiumStage.Crossword -> {'))
b=s.index('                    if(!keyboardIsOpen) {',a)
heading=s[a:b]
bs=s.index('                    ArenaCompactCrosswordBoard(',b)
clue=s[b:bs]
cs=s.index('                    ArenaPanelBlock {',bs)
board=s[bs:cs]
ce=s.index('                }\n            }\n            PremiumStage.FiveChoices -> {',cs)
controls=s[cs:ce]
assert 'CEVABI GÖNDER' in controls and 'Harf ipucu' in controls and 'PAS GEÇ' in controls
h1,h2=heading.strip().splitlines()
assert 'ArenaBanner(' in h1 and 'ArenaScoreStrip(' in h2
h1=h1.strip();h2=h2.strip()
clue=once(clue,'                        Wq179ArenaClueHeader(puzzle,currentWord)',
'                        if(!arenaWide)Wq140SoloClueHeader(puzzle,currentWord)')
controls=once(controls,'                        Spacer(Modifier.height(22.dp))',
'                        if(!arenaWide)Spacer(Modifier.height(10.dp))')
controls=once(controls,'horizontalArrangement = Arrangement.spacedBy(18.dp)',
'horizontalArrangement = Arrangement.spacedBy(if(arenaWide)6.dp else 12.dp)')
replacement='''                    if(arenaWide) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1.25f)){'''+h1+'''}
                        Column(Modifier.weight(1f)){'''+h2+'''}
                    } else {
                        '''+h1+'''
                        '''+h2+'''
                    }
                    val arenaBoardContent: @Composable () -> Unit = {
'''+board+'''                    }
                    val arenaClueContent: @Composable () -> Unit = {
'''+clue+'''                    }
                    val arenaControlContent: @Composable () -> Unit = {
'''+controls+'''                    }
                    if(arenaWide) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),
                            verticalAlignment=Alignment.Top) {
                            Column(Modifier.weight(1.5f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                val availableBoardHeight=(arenaConfig.screenHeightDp-175f).coerceAtLeast(110f)
                                CompositionLocalProvider(LocalWq140ArenaViewport provides availableBoardHeight) {
                                    arenaBoardContent()
                                }
                            }
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                arenaClueContent()
                                arenaControlContent()
                            }
                        }
                    } else {
                        arenaClueContent()
                        arenaBoardContent()
                        arenaControlContent()
                    }
'''
s=s[:a]+replacement+s[ce:]
for old,new in {
    "ArenaBanner(":"Wq140SoloBanner(",
    "ArenaPanelBlock":"Wq140SoloPanel",
    "ArenaScoreStrip(":"Wq140SoloScore(",
    "ArenaGoldButton(":"Wq140SoloGoldButton(",
    "ArenaOutlineButton(":"Wq140SoloOutlineButton(",
    "Wq182QuizAnswer(":"Wq140SoloQuizAnswer(",
    "ArenaCream":"Wq140Cream","ArenaGold":"Wq140Gold",
    "ArenaTextSoft":"Wq140Soft","ArenaSuccess":"Wq140Success",
}.items(): s=s.replace(old,new)
s=once(s,'else Modifier.fillMaxSize().background(ArenaNight)',
'else Modifier.fillMaxSize().then(wq122SettingsModifier())')
s=once(s,'.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),\n        verticalArrangement = Arrangement.spacedBy(16.dp),',
'.verticalScroll(rememberScrollState()).padding(horizontal = if(arenaWide)10.dp else 14.dp, vertical = if(arenaWide)4.dp else 9.dp),\n        verticalArrangement = Arrangement.spacedBy(if(arenaWide)6.dp else 10.dp),')
s=once(s,'    val wq121FullLobby=stage==PremiumStage.Lobby',
'''    val wq121FullLobby=stage==PremiumStage.Lobby
    LaunchedEffect(stage,feedback) {
        if(stage==PremiumStage.Lobby && feedback.isNotBlank())
            android.widget.Toast.makeText(context,feedback,android.widget.Toast.LENGTH_SHORT).show()
    }
    MaterialTheme(colorScheme=if(wq121FullLobby)MaterialTheme.colorScheme else Wq122Scheme) {''')
assert s.endswith("    }\n}\n\n"),s[-200:]
s=s[:-len("    }\n}\n\n")]+"    }\n    }\n}\n\n"
assert 'Text("SORU SAYISI"' not in s and 'size=if(selectedMode=="quiz")quizCount else size' in s
assert 'activeRoundLevels.joinToString(" + ")' in s
assert 'LocalWq140ArenaViewport provides availableBoardHeight' in s
f.write_text(original[:start]+s+original[end:])
f=p/"ArenaBoardV2.kt";s=f.read_text()
s=once(s,'        val gap=if(maxWidth<500.dp)1.dp else 1.5.dp',
'''        val fittedHeight=LocalWq140ArenaViewport.current
        val fittedOverview=fittedHeight!=null && !zoomed && !compact
        val gap=if(fittedOverview).8.dp else if(maxWidth<500.dp)1.dp else 1.5.dp''')
s=once(s,'        val cell=Wq186ArenaGridPolicy.cellDp(maxWidth.value,cols,zoomed).dp',
'''        val cell=if(fittedOverview)
            Wq140ArenaFit.cell(maxWidth.value,requireNotNull(fittedHeight),rows,cols,gap.value).dp
            else Wq186ArenaGridPolicy.cellDp(maxWidth.value,cols,zoomed).dp''')
s=once(s,'''        val viewport=Wq186ArenaGridPolicy.viewportDp(
            config.screenHeightDp.toFloat(),boardHeight.value,compact).dp''',
'''        val viewport=if(fittedHeight!=null && !compact)
            minOf(boardHeight,fittedHeight.dp)
            else Wq186ArenaGridPolicy.viewportDp(
                config.screenHeightDp.toFloat(),boardHeight.value,compact).dp''')
s=once(s,'.coerceIn(6f,9f).sp,','.coerceIn(if(fittedOverview)4f else 6f,9f).sp,')
s=once(s,'lineHeight=9.sp,','lineHeight=if(fittedOverview)(cell.value*.28f).coerceIn(5f,9f).sp else 9.sp,')
s=once(s,'.coerceIn(11f,21f).sp,','.coerceIn(if(fittedOverview)5f else 11f,21f).sp,')
s=once(s,'.coerceIn(13f,24f).sp,','.coerceIn(if(fittedOverview)6f else 13f,24f).sp,')
# The fitted landscape overview must not use a second pair of nested scroll
# containers. Give the actual cell and board accessibility bounds, not a
# synthetic test-only success signal. Preserve old zoom/IME/phone paths.
s=once(s,'import androidx.compose.ui.Modifier','import androidx.compose.ui.Modifier\nimport androidx.compose.ui.semantics.semantics\nimport androidx.compose.ui.semantics.contentDescription')
s=once(s,'        val cell=if(fittedOverview)','        val cell=if(fittedOverview)')
s=once(s,'Wq140ArenaFit.cell(maxWidth.value,requireNotNull(fittedHeight),rows,cols,gap.value).dp',
'Wq140ArenaFit.cell((maxWidth.value-4f).coerceAtLeast(1f),(requireNotNull(fittedHeight)-4f).coerceAtLeast(1f),rows,cols,gap.value).dp')
s=once(s,'            if(selectedCols.isNotEmpty() && selectedRows.isNotEmpty()){',
'            if(!fittedOverview && selectedCols.isNotEmpty() && selectedRows.isNotEmpty()){')
s=once(s,'''            .horizontalScroll(horizontal)
            .verticalScroll(vertical)){''',
'''            .then(if(fittedOverview)Modifier else Modifier.horizontalScroll(horizontal).verticalScroll(vertical))){
'''.rstrip())
s=once(s,'Column(Modifier.padding(start=leftInset).width(boardWidth)',
'''Column(Modifier.padding(start=leftInset).width(boardWidth)''')
s=once(s,'                            Box(Modifier.size(cell)',
'''                            val cellNumber=puzzle.numbers[address]
                            Box(Modifier.size(cell)
                                .then(if(fittedOverview && usable) Modifier.semantics {
                                    contentDescription=if(cellNumber!=null) "Tahta hücresi "+cellNumber
                                        else "Tahta harfi "+(row+1)+"/"+(col+1)
                                } else Modifier)''')
f.write_text(s)
shutil.copyfile(pack/"Wq140SoloUi.kt",p/"Wq140SoloUi.kt")
test=main.parent/"test/java/com/kelimebulmacasi/a1"
shutil.copyfile(pack/"Wq140ArenaFitTest.kt",test/"Wq140ArenaFitTest.kt")
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in protected.items())
print("PASS: solo Arena dark gameplay, complete selected levels, single question count, two-column landscape layout; phone and online grid policy preserved")


# First-click Arena launch. Only state and callbacks; no visual/board/rules edits.
first_start_names={"PremiumArenaScreen.kt","PremiumOnlineScreen.kt"}
first_start_protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in first_start_names}
f=p/"PremiumArenaScreen.kt";s=f.read_text()
s=once(s,'''private enum class PremiumStage { Lobby, Crossword, FiveChoices, Result }''','''private enum class PremiumStage { Lobby, Crossword, FiveChoices, Result }

internal data class WqArenaSoloLaunch(
    val id:String, val mode:String, val level:String, val topic:String,
    val size:Int, val seconds:Int, val preset:Wq186MatchPreset, val round:Wq132WebEngine.Round
)''')
s=once(s,'''    var offline by rememberSaveable { mutableStateOf(false) }
    if (offline) PremiumArenaOfflineScreen(session, { offline = false }, { offline = false },
        openLearning, openTeacher)
    else PremiumOnlineScreen(session, back, account, { offline = true }, openLearning, openTeacher)''','''    var soloLaunch by remember { mutableStateOf<WqArenaSoloLaunch?>(null) }
    val launch=soloLaunch
    if (launch!=null) key(launch.id) {
        PremiumArenaOfflineScreen(session, { soloLaunch=null }, { soloLaunch=null },
            openLearning, openTeacher, launch)
    } else PremiumOnlineScreen(session, back, account, { soloLaunch=it }, openLearning, openTeacher)''')
s=once(s,'''                                      openLearning: () -> Unit, openTeacher: (String) -> Unit) {''','''                                      openLearning: () -> Unit, openTeacher: (String) -> Unit,
                                      launch: WqArenaSoloLaunch) {''')
for old,new in [
('mutableStateOf(java.util.UUID.randomUUID().toString())','mutableStateOf(launch.id)'),
('mutableStateOf(PremiumStage.Lobby)','mutableStateOf(if(launch.mode=="quiz")PremiumStage.FiveChoices else PremiumStage.Crossword)'),
('var level by rememberSaveable { mutableStateOf("A1") }','var level by rememberSaveable { mutableStateOf(launch.level) }'),
('var topic by rememberSaveable { mutableStateOf(PremiumArenaRules.allTopics) }','var topic by rememberSaveable { mutableStateOf(launch.topic) }'),
('var size by rememberSaveable { mutableIntStateOf(30) }','var size by rememberSaveable { mutableIntStateOf(launch.size) }'),
('var quizCount by rememberSaveable { mutableIntStateOf(10) }','var quizCount by rememberSaveable { mutableIntStateOf(if(launch.mode=="quiz")launch.round.questions.size else 10) }'),
('mutableStateOf(listOf("A1"))','mutableStateOf(launch.preset.levels.toList())'),
('var activeRoundTopic by remember { mutableStateOf(PremiumArenaRules.allTopics) }','var activeRoundTopic by remember { mutableStateOf(launch.topic) }'),
('var timeSeconds by rememberSaveable { mutableIntStateOf(30) }','var timeSeconds by rememberSaveable { mutableIntStateOf(launch.seconds) }'),
('var selectedMode by rememberSaveable { mutableStateOf("crossword") }','var selectedMode by rememberSaveable { mutableStateOf(if(launch.mode=="quiz")"quiz" else "crossword") }'),
('var soloArenaMode by rememberSaveable { mutableStateOf("duel") }','var soloArenaMode by rememberSaveable { mutableStateOf(launch.mode) }'),
('mutableStateOf<PremiumArenaBoard?>(null)','mutableStateOf<PremiumArenaBoard?>(launch.round.board)'),
('var currentWord by remember { mutableIntStateOf(0) }','var currentWord by remember { mutableIntStateOf(launch.round.board?.let { Wq179ArenaOrder.next(launch.id,session.profileKey,it.puzzle.words.size,emptySet(),-1) } ?: 0) }'),
('mutableStateOf<List<PremiumQuestion>>(emptyList())','mutableStateOf<List<PremiumQuestion>>(launch.round.questions)'),
('var wq122HintBudget by remember { mutableIntStateOf(0) }','var wq122HintBudget by remember { mutableIntStateOf(launch.preset.hints) }'),
('wq122HintBudget=Wq186ArenaPresetStore.read(context,session.profileKey,level).hints','wq122HintBudget=launch.preset.hints'),
('val preset = Wq186ArenaPresetStore.read(context,session.profileKey,level)','val preset = launch.preset'),
('        stage=PremiumStage.Lobby\n        feedback=""','        feedback=""\n        back()'),
]: s=once(s,old,new)
f.write_text(s)
f=p/"PremiumOnlineScreen.kt";s=f.read_text()
s=once(s,'account: () -> Unit, offline: () -> Unit,','account: () -> Unit, offline: (WqArenaSoloLaunch) -> Unit,')
s=once(s,'                onOffline = offline,','''                onOffline = {
                    if(!creatingWebRoom && !client.busy && client.match==null) {
                        val preset=Wq186ArenaPresetStore.read(context,session.profileKey,level)
                        val requestedMode=selectedMode;val requestedTopic=topic
                        val requestedLevel=level
                        val requestedSize=if(requestedMode=="quiz"){
                            val allowed=listOf(5,10,12,15,20).filter{it>=preset.levels.size}
                            allowed.minByOrNull{kotlin.math.abs(it-size)}?:10
                        }else size
                        val requestedSeconds=seconds
                        val id=java.util.UUID.randomUUID().toString()
                        creatingWebRoom=true
                        client.note("Oyun hazırlanıyor…")
                        webRoomScope.launch {
                            try {
                                val generated=Wq132WebEngine.generate(context,requestedMode,preset.levels,requestedTopic,
                                    requestedSize,requestedSeconds,preset.hints,preset.retries,id.hashCode())
                                if(requestedMode=="quiz")require(generated.questions.isNotEmpty())
                                else require(generated.board!=null)
                                if(client.match==null)offline(WqArenaSoloLaunch(id,requestedMode,requestedLevel,requestedTopic,
                                    requestedSize,requestedSeconds,preset,generated))
                            } catch(e:kotlinx.coroutines.CancellationException){throw e}
                            catch(e:Exception){client.note("Oyun hazırlanamadı: "+(e.message?:"Başka ayar seç.").take(180))}
                            finally {creatingWebRoom=false}
                        }
                    }
                },''')
f.write_text(s)
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in first_start_protected.items())
print("PASS: a single settings route starts actual solo gameplay or creates the online room; all visual assets and other files unchanged")
