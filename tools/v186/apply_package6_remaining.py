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
        """        Wq174UnifiedMatchSettings(
            profile=avatarProfileKey,""",
        "pass profile to one unified Arena settings card")
    store("ArenaApprovedHome.kt",s)

    s=load("WordQuest174Arena.kt")
    s=once(s,
        '''internal fun Wq174UnifiedMatchSettings(
    solo:Boolean''',
        '''internal fun Wq174UnifiedMatchSettings(
    profile:String="",
    solo:Boolean''',
        "profile-scoped settings without changing other callers")
    s=once(s,
        '''            Wq174SettingRow("level","Seviye Seçiniz",
                level.ifBlank{"Seçiniz"},PremiumArenaRules.allowedLevels,
                onSelect=onLevel)''',
        '''            if(solo) {
                Wq174SettingRow("level","Seviye Seçiniz",
                    level.ifBlank{"Seçiniz"},PremiumArenaRules.allowedLevels,
                    onSelect=onLevel)
            } else {
                // Online modes share a level pool. No second single-level picker.
                Wq186ArenaPresetControls(profile,level,onLevel)
            }''',
        "remove redundant single-level picker from online game")
    s=once(s,
        '''            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFF5DB)).padding(11.dp),
                verticalAlignment=Alignment.CenterVertically){
                Wq174SettingIcon("hint")
                Spacer(Modifier.width(9.dp))
                Text("İpucu",modifier=Modifier.weight(1f),
                    color=Ink174,fontWeight=FontWeight.Bold)
                Text("3 hak · maç kurallarına bağlı",color=Color(0xFF6B438D),
                    fontSize=12.sp,fontWeight=FontWeight.SemiBold)
            }''',
        '''            if(solo) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFF5DB)).padding(11.dp),
                verticalAlignment=Alignment.CenterVertically){
                Wq174SettingIcon("hint")
                Spacer(Modifier.width(9.dp))
                Text("İpucu",modifier=Modifier.weight(1f),
                    color=Ink174,fontWeight=FontWeight.Bold)
                Text("Oyun kurallarına bağlı",color=Color(0xFF6B438D),
                    fontSize=12.sp,fontWeight=FontWeight.SemiBold)
            }''',
        "remove duplicated hint card for online Arena")
    s=once(s,
        '''            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(
                    Color(0xFFEBD9FF),Color(0xFFF7EFFF))))
                .padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){''',
        '''            var detailsOpen by remember { mutableStateOf(false) }
            TextButton(onClick={detailsOpen=!detailsOpen},
                modifier=Modifier.align(Alignment.End)) {
                Text(if(detailsOpen) "ⓘ Maç kurallarını gizle"
                    else "ⓘ Maç kuralları",fontSize=12.sp,color=Ink174)
            }
            if(detailsOpen) Column(Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(
                    Color(0xFFEBD9FF),Color(0xFFF7EFFF))))
                .padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){''',
        "collapse explanatory rules into compact native info control")
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
    question='PremiumQuestion("${target.meaning} — İngilizcesi hangisidir?", choices, choices.indexOf(target.answer), target.answer)'
    assert s.count(question)==2,"P6 expected one seeded and one offline quiz question constructor"
    s=s.replace(question,
        'PremiumQuestion("${target.meaning} — İngilizcesi hangisidir?", choices, choices.indexOf(target.answer), target.answer, target.level)')

    store("PremiumArenaContent.kt",s)

