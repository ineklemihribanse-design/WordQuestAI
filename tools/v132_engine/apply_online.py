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
        snapshot: String? = null, selectedPreset: Wq186MatchPreset? = null) {
        val preset=(selectedPreset ?: Wq186ArenaPresetStore.read(context,session.profileKey,level)).sanitized(level)
        if(snapshot!=null && runCatching{Wq132WebEngine.snapshotId(snapshot)}.getOrNull()!=boardId) {
            status="Ortak oyun verisi geçersiz.";return
        }
        if(snapshot==null && (boardId.startsWith("web:") || preset.levels.size>1)) {
            status="Karışık seviye için ortak web oyunu gerekli.";return
        }
        val actualLevel=level
        val actualBoardId=boardId
'''+s[b:]
s=once(s,'database.reference.updateChildren(mapOf<String, Any>("roomQuotaV2/$myUid/$day/$slot" to code, "premiumRooms/$code" to initial))','''database.reference.updateChildren(mapOf<String, Any>("roomQuotaV2/$myUid/$day/$slot" to code,
                        "premiumRooms/$code" to (initial + if(snapshot!=null)mapOf("roundData" to snapshot) else emptyMap())))''')
a=s.index("    fun toggleReady()");b=s.index("    /**",a)
s=s[:a]+'''    fun toggleReady() {
        val m=match ?: return
        if(m.phase!="lobby" || uid !in listOf(m.hostUid,m.guestUid) || busy)return
        val room=ref ?: return
        val acknowledgement: Any?=if(uid in m.readyBy)null else m.configRevision
        fun submitReady(){
            val current=match ?: return
            if(current.code!=m.code||current.configRevision!=m.configRevision||current.phase!="lobby"){
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
                        val requestedSize=size;val requestedSeconds=if(requestedMode=="team")0 else seconds
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
