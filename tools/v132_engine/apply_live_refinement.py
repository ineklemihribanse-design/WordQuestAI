#!/usr/bin/env python3
"""Live Arena only. Run after the verified v146 assembly and full-source fixes."""
from pathlib import Path
import hashlib
main=Path("source/app/src/main")
p=main/"java/com/kelimebulmacasi/a1"
names={"PremiumOnlineScreen.kt","ArenaReferenceComponents.kt","ArenaV2Visuals.kt","ArenaBoardV2.kt","ArenaCrosswordTyping.kt","Wq186ArenaSocialHud.kt","Wq186ArenaFairPlay.kt"}
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in names}
out={}
def once(s,a,b):
    assert s.count(a)==1,(a[:150],s.count(a))
    return s.replace(a,b,1)
def read(n): return (p/n).read_text()

# Retain revealed letters for every word, not just the selected word.
s=read("ArenaBoardV2.kt")
s=once(s,"@Composable\ninternal fun ArenaBoardV2(","""internal val LocalWq148ArenaHints=compositionLocalOf<Map<Cell,Char>>{emptyMap()}
internal val LocalWq150ArenaZoom=compositionLocalOf{0}

@Composable
internal fun ArenaBoardV2(""")
s=once(s,"    val chosen=puzzle.words.getOrNull(selected)","    val retainedHints=LocalWq148ArenaHints.current\n    val chosen=puzzle.words.getOrNull(selected)")
s=once(s,"val hinted=active && (","val hinted=address in retainedHints || (active && (")
s=once(s,"(hintPosition>=0 && hintLetters.containsKey(hintPosition))\n                            )","(hintPosition>=0 && hintLetters.containsKey(hintPosition))\n                            ))")
s=once(s,"val visibleLetter = fixed[address] ?:","val visibleLetter = fixed[address] ?: retainedHints[address] ?:")
# Arena-only zoom levels; preserve offline and keyboard drawing paths.
s=once(s,"        val cell=if(fittedOverview)","""        val arenaZoom=LocalWq150ArenaZoom.current
        val cell=if(arenaZoom>0 && fittedHeight!=null && !compact)
            (Wq140ArenaFit.cell((maxWidth.value-4f).coerceAtLeast(1f),
                (fittedHeight-4f).coerceAtLeast(1f),rows,cols,gap.value) *
                (if(arenaZoom==1)1.55f else 2.35f)).coerceIn(8f,90f).dp
            else if(fittedOverview)""")
out["ArenaBoardV2.kt"]=s

# Online-only three-state zoom cycle; offline callers keep their existing controls.
s=read("ArenaCrosswordTyping.kt")
s=once(s,"import androidx.compose.ui.unit.dp","import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.platform.LocalConfiguration")
s=once(s,"@Composable\ninternal fun ArenaCompactCrosswordBoard(","""internal val LocalWq148OnlineBoard=compositionLocalOf{false}

@Composable
internal fun ArenaCompactCrosswordBoard(""")
s=once(s,"    var zoomed by rememberSaveable(puzzle.hashCode()) { mutableStateOf(false) }","""    var zoomed by rememberSaveable(puzzle.hashCode()) { mutableStateOf(false) }
    val online=LocalWq148OnlineBoard.current
    var onlineZoom by rememberSaveable(puzzle.hashCode()) { mutableStateOf(0) }
    val config=LocalConfiguration.current""")
s=once(s,"    if(LocalWq140BoundedBoard.current && !compact) {","""    if(online) {
        if(!compact) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
            OutlinedButton(onClick={onlineZoom=(onlineZoom+1)%3},
                contentPadding=PaddingValues(horizontal=10.dp,vertical=0.dp)) {
                Text(when(onlineZoom){0->"Büyüt 1/2";1->"Büyüt 2/2";else->"Tüm tahta"},
                    fontSize=12.sp,color=Color(0xFFFFE3A4))
            }
        }
        val height=(config.screenHeightDp*(if(config.screenWidthDp>=600) .44f else .48f)).coerceAtLeast(130f)
        CompositionLocalProvider(
            LocalWq140ArenaViewport provides if(!compact)height else null,
            LocalWq150ArenaZoom provides if(!compact)onlineZoom else 0
        ) {
            ArenaBoardV2(puzzle,solved,selected,placed,compact,onlineZoom>0 && !compact,
                hintIndex,hintChar,hintLetters,onSelect)
        }
    } else if(LocalWq140BoundedBoard.current && !compact) {""")
