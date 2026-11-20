#!/usr/bin/env python3
"""Add original YDS grammar questions and sapphire visuals without replacing records."""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"

def once(s,a,b,label):
    assert s.count(a)==1,(label,s.count(a))
    return s.replace(a,b,1)

def span(s,start,end,replacement,label):
    assert s.count(start)==1,(label,"start",s.count(start))
    a=s.index(start)
    b=s.find(end,a+len(start))
    assert b!=-1,(label,"end")
    return s[:a]+replacement+s[b:]

def main():
    shutil.copyfile(PKG/"Wq175YdsGrammar.kt",SRC/"Wq175YdsGrammar.kt")
    shutil.copyfile(PKG/"WordQuest175YdsExamUi.kt",SRC/"WordQuest175YdsExamUi.kt")
    p=SRC/"YdsContent.kt"
    s=p.read_text("utf-8")
    s=once(s,'Completion("Cümle tamamlama"), Synonym("Yakın anlam")',
        'Completion("Cümle tamamlama"), Synonym("Yakın anlam"), Grammar("Dilbilgisi")',
        "new grammar category")
    s=once(s,') + YdsExtraContent.questions',
        ') + YdsExtraContent.questions + Wq175YdsGrammar.questions',
        "original questions")
    p.write_text(s,"utf-8")

    p=SRC/"YdsCenterScreen.kt"
    s=p.read_text("utf-8")
    s=span(s,'        BodyText("','        if (current != null) {',
        '''        Wq175YdsStudyHero(
            session.learningProfileName, store.totalAttempts(),
            store.percent(), YdsContent.questions.size)
''',"YDS landing header")
    s=span(s,'            if (exam) BodyText(',
        '            choices.forEachIndexed',
        '''            Wq175YdsQuestionPanel(
                activePosition+1,activeIds.size,current.topic.label,
                current.difficulty.label,if(exam)remaining else null,
                current.question)
''',"question panel")
    s=span(s,'            choices.forEachIndexed',
        '            if (answerChoice != -1)',
        '''            choices.forEachIndexed { index, (originalIndex, text) ->
                Wq175YdsOption(
                    index,text,
                    selected=answerChoice==originalIndex,
                    answered=answerChoice!=-1,
                    correct=originalIndex==current.answer
                ) {
                    if (answerChoice == -1 && (!exam || System.currentTimeMillis() < deadlineMillis)) {
                        answerChoice = originalIndex
                        val correct = originalIndex == current.answer
                        if (correct) score++
                        answeredCount++
                        store.record(current, correct)
                        session.recordYdsAttempt(current, correct, text)
                        refresh++
                    }
                }
            }
''',"live answer callbacks and premium choices")
    s=span(s,'                BodyText(if (answerChoice == current.answer)',
        '            TextButton(onClick = { finished = true })',
        '''                Wq175YdsAnswerFeedback(
                    answerChoice==current.answer,
                    current.options[current.answer], current.explanation)
                Wq175YdsPrimaryAction(
                    if(activePosition==activeIds.lastIndex)"Sonucu gör" else "Sonraki soru") {
                    if (activePosition == activeIds.lastIndex) finished = true
                    else activePosition++
                    answerChoice = -1
                }
            }
''',"results explanation and next callback")
    s=span(s,'            Text("Çalışma özeti"',
        '            PrimaryAction("Yeni çalışma"',
        '''            Wq175YdsResult(score,answeredCount,activeIds.size)
''',"final score visual")
    s=once(s,
        '''            PrimaryAction("Çevrimdışı soru çalış", { start(false) })
            PrimaryAction("15 dakikalık süreli mini deneme", { start(true) })''',
        '''            Wq175YdsPrimaryAction("Çevrimdışı soru çalış") { start(false) }
            Wq175YdsPrimaryAction("15 dakikalık süreli mini deneme") { start(true) }''',
        "start actions")
    assert s.count("store.record(current, correct)")==1
    assert s.count("session.recordYdsAttempt(current, correct, text)")==1
    assert s.count("deadlineMillis = if (modeExam)")==1
    p.write_text(s,"utf-8")

    p=TEST/"YdsCenterTest.kt"
    s=p.read_text("utf-8")
    s=once(s,"assertEquals(105, YdsContent.questions.size)",
        "assertEquals(120, YdsContent.questions.size)","YDS question test")
    s=once(s,"assertEquals(105, YdsContent.questions.map { it.id }.toSet().size)",
        "assertEquals(120, YdsContent.questions.map { it.id }.toSet().size)",
        "unique YDS IDs test")
    p.write_text(s,"utf-8")
    print("PASS: YDS grammar 15 original + premium choices + safe score/record callbacks")

if __name__=="__main__":
    main()
