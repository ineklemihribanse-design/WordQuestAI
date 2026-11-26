#!/usr/bin/env python3
"""Live Arena only. Run after the verified v146 assembly and full-source fixes."""
from pathlib import Path
import hashlib
main=Path("source/app/src/main")
p=main/"java/com/kelimebulmacasi/a1"
names={"PremiumOnlineScreen.kt","ArenaReferenceComponents.kt","ArenaV2Visuals.kt","ArenaBoardV2.kt","ArenaCrosswordTyping.kt","Wq186ArenaSocialHud.kt"}
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in names}
out={}
def once(s,a,b):
    assert s.count(a)==1,(a[:150],s.count(a))
    return s.replace(a,b,1)
def read(n): return (p/n).read_text()

# Retain revealed letters for every word, not just the selected word.
s=read("ArenaBoardV2.kt")
s=once(s,"@Composable\ninternal fun ArenaBoardV2(","""internal val LocalWq148ArenaHints=compositionLocalOf<Map<Cell,Char>>{emptyMap()}

@Composable
internal fun ArenaBoardV2(""")
s=once(s,"    val chosen=puzzle.words.getOrNull(selected)","    val retainedHints=LocalWq148ArenaHints.current\n    val chosen=puzzle.words.getOrNull(selected)")
s=once(s,"val hinted=active && (","val hinted=address in retainedHints || (active && (")
s=once(s,"(hintPosition>=0 && hintLetters.containsKey(hintPosition))\n                            )","(hintPosition>=0 && hintLetters.containsKey(hintPosition))\n                            ))")
s=once(s,"val visibleLetter = fixed[address] ?:","val visibleLetter = fixed[address] ?: retainedHints[address] ?:")
out["ArenaBoardV2.kt"]=s

# Online-only fitted overview and a genuine full-screen board; offline callers unchanged.
s=read("ArenaCrosswordTyping.kt")
s=once(s,"import androidx.compose.ui.unit.dp","import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.window.Dialog\nimport androidx.compose.ui.window.DialogProperties\nimport androidx.compose.ui.platform.LocalConfiguration\nimport androidx.compose.material3.Surface\nimport androidx.compose.material3.TextButton")
s=once(s,"@Composable\ninternal fun ArenaCompactCrosswordBoard(","""internal val LocalWq148OnlineBoard=compositionLocalOf{false}

@Composable
internal fun ArenaCompactCrosswordBoard(""")
s=once(s,"    var zoomed by rememberSaveable(puzzle.hashCode()) { mutableStateOf(false) }","""    var zoomed by rememberSaveable(puzzle.hashCode()) { mutableStateOf(false) }
    val online=LocalWq148OnlineBoard.current
    var fullscreen by rememberSaveable(puzzle.hashCode()) { mutableStateOf(false) }
    val config=LocalConfiguration.current""")
s=once(s,"        OutlinedButton(onClick={zoomed=!zoomed},","""        Row(verticalAlignment=Alignment.CenterVertically) {
        if(online && !compact) TextButton(onClick={fullscreen=true},
            contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)){
            Text("Tam ekran",color=Color(0xFFFFE3A4),fontSize=12.sp)
        }
        OutlinedButton(onClick={zoomed=!zoomed},""")
s=once(s,"color=androidx.compose.ui.graphics.Color(0xFF34214F),fontSize=12.sp)\n        }\n    }\n    }","color=androidx.compose.ui.graphics.Color(0xFF34214F),fontSize=12.sp)\n        }\n        }\n    }\n    }")
s=once(s,"    if(LocalWq140BoundedBoard.current && !compact) {","""    if(online) {
        header()
        val height=(config.screenHeightDp*(if(config.screenWidthDp>=600) .44f else .48f)).coerceAtLeast(130f)
        CompositionLocalProvider(LocalWq140ArenaViewport provides if(!compact && !zoomed)height else null) {
            ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,hintLetters,onSelect)
        }
        if(fullscreen) Dialog(onDismissRequest={fullscreen=false},
            properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            Surface(Modifier.fillMaxSize(),color=Color(0xFF21143F)) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("TÜM BULMACA",Modifier.weight(1f),color=Color(0xFFFFE3A4),fontWeight=FontWeight.Bold)
                        TextButton(onClick={fullscreen=false}){Text("Kapat",color=Color.White)}
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                        CompositionLocalProvider(LocalWq140ArenaViewport provides maxHeight.value) {
                            ArenaBoardV2(puzzle,solved,selected,placed,false,false,hintIndex,hintChar,hintLetters,onSelect)
                        }
                    }
                }
            }
        }
    } else if(LocalWq140BoundedBoard.current && !compact) {""")
