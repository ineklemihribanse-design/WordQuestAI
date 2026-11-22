#!/usr/bin/env python3
"""Package 6 follow-up. Only modifies fully assembled P4/P5/P6 QA sources."""
from pathlib import Path
import json, shutil

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
P6=ROOT/"tools/v186/package6"
CHANGES={}

def load(name):
    p=APP/name
    if not p.is_file(): raise AssertionError("Missing "+name)
    return CHANGES.get(p,p.read_text("utf-8"))
def store(name,s): CHANGES[APP/name]=s
def once(s,old,new,label):
    n=s.count(old)
    if n!=1: raise AssertionError(f"P6 {label}: expected 1 anchor, got {n}")
    return s.replace(old,new,1)

def ui():
    s=load("ArenaApprovedHome.kt")
    s=once(s,"        Wq174UnifiedMatchSettings(",
        """        Wq186ArenaPresetControls(
            profile=avatarProfileKey,selectedLevel=level,onLevel=onLevel
        )
        Wq174UnifiedMatchSettings(""","visible multilevel settings")
    store("ArenaApprovedHome.kt",s)
    s=load("WordQuest174Arena.kt")
    s=once(s,'"3 hak · maç kurallarına bağlı"',
        '"İpucu sınırı maç ayarlarından seçilir"',"old fixed hint label")
    store("WordQuest174Arena.kt",s)

def packs():
    s=load("PremiumArenaContent.kt")
    s=once(s,'val correct: Int, val answer: String)',
        'val correct: Int, val answer: String, val level: String = "A1")',"question level")
    s=once(s,'    fun nextPuzzle(level: String, size: Int, topic: String, profile: String): PremiumArenaBoard? {',
        '''    fun levelForWord(answer:String,fallback:String):String {
        val hit=vocabulary.firstOrNull { it.answer.equals(answer,ignoreCase=true) && it.level==fallback }
            ?: vocabulary.firstOrNull { it.answer.equals(answer,ignoreCase=true) }
        return hit?.level ?: fallback
    }

    fun nextPuzzle(level: String, size: Int, topic: String, profile: String): PremiumArenaBoard? {''',
        "crossword word CEFR lookup")
    s=once(s,
        'fun questionsForSeed(level: String, topic: String, seed: Int, size: Int = 12): List<PremiumQuestion> {',
        'fun questionsForSeed(level: String, topic: String, seed: Int, size: Int = 12, levelPool: List<String> = listOf(level)): List<PremiumQuestion> {',
        "multilevel quiz signature")
    s=once(s,'val eligible = vocabulary.filter { PremiumArenaRules.matchesLevel(it.level, level) }',
        'val eligible = vocabulary.filter { lex -> levelPool.any { PremiumArenaRules.matchesLevel(lex.level, it) } }',
        "multiple CEFR vocabulary")
    s=once(s,'PremiumQuestion("${target.meaning} — İngilizcesi hangisidir?", choices, choices.indexOf(target.answer), target.answer)',
        'PremiumQuestion("${target.meaning} — İngilizcesi hangisidir?", choices, choices.indexOf(target.answer), target.answer, target.level)',
        "individual answer level")
    store("PremiumArenaContent.kt",s)

def screen():
    s=load("PremiumOnlineScreen.kt")
    s=once(s,'remember(match.code, match.quizSeed, match.level, match.topic, match.size)',
        'remember(match.code, match.quizSeed, match.level, match.topic, match.size, client.package6Rules.levels)',
        "mixed quiz cache")
    s=once(s,'packs.questionsForSeed(match.level, match.topic, match.quizSeed, match.size)',
        'packs.questionsForSeed(match.level, match.topic, match.quizSeed, match.size, client.package6Rules.levels)',
        "same quiz on both phones")
    s=once(s,'q.options.getOrNull(q.correct)?.count(Char::isLetter) ?: 4,\n                        match.level',
        'q.answer.count(Char::isLetter),\n                        q.level',
        "quiz points weighted by level")
    s=once(s,'{ _ -> match.level },match.mistakesBy',
        '{ index -> board?.puzzle?.words?.getOrNull(index)?.entry?.answer?.let { packs.levelForWord(it,match.level) } ?: match.level },match.mistakesBy',
        "crossword points weighted by level")
    store("PremiumOnlineScreen.kt",s)

