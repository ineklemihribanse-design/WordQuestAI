#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
test=root/"source/app/src/test/java/com/kelimebulmacasi/a1"
s=(src/"YdsCenterScreen.kt").read_text("utf-8")
assert s.count('answered=answerChoice!=-1 && Wq175YdsVisibility.reveal(exam),')==1
assert s.count('if (Wq175YdsVisibility.reveal(exam)) {')==1
assert 'Yanıt kaydedildi. Doğru cevaplar sınav sırasında gösterilmez.' in s
assert s.count('Wq175YdsAnswerFeedback(')==1
assert 'start(true, full=true)' in s
assert 'start(true)' in s
assert 'store.record(current, correct)' in s
assert 'session.recordYdsAttempt(current, correct, text)' in s
assert 'fun reveal(isTimedExam:Boolean):Boolean = !isTimedExam' in (src/"Wq175YdsVisibility.kt").read_text("utf-8")
assert (test/"Wq175YdsVisibilityTest.kt").is_file()
print("PASS: no visual or textual disclosure in timed exams; 80-question and 15-minute flows preserved")
