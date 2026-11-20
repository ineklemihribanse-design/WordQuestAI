#!/usr/bin/env python3
"""Static verification of genuine, profile-scoped timed crossword integration."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
newgame=(src/"NewGameScreen.kt").read_text("utf-8")
puzzle=(src/"PuzzleScreen.kt").read_text("utf-8")
home=(src/"HomeScreen.kt").read_text("utf-8")
timer=(src/"WordQuest175TimedPuzzle.kt").read_text("utf-8")
assert 'var timedMinutes by rememberSaveable' in newgame
assert 'Wq175TimedPuzzle.configure(context,session.profileKey,' in newgame
for minutes in ('3 to "3 dk"','5 to "5 dk"','10 to "10 dk"'):
    assert minutes in newgame
assert 'profileKey: String = ""' in puzzle
assert 'Wq175TimedPuzzle.read(context,profileKey,game.progressId)' in puzzle
assert 'LaunchedEffect(timer,game.progressId)' in puzzle
assert 'if (shuffling || timedOut) return' in puzzle
assert 'if(timedOut)return' in puzzle
assert 'Wq175TimeRemainingBadge(remainingMs)' in puzzle
assert 'if(timedOut) {' in puzzle
assert 'session.preferences, pronunciation, session.profileKey)' in home
assert 'SystemClock.elapsedRealtime()' in timer
assert 'stableHash(profile+' in timer
assert 'minutes in setOf(0,3,5,10)' in timer
assert 'nowWall-wallStart' in timer
assert (root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175TimerMathTest.kt").exists()
print("PASS 1.0.75 timed crossword static gates and JVM timing tests staged")
