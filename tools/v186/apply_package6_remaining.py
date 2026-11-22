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

def rules():
    source=ROOT/"source/database.rules.json"
    official=ROOT/"firebase/database.rules.json"
    obj=json.loads(source.read_text("utf-8"))
    want=json.loads(official.read_text("utf-8"))["rules"]["premiumRooms"]["$code"]["package6"]["settings"]
    sub=obj["rules"]["premiumRooms"]["$code"]["package6"]
    if "settings" in sub: raise AssertionError("P6 duplicate RTDB rules")
    sub["settings"]=want
    CHANGES[source]=json.dumps(obj,indent=2,ensure_ascii=False)+"\n"

def main():
    assert (APP/"Wq186ArenaFairPlay.kt").is_file()
    assert not (APP/"Wq186ArenaPresetControls.kt").exists()
    ui()
    packs()
    screen()
    room()
    rules()
    for p,s in CHANGES.items():
        p.write_text(s,"utf-8")
        print("PASS P6",p.relative_to(ROOT))
    shutil.copyfile(P6/"Wq186ArenaPresetControls.kt",APP/"Wq186ArenaPresetControls.kt")
    print("P6 follow-up source staged without deployment/release.")
if __name__=="__main__": main()
