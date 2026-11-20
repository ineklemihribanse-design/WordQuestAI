#!/usr/bin/env python3
"""Complete Daily Plan personal targets and weekly activity without replacing the profile store."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
PKG = ROOT / "tools/v175"

def once(source, old, new, name):
    count = source.count(old)
    assert count == 1, f"{name}: expected 1 anchor, got {count}"
    return source.replace(old, new, 1)

def run():
    daily = SOURCE / "PracticeHubScreen.kt"
    text = daily.read_text("utf-8")
    # Keep the existing preference filename, profile key, daily data and backup schema.
    text = once(text, 'getInt(key, 15).takeIf { it in listOf(15, 30, 60) } ?: 15',
                'getInt(key, 15).takeIf { it in 5..180 } ?: 15',
                "custom target retrieval")
    text = once(text, 'set(value) { if (value in listOf(15, 30, 60)) prefs.edit().putInt(key, value).apply() }',
                'set(value) { if (value in 5..180) prefs.edit().putInt(key, value).apply() }',
                "custom target persistence")
    text = once(text,
                'prefs.getInt("plan.$date", 15).takeIf { it in listOf(15, 30, 60) } ?: 15',
                'prefs.getInt("plan.$date", 15).takeIf { it in 5..180 } ?: 15',
                "export custom goals")
    text = once(text,
                'if (day.goalMinutes !in listOf(15, 30, 60) || day.activeSeconds !in 0L..86_400L ||',
                'if (day.goalMinutes !in 5..180 || day.activeSeconds !in 0L..86_400L ||',
                "import custom goals")
    text = once(text,
                '''    fun mark(kind: PracticeKind, id: String): Boolean {''',
                '''    /** Number of genuinely active days in the most recent seven-day window. */
    fun activeDaysLast7(now: LocalDate = LocalDate.now()): Int =
        (0L..6L).count { offset ->
            val date = now.minusDays(offset).toString()
            prefs.getLong("active.$date", 0L) >= 60L ||
                !prefs.getStringSet("done.$date", emptySet()).isNullOrEmpty()
        }

    fun mark(kind: PracticeKind, id: String): Boolean {''',
                "profile-scoped weekly activity")
    text = once(text,
                '''            minutes=mins,done=done,activeMinutes=doneMinutes,
            remaining=remaining,due=due,fraction=fraction,''',
                '''            minutes=mins,done=done,activeMinutes=doneMinutes,
            remaining=remaining,due=due,fraction=fraction,
            activeDays=store.activeDaysLast7(),''',
                "weekly total on existing banner")
    daily.write_text(text, "utf-8")

    shutil.copyfile(PKG / "WordQuest175PracticeVisuals.kt",
                    SOURCE / "WordQuest175PracticeVisuals.kt")
    print("PASS: custom 5..180-minute plan goals with backup and seven-day activity are installed.")

if __name__ == "__main__":
    run()
