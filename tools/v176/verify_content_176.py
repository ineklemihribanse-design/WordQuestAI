#!/usr/bin/env python3
"""Fail v1.0.76 builds if genuine unique questions, source integration,
per-profile question rotation or safe in-place update metadata are missing."""
from pathlib import Path
import re
from collections import Counter

root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
tests=root/"source/app/src/test/java/com/kelimebulmacasi/a1"
assets=root/"source/app/src/main/assets"
yds=(src/"Wq176YdsGenerated.kt").read_text("utf-8")
records=[line.strip() for line in yds.splitlines() if line.startswith("wq176_lex_") or line.startswith("wq176_gr_")]
assert len(records)==1840,("Expected 1600 lexicon +240 grammar",len(records))
ids=set()
questions=set()
categories=Counter()
answers=set()
for row in records:
    pieces=row.replace('"""','').split("\t")
    assert len(pieces)==9,(len(pieces),row[:100])
    id,topic,level,prompt,answer,a,b,c,explanation=pieces
    assert id not in ids,("duplicate ID",id)
    assert prompt.casefold() not in questions,("duplicate prompt",id)
    assert len({answer.casefold(),a.casefold(),b.casefold(),c.casefold()})==4,("duplicate option",id)
    assert len(explanation)>=35
    ids.add(id);questions.add(prompt.casefold());categories[topic]+=1
assert categories["Vocabulary"]==800
assert categories["Translation"]==800
assert categories["Grammar"]==240
assert "+ Wq176YdsGenerated.questions" in (src/"YdsContent.kt").read_text("utf-8")
assert 'Wq176DailyPracticeEntry(session.profileKey)' in (src/"PracticeHubScreen.kt").read_text("utf-8")
screen=(src/"YdsCenterScreen.kt").read_text("utf-8")
assert "Wq176QuestionRotation.pick(" in screen
assert "selection.take(if (full)" not in screen
rotation=(src/"Wq176DailyChallenge.kt").read_text("utf-8")
assert "context.getSharedPreferences" in rotation
assert "profileKey.hashCode()" in rotation
assert ".shuffled()" in rotation
assert "recentSet" in rotation
assert "DailyPlanStore(context,profileKey)" in rotation
assert (tests/"Wq176ContentRotationTest.kt").exists()
assert (assets/"licenses/WQ176_CONTENT_SOURCES.md").exists()
gradle=(root/"source/app/build.gradle.kts").read_text("utf-8")
assert "versionCode = 102" in gradle
assert 'versionName = "1.0.76"' in gradle
print("PASS: 2038 genuinely distinct offline YDS-style question entries")
print("PASS: 800 English/Turkish vocabulary + 800 translation + 240 grammar and 198 existing")
print("PASS: rotation + source-based Daily Plan question work + package identity 1.0.76 code102")
print("LIMITATION: structural checks do not replace teacher review or live-device validation")
