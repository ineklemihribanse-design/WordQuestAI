#!/usr/bin/env python3
"""v1.0.79: compact/zoomable Arena, random clue order, sounds, score emphasis.

This runs LAST, after v178. Version and package/signature continuity are guarded.
No Firebase write schema changes are required; ready/rules remain v178.
"""
from pathlib import Path
import shutil
import re

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v179"

def once(s,old,new,why):
    n=s.count(old)
    if n!=1:
        raise AssertionError(f"{why}: expected one occurrence, got {n}: {old[:110]!r}")
    return s.replace(old,new,1)

def write(name,func):
    f=SRC/name
    old=f.read_text("utf-8")
    new=func(old)
    assert new!=old,name
    f.write_text(new,"utf-8")
    print("PASS: v179 patched",name)

def online(s):
    s=once(s,
      '    LaunchedEffect(match?.code, match?.solvedBy?.size, match?.phase) {',
      '''    // Stable shuffled order instead of always giving the first word.
    LaunchedEffect(match?.code, match?.phase) {
        val m=match
        if(m!=null && m.phase=="playing" && m.isCrossword) {
            wordIndex=Wq179ArenaOrder.next(m.code,myUid,m.size,m.solvedBy.keys,-1)
            answerText=""
        }
    }
    LaunchedEffect(match?.code, match?.solvedBy?.size, match?.phase) {''',
      "shuffled opening clue")
    s=once(s,
      'wordIndex = (0 until match.size).firstOrNull { it !in match.solvedBy } ?: wordIndex',
      'wordIndex = Wq179ArenaOrder.next(match.code,myUid,match.size,match.solvedBy.keys,wordIndex)',
      "shuffled next unsolved clue")
    # Match composable has its own context and receives myUid as a parameter.
    anchor='    val context = LocalContext.current\n    val board = remember(match.boardId) {'
    s=once(s,anchor,
      '''    val context = LocalContext.current
    var freeSkipUsed by rememberSaveable(match.code,myUid) {
        mutableStateOf(Wq179ArenaSkip.used(context,match.code,myUid))
    }
    val board = remember(match.boardId) {''',
      "one persisted free change per user/room")
    clue='                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen, answerText) { setWordIndex(it) }'
    s=once(s,clue,
      '''                if(!keyboardIsOpen) {
                    Wq179ArenaClueHeader(puzzle,selected)
                    Wq179ArenaWordChooser(puzzle,solved,selected){ newIndex ->
                        setWordIndex(newIndex)
                        setAnswerText("")
                    }
                }
                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen, answerText) {
                    setWordIndex(it)
                    setAnswerText("")
                }''',
      "full list and clue visible before board")
    s=once(s,"val pos=fixedPattern.indexOf('.')",
      "val pos=Wq179ArenaOrder.hintPosition(match.code,selected,fixedPattern)",
      "random unrevealed hint letter instead of first letter")
    s=once(s,
      '                ArenaLiveScoreV2(',
      '''                Wq179ArenaGameAudio(match.phase=="playing",countdown,
                    session.preferences.correctSoundEnabled)
                ArenaLiveScoreV2(''',
      "Arena ambience and last ten seconds during crossword")
    s=once(s,
      '                    ArenaScoreStrip(own.size, questions.size, myScore, null)',
      '''                    Wq179ArenaGameAudio(match.phase=="playing",
                        if(quizDeadline>0) PremiumOnlineLimits.remainingSeconds(
                            quizDeadline,client.serverNow(wallTime)) else null,
                        session.preferences.correctSoundEnabled)
                    ArenaScoreStrip(own.size, questions.size, myScore, null)''',
      "quiz time warning")
    # A player may freely change the word ONCE without giving up the turn.
    # Existing PAS action then remains the supported turn/forfeit operation.
    marker='                            ArenaOutlineButton("»  PAS GEÇ", {'
    s=once(s,marker,
      '''                            if(!freeSkipUsed && canAnswer &&
                                puzzle.words.indices.count{it !in solved}>1) {
                                ArenaOutlineButton("1 ÜCRETSİZ KELİME GEÇ",{
                                    Wq179ArenaSkip.consume(context,match.code,myUid)
                                    freeSkipUsed=true
                                    setWordIndex(Wq179ArenaOrder.next(
                                        match.code,myUid,puzzle.words.size,solved,selected))
                                    setAnswerText("")
                                },Modifier.fillMaxWidth())
                            }
                            ArenaOutlineButton("»  PAS GEÇ", {''',
      "single free change before normal PAS")
    # Keep the content readable on the themed purple tournament surface.
    s=once(s,
      '    Box(Modifier.fillMaxSize().background(OnlineNight)) {',
      '''    Box(Modifier.fillMaxSize().background(OnlineNight)) {
        Wq179ArenaStageBackground(
            Modifier.matchParentSize(), dark = match?.phase == "playing")''',
      "arena lights and stadium backdrop")
    return s

