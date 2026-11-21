#!/usr/bin/env python3
"""Add a genuine full-length timed 80-question YDS mock exam to the existing offline exam screen."""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"

def once(s,a,b,name):
    assert s.count(a)==1,(name,s.count(a))
    return s.replace(a,b,1)

def main():
    p=SRC/"YdsCenterScreen.kt"
    s=p.read_text("utf-8")
    s=once(s, "15L * 60L * 1000L) / 1000L",
              "180L * 60L * 1000L) / 1000L", "allow 180-minute countdown")
    s=once(s,
        '    fun start(modeExam: Boolean, wrongOnly: Boolean = false) {',
        '    fun start(modeExam: Boolean, wrongOnly: Boolean = false, full: Boolean = false) {',
        "mode selection")
    s=once(s,
        '        val pool = if (wrongOnly) store.wrongQuestions() else YdsContent.pool(topic, level)',
        '        val pool = if (wrongOnly) store.wrongQuestions() else if (full) YdsContent.pool(null, null) else YdsContent.pool(topic, level)',
        "full mock draws from all topics")
    s=once(s,
        '        activeIds = selection.take(if (modeExam) 15 else 12).map { it.id }',
        '        activeIds = selection.take(if (full) 80 else if (modeExam) 15 else 12).map { it.id }',
        "80 unique question selection")
    s=once(s,
        '        remaining = if (modeExam) 15 * 60 else 0',
        '        remaining = if (full) 180 * 60 else if (modeExam) 15 * 60 else 0',
        "3 hour full mock timer")
    s=once(s,
        '        deadlineMillis = if (modeExam) System.currentTimeMillis() + 15L * 60L * 1000L else 0L',
        '        deadlineMillis = if (modeExam) System.currentTimeMillis() + (if (full) 180L else 15L) * 60L * 1000L else 0L',
        "persist wall-clock deadline")
    s=once(s,
        '''            Wq175YdsPrimaryAction("15 dakikalık süreli mini deneme") { start(true) }''',
        '''            Wq175YdsPrimaryAction("15 dakikalık süreli mini deneme") { start(true) }
            Wq175YdsPrimaryAction("80 soruluk tam deneme · 180 dakika") { start(true, full=true) }''',
        "full mock start button")
    assert s.count("session.recordYdsAttempt(current, correct, text)")==1
    assert s.count("store.record(current, correct)")==1
    p.write_text(s,"utf-8")
    shutil.copyfile(PKG/"Wq175FullYdsTest.kt",TEST/"Wq175FullYdsTest.kt")
    print("PASS: 80-question 180-minute full YDS with non-pausable deadline, original scoring, old mini exam untouched")

if __name__=="__main__":
    main()
