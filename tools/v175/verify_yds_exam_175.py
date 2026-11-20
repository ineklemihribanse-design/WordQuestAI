#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
yds=(src/"YdsContent.kt").read_text("utf-8")
ui=(src/"YdsCenterScreen.kt").read_text("utf-8")
art=(src/"WordQuest175YdsExamUi.kt").read_text("utf-8")
grammar=(src/"Wq175YdsGrammar.kt").read_text("utf-8")
tests=(root/"source/app/src/test/java/com/kelimebulmacasi/a1/YdsCenterTest.kt").read_text("utf-8")
assert 'Grammar("Dilbilgisi")' in yds
assert '+ YdsExtraContent.questions + Wq175YdsGrammar.questions' in yds
assert grammar.count('q("e')==5
assert grammar.count('q("m')==5
assert grammar.count('q("h')==5
assert 'assertEquals(120, YdsContent.questions.size)' in tests
assert 'assertEquals(120, YdsContent.questions.map { it.id }.toSet().size)' in tests
assert ui.count("Wq175YdsStudyHero(")==1
assert ui.count("Wq175YdsQuestionPanel(")==1
assert ui.count("Wq175YdsOption(")==1
assert ui.count("Wq175YdsAnswerFeedback(")==1
assert ui.count("Wq175YdsResult(")==1
assert ui.count('store.record(current, correct)')==1
assert ui.count('session.recordYdsAttempt(current, correct, text)')==1
assert 'ydsRemainingSeconds(deadlineMillis, System.currentTimeMillis())' in ui
assert 'store.wrongQuestions()' in ui
assert 'teacher(store.teacherPrompt())' in ui
assert 'Wq175GlossIcon("yds"' in art
print("PASS: 120 YDS questions, new grammar, action-tied premium pages, preserved timer/progress/AI.")