def typing(s):
    begin=s.index('@Composable\ninternal fun ArenaCompactCrosswordBoard(')
    # This is the last composable in the v122 dedicated typing file.
    assert "crosswordFullWordPreview" in s[begin:],"full-word typed letters must survive"
    return s[:begin]+'''@Composable
internal fun ArenaCompactCrosswordBoard(
    puzzle: Puzzle, solved: Set<Int>, selected: Int,
    compact: Boolean, typed: String, onSelect: (Int)->Unit
) {
    val placed=crosswordFullWordPreview(crosswordFixedPattern(puzzle,solved,selected),typed)
    // One viewport owns BOTH scroll axes; no nested vertical-scroller crash.
    ArenaBoardV2(puzzle,solved,selected,placed,compact,onSelect)
}
'''

def offline(s):
    start=s.index('    fun nextUnsolved(after: Int, words: Int): Int {')
    end=s.index('    fun resetBoard() {',start)
    s=s[:start]+'''    fun nextUnsolved(after:Int,words:Int):Int =
        Wq179ArenaOrder.next(roundId,session.profileKey,words,solved.toSet(),after)
'''+s[end:]
    s=once(s,'        currentWord = 0\n        turns = 0',
      '''        currentWord = Wq179ArenaOrder.next(roundId,session.profileKey,
            board?.puzzle?.words?.size ?: 0,emptySet(),-1)
        turns = 0''',"random first offline word")
    marker='                    ArenaCompactCrosswordBoard(puzzle, solved.toSet(), currentWord, keyboardIsOpen, wordAnswer) { index ->'
    s=once(s,marker,
      '''                    if(!keyboardIsOpen) {
                        Wq179ArenaClueHeader(puzzle,currentWord)
                        Wq179ArenaWordChooser(puzzle,solved.toSet(),currentWord){ index ->
                            currentWord=index
                            wordAnswer=""
                            hintLetter=""
                            feedback=""
                        }
                    }
                    ArenaCompactCrosswordBoard(puzzle, solved.toSet(), currentWord, keyboardIsOpen, wordAnswer) { index ->''',
      "offline all-clues picker")
    s=once(s,
      'val index = word.entry.letters.indices.firstOrNull { it !in known } ?: 0',
      '''val open=word.entry.letters.indices.filter{it !in known}
                                val index=if(open.isNotEmpty())
                                    open[java.util.Random((roundId+currentWord).hashCode().toLong())
                                        .nextInt(open.size)] else 0''',
      "offline hint reveals a nonsequential available letter")
    return s