out["ArenaCrosswordTyping.kt"]=s

# Keep one hint network/budget for keyboard and non-keyboard.
s=read("PremiumOnlineScreen.kt")
s=once(s,"verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else 5.dp)","verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else if(match.phase==\"playing\") 3.dp else 5.dp)")
s=once(s,"                val myTurn = match.cooperative || match.turnUid == myUid","""                val retainedHints=remember(puzzle,claimedHints) {
                    buildMap<Cell,Char> {
                        claimedHints.forEach { claim ->
                            val w=puzzle.words.getOrNull(claim.word)
                            val cell=w?.cells?.getOrNull(claim.position)
                            val letter=w?.entry?.letters?.getOrNull(claim.position)
                            if(cell!=null && letter!=null)put(cell,letter)
                        }
                    }
                }
                val hintPattern=word.cells.mapIndexed { i,cell ->
                    retainedHints[cell] ?: fixedPattern.getOrElse(i){'.'}
                }.joinToString("")
                val hintBudget=Wq186ArenaFairPlay.hintsLeft(claimedHints.size,client.package6Rules.hints)
                var hintPending by remember(match.code){mutableStateOf(false)}
                val myTurn = match.cooperative || match.turnUid == myUid""")
a=s.index("                if(!keyboardIsOpen) {\n                    Wq179ArenaClueHeader")
b=s.index("                ArenaV2Panel {",a)
old=s[a:b]
assert "Wq186ArenaHintAction(" in old and "Wq181ArenaHintBar(" in old and "ArenaCompactCrosswordBoard(" in old
s=s[:a]+'''                val canHint=match.rulesVersion>=2 && myTurn &&
                    (countdown==null || countdown>0) && selected !in solved &&
                    hintBudget>0 && !hintPending && hintPattern.contains('.') &&
                    (match.cooperative || PremiumOnlineLimits.canAttempt(match,selected,myUid))
                fun requestHint() {
                    if(!canHint)return
                    hintPending=true
                    Wq186ArenaHintNetwork.reveal(session,match.code,myUid,selected,
                        hintPattern,client.package6Rules.hints) { issue ->
                        hintPending=false
                        if(issue!=null)client.note(issue)
                    }
                }
                if(!keyboardIsOpen) {
                    Wq179ArenaClueHeader(puzzle,selected)
                    if(!match.cooperative) Wq179ArenaReplayPanel(match,puzzle,myUid){chosen->
                        setWordIndex(chosen)
                        setAnswerText("")
                    }
                }
                CompositionLocalProvider(
                    LocalWq148ArenaHints provides retainedHints,
                    LocalWq148OnlineBoard provides true
                ) {
                    ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen,
                        answerText,hintIndex=revealedIndex,hintChar=revealedChar,hintLetters=hintLetters) {
                        // Keep the selected clue controlled by the existing online turn rules.
                    }
                }
'''+s[b:]
# Preserve the input node across the transient solved snapshot and opponent turns.
a=s.index('                    if (selected in solved) {',s.index('val canHint='))
b=s.index('                        if (match.cooperative || (match.rulesVersion >= 2 && canAnswer)',a)
part=s[a:b]
oldcan='                        val canAnswer=(myTurn && (countdown == null || countdown > 0) && (if(match.rulesVersion >= 2 && !match.cooperative) PremiumOnlineLimits.canAttempt(match,selected,myUid) else !alreadyUnknown)) && replayAllowed'
assert part.count(oldcan)==1
# Extract state/eligibility from the old "else" branch, then render the input once.
state_start=part.index("                        val alreadyUnknown")
state_end=part.index("                        if(match.rulesVersion >= 2 && !match.cooperative)",part.index(oldcan))
state=part[state_start:state_end]
# Messages stay in the status branch. Eligibility only is hoisted.
msg_start=state.index("                        if (alreadyUnknown)")
msg_end=state.index("                        val previouslyFailed",msg_start)
messages=state[msg_start:msg_end]
state=state[:msg_start]+state[msg_end:]
state=state.replace("val canAnswer=(myTurn","val canAnswer=selected !in solved && (myTurn")
solved=part[:state_start]
assert solved.endswith("                    } else {\n")
status=part[state_end:part.index("                        fun submitAnswer()")]
submit=part[part.index("                        fun submitAnswer()"):part.index("                        OutlinedTextField(")]
field='''                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            OutlinedTextField(answerText, { if(canAnswer)setAnswerText(englishLetters(it).take(word.entry.letters.length)) },
                                Modifier.weight(1f),label={Text(if(myTurn)"Kelimeyi yaz" else "Rakibin sırası")},
                                singleLine=true,
                                keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters,
                                    imeAction=ImeAction.Go),
                                keyboardActions=KeyboardActions(onGo={submitAnswer()}),
                                colors=premiumTextFieldColors())
                            if(match.rulesVersion>=2) Button(onClick={requestHint()},enabled=canHint,
                                shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=7.dp,vertical=3.dp)){
                                Text("Harf aç · $hintBudget",fontSize=11.sp)
                            }
                        }
                        if(hintLetters.isNotEmpty()) Text(hintLetters.toSortedMap().entries.joinToString(" · "){
                            "${it.key+1}. ${it.value}"
                        },color=OnlineGold,fontSize=11.sp)
                        ArenaGoldButton("✓  CEVABI GÖNDER", { submitAnswer() },
                            enabled = crosswordFullWordReady(word.entry.letters.length, answerText) && canAnswer)
                        if(selected !in solved) {
'''
# The retained opening brace replaces the original unsolved-only controls scope.
s=s[:a]+state+solved+messages+status+"                    }\n"+submit+field+s[b:]
out["PremiumOnlineScreen.kt"]=s

