#!/usr/bin/env python3
from pathlib import Path
import hashlib
main=Path("source/app/src/main")
p=main/"java/com/kelimebulmacasi/a1"
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in ("PremiumOnlineRoom.kt","PremiumOnlineScreen.kt")}
def once(s,a,b):
    assert s.count(a)==1,(a[:120],s.count(a))
    return s.replace(a,b,1)
f=p/"PremiumOnlineRoom.kt";s=f.read_text()
s=once(s,'    val configRevision: Int = 1,','    val configRevision: Int = 1,\n    val roundData: String = "",')
s=once(s,'childString("guestAvatar").ifBlank {"fox"}, revision)','childString("guestAvatar").ifBlank {"fox"}, revision, childString("roundData"))')
s=once(s,'    var busy by mutableStateOf(false)','''    private var validatedRound: String? = null
    private fun validationKey(m:PremiumOnlineMatch)=m.code+":"+m.configRevision+":"+m.boardId
    fun confirmSharedRound(m:PremiumOnlineMatch) {
        val current=match ?: return
        if(current.code==m.code && current.configRevision==m.configRevision &&
            current.boardId==m.boardId && current.roundData==m.roundData) validatedRound=validationKey(m)
    }
    var busy by mutableStateOf(false)''')
a=s.index("    fun create(");b=s.index("        refreshQuotaForToday()",a)
s=s[:a]+'''    fun create(mode: String, level: String, topic: String, size: Int, seconds: Int, boardId: String,
        roundPayload: String? = null, selectedPreset: Wq186MatchPreset? = null) {
        val preset=(selectedPreset ?: Wq186ArenaPresetStore.read(context,session.profileKey,level)).sanitized(level)
        if(roundPayload!=null && runCatching{Wq132WebEngine.snapshotId(roundPayload)}.getOrNull()!=boardId) {
            status="Ortak oyun verisi geçersiz.";return
        }
        if(roundPayload==null && (boardId.startsWith("web:") || preset.levels.size>1)) {
            status="Karışık seviye için ortak web oyunu gerekli.";return
        }
        val actualLevel=level
        val actualBoardId=boardId
'''+s[b:]
s=once(s,'database.reference.updateChildren(mapOf<String, Any>("roomQuotaV2/$myUid/$day/$slot" to code, "premiumRooms/$code" to initial))','''database.reference.updateChildren(mapOf<String, Any>("roomQuotaV2/$myUid/$day/$slot" to code,
                        "premiumRooms/$code" to (initial + if(roundPayload!=null)mapOf("roundData" to roundPayload) else emptyMap())))''')
a=s.index("    fun toggleReady()");b=s.index("    /**",a)
s=s[:a]+'''    fun toggleReady() {
        val m=match ?: return
        if(m.phase!="lobby" || uid !in listOf(m.hostUid,m.guestUid) || busy)return
        val room=ref ?: return
        val acknowledgement: Any?=if(uid in m.readyBy)null else m.configRevision
        fun submitReady(){
            val current=match ?: run { busy=false;return }
            if(current.code!=m.code||current.configRevision!=m.configRevision||current.boardId!=m.boardId||current.roundData!=m.roundData||current.phase!="lobby"){
                busy=false;return
            }
            room.child("ready").child(uid).setValue(acknowledgement).addOnCompleteListener{task->
                busy=false
                status=if(task.isSuccessful) {
                    if(acknowledgement==null)"Hazır durumun kaldırıldı." else "Hazırsın! Diğer oyuncu bekleniyor."
                } else "Hazır durumu kaydedilemedi: "+task.exception?.localizedMessage.orEmpty()
            }
        }
        if(m.boardId.startsWith("web:")&&acknowledgement!=null) {
            if(validatedRound!=validationKey(m)){status="Ortak oyun henüz doğrulanmadı.";return}
            busy=true
            room.child("roundAck").child(uid).setValue(m.configRevision).addOnCompleteListener{task->
                if(task.isSuccessful)submitReady()
                else {busy=false;status="Ortak oyun onayı kaydedilemedi. Yeni Firebase kurallarını kontrol et."}
            }
        } else {busy=true;submitReady()}
    }

'''+s[b:]
a=s.index("    fun updateLobbySettings(");b=s.index("    fun start()",a)
part=s[a:b]
part=once(part,'boardId: String, preset: Wq186MatchPreset? = null','boardId: String, preset: Wq186MatchPreset? = null, snapshot: String? = null')
part=once(part,'        val room=ref ?: return','''        if(snapshot!=null && runCatching{Wq132WebEngine.snapshotId(snapshot)}.getOrNull()!=boardId){
            status="Ortak oyun verisi geçersiz.";return
        }
        if(snapshot==null && (m.boardId.startsWith("web:") || boardId.startsWith("web:") || (preset ?: package6Rules).levels.size>1)){
            status="Yeni web oyun verisi gerekli.";return
        }
        val room=ref ?: return''')
part=once(part,'        room.updateChildren(mapOf(','        room.updateChildren(mapOf<String,Any>(')
part=once(part,'        )).addOnCompleteListener {task->','        ) + if(snapshot!=null)mapOf("roundData" to snapshot) else emptyMap()).addOnCompleteListener {task->')
s=s[:a]+part+s[b:]
s=once(s,'        val room = ref ?: return\n        busy = true\n        status = "Oyuncular hazırlanıyor…"','''        if(m.boardId.startsWith("web:")&&validatedRound!=validationKey(m)){
            status="Ortak oyun henüz doğrulanmadı.";return
        }
        val room = ref ?: return
        busy = true
        status = "Oyuncular hazırlanıyor…"''')
s=once(s,'ref = null; listener = null; match = null; roomCode = ""','ref = null; listener = null; match = null; roomCode = ""; validatedRound=null')
# The quota callback's snapshot is a Firebase DataSnapshot, NOT game JSON.
# Assert the actual assembled create function sends its String parameter.
create=s[s.index('    fun create('):s.index('    private fun reserve(',s.index('    fun create('))]
assert 'roundPayload: String?' in create and 'mapOf("roundData" to roundPayload)' in create
assert 'mapOf("roundData" to snapshot)' not in create
f.write_text(s)
f=p/"PremiumOnlineScreen.kt";s=f.read_text()
if "import kotlinx.coroutines.launch\n" not in s and "import kotlinx.coroutines.*\n" not in s:
    s=once(s,"package com.kelimebulmacasi.a1\n","package com.kelimebulmacasi.a1\n\nimport kotlinx.coroutines.launch\n")