def room():
    s=load("PremiumOnlineRoom.kt")
    s=once(s,'    var status by mutableStateOf("")',
        '''    var status by mutableStateOf("")
    var package6Rules by mutableStateOf(Wq186MatchPreset(listOf("A1")))
        private set''',"room preset state")
    s=once(s,'        refreshQuotaForToday()\n        if (!canCreate)',
        '''        val preset=Wq186ArenaPresetStore.read(context,session.profileKey,level)
        // Select ONE compatible prebuilt crossword from the chosen levels.
        // Quiz questions are actually mixed across the entire level pool.
        val picked=if(mode=="quiz") null else {
            val content=PremiumArenaContent(context)
            preset.levels.shuffled().firstNotNullOfOrNull { chosen ->
                content.nextPuzzle(chosen,size,topic,session.profileKey)
            }
        }
        val actualLevel=picked?.level ?: level
        val actualBoardId=picked?.id ?: boardId
        refreshQuotaForToday()
        if (!canCreate)''',"persist room selection")
    s=once(s,'if (!PremiumOnlineLimits.validConfig(level, size, seconds, mode) ||\n            (mode != "quiz" && boardId.isBlank()))',
        'if (!PremiumOnlineLimits.validConfig(actualLevel, size, seconds, mode) ||\n            (mode != "quiz" && actualBoardId.isBlank()))',
        "validate selected board")
    s=once(s,'"phase" to "lobby", "level" to level, "topic" to topic.take(75),',
        '"phase" to "lobby", "level" to actualLevel, "topic" to topic.take(75),',
        "save board level")
    s=once(s,'"size" to size, "seconds" to seconds, "boardId" to boardId,',
        '"size" to size, "seconds" to seconds, "boardId" to actualBoardId,',
        "save board id")
    s=once(s,'''"quotaSlot" to slot,
                )''',
        '''"quotaSlot" to slot,
                    "package6" to mapOf(
                        "settings" to mapOf(
                            "levels" to preset.levels.joinToString(","),
                            "hints" to preset.hints,
                            "retries" to preset.retries
                        )
                    )
                )''',"save settings with Firebase room")
    s=once(s,'                match = PremiumOnlineMatch(code,',
        '''                package6Rules=Wq186MatchPreset.fromRoom(
                    snapshot.child("package6").child("settings"), childString("level")
                )
                match = PremiumOnlineMatch(code,''',
        "read same settings for both players")
    store("PremiumOnlineRoom.kt",s)

def hint_network():
    s=load("PremiumOnlineScreen.kt")
    s=once(s,
        "                val revealedChar=word.entry.letters.getOrNull(revealedIndex)",
        """                val revealedChar=word.entry.letters.getOrNull(revealedIndex)
                val claimedHints=Wq186ArenaHintNetwork.claims(
                    session,match.code,myUid
                )
                val hintLetters=claimedHints.filter{it.word==selected}
                    .mapNotNull { claim ->
                        word.entry.letters.getOrNull(claim.position)?.let {
                            claim.position to it
                        }
                    }.toMap()""",
        "read all claimed hint letters")
    a=s.index('                    if(match.rulesVersion>=2 && match.mode=="duel" && !match.cooperative) {')
    b=s.index('\n                    }\n',a)+len('\n                    }')
    old=s[a:b]
    if old.count("Wq181ArenaHintBar(")!=1 or "ArenaDuelEngine.hintsFor(" not in old:
        raise AssertionError("P6 prior hint card differs")
    new='''                    if(match.rulesVersion>=2 && match.isCrossword) {
                        Wq186ArenaHintAction(
                            left=Wq186ArenaFairPlay.hintsLeft(
                                claimedHints.size,client.package6Rules.hints
                            ),
                            revealed=hintLetters,
                            canUse=(myTurn && (countdown==null || countdown>0)
                                && selected !in solved && fixedPattern.contains('.')),
                            onHint={
                                Wq186ArenaHintNetwork.reveal(
                                    session,match.code,myUid,selected,fixedPattern,
                                    client.package6Rules.hints
                                ) { issue -> if(issue != null)client.note(issue) }
                            }
                        )
                    }'''
    s=s[:a]+new+s[b:]
    s=once(s,'answerText,hintIndex=revealedIndex,hintChar=revealedChar) {',
        'answerText,hintIndex=revealedIndex,hintChar=revealedChar,hintLetters=hintLetters) {',
        "pass all revealed letters to real crossword board")
    store("PremiumOnlineScreen.kt",s)

    s=load("ArenaCrosswordTyping.kt")
    s=once(s,
        'hintChar:Char?=null, onSelect: (Int)->Unit',
        'hintChar:Char?=null,hintLetters:Map<Int,Char> = emptyMap(), onSelect: (Int)->Unit',
        "typing forwards multi-letter hint state")
    s=once(s,
        'ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,onSelect)',
        'ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,hintLetters,onSelect)',
        "actual grid receives all hint letters")
    store("ArenaCrosswordTyping.kt",s)

    s=load("ArenaBoardV2.kt")
    s=once(s,
        'hintIndex:Int=-1,hintChar:Char?=null,onSelect:(Int)->Unit',
        'hintIndex:Int=-1,hintChar:Char?=null,hintLetters:Map<Int,Char> = emptyMap(),onSelect:(Int)->Unit',
        "board accepts multiple hint positions")
    s=once(s,
        'val hinted=active && place==hintCell && hintChar!=null',
        'val hinted=active && ((place==hintCell && hintChar!=null) || hintLetters.containsKey(chosen?.cells?.indexOf(place) ?: -1))',
        "each hint cell uses gold highlight")
    s=once(s,
        'Text((fixed[place]?:if(hinted)hintChar else if(active)draft[place] else null)',
        'Text((fixed[place]?:if(active) (hintLetters[chosen?.cells?.indexOf(place) ?: -1] ?: if(hinted)hintChar else draft[place]) else null)',
        "multiple revealed letters printed inside grid")
    store("ArenaBoardV2.kt",s)

