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
