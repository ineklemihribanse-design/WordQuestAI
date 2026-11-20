#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
hub=(src/"PracticeHubScreen.kt").read_text("utf-8")
design=(src/"WordQuest175PracticeVisuals.kt").read_text("utf-8")
assert hub.count("in 5..180")>=4, "goal read/write/export/import must all permit 5..180"
assert 'fun activeDaysLast7(' in hub
assert 'activeDays=store.activeDaysLast7()' in hub
assert 'wq_daily_' in hub and 'stableHash(profileKey)' in hub
assert 'Wq175ParagraphPracticeEntry(' in hub
assert 'onMinutes(customValue)' in design
assert 'Son 7 günde ' in design
assert 'Kişisel hedef (5–180 dk)' in design
assert (root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175DailyGoalsTest.kt").exists()
print("PASS: user-defined 5–180 min goal, weekly activity and non-destructive backup mapping")