def replays():
    s=load("PremiumOnlineScreen.kt")
    s=once(s,
        "wordIndex=Wq186ArenaNext.choose(m,myUid,-1)",
        "wordIndex=Wq186ArenaNext.choose(m,myUid,-1,client.package6Rules.retries,client.package6Retries)",
        "opening clue respects retry budget")
    s=once(s,
        "wordIndex = Wq186ArenaNext.choose(match,myUid,wordIndex)",
        "wordIndex = Wq186ArenaNext.choose(match,myUid,wordIndex,client.package6Rules.retries,client.package6Retries)",
        "next clue respects retry budget")
    s=once(s,
        "setWordIndex(Wq186ArenaNext.choose(match,myUid,selected))",
        "setWordIndex(Wq186ArenaNext.choose(match,myUid,selected,client.package6Rules.retries,client.package6Retries))",
        "skip does not revisit exhausted clue")
    s=once(s,
        'val canAnswer = myTurn && (countdown == null || countdown > 0) && !alreadyUnknown',
        '''val previouslyFailed=PremiumOnlineLimits.gaveUpOrFailed(match,selected,myUid)
                        val pendingFresh=(0 until match.size).any { index ->
                            index !in solved &&
                                !PremiumOnlineLimits.gaveUpOrFailed(match,index,myUid)
                        }
                        val replayAllowed=(!previouslyFailed ||
                            (!pendingFresh && (client.package6Retries[selected] ?: 0) <
                                client.package6Rules.retries))
                        val canAnswer=myTurn && (countdown==null || countdown>0) &&
                            replayAllowed''',
        "allow last-round retry only within budget")
    s=once(s,
        '        client.syncFailedAttemptReveals()\\n    }'.replace('\\n','\n'),
        '''        client.syncFailedAttemptReveals()
    }
    LaunchedEffect(match?.code,match?.unknownBy,match?.mistakesBy,
        match?.solvedBy,client.package6Retries,myUid) {
        val current=match
        if(current!=null && current.phase=="playing" && current.isCrossword &&
            PremiumOnlineLimits.gaveUpOrFailed(current,wordIndex,myUid) &&
            (0 until current.size).any { index ->
                index !in current.solvedBy &&
                    !PremiumOnlineLimits.gaveUpOrFailed(current,index,myUid)
            }) {
            wordIndex=Wq186ArenaNext.choose(current,myUid,wordIndex,
                client.package6Rules.retries,client.package6Retries)
            answerText=""
        }
    }''',
        "automatically defer failed clues rather than trapping the same word")
    s=once(s,"client.markSolved(selected)","client.submitWithRetry(selected,true)",
        "correct answer consumes replay")
    s=once(s,"client.wrong(selected)","client.submitWithRetry(selected,false)",
        "wrong answer consumes replay")
    store("PremiumOnlineScreen.kt",s)
    s=load("PremiumOnlineRoom.kt")
    s=once(s,
        '''    var package6Rules by mutableStateOf(Wq186MatchPreset(listOf("A1")))
        private set''',
        '''    var package6Rules by mutableStateOf(Wq186MatchPreset(listOf("A1")))
        private set
    var package6Retries by mutableStateOf<Map<Int,Int>>(emptyMap())
        private set''',"room replay counters")
    s=once(s,
        '''                package6Rules=Wq186MatchPreset.fromRoom(
                    snapshot.child("package6").child("settings"), childString("level")
                )''',
        '''                package6Rules=Wq186MatchPreset.fromRoom(
                    snapshot.child("package6").child("settings"), childString("level")
                )
                package6Retries=snapshot.child("package6").child("retrySpent").child(uid)
                    .children.mapNotNull { e ->
                        val n=e.key?.toIntOrNull()
                        val count=(e.value as? Number)?.toInt()
                        if(n!=null && count!=null)n to count else null
                    }.toMap()''',"server retry counters")
    s=once(s,"    fun pass() {",'''    fun submitWithRetry(index:Int,correct:Boolean) {
        val m=match ?:return
        if(m.phase!="playing" || !m.isCrossword || index !in 0 until m.size)return
        val failed=PremiumOnlineLimits.gaveUpOrFailed(m,index,uid)
        fun submit() { if(correct)markSolved(index) else wrong(index) }
        if(!failed) { submit();return }
        val limit=package6Rules.retries
        if(limit==0) {status="Bu kelimede tekrar hakkın yok.";return}
        val attempt=ref?.child("package6")?.child("retrySpent")
            ?.child(uid)?.child(index.toString()) ?:return
        attempt.runTransaction(object:Transaction.Handler {
            override fun doTransaction(data:MutableData):Transaction.Result {
                val used=(data.value as? Number)?.toInt() ?: 0
                if(used>=limit)return Transaction.abort()
                data.value=used+1
                return Transaction.success(data)
            }
            override fun onComplete(error:DatabaseError?,committed:Boolean,latest:DataSnapshot?) {
                if(error!=null)status="Tekrar Firebase'de kaydedilemedi: ${error.message}"
                else if(!committed)status="Bu kelimedeki tekrar hakkın doldu."
                else submit()
            }
        },false)
    }

    fun pass() {''',"guard repeated submission with RTDB transaction")
    store("PremiumOnlineRoom.kt",s)