s=once(s,'    val match = client.match','''    val webRoomScope=rememberCoroutineScope()
    var creatingWebRoom by remember{mutableStateOf(false)}
    val match = client.match''')
s=once(s,'canCreate = client.canCreate,','canCreate = client.canCreate && !creatingWebRoom,')
a=s.index('                onCreate = {');b=s.index('                onJoin =',a)
assert 'client.create(' in s[a:b]
s=s[:a]+'''                onCreate = {
                    if(!creatingWebRoom && client.canCreate) {
                        val preset=Wq186ArenaPresetStore.read(context,session.profileKey,level)
                        val requestedMode=selectedMode;val requestedTopic=topic
                        val requestedSize=if(requestedMode=="quiz"){
                            val allowed=listOf(5,10,12,15,20).filter{it>=preset.levels.size}
                            size.takeIf{it in allowed}?:allowed.minByOrNull{kotlin.math.abs(it-size)}?:10
                        }else size
                        val requestedSeconds=if(requestedMode=="team")0 else seconds
                        creatingWebRoom=true
                        client.note("Ortak oyun hazırlanıyor…")
                        webRoomScope.launch {
                            try {
                                val generated=Wq132WebEngine.generate(context,requestedMode,preset.levels,requestedTopic,
                                    requestedSize,requestedSeconds,preset.hints,preset.retries,java.security.SecureRandom().nextInt())
                                if(client.match==null && client.canCreate)client.create(requestedMode,preset.levels.first(),
                                    requestedTopic,requestedSize,requestedSeconds,generated.snapshotId,generated.snapshot,preset)
                            } catch(e:kotlinx.coroutines.CancellationException){throw e}
                            catch(e:Exception){client.note("Oyun hazırlanamadı: "+(e.message?:"Başka ayar seç.").take(180))}
                            finally {creatingWebRoom=false}
                        }
                    }
                },
'''+s[b:]
s=once(s,'onJoin = {client.join(codeInput)},','onJoin = {if(!creatingWebRoom)client.join(codeInput)},')
a=s.index('    val board = remember(match.boardId)');b=s.index('    fun arenaScore(',a)
assert 'packs.puzzleById(match.boardId)' in s[a:b] and 'packs.questionsForSeed(' in s[a:b]
s=s[:a]+'''    val hybrid=match.boardId.startsWith("web:")
    val rules=client.package6Rules
    var shared by remember(match.code,match.configRevision,match.boardId,match.roundData,rules){mutableStateOf<Wq132WebEngine.Round?>(null)}
    var sharedError by remember(match.code,match.configRevision,match.boardId,match.roundData,rules){mutableStateOf("")}
    LaunchedEffect(match.code,match.configRevision,match.boardId,match.roundData,rules){
        if(hybrid){
            try {
                val decoded=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default){
                    Wq132WebEngine.decodeShared(context,match.mode,rules.levels,match.topic,match.size,match.seconds,
                        rules.hints,rules.retries,match.boardId,match.roundData)
                }
                shared=decoded;client.confirmSharedRound(match)
            } catch(e:kotlinx.coroutines.CancellationException){throw e}
            catch(e:Exception){sharedError="Ortak oyun doğrulanamadı: "+(e.message?:"Sürümü kontrol et.").take(160)}
        }
    }
    if(hybrid&&shared==null){
        Text(sharedError.ifBlank{"Ortak oyun doğrulanıyor…"},color=Color.White,fontSize=14.sp)
        TextButton(onClick={client.leave()}){Text("Odadan ayrıl",color=Color.White)}
        return
    }
    val board = remember(match.boardId,shared) {
        if(hybrid)shared?.board else if(match.isCrossword)packs.puzzleById(match.boardId) else null
    }
    val questions = remember(match.code,match.quizSeed,match.level,match.topic,match.size,rules.levels,shared) {
        if(hybrid)shared?.questions.orEmpty() else if(match.mode=="quiz")packs.questionsForSeed(match.level,match.topic,match.quizSeed,match.size,rules.levels) else emptyList()
    }
'''+s[b:]
s=once(s,'packs.levelForWord(it,match.level)','shared?.wordLevels?.get(it.uppercase()) ?: packs.levelForWord(it,match.level)')
s=once(s,'Text("${match.level} · ${if (match.phase == "lobby")','Text("${rules.levels.joinToString(" · ")} · ${if (match.phase == "lobby")')
f.write_text(s)
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in protected.items())
print("PASS: online room create/edit uses frozen validated web round, ready revision and unchanged board renderer")


# Online Arena lifecycle and portrait regression repair.
repair_names={"PremiumOnlineRoom.kt","PremiumOnlineScreen.kt","ArenaV2Visuals.kt",
              "Wq128PremiumRoomLobby.kt","WordQuest2DCharacters.kt","Wq183OnlinePortraitStrip.kt"}
repair_protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in repair_names}
f=p/"PremiumOnlineRoom.kt";s=f.read_text()
s=once(s,'''    val complete: Boolean get() = if (mode == "quiz") {
        hostUid.isNotBlank() && guestUid.isNotBlank() &&
            (answers[hostUid]?.size ?: 0) >= size && (answers[guestUid]?.size ?: 0) >= size
    } else solvedBy.size >= size''','''    val complete: Boolean get() = size > 0 && hostUid.isNotBlank() && guestUid.isNotBlank() &&
        if (mode == "quiz") {
            (0 until size).all { i ->
                (answers[hostUid]?.get(i) ?: -2) in -1..4 && (answers[guestUid]?.get(i) ?: -2) in -1..4
            }
        } else (0 until size).all { it in solvedBy }''')
s=once(s,'    private var validatedRound: String? = null','''    private var validatedRound: String? = null
    private var finishingRoom: DatabaseReference? = null
    private val pendingQuizAnswers = mutableSetOf<Int>()''')
s=once(s,'        ref = next\n        listener = object : ValueEventListener {','''        ref = next
        busy = true
        var initialSnapshot = true
        listener = object : ValueEventListener {''')
s=once(s,'                if (!snapshot.exists()) { match = null; status = "Oda bulunamadı."; return }','''                if (ref !== next) return
                if (!snapshot.exists()) {
                    if (initialSnapshot) { busy=false; status="Oda bulunamadı." }
                    else status="Oda verisi geçici olarak alınamadı."
                    return
                }''')