def screen():
    s=load("PremiumOnlineScreen.kt")
    s=once(s,
        'Wq183ArenaBackdrop(\n            Modifier.matchParentSize(), active = match?.phase == "playing")',
        '''if(match==null) {
            Box(Modifier.matchParentSize().background(Color.White))
        } else Wq183ArenaBackdrop(
            Modifier.matchParentSize(), active = match.phase == "playing")''',
        "pure white selector background without changing tournament art in matches")
    s=once(s,
        'Text("‹ Geri", color = OnlineGold, fontWeight = FontWeight.SemiBold)',
        'Text("‹ Geri", color = if(match==null) Color(0xFF312158) else OnlineGold, fontWeight = FontWeight.SemiBold)',
        "readable back action on white lobby")
    s=once(s,
        'color = OnlineGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)',
        'color = if(match==null) Color(0xFF312158) else OnlineGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)',
        "readable white-background Arena title")
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
    s=once(s,'    var status by mutableStateOf("")\n        private set',
        '''    var status by mutableStateOf("")
        private set
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
    marker='"size" to size, "seconds" to seconds, "boardId" to boardId,'
    assert s.count(marker)==2 and s.index(marker)>s.index('val initial = mapOf<String, Any>('),"P6 source needs the original room create map"
    s=s.replace(marker,
        '"size" to size, "seconds" to seconds, "boardId" to actualBoardId,',1)

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
    if 'hintIndex:Int=-1, hintChar:Char?=null, onSelect:(Int)->Unit' in s:
        s=once(s,
            'hintIndex:Int=-1, hintChar:Char?=null, onSelect:(Int)->Unit',
            'hintIndex:Int=-1, hintChar:Char?=null, hintLetters:Map<Int,Char> = emptyMap(), onSelect:(Int)->Unit',
            "P1 responsive board accepts multiple hint positions")
    else:
        s=once(s,
            'hintIndex:Int=-1,hintChar:Char?=null,onSelect:(Int)->Unit',
            'hintIndex:Int=-1,hintChar:Char?=null,hintLetters:Map<Int,Char> = emptyMap(),onSelect:(Int)->Unit',
            "legacy board accepts multiple hint positions")
    if 'val hinted=active && address==hintCell && hintChar!=null' in s:
        # Package 1 replaced the old board with a responsive, zoomable
        # viewport and renamed 'place' to 'address'. Keep that geometry
        # unchanged and layer P6's multiple server-authorized hint cells.
        s=once(s,
            'val hinted=active && address==hintCell && hintChar!=null',
            '''val hintPosition=chosen?.cells?.indexOf(address) ?: -1
                            val hinted=active && (
                                (address==hintCell && hintChar!=null) ||
                                (hintPosition>=0 && hintLetters.containsKey(hintPosition))
                            )''',
            "retain Package 1 cell coordinates with multi-position P6 hints")
        s=once(s,
            '''val visibleLetter = fixed[address] ?:
                                        (if(hinted)hintChar else if(active)draft[address] else null)''',
            '''val visibleLetter = fixed[address] ?:
                                        if(active) (
                                            hintLetters[hintPosition]
                                                ?: if(address==hintCell && hintChar!=null) hintChar
                                                   else draft[address]
                                        ) else null''',
            "solved > claimed hint > single hint > draft on P1 responsive board")
        assert "val cell=Wq186ArenaGridPolicy.cellDp" in s
        assert "val visibleLetter = fixed[address]" in s
    else:
        # Original pre-P1 board variant, kept for independent P6 QA.
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
    # Two approved source shapes: P6-only uses Wq186ArenaNext.choose,
    # while the cumulative P1+P6 build uses a server-authorized selector.
    # Never silently drop P1's server snapshot / playable-clue predicate.
    legacy_auto="wordIndex = Wq186ArenaNext.choose(match,myUid,wordIndex)"
    p1_auto='''wordIndex = Wq186ArenaNextWord.next(
                    match.code,myUid,match.size,match.solvedBy.keys,wordIndex) {
                    index->PremiumOnlineLimits.canAttempt(match,index,myUid)
                } ?: wordIndex'''
    if legacy_auto in s:
        s=once(s,legacy_auto,
            "wordIndex = Wq186ArenaNext.choose(match,myUid,wordIndex,client.package6Rules.retries,client.package6Retries)",
            "legacy next clue respects retry budget")
    elif p1_auto in s:
        s=once(s,p1_auto,'''wordIndex = Wq186ArenaNextWord.next(
                    match.code,myUid,match.size,match.solvedBy.keys,wordIndex) {
                    index->PremiumOnlineLimits.canAttempt(match,index,myUid)
                } ?: Wq186ArenaNext.choose(
                    match,myUid,wordIndex,client.package6Rules.retries,client.package6Retries)''',
            "P1 server-authorized next word with P6 bounded final-round fallback")
        # P1's snapshot listener must not erase a legitimate final-round
        # retry answer when its legacy canAttempt guard still says 'failed'.
        p1_guard='''            !PremiumOnlineLimits.canAttempt(latest,wordIndex,myUid)) {'''
        p1_retry_guard='''            !PremiumOnlineLimits.canAttempt(latest,wordIndex,myUid) &&
            !(PremiumOnlineLimits.gaveUpOrFailed(latest,wordIndex,myUid) &&
                wordIndex !in latest.solvedBy.keys &&
                (client.package6Retries[wordIndex] ?: 0) < client.package6Rules.retries &&
                (0 until latest.size).none { index ->
                    index !in latest.solvedBy.keys &&
                        !PremiumOnlineLimits.gaveUpOrFailed(latest,index,myUid)
                })) {'''
        s=once(s,p1_guard,p1_retry_guard,
            "P1 snapshot listener retains P6 authorized replay draft")
    else:
        raise AssertionError("P6 automatic next clue differs from both approved P1 and P6 shapes")
    legacy_skip="setWordIndex(Wq186ArenaNext.choose(match,myUid,selected))"
    if legacy_skip in s:
        s=once(s,legacy_skip,
            "setWordIndex(Wq186ArenaNext.choose(match,myUid,selected,client.package6Rules.retries,client.package6Retries))",
            "legacy skip respects retries")
    else:
        # P1 skip stays snapshot-authorized and is advanced only after
        # the network transaction commits in apply_package6_to_186.py.
        assert s.count("setWordIndex(nextPlayable)")==1, (
            "P1 network-committed skip callback must keep nextPlayable")
    # Keep any later turn/deadline validations while changing the unknown rule.
    import re
    matches=list(re.finditer(r'(?m)^([ \t]*)val canAnswer\s*=\s*([^\n]+)$',s))
    if len(matches)!=1:
        raise AssertionError("P6 expected one online crossword canAnswer rule")
    rule=matches[0]
    indent=rule.group(1)
    expression=rule.group(2).strip()
    if "myTurn" not in expression or "countdown" not in expression or expression.endswith("&&"):
        raise AssertionError("P6 changed online turn guard: "+expression)
    # Unknown words are replayable ONLY in the final round within the agreed budget.
    expression=re.sub(r'\s*&&\s*!alreadyUnknown\b','',expression)
    replacement=(
        indent+'val previouslyFailed=PremiumOnlineLimits.gaveUpOrFailed(match,selected,myUid)\n'+
        indent+'val pendingFresh=(0 until match.size).any { index ->\n'+
        indent+'    index !in solved &&\n'+
        indent+'        !PremiumOnlineLimits.gaveUpOrFailed(match,index,myUid)\n'+
        indent+'}\n'+
        indent+'val replayAllowed=(!previouslyFailed ||\n'+
        indent+'    (!pendingFresh && (client.package6Retries[selected] ?: 0) <\n'+
        indent+'        client.package6Rules.retries))\n'+
        indent+'val canAnswer=('+expression+') && replayAllowed'
    )
    s=s[:rule.start()]+replacement+s[rule.end():]
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
    # The approved v183-v185 may have moved scores into the central scoreboard.
    # Hide only a uniquely identified redundant Row, never game controls.
    host_token='Text("$hostName'
    if s.count(host_token)==1:
        at=s.index(host_token)
        begin=s.rfind("    Row(",0,at)
        if begin>=0 and at-begin<420 and "SpaceBetween" in s[begin:at]:
            s=s[:begin]+'    if(match.phase!="playing") '+s[begin+4:]
            print("PASS: redundant Arena score row hidden")
        else:
            print("KEEP: approved compact Arena scoreboard")
    else:
        print("KEEP: approved Arena player labels")
    team='Text("Takım: ${solved.size * 10} puan"'
    if s.count(team)==1:
        s=s.replace(team,
            'Text("Takım: ${arenaScore(match.hostUid)+arenaScore(match.guestUid)} puan"',1)
    elif s.count(team)>1:
        raise AssertionError("P6 ambiguous cooperative score label")
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
