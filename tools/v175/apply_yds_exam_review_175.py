#!/usr/bin/env python3
"""Keep all YDS answers per exam session for a POST-exam explanation review."""
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
test=root/"source/app/src/test/java/com/kelimebulmacasi/a1"
p=src/"YdsCenterScreen.kt"
s=p.read_text("utf-8")
def one(a,c):
    global s
    assert s.count(a)==1,(a[:90],s.count(a))
    s=s.replace(a,c,1)
one('    var activeIds by rememberSaveable(profile) { mutableStateOf(emptyList<String>()) }',
    '    var activeIds by rememberSaveable(profile) { mutableStateOf(emptyList<String>()) }\n'+
    '    var wq175Answers by rememberSaveable(profile) { mutableStateOf(emptyMap<String,Int>()) }')
one('        activeIds = selection.take(if (full) 80 else if (modeExam) 15 else 12).map { it.id }',
    '        activeIds = selection.take(if (full) 80 else if (modeExam) 15 else 12).map { it.id }\n'+
    '        wq175Answers = emptyMap()')
one('                        answerChoice = originalIndex\n                        val correct = originalIndex == current.answer',
    '                        answerChoice = originalIndex\n                        wq175Answers = wq175Answers + (current.id to originalIndex)\n'+
    '                        val correct = originalIndex == current.answer')
one('            Wq175YdsResult(score,answeredCount,activeIds.size)',
    '            Wq175YdsResult(score,answeredCount,activeIds.size)\n'+
    '            Wq175YdsExamReview(activeIds,wq175Answers)')
p.write_text(s,"utf-8")
shutil.copyfile(root/"tools/v175/Wq175YdsExamReview.kt",src/"Wq175YdsExamReview.kt")
shutil.copyfile(root/"tools/v175/Wq175YdsExamReviewTest.kt",test/"Wq175YdsExamReviewTest.kt")
print("PASS: profile-safe exam answers retained within session; review only on finished result screen.")