s=once(s,'                    childString("guestAvatar").ifBlank {"fox"}, revision, childString("roundData"))','''                    childString("guestAvatar"), revision, childString("roundData"))
                if(initialSnapshot) { initialSnapshot=false; busy=false }''')
s=once(s,'                    childString("hostAvatar").ifBlank {"robot"},','                    childString("hostAvatar"),')
s=once(s,'                status = "Bağlantı kesildi: '+chr(36)+'{error.message}. Oda saklandı; internet geldiğinde yeniden katılabilirsin."','''                if (ref !== next) return
                if (initialSnapshot) busy=false
                status = "Bağlantı kesildi: '''+chr(36)+'''{error.message}. Oda saklandı; internet geldiğinde yeniden katılabilirsin."''')
s=once(s,'''                    status = "Odaya katıldın."; observe(code); onJoined?.invoke()
                    ref.child("guestAvatar").setValue(myAvatar).addOnFailureListener {
                        status = "Avatarın çevrim içi odaya kaydedilemedi."
                    }''','''                    busy=true
                    ref.child("guestAvatar").setValue(myAvatar).addOnCompleteListener { avatarResult ->
                        status=if(avatarResult.isSuccessful) "Odaya katıldın."
                            else "Maskot henüz eşitlenemedi."
                        observe(code); onJoined?.invoke()
                    }''')
a=s.index('    fun answer(index: Int, choice: Int) {');z=s.index('    fun useHint(',a)
s=s[:a]+'''    fun answer(index: Int, choice: Int): Boolean {
        val m = match ?: return false
        val room = ref ?: return false
        if (m.mode != "quiz" || m.phase != "playing" || index !in 0 until m.size || choice !in -1..4 ||
            uid !in listOf(m.hostUid,m.guestUid) ||
            m.answers[uid]?.containsKey(index) == true || !pendingQuizAnswers.add(index)) return false
        room.child("answers").child(uid).child(index.toString()).setValue(choice)
            .addOnCompleteListener { task ->
                if(ref === room) {
                    pendingQuizAnswers.remove(index)
                    if(!task.isSuccessful) status="Cevap kaydedilemedi. Tekrar dene."
                }
            }
        return true
    }

'''+s[z:]
a=s.index('    fun finish() {');z=s.index('    fun note(',a)
s=s[:a]+'''    fun finish() {
        val m = match ?: return
        val room = ref ?: return
        if (!isHost || m.phase != "playing" || !m.complete || finishingRoom === room) return
        finishingRoom=room
        room.child("phase").setValue("finished").addOnCompleteListener { task ->
            if(finishingRoom === room) finishingRoom=null
            if(ref === room && !task.isSuccessful) status="Sonuç eşitleniyor. Bağlantı kontrol ediliyor."
        }
    }

'''+s[z:]
s=once(s,'ref = null; listener = null; match = null; roomCode = ""; validatedRound=null','ref = null; listener = null; match = null; roomCode = ""; validatedRound=null; finishingRoom=null; pendingQuizAnswers.clear(); busy=false')
f.write_text(s)
f=p/"PremiumOnlineScreen.kt";s=f.read_text()
s=once(s,'''        if (match != null && match.complete && match.phase == "playing" && isHost) client.finish()
    }''','''    }
    LaunchedEffect(match?.code,match?.configRevision,match?.phase,match?.complete,isHost) {
        if(match?.complete==true && match.phase=="playing" && isHost) {
            val code=match.code
            while(true) {
                val latest=client.match ?: break
                if(latest.code!=code || latest.phase!="playing" || !latest.complete || !client.isHost) break
                client.finish()
                delay(3000)
            }
        }
    }
    LaunchedEffect(match?.code,match?.configRevision) {
        quizIndex=0
        answerText=""
    }''')
s=once(s,'onJoin = {if(!creatingWebRoom)client.join(codeInput)},','onJoin = {if(!creatingWebRoom && !client.busy)client.join(codeInput)},')
s=once(s,'    if (match.phase == "finished") Wq183OnlinePortraitStrip(','    if (match.phase == "finished" || (match.phase == "playing" && match.mode == "quiz")) Wq183OnlinePortraitStrip(')
s=once(s,'                    val index = if (quizIndex !in questions.indices) 0 else quizIndex','''                    val index = quizIndex.coerceIn(0,questions.lastIndex)
                    val unanswered=questions.indices.firstOrNull { it !in own }
                    if(unanswered==null) {
                        ArenaV2Panel {
                            Text("Tüm sorularını tamamladın!",color=OnlineGood,fontWeight=FontWeight.Bold)
                            Text(if(match.complete) "Sonuçlar eşitleniyor…" else "Rakibinin kalan soruları tamamlaması bekleniyor.",color=OnlineCream)
                            Text("Sen: "+own.size+"/"+questions.size+" · Rakip: "+
                                (match.answers[PremiumOnlineLimits.otherPlayer(match,myUid)]?.size ?: 0)+"/"+questions.size,color=OnlineCream)
                        }
                    } else {''')
s=once(s,'''                                        client.answer(index,n)
                                        if(n==question.correct) sounds.correct(session.preferences)
                                        else sounds.wrong(session.preferences)''','''                                        if(client.answer(index,n)) {
                                            if(n==question.correct) sounds.correct(session.preferences)
                                            else sounds.wrong(session.preferences)
                                        }''')
s=once(s,'''                            ArenaOutlineButton("Sonraki", { setQuizIndex((index + 1).coerceAtMost(questions.lastIndex)) }, Modifier.weight(1f))
                        }
                    }
                }
            } else if (board''','''                            ArenaOutlineButton("Sonraki", {
                                setQuizIndex(if(index<questions.lastIndex)index+1 else unanswered)
                            }, Modifier.weight(1f))
                        }
                    }
                    }
                }
            } else if (board''')
s=once(s,'''                match.score(match.hostUid,questions),
                match.score(match.guestUid,questions))''','''                arenaScore(match.hostUid),
                arenaScore(match.guestUid))''')
f.write_text(s)
f=p/"ArenaV2Visuals.kt";s=f.read_text()
s=once(s,'''    if(Wq2DMascots.valid(kind)) Wq2DPortrait(kind,modifier)
    else ArenaRiggedMascot(kind,modifier)''','''    Wq2DPortrait(kind,modifier)''')
