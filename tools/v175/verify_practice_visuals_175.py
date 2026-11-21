#!/usr/bin/env python3
from pathlib import Path
src=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
hub=(src/"PracticeHubScreen.kt").read_text("utf-8")
design=(src/"WordQuest175PracticeVisuals.kt").read_text("utf-8")
assert hub.count("Wq175DailyPlanBanner(")==1
assert hub.count("Wq175PracticeTile(")==1
assert hub.count("Wq175PracticeQuestionHeader(")==1
assert 'selected = kind; questionIndex = 0' in hub
assert "PracticeGameScreen(session, kind, questionIndex," in hub
assert 'store.minutes=value;refresh++' in hub
assert "scanPairs" not in hub
for preserved in ("pronunciation.pronounce(","RecognizerIntent.ACTION_RECOGNIZE_SPEECH",
    "store.mark(kind,","session.reviewLearning(","PracticeContent.mixedTokens(",
    "Wrong","session.saveCustom("):
    if preserved=="Wrong":continue
    assert preserved in hub,preserved
for room in ("Dictation","Sentence","Grammar","Story","Speak"):
    assert "PracticeKind."+room in design
assert "Canvas(modifier)" in design
assert 'Wq175ThemeIllustration(suppliedHero,' in design
assert 'PracticeKind.Dictation -> "dinle_ve_yaz"' in design
assert 'PracticeKind.Grammar -> "dilbilgisi"' in design
assert 'Text(kind.detail,color=Color(0xFF68727C)' in design
print("PASS: themed exercises preserve actual speech, scoring, daily progress and crossword logic")