def scoreboard(s):
    s=once(s,'import androidx.compose.foundation.*',
      'import androidx.compose.animation.core.*\nimport androidx.compose.foundation.*',
      "animated turn highlight imports")
    s=once(s,
      '    Row(modifier.heightIn(min=86.dp).clip(RoundedCornerShape(18.dp))',
      '''    val transition=rememberInfiniteTransition(label="arena-turn-glow")
    val halo by transition.animateFloat(initialValue=.25f,targetValue=1f,
        animationSpec=infiniteRepeatable(tween(850),RepeatMode.Reverse),label="halo")
    Row(modifier.heightIn(min=95.dp)
        .shadow(if(active)(5f+halo*9f).dp else 3.dp,RoundedCornerShape(18.dp))
        .clip(RoundedCornerShape(18.dp))''',
      "glow for active participant")
    s=once(s,'.border(1.dp,Wq172Gold,RoundedCornerShape(18.dp))',
      '''.border(if(active)(2f+halo).dp else 1.dp,
            if(active)Color(0xFFFFD36E) else Color(0xA8E4C8FF),
            RoundedCornerShape(18.dp))''',
      "strong golden active ring")
    s=once(s,'ArenaMascotV2(avatar,Modifier.size(39.dp))',
      'ArenaMascotV2(avatar,Modifier.size(if(active)54.dp else 47.dp))',
      "larger selected mascots")
    s=once(s,'Box(Modifier.size(92.dp),contentAlignment=Alignment.Center) {',
      '''val urgent=c!=null && c in 1..10
        val pulse=rememberInfiniteTransition(label="last-ten")
        val warning by pulse.animateFloat(initialValue=.98f,targetValue=1.12f,
            animationSpec=infiniteRepeatable(tween(400),RepeatMode.Reverse),
            label="warning-clock")
        Box(Modifier.size(if(urgent)103.dp else 87.dp)
            .then(if(urgent)Modifier.graphicsLayer(scaleX=warning,scaleY=warning)
                  else Modifier),contentAlignment=Alignment.Center) {''',
      "enlarge and pulse countdown during last ten seconds")
    s=once(s,'import androidx.compose.ui.draw.shadow',
      'import androidx.compose.ui.draw.shadow\nimport androidx.compose.ui.graphics.graphicsLayer',
      "pulse drawing import")
    s=once(s,'drawCircle(Color(0xFF38E2FF),style=Stroke(width=s))',
      'drawCircle(if(urgent)Color(0xFFFFD36E) else Color(0xFF38E2FF),style=Stroke(width=s))',
      "warning clock ring")
    s=once(s,'Text(clock,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.ExtraBold)',
      'Text(clock,color=Color.White,fontSize=if(urgent)25.sp else 18.sp,fontWeight=FontWeight.ExtraBold)',
      "bigger last-ten time text")
    s=once(s,
      'listOf(Color(0xFF4D45A6),Color(0xFF9470CE),Color(0xFF5545AC))',
      'listOf(Color(0xFF28134F),Color(0xFF8B53BB),Color(0xFF422877))',
      "saturated tournament scoreboard")
    return s

def settings(s):
    return once(s,
       '        SoundSwitch("Telaffuz sesini aç/kapat"',
       '''        Wq179ArenaAudioSettings()
        SoundSwitch("Telaffuz sesini aç/kapat"''',
       "toggles for atmosphere and urgent ticks")

shutil.copyfile(PKG/"Wq179ArenaBoard.kt",SRC/"ArenaBoardV2.kt")
shutil.copyfile(PKG/"Wq179ArenaExtras.kt",SRC/"Wq179ArenaExtras.kt")
write("ArenaCrosswordTyping.kt",typing)
write("PremiumOnlineScreen.kt",online)
write("PremiumArenaScreen.kt",offline)
write("ArenaReferenceComponents.kt",scoreboard)
write("WordQuestSettings.kt",settings)

gradle=ROOT/"source/app/build.gradle.kts"
v=gradle.read_text("utf-8")
assert v.count('versionCode = 104')==1 and v.count('versionName = "1.0.78"')==1
assert 'applicationIdSuffix = ".fresh"' in v
gradle.write_text(v.replace('versionCode = 104','versionCode = 105',1)
    .replace('versionName = "1.0.78"','versionName = "1.0.79"',1),"utf-8")
print("PASS: WordQuest AI v1.0.79 code105 with unchanged installed app package.")