s=once(s,'''            if(Wq2DMascots.valid(winnerAvatar))
                Wq2DPortrait(winnerAvatar,
                    Modifier.size(110.dp).scale(jump.value),victory=!isDraw)
            else ArenaRiggedMascot(winnerAvatar,
                Modifier.size(86.dp).scale(jump.value),holdingTrophy=!isDraw)''','''            Wq2DPortrait(winnerAvatar,
                Modifier.size(110.dp).scale(jump.value),victory=!isDraw)''')
f.write_text(s)
f=p/"WordQuest2DCharacters.kt";s=f.read_text()
s=once(s,'''            return Wq183MascotStore.picture(context,id,victory)
                ?: picture(context,"piko",victory)''','''            return Wq183MascotStore.picture(context,id,victory)''')
s=once(s,'        opponentPreview ?: Wq2DMascots.picture(context,id,victory)','        if(Wq2DMascots.valid(id)) opponentPreview ?: Wq2DMascots.picture(context,id,victory) else null')
s=once(s,'''                Text(Wq2DMascots.label(id).take(1),fontSize=24.sp,
                    color=Color(0xFF513587),fontWeight=FontWeight.ExtraBold)''','''                Text(if(Wq183MascotStore.isCustomId(id)) "Maskot" else "—",fontSize=12.sp,
                    color=Color(0xFF513587),fontWeight=FontWeight.ExtraBold)''')
f.write_text(s)
f=p/"Wq183OnlinePortraitStrip.kt";s=f.read_text()
s=once(s,'    val safeId = if (Wq2DMascots.valid(avatar)) avatar!! else "piko"','    val safeId = avatar.orEmpty()')
f.write_text(s)
f=p/"Wq128PremiumRoomLobby.kt";s=f.read_text()
s=once(s,'    val hostReady=match.hostUid in match.readyBy','''    val selectedMaskot by rememberWq2DSelection(profileKey)
    val shareMaskot by rememberWq183ShareConsent(profileKey)
    val hostReady=match.hostUid in match.readyBy''')
s=once(s,'        Text(when {\n            allReady -> "✓ İki oyuncu da hazır."','''        if(Wq183MascotStore.isCustomId(selectedMaskot)) {
            Row(Modifier.fillMaxWidth().lobbyPlate128(radius=12).padding(horizontal=8.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Text("Maskotumu rakibime göster",Modifier.weight(1f),color=LobbyWhite128,fontSize=12.sp)
                Switch(checked=shareMaskot,onCheckedChange={ Wq183ArenaShare.setConsent(context,profileKey,it) })
            }
        }
        Text(when {
            allReady -> "✓ İki oyuncu da hazır."''')
s=once(s,'.clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center) {','.heightIn(min=44.dp).clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center) {')
f.write_text(s)
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in repair_protected.items())
test=main.parent/"test/java/com/kelimebulmacasi/a1/PremiumOnlineCompletionRegressionTest.kt"
test.write_text('''package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class PremiumOnlineCompletionRegressionTest {
    private val base=PremiumOnlineMatch("ABCDEF234","h","g","Host","Guest","playing",
        "quiz","board","A1","Tüm konular",5,30,1,"h",0L,emptyMap(),emptyMap())
    @Test fun quizNeedsEveryAnswerFromBothPlayers() {
        val all=(0..4).associateWith { if(it==4)-1 else 0 }
        assertFalse(base.copy(answers=mapOf("h" to all)).complete)
        assertFalse(base.copy(answers=mapOf("h" to all,"g" to (all-4))).complete)
        assertFalse(base.copy(answers=mapOf("h" to all,"g" to ((all-4)+(7 to 1)))).complete)
        assertTrue(base.copy(answers=mapOf("h" to all,"g" to all)).complete)
    }
    @Test fun sharedBoardCompletesOnlyWhenEveryClueIsResolved() {
        val all=(0..4).associateWith { if(it==4)"reveal" else "h" }
        assertFalse(base.copy(mode="team",solvedBy=all-4).complete)
        assertTrue(base.copy(mode="team",solvedBy=all).complete)
        assertTrue(base.copy(mode="duel",solvedBy=all).complete)
        assertFalse(base.copy(mode="team",size=0).complete)
    }
}
''')
print("PASS: targeted completion, answer deduplication, stable room listener and no legacy mascot fallback patch applied")


# Compact independent Arena player cards and incoming finish offer dialog.
f=p/"ArenaReferenceComponents.kt";s=f.read_text()
s=once(s,'import androidx.compose.runtime.*','import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable')
s=once(s,'''    val tablet=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    Row(modifier.heightIn(min=if(tablet)116.dp else 99.dp)''','''    val tablet=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    var expanded by rememberSaveable { mutableStateOf(true) }
    Row(modifier.heightIn(min=if(expanded) { if(tablet)90.dp else 78.dp } else 48.dp)''')
s=once(s,'''        .padding(horizontal=5.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        ArenaMascotV2(avatar,Modifier.size(
            if(tablet) { if(active)85.dp else 73.dp }
            else { if(active)66.dp else 57.dp }))''','''        .clickable(onClickLabel=if(expanded)"Profili küçült" else "Profili genişlet") { expanded=!expanded }
        .padding(horizontal=7.dp,vertical=4.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        if(expanded) ArenaMascotV2(avatar,Modifier.size(
            if(tablet) { if(active)75.dp else 68.dp }
            else { if(active)51.dp else 45.dp }))''')
s=once(s,'''            Text(points.toString(),color=Color.White,fontSize=22.sp,
                fontWeight=FontWeight.ExtraBold)
            if(active) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)
        }
    }
}''','''            Text(points.toString(),color=Color.White,fontSize=if(expanded)22.sp else 16.sp,
                fontWeight=FontWeight.ExtraBold)
            if(active && expanded) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)
        }
        Text(if(expanded)"⌃" else "⌄",color=Color.White,fontSize=14.sp,
            fontWeight=FontWeight.Bold)
    }
}''')
s=once(s,'''    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.horizontalGradient(
            listOf(Color(0xFF28134F),Color(0xFF8B53BB),Color(0xFF422877))))
        .padding(horizontal=5.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(4.dp)) {''','''    val wide=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    Row(Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(if(wide)10.dp else 5.dp)) {''')