# Persistent mascot independent of disclosure state. Only the live scoreboard
# uses this private player-card function; all entry, lobby and settings are untouched.
s=read("ArenaReferenceComponents.kt")
s=once(s,"""        if(expanded) ArenaMascotV2(avatar,Modifier.size(
            if(tablet) { if(active)75.dp else 68.dp }
            else { if(active)51.dp else 45.dp }))""","""        ArenaMascotV2(avatar,Modifier.size(
            if(tablet)104.dp else 64.dp))""")
s=once(s,".heightIn(min=if(expanded) { if(tablet)90.dp else 78.dp } else 48.dp)",".heightIn(min=if(expanded) { if(tablet)116.dp else 86.dp } else if(tablet)104.dp else 64.dp)")
out["ArenaReferenceComponents.kt"]=s


# Move reaction/profile actions to the opposing card and reuse existing
# room-scoped Firebase messages. Do not modify security rules or permissions.
s=read("Wq186ArenaSocialHud.kt")
s=once(s,"    guestScore: Int\n) {","    guestScore: Int,\n    cardSocial: Wq148LiveSocial? = null\n) {")
s=once(s,'    if (match.phase == "playing" || match.phase == "lobby") {', '''    SideEffect {
        cardSocial?.let { card ->
            card.events=emoji
            card.send={ symbol ->
                if(myUid.isNotBlank() && match.phase=="playing" &&
                    Wq186ArenaFairPlay.validEmoji(symbol)) {
                    extra?.child("emoji")?.child(myUid)?.setValue(
                        mapOf("symbol" to symbol,"sentAt" to ServerValue.TIMESTAMP))
                        ?.addOnFailureListener { notice="Emoji gönderilemedi." }
                }
            }
            card.profile={profileOpen=opponentUid}
            card.finishLabel=if(myUid in votes)"Teklifi geri çek" else "Bitirme teklifi"
            card.finish={
                if(myUid in votes)extra?.child("finishVotes")?.child(myUid)?.removeValue()
                else askFinish=true
            }
        }
    }
    if (match.phase == "playing" || match.phase == "lobby") {''')
