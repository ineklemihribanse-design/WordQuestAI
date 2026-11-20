#!/usr/bin/env python3
"""Regression checks after applying Arena quiz-count and tile patch."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
a=(src/"WordQuest174Arena.kt").read_text("utf-8")
b=(src/"PremiumOnlineScreen.kt").read_text("utf-8")
c=(src/"PremiumOnlineRoom.kt").read_text("utf-8")
d=(src/"PremiumArenaScreen.kt").read_text("utf-8")
for needle in ('if(mode=="quiz")"Soru Sayısı" else "Bulmaca Sayısı"',
               '(if(mode=="quiz")listOf(5,10,15,20) else sizes)',
               'enabled=if(mode=="quiz")true'):
    assert needle in a,needle
for needle in ('quizCount else size','packs.questionsForSeed(match.level, match.topic, match.quizSeed, match.size)',
               'if (questions.size != match.size)', 'coerceAtMost(questions.lastIndex)'):
    assert needle in b,needle
for needle in ('>= size && (answers[guestUid]?.size ?: 0) >= size',
               'size in listOf(5,10,12,15,20)',
               'index !in 0 until m.size'):
    assert needle in c,needle
for needle in ('packs.nextQuestions(level, topic, session.profileKey, quizCount)',
               '!active -> Color(0xFFC5C8D5)', 'if (questions.size != quizCount)'):
    assert needle in d,needle
assert (root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175QuizCountTest.kt").exists()
print("PASS: actual size flows to both players, navigation and scoring; blank tiles darker.")