s=once(s,'Box(Modifier.size(if(urgent)103.dp else 87.dp)','Box(Modifier.size(if(wide) { if(urgent)91.dp else 78.dp } else { if(urgent)76.dp else 65.dp })')
s=once(s,'fontSize=if(urgent)25.sp else 18.sp','fontSize=if(urgent) { if(wide)24.sp else 20.sp } else 17.sp')
f.write_text(s)
f=p/"PremiumOnlineScreen.kt";s=f.read_text()
s=once(s,'.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),\n        verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else 9.dp)) {',
'''.verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else 5.dp)) {''')
s=once(s,'TextButton(onClick = { if (match != null) confirmRoomExit = true else back() }) {',
'''TextButton(onClick = { if (match != null) confirmRoomExit = true else back() },
                contentPadding=PaddingValues(horizontal=6.dp,vertical=0.dp)) {''')
s=once(s,'Text(gameTitle, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)',
'Text(gameTitle, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)')
s=once(s,'if (match.phase == "finished" || (match.phase == "playing" && match.mode == "quiz")) Wq183OnlinePortraitStrip(',
'if (match.phase == "finished") Wq183OnlinePortraitStrip(')
a=s.index('    // Two scores remain visible while the large settings card is hidden.')
z=s.index('    if (match.cooperative)',a)
s=s[:a]+s[z:]
s=once(s,'                    ArenaScoreStrip(own.size, questions.size, myScore, null)',
'''                    ArenaLiveScoreV2(hostName,guestName,arenaScore(match.hostUid),arenaScore(match.guestUid),
                        false,if(quizDeadline>0) PremiumOnlineLimits.remainingSeconds(quizDeadline,client.serverNow(wallTime)) else null,
                        own.size,questions.size,true,match.hostAvatar,match.guestAvatar)''')
s=once(s,'''                ArenaLiveScoreV2(hostName, guestName,
                    match.score(match.hostUid,questions), match.score(match.guestUid,questions),''',
'''                ArenaLiveScoreV2(hostName, guestName,
                    arenaScore(match.hostUid), arenaScore(match.guestUid),''')
f.write_text(s)
f=p/"Wq186ArenaSocialHud.kt";s=f.read_text()
s=once(s,'    var askFinish by remember(match.code) { mutableStateOf(false) }',
'''    var askFinish by remember(match.code) { mutableStateOf(false) }
    var offerDismissed by remember(match.code) { mutableStateOf(false) }
    var acceptingOffer by remember(match.code) { mutableStateOf(false) }''')
s=once(s,'    val opponent = if (isHost) guestName else hostName',
'''    val opponent = if (isHost) guestName else hostName
    val incomingOffer=match.phase=="playing" && opponentUid.isNotBlank() &&
        opponentUid in votes && myUid !in votes
    LaunchedEffect(incomingOffer) {
        if(!incomingOffer) { offerDismissed=false; acceptingOffer=false }
    }''')
a=s.index('        if (match.phase=="playing" && opponentUid in votes && myUid !in votes) {')
z=s.index('        if (myUid in votes',a)
s=s[:a]+'''        if(incomingOffer && offerDismissed) {
            OutlinedButton(onClick={offerDismissed=false},modifier=Modifier.fillMaxWidth()) {
                Text("Bekleyen bitirme teklifini aç",color=Color(0xFFFFDF9B))
            }
        }
'''+s[z:]
s=once(s,'    if (askFinish) AlertDialog(',
'''    if(incomingOffer && !offerDismissed) AlertDialog(
        onDismissRequest={if(!acceptingOffer)offerDismissed=true},
        title={Text("Maçı bitirme teklifi",fontWeight=FontWeight.Bold)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("$opponent maçı mevcut puanlarla tamamlamak istiyor.")
                Text("$hostName: $hostScore puan · $guestName: $guestScore puan",
                    fontWeight=FontWeight.SemiBold)
                Text("Kabul edersen sonuç ekranı açılır. İstersen oynamaya devam edebilirsin.")
            }
        },
        confirmButton={
            Button(enabled=!acceptingOffer && extra!=null,onClick={
                acceptingOffer=true
                extra?.child("finishVotes")?.child(myUid)?.setValue(true)
                    ?.addOnCompleteListener { task ->
                        acceptingOffer=false
                        if(!task.isSuccessful)notice="Teklif kabul edilemedi. Tekrar dene."
                    }
            }) {Text(if(acceptingOffer)"Kaydediliyor…" else "Kabul et")}
        },
        dismissButton={OutlinedButton(enabled=!acceptingOffer,onClick={offerDismissed=true}) {
            Text("Oyuna devam et")
        }}
    )

    if (askFinish && !incomingOffer) AlertDialog(''')
s=once(s,'if (match.phase=="playing") TextButton(onClick={','if (match.phase=="playing") OutlinedButton(onClick={')
s=once(s,'color=Color(0xFFFFDF9B),fontSize=11.sp)\n            }','color=Color(0xFFFFDF9B),fontSize=12.sp,fontWeight=FontWeight.SemiBold)\n            }')
s=once(s,'Wq2DPortrait(avatar.orEmpty().ifBlank { "piko" },','Wq2DPortrait(avatar.orEmpty(),')
f.write_text(s)
print("PASS: separate collapsible score cards, compact live match header, incoming finish vote dialog")


# Reliable per-participant mascot delivery. Privacy remains explicit opt-in.
f=p/"Wq183MascotStore.kt";s=f.read_text()
s=once(s,'''    fun picture(context: Context, id: String, victory: Boolean): Bitmap? {
        if (!isCustomId(id)) return null
        val token = context.getSharedPreferences(ownerPrefs, Context.MODE_PRIVATE)
            .getString("owner", null) ?: return null''','''    fun picture(context: Context, id: String, victory: Boolean): Bitmap? {
        val token = context.getSharedPreferences(ownerPrefs, Context.MODE_PRIVATE)
            .getString("owner", null) ?: return null
        return pictureForOwner(context,token,id,victory)
    }

    fun pictureForProfile(context:Context,profile:String,id:String,victory:Boolean):Bitmap? =
        if(profile.isBlank()) null else pictureForOwner(context,owner(profile),id,victory)

    private fun pictureForOwner(context:Context,token:String,id:String,victory:Boolean):Bitmap? {
        if (!isCustomId(id)) return null''')