s=once(s,"        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,\n            horizontalArrangement=Arrangement.spacedBy(7.dp)) {","        if(cardSocial==null || match.phase!=\"playing\") Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,\n            horizontalArrangement=Arrangement.spacedBy(7.dp)) {")
out["Wq186ArenaSocialHud.kt"]=s

s=out["PremiumOnlineScreen.kt"]
s=once(s,'    if (match.phase != "lobby") {\n    Wq186ArenaSocialHud(','''    val cardSocial=remember(match.code,myUid,match.hostUid,match.guestUid) {
        Wq148LiveSocial(myUid,match.hostUid,match.guestUid)
    }
    if (match.phase != "lobby") {
    Wq186ArenaSocialHud(''')
s=once(s,'hostScore=arenaScore(match.hostUid),guestScore=arenaScore(match.guestUid)\n    )','hostScore=arenaScore(match.hostUid),guestScore=arenaScore(match.guestUid),\n        cardSocial=cardSocial\n    )')
s=once(s,'        Box {\n            TextButton(onClick = { optionsOpen = true }) {','''        if(match.phase=="playing") TextButton(onClick={cardSocial.finish()},
            contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)) {
            Text(cardSocial.finishLabel,color=Color(0xFFFFDF9B),fontSize=11.sp)
        }
        Box {
            TextButton(onClick = { optionsOpen = true }) {''')
s=once(s,'own.size,questions.size,true,match.hostAvatar,match.guestAvatar)','own.size,questions.size,true,match.hostAvatar,match.guestAvatar,cardSocial)')
s=once(s,'match.hostAvatar,match.guestAvatar)\n                if (!match.cooperative)','match.hostAvatar,match.guestAvatar,cardSocial)\n                if (!match.cooperative)')
out["PremiumOnlineScreen.kt"]=s

s=read("ArenaV2Visuals.kt")
s=once(s,'hostAvatar: String = "robot", guestAvatar: String = "fox"\n) {','hostAvatar: String = "robot", guestAvatar: String = "fox",\n    social: Wq148LiveSocial? = null\n) {')
s=once(s,'countdown,completed,total,cooperative,hostAvatar,guestAvatar)','countdown,completed,total,cooperative,hostAvatar,guestAvatar,social)')
out["ArenaV2Visuals.kt"]=s

s=out["ArenaReferenceComponents.kt"]
s=once(s,"import androidx.compose.ui.unit.dp","""import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt""")
s=once(s,"@Composable\nprivate fun Arena172ScorePlayer(","""internal class Wq148LiveSocial(val myUid:String,val hostUid:String,val guestUid:String) {
    var events by mutableStateOf<Map<String,Pair<String,Long>>>(emptyMap())
    var send:(String)->Unit={}
    var profile:()->Unit={}
    var finish:()->Unit={}
    var finishLabel by mutableStateOf("Bitirme teklifi")
}

@Composable
private fun Wq148EmojiFlight(event:Pair<String,Long>?,from:Offset?,to:Offset?,origin:Offset) {
    val progress=remember{Animatable(1f)}
    var displayed by remember {mutableStateOf("")}
    var last by remember {mutableLongStateOf(0L)}
    val density=LocalDensity.current
    LaunchedEffect(event) {
        val next=event ?: return@LaunchedEffect
        val age=System.currentTimeMillis()-next.second
        if(age !in -1500L..8000L || next.second<=last) return@LaunchedEffect
        // Firebase may replace a local server timestamp estimate. Avoid replaying
        // the same event twice, while still allowing repeated reactions.
        if(next.first==displayed && next.second-last<350L)return@LaunchedEffect
        last=next.second
        displayed=next.first
        progress.snapTo(0f)
        progress.animateTo(1f,tween(950,easing=FastOutSlowInEasing))
    }
    if(progress.value<1f && from!=null && to!=null && displayed.isNotBlank()) {
        val t=progress.value
        val arc=with(density){34.dp.toPx()}*kotlin.math.sin(Math.PI*t).toFloat()
        val center=with(density){16.dp.toPx()}
        Text(displayed,Modifier.offset{
            IntOffset((from.x+(to.x-from.x)*t-origin.x-center).roundToInt(),
                (from.y+(to.y-from.y)*t-origin.y-arc-center).roundToInt())
        },fontSize=28.sp)
    }
}

@Composable
private fun Arena172ScorePlayer(""")
s=once(s,'    name:String,points:Int,avatar:String,active:Boolean,modifier:Modifier\n) {','''    name:String,points:Int,avatar:String,active:Boolean,modifier:Modifier,
    onMascotPosition:(Offset)->Unit={},onEmoji:((String)->Unit)?=null,onProfile:(()->Unit)?=null
) {''')
s=once(s,"        ArenaMascotV2(avatar,Modifier.size(\n            if(tablet)104.dp else 64.dp))","""        ArenaMascotV2(avatar,Modifier.size(
            if(tablet)104.dp else 64.dp).onGloballyPositioned {
                onMascotPosition(it.positionInWindow()+Offset(it.size.width/2f,it.size.height/2f))
            })""")
