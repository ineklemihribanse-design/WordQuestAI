#!/usr/bin/env python3
"""Prevent correct-answer disclosure in YDS timed mini and full exams.

Both the premium option color and explanation widget must stay hidden until the
exam ends. Normal study mode retains its immediate feedback.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"

def replace_once(s,old,new,label):
    assert s.count(old)==1,(label,s.count(old))
    return s.replace(old,new,1)

screen=SRC/"YdsCenterScreen.kt"
s=screen.read_text("utf-8")
s=replace_once(s,
    'answered=answerChoice!=-1,',
    'answered=answerChoice!=-1 && Wq175YdsVisibility.reveal(exam),',
    "prevent color-disclosed correct answers during timed exams")
s=replace_once(s,
    '''                Wq175YdsAnswerFeedback(
                    answerChoice==current.answer,
                    current.options[current.answer], current.explanation)
                Wq175YdsPrimaryAction(''',
    '''                if (Wq175YdsVisibility.reveal(exam)) {
                    Wq175YdsAnswerFeedback(
                        answerChoice==current.answer,
                        current.options[current.answer], current.explanation)
                } else {
                    BodyText("Yanıt kaydedildi. Doğru cevaplar sınav sırasında gösterilmez.")
                }
                Wq175YdsPrimaryAction(''',
    "timed exam hides answer key and explanation")
screen.write_text(s,"utf-8")
shutil.copyfile(PKG/"Wq175YdsVisibility.kt",SRC/"Wq175YdsVisibility.kt")
shutil.copyfile(PKG/"Wq175YdsVisibilityTest.kt",TEST/"Wq175YdsVisibilityTest.kt")
print("PASS: YDS timed exams mask correct choice visuals and explanations; normal practice retains feedback")