f.write_text(s)
f=p/"Wq183ArenaShare.kt";s=f.read_text()
s=once(s,'import kotlinx.coroutines.Dispatchers','import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.withTimeoutOrNull')
s=once(s,'    private val remote = mutableStateMapOf<String, Bitmap>()','''    private val remote = mutableStateMapOf<String, Bitmap>()
    val delivery = mutableStateOf("")
    suspend fun completeWrite(task:com.google.android.gms.tasks.Task<Void>):Boolean =
        withTimeoutOrNull(8000L) {
            suspendCancellableCoroutine<Boolean> { c ->
                task.addOnCompleteListener { if(c.isActive)c.resume(it.isSuccessful) }
            }
        } ?: false''')
s=once(s,'    Wq128RoomHdBridge(room,side,myId,rivalId,selected,allowed,connected.value)','    Wq128RoomHdBridge(room,side,myId,rivalId,selected,allowed,connected.value,profileKey)')
a=s.index('        val retracted = suspendCancellableCoroutine<Boolean>')
z=s.index('\n    }\n',a)
s=s[:a]+'''        val avatarField = if (side == "host") "hostAvatar" else "guestAvatar"
        val hdField = Wq128RoomHd.field(side ?: return@LaunchedEffect)
        // Change avatar and retract stale previews in a single atomic update.
        val updates=mapOf<String,Any?>(avatarField to selected,myField to null,hdField to null)
        repeat(3) { attempt ->
            if(Wq183ArenaShare.completeWrite(room.updateChildren(updates)))return@LaunchedEffect
            Wq183ArenaShare.delivery.value="Maskot kimliği eşitleniyor…"
            delay(1000L*(attempt+1))
        }
        Wq183ArenaShare.delivery.value="Maskot kimliği gönderilemedi. Bağlantıyı ve Arena izinlerini kontrol et."'''+s[z:]
s=once(s,'''        val node = room.child(myField)
        if (!allowed''','''        val node = room.child(myField)
        if (!allowed''')
s=once(s,'''            node.removeValue()
            node.onDisconnect().cancel()
            return@LaunchedEffect''','''            node.removeValue()
            node.onDisconnect().cancel()
            Wq183ArenaShare.delivery.value=if(!allowed)"Maskot paylaşımı kapalı" else "Maskot eşitleniyor…"
            return@LaunchedEffect''')
s=once(s,'val image = Wq183MascotStore.picture(context, selected, false)','val image = Wq183MascotStore.pictureForProfile(context, profileKey, selected, false)')
a=s.index('        // Register server-side deletion first')
z=s.index('\n    }\n',a)
s=s[:a]+'''        // Register server cleanup first, then observe actual write success.
        repeat(3) { attempt ->
            currentCoroutineContext().ensureActive()
            val registered=Wq183ArenaShare.completeWrite(node.onDisconnect().removeValue())
            if(registered && connected.value &&
                Wq183ArenaShare.completeWrite(node.setValue(value))) {
                Wq183ArenaShare.delivery.value="Maskot rakibinle paylaşıldı"
                return@LaunchedEffect
            }
            Wq183ArenaShare.delivery.value="Maskot gönderimi tekrar deneniyor…"
            delay(1000L*(attempt+1))
        }
        Wq183ArenaShare.delivery.value="Maskot gönderilemedi. Bağlantı veya Firebase izinlerini kontrol et."'''+s[z:]
s=once(s,'''        } ?: return@LaunchedEffect
        // Register server cleanup''','''        } ?: run {
            Wq183ArenaShare.delivery.value="Maskot görseli okunamadı. Maskotu yeniden seç."
            return@LaunchedEffect
        }
        // Register server cleanup''')
f.write_text(s)
f=p/"Wq128RoomHd.kt";s=f.read_text()
s=once(s,'import kotlinx.coroutines.Dispatchers','import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay')
s=once(s,'selected:String,allowed:Boolean,connected:Boolean) {','selected:String,allowed:Boolean,connected:Boolean,profileKey:String) {')
s=once(s,'Wq183MascotStore.picture(context,selected,false)?.let {Wq128RoomHd.encode(selected,it)}','Wq183MascotStore.pictureForProfile(context,profileKey,selected,false)?.let { image ->\n                try { Wq128RoomHd.encode(selected,image) } finally { image.recycle() }\n            }')
a=s.index('        val registered=suspendCancellableCoroutine<Boolean>')
z=s.index('\n    }\n',a)
s=s[:a]+'''        repeat(3) { attempt ->
            currentCoroutineContext().ensureActive()
            if(Wq183ArenaShare.completeWrite(node.onDisconnect().removeValue()) && connected &&
                Wq183ArenaShare.completeWrite(node.setValue(value))) return@LaunchedEffect
            delay(1000L*(attempt+1))
        }'''+s[z:]
f.write_text(s)
f=p/"PremiumOnlineScreen.kt";s=f.read_text()
a=s.index('    LaunchedEffect(match?.code,match?.phase,match?.hostAvatar,match?.guestAvatar,')
z=s.index('    val isHost = client.isHost',a)
s=s[:a]+'''    // Wq183ArenaShareBridge owns avatar updates; avoid competing writes.
'''+s[z:]
f.write_text(s)
f=p/"Wq128PremiumRoomLobby.kt";s=f.read_text()
s=once(s,'''        else ArenaMascotV2(avatar,modifier)''','''        else if(!mine && custom) Box(modifier,contentAlignment=Alignment.Center) {
            Text("Rakibin maskotu bekleniyor",color=Color.White.copy(alpha=.85f),fontSize=13.sp)
        }
        else ArenaMascotV2(avatar,modifier)''')
# Exact insertion after the permission label without altering player card layout.
a=s.index('Text("Maskotumu rakibime göster"')
z=s.index('\n',a)
# Keep the existing label; display status in the lobby row, below the label if it has a Column.
# Use a single line to avoid altering Compose hierarchy.
s=s[:a]+s[a:].replace('Text("Maskotumu rakibime göster"', 'Text(if(Wq183ArenaShare.delivery.value.isBlank())"Maskotumu rakibime göster" else "Maskotumu rakibime göster · "+Wq183ArenaShare.delivery.value',1)
f.write_text(s)
print("PASS: profile-scoped custom mascot decoding, atomic avatar update and acknowledged retries")