s=once(s,'            if(active && expanded) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)','''            if(active && expanded) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)
            if(onEmoji!=null) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    Text("☺",Modifier.clickable(onClickLabel="Emoji gönder"){menu=true},
                        color=Wq172Gold,fontSize=19.sp,fontWeight=FontWeight.Bold)
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(4.dp)) {
                            Wq186ArenaFairPlay.allowedEmojis.forEach { reaction ->
                                TextButton(onClick={menu=false;onEmoji(reaction)}) {
                                    Text(reaction,fontSize=22.sp)
                                }
                            }
                        }
                    }
                }
            }
            if(expanded && onProfile!=null) Text("Profil",Modifier.clickable(onClick=onProfile),
                color=Color(0xFFE4D4FF),fontSize=10.sp)''')
s=once(s,'    hostAvatar:String,guestAvatar:String\n) {','    hostAvatar:String,guestAvatar:String,social:Wq148LiveSocial?=null\n) {')
a=s.index('    Row(Modifier.fillMaxWidth(),',s.index("internal fun Arena172LiveScoreboard("))
s=s[:a]+'''    var origin by remember{mutableStateOf(Offset.Zero)}
    var hostPosition by remember{mutableStateOf<Offset?>(null)}
    var guestPosition by remember{mutableStateOf<Offset?>(null)}
    Box(Modifier.fillMaxWidth().onGloballyPositioned{origin=it.positionInWindow()}) {
'''+s[a:]
s=once(s,'Arena172ScorePlayer(hostName,hostScore,hostAvatar,!cooperative&&isHostTurn,\n            Modifier.weight(1f))','''Arena172ScorePlayer(hostName,hostScore,hostAvatar,!cooperative&&isHostTurn,
            Modifier.weight(1f),onMascotPosition={hostPosition=it},
            onEmoji=if(social!=null && social.myUid!=social.hostUid) ({ symbol:String -> social.send(symbol) }) else null,
            onProfile=if(social!=null && social.myUid!=social.hostUid) ({ social.profile() }) else null)''')
s=once(s,'Arena172ScorePlayer(guestName,guestScore,guestAvatar,!cooperative&&!isHostTurn,\n            Modifier.weight(1f))\n    }\n}','''Arena172ScorePlayer(guestName,guestScore,guestAvatar,!cooperative&&!isHostTurn,
            Modifier.weight(1f),onMascotPosition={guestPosition=it},
            onEmoji=if(social!=null && social.myUid!=social.guestUid) ({ symbol:String -> social.send(symbol) }) else null,
            onProfile=if(social!=null && social.myUid!=social.guestUid) ({ social.profile() }) else null)
    }
    if(social!=null) {
        Wq148EmojiFlight(social.events[social.hostUid],hostPosition,guestPosition,origin)
        Wq148EmojiFlight(social.events[social.guestUid],guestPosition,hostPosition,origin)
    }
    }
}''')
out["ArenaReferenceComponents.kt"]=s

assert set(out)<=names
for n,s in out.items(): (p/n).write_text(s)
assert all(hashlib.sha256(f.read_bytes()).digest()==h for f,h in protected.items())
print("PASS: staged live-only profile, emoji flight, retained hints, fitted board, fullscreen and stable IME input. No unrelated source or artwork changed.")