# Online draft contains only missing letters. Offline input is unchanged.
s=once(s,"    val placed=crosswordFullWordPreview(crosswordFixedPattern(puzzle,solved,selected),typed)", """    val fixed=crosswordFixedPattern(puzzle,solved,selected)
    val known=LocalWq148ArenaHints.current
    val word=puzzle.words.getOrNull(selected)
    val pattern=fixed.mapIndexed { index, ch ->
        if(ch!='.')ch else word?.cells?.getOrNull(index)?.let{known[it]}
            ?: hintLetters[index] ?: (if(index==hintIndex)hintChar else null) ?: '.'
    }.joinToString("")
    val placed=if(LocalWq148OnlineBoard.current)fillCrosswordGaps(pattern,typed)
        else crosswordFullWordPreview(fixed,typed)""")
s += """

/** Keep the draft letters at their original cells when a hint or crossing becomes fixed. */
internal fun wq150RebaseDraft(before:String, after:String, typed:String):String {
    if(before.length!=after.length ||
        before.indices.any{before[it]!='.' && after[it]!=before[it]})return ""
    val positioned=fillCrosswordGaps(before,typed)
    return after.indices.filter{after[it]=='.'}.map{positioned[it]}
        .takeWhile{it!='.'}.joinToString("")
}
"""
out["ArenaCrosswordTyping.kt"]=s

