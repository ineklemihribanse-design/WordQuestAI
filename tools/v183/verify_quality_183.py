#!/usr/bin/env python3
"""Static source checks. These do not replace Android UI or two-account device tests."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
code=lambda name:(SRC/name).read_text("utf-8")
online=code("PremiumOnlineScreen.kt")
solo=code("PremiumArenaScreen.kt")
quality=code("Wq183QuizQuality.kt")
assert online.count("Wq183QuizPrompt(question.prompt)")==1
assert "Text(question.prompt, color = Color.White" not in online
assert online.count("Wq183QuizIsValid(question.prompt, question.options, question.correct)")==1
assert solo.count("Wq183QuizIsValid(current.prompt, current.options, current.correct)")==1
assert "Wq183QuizUnavailableQuestion()" in online
assert "Wq183QuizUnavailableQuestion()" in solo
assert "client.answer(index,n)" in online
assert "score+=10" in solo and "quizAnswers[quizIndex]=index" in solo
assert "softWrap=true)" in code("Wq182QuizAndInfo.kt")
assert "maxLines=4,overflow=TextOverflow.Ellipsis)" not in code("Wq182QuizAndInfo.kt")
assert "color = Color(0xFF241C35)" in quality
gradle=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert "versionCode = 109" in gradle
assert 'versionName = "1.0.83"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
print("PASS: readable question, five validated answers, score callbacks intact, version109")
print("NOTE: no claim of emulator, live Firebase or full game coverage from this static check")