# Google Drive app-data backup: explicit consent, stable IDs and merge-only restore.
f=p/"Wq183MascotStore.kt";s=f.read_text()
s=once(s,'fun importPack(context: Context, uri: Uri, profile: String): Item {',
'fun importPack(context: Context, uri: Uri, profile: String, restoredId:String?=null): Item {')
s=once(s,'''            val id = "custom_" + UUID.randomUUID().toString().replace("-", "")
            val target = File(ownedRoot, id)''','''            val id = restoredId ?: ("custom_" + UUID.randomUUID().toString().replace("-", ""))
            require(isCustomId(id))
            val target = File(ownedRoot, id)
            require(!target.exists()) { "Maskot zaten cihazda var" }''')
s=once(s,'    fun rename(context: Context, profile: String, id: String, newName: String): Boolean {','''    fun exportPack(context:Context,profile:String,id:String):File {
        require(exists(context,profile,id))
        val folder=File(root(context,profile),id)
        val out=File.createTempFile("wq-drive-mascot-",".zip",context.cacheDir)
        try {
            java.util.zip.ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zip ->
                val files=folder.walkTopDown().filter { it.isFile }.toList()
                require(files.size<=FILE_MAX)
                for(file in files.sortedBy { it.relativeTo(folder).invariantSeparatorsPath }) {
                    val relative=file.relativeTo(folder).invariantSeparatorsPath
                    require(relative=="manifest.json" || relative=="README_TR.txt" || pathPattern.matches(relative))
                    require(file.length()<=ENTRY_MAX)
                    zip.putNextEntry(java.util.zip.ZipEntry(relative))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            require(out.length() in 1..ZIP_MAX) { "Maskot yedek boyutu sınırı aşıldı" }
            return out
        } catch(e:Exception) { out.delete(); throw e }
    }

    fun rename(context: Context, profile: String, id: String, newName: String): Boolean {''')
f.write_text(s)
f=p/"Wq183MascotManager.kt";s=f.read_text()
s=once(s,'    onlineEligible: Boolean\n)','    onlineEligible: Boolean,\n    driveAuth:com.google.firebase.auth.FirebaseAuth?=null\n)')
s=once(s,'Text("Yalnızca WordQuest Mascot Pack v1 ZIP yüklenir. Asıl ZIP dosyası cihazında kalır.")','''Text("WordQuest Mascot Pack v1 ZIP yüklenir. Drive yedeklemesi isteğe bağlıdır.")
                WqDriveMascotPanel(profileKey,driveAuth) {
                    items=Wq183MascotStore.list(context,profileKey)
                }''')