def compact_live_hud():
    s=load("PremiumOnlineScreen.kt")
    s=once(s,
        '''    // Two scores remain visible while the large settings card is hidden.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {''',
        '''    // Score is already inside the real purple match scoreboard.
    // Do not display a second host/guest line above it during live play.
    if(match.phase!="playing") Row(Modifier.fillMaxWidth(),
        horizontalArrangement=Arrangement.SpaceBetween) {''',
        "hide redundant score names during live Arena")
    s=once(s,
        'Text("Takım: ${solved.size * 10} puan"',
        'Text("Takım: ${arenaScore(match.hostUid)+arenaScore(match.guestUid)} puan"',
        "cooperative team score uses same weighted formula")
    store("PremiumOnlineScreen.kt",s)

def rules():
    source=ROOT/"source/database.rules.json"
    official=ROOT/"firebase/database.rules.json"
    obj=json.loads(source.read_text("utf-8"))
    want=json.loads(official.read_text("utf-8"))["rules"]["premiumRooms"]["$code"]["package6"]["settings"]
    sub=obj["rules"]["premiumRooms"]["$code"]["package6"]
    if "settings" in sub: raise AssertionError("P6 duplicate RTDB rules")
    sub["settings"]=want
    sub["hintClaims"]=json.loads(official.read_text("utf-8"))["rules"]["premiumRooms"]["$code"]["package6"]["hintClaims"]
    sub["retrySpent"]=json.loads(official.read_text("utf-8"))["rules"]["premiumRooms"]["$code"]["package6"]["retrySpent"]
    # Apply the v1.0.86 finish gate only now; never defeat historical v178 audit.
    historic=obj["rules"]["premiumRooms"]["$code"]["phase"][".write"]
    future=json.loads(official.read_text("utf-8"))["rules"]["premiumRooms"]["$code"]["phase"][".write"]
    assert "data.val() === 'playing' && newData.val() === 'finished'" in historic
    assert "finishVotes" in future and "numChildren()" in future
    assert future.startswith(historic.split("(data.val() === 'playing'")[0])
    obj["rules"]["premiumRooms"]["$code"]["phase"][".write"]=future
    CHANGES[source]=json.dumps(obj,indent=2,ensure_ascii=False)+"\n"

def main():
    assert (APP/"Wq186ArenaFairPlay.kt").is_file()
    assert not (APP/"Wq186ArenaPresetControls.kt").exists()
    assert not (APP/"Wq186ArenaHintNetwork.kt").exists()
    ui()
    packs()
    screen()
    room()
    hint_network()
    replays()
    compact_live_hud()
    rules()
    for p,s in CHANGES.items():
        p.write_text(s,"utf-8")
        print("PASS P6",p.relative_to(ROOT))
    shutil.copyfile(P6/"Wq186ArenaPresetControls.kt",APP/"Wq186ArenaPresetControls.kt")
    shutil.copyfile(P6/"Wq186ArenaHintNetwork.kt",APP/"Wq186ArenaHintNetwork.kt")
    print("P6 follow-up source staged without deployment/release.")
if __name__=="__main__": main()
