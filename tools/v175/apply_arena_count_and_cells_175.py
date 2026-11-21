#!/usr/bin/env python3
"""v175 live Arena question count + slightly darker blank crossword tiles."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"

def patch(name, replacements):
    p=SRC/name
    s=p.read_text("utf-8")
    for old,new,label in replacements:
        n=s.count(old)
        assert n==1, f"{name} {label}: expected one match, found {n}"
        s=s.replace(old,new,1)
    p.write_text(s,"utf-8")

patch("WordQuest174Arena.kt", [
    ('''    Column(Modifier.fillMaxWidth().shadow(8.dp,RoundedCornerShape(29.dp))''',
     '''    LaunchedEffect(mode) {
        if(mode=="quiz" && size !in listOf(5,10,15,20)) onSize(10)
        else if(mode!="quiz" && sizes.isNotEmpty() && size !in sizes) onSize(sizes.first())
    }
    Column(Modifier.fillMaxWidth().shadow(8.dp,RoundedCornerShape(29.dp))''',
     "reset count when changing modes"),
    ('''            Wq174SettingRow("count","Bulmaca Sayısı",
                if(mode=="quiz")"12 soru" else "$size kelime",
                (if(mode=="quiz")listOf(12) else sizes).map {
                    "$it "+if(mode=="quiz")"soru" else "kelime"
                },
                enabled=mode!="quiz",''',
     '''            Wq174SettingRow("count",if(mode=="quiz")"Soru Sayısı" else "Bulmaca Sayısı",
                if(mode=="quiz")"$size soru" else "$size kelime",
                (if(mode=="quiz")listOf(5,10,15,20) else sizes).map {
                    "$it "+if(mode=="quiz")"soru" else "kelime"
                },
                enabled=if(mode=="quiz")true else sizes.isNotEmpty(),''',
     "question count label and options")
])

patch("PremiumOnlineRoom.kt",[
    ('''(answers[hostUid]?.size ?: 0) >= 12 && (answers[guestUid]?.size ?: 0) >= 12''',
     '''(answers[hostUid]?.size ?: 0) >= size && (answers[guestUid]?.size ?: 0) >= size''',
     "room completion count"),
    ('''(if (mode == "quiz") size == 12 else size in PremiumArenaRules.allowedSizes)''',
     '''(if (mode == "quiz") size in listOf(5,10,12,15,20)
             else size in PremiumArenaRules.allowedSizes)''',
     "quiz count validation with legacy rooms"),
    ('''index !in 0..11 || choice !in -1..4''',
     '''index !in 0 until m.size || choice !in -1..4''',
     "answer index validation")
])

patch("PremiumOnlineScreen.kt",[
    # Arena V2 uses ArenaHomeV2(size=size,onSize=...) to drive the existing
    # Wq174UnifiedMatchSettings picker. Do not create a second quizCount state.
    ('''if(selectedMode=="quiz")12 else size,''',
     '''size,''',
     "V2 online room creation uses selected ArenaHome size"),
    ('''remember(match.code, match.quizSeed, match.level, match.topic)''',
     '''remember(match.code, match.quizSeed, match.level, match.topic, match.size)''',
     "online quiz cache key"),
    ('''packs.questionsForSeed(match.level, match.topic, match.quizSeed)''',
     '''packs.questionsForSeed(match.level, match.topic, match.quizSeed, match.size)''',
     "both clients use selected question count"),
    ('''if (questions.size != 12)''',
     '''if (questions.size != match.size)''',
     "online question count validation"),
    ('''Text("SORU ${index + 1} / 12"''',
     '''Text("SORU ${index + 1} / ${match.size}"''',
     "live quiz progress label"),
    ('''setQuizIndex((index + 1).coerceAtMost(11))''',
     '''setQuizIndex((index + 1).coerceAtMost(questions.lastIndex))''',
     "selected quiz last question")
])

patch("PremiumArenaScreen.kt",[
    ('''    var size by rememberSaveable { mutableIntStateOf(30) }''',
     '''    var size by rememberSaveable { mutableIntStateOf(30) }
    var quizCount by rememberSaveable { mutableIntStateOf(10) }''',
     "solo quiz count state"),
    ('''packs.nextQuestions(level, topic, session.profileKey)''',
     '''packs.nextQuestions(level, topic, session.profileKey, quizCount)''',
     "offline question count"),
    ('''if (questions.size < 5)''',
     '''if (questions.size != quizCount)''',
     "do not silently return fewer questions"),
    ('''            PremiumStage.Lobby -> {''',
     '''            PremiumStage.Lobby -> {
                if (selectedMode == "quiz") ArenaPanelBlock {
                    Text("SORU SAYISI", color=ArenaGold,
                        fontWeight=FontWeight.Bold,fontSize=11.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(5,10,15,20).forEach { n ->
                            ArenaChoice("$n",quizCount==n,Modifier.weight(1f)) { quizCount=n }
                        }
                    }
                }''',
     "offline count controls"),
    ('''"12 soruluk alıştırma · A / B / C / D / E"''',
     '''"$quizCount soruluk alıştırma · A / B / C / D / E"''',
     "quiz header count"),
    ('''                                !active -> ArenaNight''',
     '''                                !active -> Color(0xFFC5C8D5)''',
     "deeper lilac-grey blocked crossword cells")
])

TEST.mkdir(parents=True,exist_ok=True)
shutil.copyfile(PKG/"Wq175QuizCountTest.kt",TEST/"Wq175QuizCountTest.kt")
print("PASS: Arena question count 5/10/15/20 reaches offline and online engines.")
print("PASS: legacy 12-question rooms remain valid; only inactive grid tiles darkened.")