# Keep one hint network/budget for keyboard and non-keyboard.
s=read("PremiumOnlineScreen.kt")
s=once(s,"verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else 5.dp)","verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else if(match.phase==\"playing\") 3.dp else 5.dp)")
s=once(s,"                val myTurn = match.cooperative || match.turnUid == myUid","""                val retainedHints=remember(puzzle,claimedHints,match.hintBy[myUid],solved) {
                    buildMap<Cell,Char> {
                        match.hintBy[myUid].orEmpty().forEach { index ->
                            val w=puzzle.words.getOrNull(index) ?: return@forEach
                            val position=Wq181ArenaHintSlot.reserve(context,match.code,myUid,index,
                                crosswordFixedPattern(puzzle,solved,index))
                            val cell=w.cells.getOrNull(position)
                            val letter=w.entry.letters.getOrNull(position)
                            if(cell!=null && letter!=null)put(cell,letter)
                        }
                        claimedHints.forEach { claim ->
                            val w=puzzle.words.getOrNull(claim.word)
                            val cell=w?.cells?.getOrNull(claim.position)
                            val letter=w?.entry?.letters?.getOrNull(claim.position)
                            if(cell!=null && letter!=null)put(cell,letter)
                        }
                    }
                }
                val hintPattern=word.cells.mapIndexed { i,cell ->
                    retainedHints[cell] ?: (if(i==revealedIndex)revealedChar else null) ?: fixedPattern.getOrElse(i){'.'}
                }.joinToString("")
                var draftPattern by remember(match.code,selected){mutableStateOf(hintPattern)}
                LaunchedEffect(match.code,selected,hintPattern) {
                    if(draftPattern!=hintPattern){
                        setAnswerText(wq150RebaseDraft(draftPattern,hintPattern,answerText))
                        draftPattern=hintPattern
                    }
                }
                val missingLetters=hintPattern.count{it=='.'}
                val completedDraft=fillCrosswordGaps(hintPattern,answerText)
                val draftReady=draftPattern==hintPattern &&
                    englishLetters(answerText).length==missingLetters &&
                    completedDraft.isNotEmpty() && !completedDraft.contains('.')
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
submit=once(submit,"!crosswordFullWordReady(word.entry.letters.length, answerText)","!draftReady || hintPending")
submit=once(submit,"crosswordFullWordMatches(fixedPattern, answerText, word.entry.letters)","crosswordFullWordMatches(hintPattern, completedDraft, word.entry.letters)")
field='''                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            OutlinedTextField(answerText, { if(canAnswer && !hintPending && draftPattern==hintPattern)setAnswerText(englishLetters(it).take(missingLetters)) },
                                Modifier.weight(1f),label={Text(if(!myTurn)"Rakibin sırası" else if(missingLetters==0)"Tüm harfler açık" else "Eksik $missingLetters harfi yaz")},
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
                            enabled = draftReady && canAnswer && !hintPending)
                        if(selected !in solved) {
'''
# The retained opening brace replaces the original unsolved-only controls scope.
s=s[:a]+state+solved+messages+status+"                    }\n"+submit+field+s[b:]
out["PremiumOnlineScreen.kt"]=s

# Exercise hint-completed answers and draft retention without changing solo tests.
test_path=main.parent/"test/java/com/kelimebulmacasi/a1/Wq150ArenaHintDraftTest.kt"
test_path.parent.mkdir(parents=True,exist_ok=True)
test_path.write_text("""package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq150ArenaHintDraftTest {
    @Test fun onlyMissingLettersAreTyped() {
        assertEquals("PRICE",fillCrosswordGaps("P.I.E","RC"))
        assertTrue(crosswordFullWordMatches("P.I.E","PRICE","PRICE"))
        assertEquals("PRICE",fillCrosswordGaps("PRICE",""))
        assertEquals("P.I.E",fillCrosswordGaps("P.I.E",""))
    }
    @Test fun newlyLockedCellDoesNotShiftTheDraft() {
        assertEquals("RCE",wq150RebaseDraft(".....","P.I..","PRICE"))
        assertEquals("PR",wq150RebaseDraft(".....","..I..","PRI"))
        assertEquals("PIE",wq150RebaseDraft(".....",".R.C.","PRICE"))
        assertEquals("",wq150RebaseDraft("P....",".....","RICE"))
    }
}
""",encoding="utf-8")

# Persistent mascot independent of disclosure state. Only the live scoreboard
# uses this private player-card function; all entry, lobby and settings are untouched.
s=read("ArenaReferenceComponents.kt")
s=once(s,"""        if(expanded) ArenaMascotV2(avatar,Modifier.size(
            if(tablet) { if(active)75.dp else 68.dp }
            else { if(active)51.dp else 45.dp }))""","""        ArenaMascotV2(avatar,Modifier.size(
            if(tablet)120.dp else 76.dp))""")
s=once(s,".heightIn(min=if(expanded) { if(tablet)90.dp else 78.dp } else 48.dp)",".heightIn(min=if(expanded) { if(tablet)132.dp else 90.dp } else if(tablet)120.dp else 76.dp)")
out["ArenaReferenceComponents.kt"]=s


# Move reaction/profile actions to the opposing card and reuse existing
# room-scoped Firebase messages. Do not modify security rules or permissions.
s=read("Wq186ArenaSocialHud.kt")
s=once(s,"    guestScore: Int\n) {","    guestScore: Int,\n    cardSocial: Wq148LiveSocial? = null\n) {")
s=once(s,'    if (match.phase == "playing" || match.phase == "lobby") {', '''    SideEffect {
        cardSocial?.let { card ->
            card.events=emoji
            card.hostDetails=if(match.mode=="quiz")"" else "Doğru: "+match.solvedBy.values.count{it==match.hostUid}+" · Yanlış: "+match.mistakesBy.values.sumOf{it[match.hostUid]?:0}
            card.guestDetails=if(match.mode=="quiz")"" else "Doğru: "+match.solvedBy.values.count{it==match.guestUid}+" · Yanlış: "+match.mistakesBy.values.sumOf{it[match.guestUid]?:0}
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
# Versioned finish-offer UI uses a single immutable proposal identity. Old vote UI
# is retained only for callers without the new live scoreboard.
s=once(s,'''            card.finishLabel=if(myUid in votes)"Teklifi geri çek" else "Bitirme teklifi"
            card.finish={
                if(myUid in votes)extra?.child("finishVotes")?.child(myUid)?.removeValue()
                else askFinish=true
            }
''',"")
s=once(s,'val incomingOffer=match.phase=="playing" && opponentUid.isNotBlank() &&',
'val incomingOffer=cardSocial==null && match.phase=="playing" && opponentUid.isNotBlank() &&')
s=once(s,'if (isHost && room != null && match.phase == "playing" &&',
'if (cardSocial==null && isHost && room != null && match.phase == "playing" &&')
s=once(s,'        if (myUid in votes && opponentUid !in votes && match.phase=="playing")',
'        if (cardSocial==null && myUid in votes && opponentUid !in votes && match.phase=="playing")')
s=once(s,'    if (askFinish && !incomingOffer) AlertDialog(','''    if(cardSocial!=null && room!=null && extra!=null) Wq150FinishOfferDialog(
        room,extra,myUid,opponentUid,match.phase,isHost,hostName,guestName,hostScore,guestScore,cardSocial)
    if (askFinish && cardSocial==null) AlertDialog(''')
s += """

private data class Wq150Offer(val id:String,val from:String,val to:String,
    val status:String,val createdAt:Long,val updatedAt:Long) {
    fun transition(next:String):Map<String,Any> = mapOf(
        "id" to id,"from" to from,"to" to to,"status" to next,
        "createdAt" to createdAt,"updatedAt" to ServerValue.TIMESTAMP)
}
@Composable
private fun Wq150FinishOfferDialog(
    room:DatabaseReference,extra:DatabaseReference,myUid:String,opponentUid:String,
    phase:String,isHost:Boolean,hostName:String,guestName:String,hostScore:Int,guestScore:Int,
    social:Wq148LiveSocial
) {
    val ref=remember(extra){extra.child("finishOffer")}
    var offer by remember(ref){mutableStateOf<Wq150Offer?>(null)}
    var loaded by remember(ref){mutableStateOf(false)}
    var offset by remember(ref){mutableLongStateOf(0L)}
    var clock by remember(ref){mutableLongStateOf(System.currentTimeMillis())}
    var ask by remember(ref){mutableStateOf(false)}
    var busy by remember(ref){mutableStateOf(false)}
    var notice by remember(ref){mutableStateOf("")}
    DisposableEffect(ref) {
        val listener=object:ValueEventListener {
            override fun onDataChange(snapshot:DataSnapshot) {
                val id=snapshot.child("id").getValue(String::class.java)
                val from=snapshot.child("from").getValue(String::class.java)
                val to=snapshot.child("to").getValue(String::class.java)
                val status=snapshot.child("status").getValue(String::class.java)
                val created=(snapshot.child("createdAt").value as? Number)?.toLong()
                val updated=(snapshot.child("updatedAt").value as? Number)?.toLong()
                offer=if(id!=null && from!=null && to!=null && status!=null && created!=null && updated!=null)
                    Wq150Offer(id,from,to,status,created,updated) else null
                loaded=true
            }
            override fun onCancelled(error:DatabaseError){loaded=false;notice="Teklif bağlantısı kurulamadı."}
        }
        val offsetRef=room.root.child(".info/serverTimeOffset")
        val offsetListener=object:ValueEventListener {
            override fun onDataChange(snapshot:DataSnapshot){offset=(snapshot.value as? Number)?.toLong()?:0L}
            override fun onCancelled(error:DatabaseError){}
        }
        ref.addValueEventListener(listener);offsetRef.addValueEventListener(offsetListener)
        onDispose{ref.removeEventListener(listener);offsetRef.removeEventListener(offsetListener)}
    }
    LaunchedEffect(phase) {
        while(phase=="playing"){clock=System.currentTimeMillis();delay(1000)}
    }
    val now=clock+offset
    val current=offer
    fun respond(item:Wq150Offer,status:String) {
        if(busy || phase!="playing")return
        busy=true
        ref.setValue(item.transition(status)).addOnCompleteListener{
            busy=false
            if(!it.isSuccessful)notice="Teklif işlemi tamamlanamadı."
        }
    }
    val pending=current?.status=="pending"
    val remaining=if(current!=null)((current.createdAt+30000L-now+999L)/1000L).coerceAtLeast(0L) else 0L
    val cooldown=if(current!=null)((current.createdAt+60000L-now+999L)/1000L).coerceAtLeast(0L) else 0L
    LaunchedEffect(current?.id,current?.status,remaining,phase) {
        if(current!=null && pending && remaining==0L && phase=="playing")respond(current,"expired")
    }
    LaunchedEffect(current?.id,current?.status,phase,isHost) {
        if(current?.status=="accepted" && isHost && phase=="playing")
            room.child("phase").setValue("finished").addOnFailureListener{notice="Maç sonucu kaydedilemedi."}
        if(current!=null && current.status!="pending" &&
            (System.currentTimeMillis()+offset-current.updatedAt) in 0L..6000L) {
            notice=when(current.status){
                "accepted"->"Bitirme teklifi kabul edildi."
                "rejected"->if(current.from==myUid)"Rakibin bitirme teklifini reddetti." else "Bitirme teklifini reddettin."
                "withdrawn"->if(current.from==myUid)"Teklif geri çekildi." else "Rakibin teklifini geri çekti."
                "expired"->"Bitirme teklifinin süresi doldu."
                else->""
            }
        }
    }
    LaunchedEffect(notice){if(notice.isNotBlank()){delay(3000);notice=""}}
    SideEffect {
        social.finishLabel=when{
            pending && current?.from==myUid->"Teklifi geri çek"
            pending->"Bitirme teklifi"
            cooldown>0L->"Teklif · "+cooldown+" sn"
            else->"Bitirme teklifi"
        }
        social.finish={
            if(!loaded || phase!="playing" || opponentUid.isBlank() || busy)Unit
            else if(current!=null && pending && current.from==myUid && remaining>0L)respond(current,"withdrawn")
            else if(!pending && cooldown==0L && current?.status!="accepted")ask=true
        }
    }
    if(ask && phase=="playing") AlertDialog(
        onDismissRequest={ask=false},title={Text("Maçı birlikte bitir")},
        text={Text("İki oyuncu kabul ederse maç mevcut puanlarla tamamlanır.")},
        confirmButton={TextButton(onClick={
            ask=false
            if(!busy && loaded && !pending && cooldown==0L && opponentUid.isNotBlank()){
                busy=true
                val id=java.util.UUID.randomUUID().toString().replace("-","")
                ref.setValue(mapOf("id" to id,"from" to myUid,"to" to opponentUid,
                    "status" to "pending","createdAt" to ServerValue.TIMESTAMP,"updatedAt" to ServerValue.TIMESTAMP))
                    .addOnCompleteListener{busy=false;if(!it.isSuccessful)notice="Bitirme teklifi gönderilemedi."}
            }
        }){Text("Teklifi gönder")}},
        dismissButton={TextButton(onClick={ask=false}){Text("Devam et")}})
    if(phase=="playing" && current!=null && pending && current.to==myUid && remaining>0L)
        AlertDialog(onDismissRequest={},title={Text("Maç bitirme teklifi")},
            text={Text("Rakibin maçı mevcut puanlarla tamamlamak istiyor.\\n"+
                hostName+": "+hostScore+" puan · "+guestName+": "+guestScore+" puan\\nKalan süre: "+remaining+" sn")},
            confirmButton={TextButton(enabled=!busy,onClick={respond(current,"accepted")}){Text("Kabul Et")}},
            dismissButton={TextButton(enabled=!busy,onClick={respond(current,"rejected")}){Text("Reddet")}})
    if(notice.isNotBlank()) androidx.compose.ui.window.Popup(alignment=Alignment.TopCenter) {
        Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFF302049),
            modifier=Modifier.padding(12.dp)) {
            Text(notice,color=Color.White,modifier=Modifier.padding(12.dp),fontSize=13.sp)
        }
    }
}
"""
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
    var hostDetails by mutableStateOf("")
    var guestDetails by mutableStateOf("")
    var send:(String)->Unit={}
    var profile:()->Unit={}
    var finish:()->Unit={}
    var finishLabel by mutableStateOf("Bitirme teklifi")
}

@Composable
private fun Wq148EmojiFlight(event:Pair<String,Long>?,from:Offset?,to:Offset?,origin:Offset) {
    val progress=remember{Animatable(1f)}
    var displayed by remember {mutableStateOf("")}
    var last by remember {mutableLongStateOf(System.currentTimeMillis())}
    val density=LocalDensity.current
    LaunchedEffect(event) {
        val next=event ?: return@LaunchedEffect
        val age=System.currentTimeMillis()-next.second
        if(age !in -5000L..8000L || next.second<=last) return@LaunchedEffect
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
        }.graphicsLayer { alpha=minOf(1f,t*10f,(1f-t)*8f); scaleX=.8f+.2f*kotlin.math.sin(Math.PI*t).toFloat();scaleY=scaleX },fontSize=28.sp)
    }
}

@Composable
private fun Arena172ScorePlayer(""")
s=once(s,'    name:String,points:Int,avatar:String,active:Boolean,modifier:Modifier\n) {','''    name:String,points:Int,avatar:String,active:Boolean,modifier:Modifier,
    onMascotPosition:(Offset)->Unit={},onEmoji:((String)->Unit)?=null,onProfile:(()->Unit)?=null,
    details:String=""
) {''')
s=once(s,"        ArenaMascotV2(avatar,Modifier.size(\n            if(tablet)120.dp else 76.dp))","""        ArenaMascotV2(avatar,Modifier.size(
            if(tablet)120.dp else 76.dp).onGloballyPositioned {
                onMascotPosition(it.positionInWindow()+Offset(it.size.width/2f,it.size.height/2f))
            })""")
