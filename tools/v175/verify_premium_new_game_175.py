#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"NewGameScreen.kt").read_text("utf-8")
hero=(src/"WordQuest175NewGameHero.kt").read_text("utf-8")
assert s.count("Wq175NewGameHero(level,session.profileKey,available)")==1
assert s.count("Wq175NewGameSummary(level,topic,kind,count,available,timedMinutes)")==1
assert s.index("Wq175NewGameHero(")<s.index('BodyText("Seviye")')
assert s.index("Wq175NewGameSummary(")<s.index('PrimaryAction(if (loading)')
assert 'session.newGame(level, topic, kind, size)' in s
assert 'Wq175TimedPuzzle.configure(context,session.profileKey' in s
assert 'Wq2DPortrait(' in hero
assert 'available+" kelime hazır"' in hero
assert '"OYUN ÖZETİ"' in hero
print("PASS: premium crossword builder uses real selected mascot and real game data.")