f.write_text(s)
f=p/"HomeScreen.kt";s=f.read_text()
s=once(s,'                    onlineEligible=session.accountName!=null,\n                    onSelected={id->','                    onlineEligible=session.accountName!=null,\n                    driveAuth=session.accounts.socialAuth(),\n                    onSelected={id->')
f.write_text(s)
gradle=Path("source/app/build.gradle.kts");s=gradle.read_text()
s=once(s,'implementation("androidx.credentials:credentials-play-services-auth:1.5.0")','implementation("androidx.credentials:credentials-play-services-auth:1.5.0")\n    implementation("com.google.android.gms:play-services-auth:22.0.0")')
gradle.write_text(s)
(p/"WqDriveMascot.kt").write_text('''package com.kelimebulmacasi.a1

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

private object WqDriveMascot {
    const val APPDATA="https://www.googleapis.com/auth/drive.appdata"
    const val EMAIL="https://www.googleapis.com/auth/userinfo.email"
    private const val API="https://www.googleapis.com/drive/v3"
    private const val UPLOAD="https://www.googleapis.com/upload/drive/v3"
    private const val LIMIT=25L*1024*1024
    private fun hex(value:String)=MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun prefix(uid:String)="wq-mascot-"+hex(uid).take(24)+"-"
    private fun request(url:String,token:String,method:String="GET"):HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod=method;connectTimeout=15000;readTimeout=45000
            instanceFollowRedirects=false
            setRequestProperty("Authorization","Bearer "+token)
        }
    private fun checked(c:HttpURLConnection) {
        val code=c.responseCode
        require(code in 200..299) {
            when(code) { 401 -> "Google izni sona erdi. Tekrar izin ver."
                403 -> "Google Drive erişimi reddedildi. Drive API ve hesap izinlerini kontrol et."
                429 -> "Google Drive yoğun. Tekrar dene."
                else -> "Google Drive işlemi başarısız (HTTP "+code+")." }
        }
    }
    private fun json(url:String,token:String):JSONObject {
        val c=request(url,token)
        try { checked(c); return JSONObject(c.inputStream.bufferedReader().use { it.readText() }) }
        finally { c.disconnect() }
    }
    fun verifyAccount(token:String,email:String) {
        val info=json("https://www.googleapis.com/oauth2/v3/userinfo",token)
        require(info.optBoolean("email_verified") && info.optString("email").equals(email,true)) {
            "WordQuest ve Google Drive hesapları farklı. Aynı Google hesabını seç."
        }
    }
    private fun files(token:String,uid:String):Map<String,String> {
        val found=linkedMapOf<String,String>()
        var page=""
        var count=0
        do {
            require(++count<=100) { "Yedek listesi çok büyük" }
            val q=Uri.encode("trashed = false")
            val data=json(API+"/files?spaces=appDataFolder&q="+q+
                "&fields=nextPageToken,files(id,name)&pageSize=100"+
                if(page.isEmpty()) "" else "&pageToken="+Uri.encode(page),token)
            val list=data.getJSONArray("files")
            for(i in 0 until list.length()) {
                val f=list.getJSONObject(i);val name=f.optString("name")
                if(!name.startsWith(prefix(uid))||!name.endsWith(".zip"))continue
                val mascot=name.removePrefix(prefix(uid)).removeSuffix(".zip")
                if(!Wq183MascotStore.isCustomId(mascot))continue
                require(found.put(mascot,f.getString("id"))==null) { "Çift yedek kaydı; yerel maskotlar korundu." }
            }
            page=data.optString("nextPageToken")
        } while(page.isNotBlank())
        return found
    }
    private fun upload(token:String,uid:String,id:String,file:File,remote:String?) {
        val boundary="wq"+UUID.randomUUID().toString().replace("-","")
        val meta=JSONObject().put("name",prefix(uid)+id+".zip").put("mimeType","application/zip")
        if(remote==null)meta.put("parents",org.json.JSONArray().put("appDataFolder"))
        val pre=("--"+boundary+"\\\\r\\\\nContent-Type: application/json; charset=UTF-8\\\\r\\\\n\\\\r\\\\n"+meta+
            "\\\\r\\\\n--"+boundary+"\\\\r\\\\nContent-Type: application/zip\\\\r\\\\n\\\\r\\\\n").replace("\\\\r","\\r").replace("\\\\n","\\n").toByteArray()
        val end=("\\\\r\\\\n--"+boundary+"--\\\\r\\\\n").replace("\\\\r","\\r").replace("\\\\n","\\n").toByteArray()
        val url=UPLOAD+"/files"+(remote?.let{"/"+Uri.encode(it)} ?: "")+"?uploadType=multipart&fields=id"
        val c=request(url,token,if(remote==null)"POST" else "PATCH")
        try {
            c.doOutput=true;c.setRequestProperty("Content-Type","multipart/related; boundary="+boundary)
            c.setFixedLengthStreamingMode(pre.size.toLong()+file.length()+end.size)
            c.outputStream.use { out -> out.write(pre);file.inputStream().use{it.copyTo(out)};out.write(end) }
            checked(c);c.inputStream.close()
        } finally { c.disconnect() }
    }
    private fun download(token:String,id:String,target:File) {
        val c=request(API+"/files/"+Uri.encode(id)+"?alt=media",token)
        try {
            checked(c)
            c.inputStream.use { input -> target.outputStream().use { output ->
                val b=ByteArray(32768);var total=0L
                while(true) {
                    val n=input.read(b);if(n<0)break
                    total+=n;require(total<=LIMIT) { "Yedek boyutu sınırı aşıldı" }
                    output.write(b,0,n)
                }
            }}
        } finally { c.disconnect() }
    }
    fun sync(context:Context,profile:String,uid:String,token:String,restore:Boolean,stillOwner:()->Boolean):Int {
        require(stillOwner()) { "Hesap değişti." }
        val remote=files(token,uid)
        var count=0
        if(restore) {
            for((id,fileId) in remote) {
                require(stillOwner()) { "Hesap değişti." }
                if(Wq183MascotStore.exists(context,profile,id))continue
                val temp=File.createTempFile("wq-drive-restore-",".zip",context.cacheDir)
                try {
                    download(token,fileId,temp)
                    require(stillOwner()) { "Hesap değişti." }
                    Wq183MascotStore.importPack(context,Uri.fromFile(temp),profile,id)
                    count++
                } finally { temp.delete() }
            }
        } else {
            for(item in Wq183MascotStore.list(context,profile)) {
                require(stillOwner()) { "Hesap değişti." }
                val temp=Wq183MascotStore.exportPack(context,profile,item.id)
                try {
                    require(stillOwner()) { "Hesap değişti." }
                    upload(token,uid,item.id,temp,remote[item.id]);count++
                } finally { temp.delete() }
            }
        }
        return count
    }
}

@Composable internal fun WqDriveMascotPanel(profile:String,auth:FirebaseAuth?,onRestored:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val user=auth?.currentUser
    val googleEmail=user?.providerData?.firstOrNull { it.providerId=="google.com" }?.email.orEmpty()
    val uid=user?.uid.orEmpty()
    val client=remember(context){Identity.getAuthorizationClient(context)}
    var busy by remember(profile,uid){mutableStateOf(false)}
    var message by remember(profile,uid){mutableStateOf("")}
    var restore by remember(profile,uid){mutableStateOf(false)}
    fun useResult(result:AuthorizationResult) {
        val token=result.accessToken
        if(token.isNullOrBlank()) { busy=false;message="Google Drive izni verilmedi.";return }
        scope.launch {
            try {
                val count=withContext(Dispatchers.IO) {
                    WqDriveMascot.verifyAccount(token,googleEmail)
                    WqDriveMascot.sync(context,profile,uid,token,restore){auth?.currentUser?.uid==uid}
                }
                if(restore)onRestored()
                message=if(restore)"Geri yüklenen maskot: "+count+". Cihazdaki maskotlar korundu."
                    else "Google Drive'a yedeklenen maskot: "+count
            } catch(e:kotlinx.coroutines.CancellationException){throw e}
              catch(e:Exception){message=e.message ?: "Drive işlemi tamamlanamadı."}
            finally { busy=false }
        }
    }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if(result.resultCode!=Activity.RESULT_OK) { busy=false;message="Google Drive izni iptal edildi." }
        else runCatching { client.getAuthorizationResultFromIntent(result.data) }
            .onSuccess { useResult(it) }.onFailure {busy=false;message="Google Drive izni alınamadı."}
    }
    fun start(wantRestore:Boolean) {
        if(busy||googleEmail.isBlank()||uid.isBlank())return
        restore=wantRestore;busy=true;message="Google Drive izni kontrol ediliyor…"
        val request=AuthorizationRequest.builder()
            .setAccount(Account(googleEmail,"com.google"))
            .setRequestedScopes(listOf(Scope(WqDriveMascot.APPDATA),Scope(WqDriveMascot.EMAIL)))
            .build()
        client.authorize(request).addOnSuccessListener { result ->
            if(auth?.currentUser?.uid!=uid){busy=false;message="Hesap değişti.";return@addOnSuccessListener}
            if(result.hasResolution()) {
                val pending=result.pendingIntent
                if(pending==null){busy=false;message="İzin penceresi açılamadı."}
                else launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            } else useResult(result)
        }.addOnFailureListener {busy=false;message="Google Drive izni alınamadı. Google hesabını ve Drive API ayarını kontrol et."}
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("Google Drive maskot yedeği",style=MaterialTheme.typography.titleSmall)
        Text("Yalnızca uygulamanın gizli yedek klasörü kullanılır. Geri yükleme mevcut maskotları silmez.",style=MaterialTheme.typography.bodySmall)
        if(googleEmail.isBlank())Text("Google hesabıyla giriş yap.",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick={start(false)},enabled=!busy&&googleEmail.isNotBlank(),modifier=Modifier.weight(1f)){Text("Yedekle")}
            OutlinedButton(onClick={start(true)},enabled=!busy&&googleEmail.isNotBlank(),modifier=Modifier.weight(1f)){Text("Geri yükle")}
        }
        if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
    }
}
'''.replace('\\\\r\\\\n','\\r\\n'))
print("PASS: optional Drive appData consent and profile-bound bounded mascot backup/restore")