s=once(s,'            if(active && expanded) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)','''            if(active) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)
            if(expanded && details.isNotBlank())Text(details,color=Color(0xFFF3E6FF),fontSize=9.sp)
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
            onProfile=if(social!=null && social.myUid!=social.hostUid) ({ social.profile() }) else null,
            details=social?.hostDetails.orEmpty())''')
s=once(s,'Arena172ScorePlayer(guestName,guestScore,guestAvatar,!cooperative&&!isHostTurn,\n            Modifier.weight(1f))\n    }\n}','''Arena172ScorePlayer(guestName,guestScore,guestAvatar,!cooperative&&!isHostTurn,
            Modifier.weight(1f),onMascotPosition={guestPosition=it},
            onEmoji=if(social!=null && social.myUid!=social.guestUid) ({ symbol:String -> social.send(symbol) }) else null,
            onProfile=if(social!=null && social.myUid!=social.guestUid) ({ social.profile() }) else null,
            details=social?.guestDetails.orEmpty())
    }
    if(social!=null) {
        Wq148EmojiFlight(social.events[social.hostUid],hostPosition,guestPosition,origin)
        Wq148EmojiFlight(social.events[social.guestUid],guestPosition,hostPosition,origin)
    }
    }
}''')
out["ArenaReferenceComponents.kt"]=s

s=read("Wq186ArenaFairPlay.kt")
s=once(s,'setOf("👏", "🔥", "🎉", "😄", "👍", "💪", "🤝", "🏆")','setOf("👏", "🔥", "🎉", "😄", "👍", "💪", "🤝", "🏆", "❤️", "😮")')
out["Wq186ArenaFairPlay.kt"]=s

assert set(out)<=names
for n,s in out.items(): (p/n).write_text(s)
assert all(hashlib.sha256(f.read_bytes()).digest()==h for f,h in protected.items())
print("PASS: staged live-only profile, emoji flight, retained hints, fitted board, two-step zoom and stable IME input. No unrelated source or artwork changed.")
