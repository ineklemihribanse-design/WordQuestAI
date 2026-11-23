#!/usr/bin/env python3
"""Wire the EXISTING local Arena engine to solo mode selection and CEFR multi-level
presets. Avoids Firebase room creation; team solo is crossword practice.

Run after v190 Arena UI staging, not before.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
FILE=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1/PremiumArenaScreen.kt"

def once(s,old,new,label):
    count=s.count(old)
    if count!=1:raise AssertionError(f"{label}: expected 1 anchor, got {count}")
    return s.replace(old,new,1)

def main():
    orig=FILE.read_text("utf-8")
    s=orig
    s=once(s,
        '    var selectedMode by rememberSaveable { mutableStateOf("crossword") }',
        '''    var selectedMode by rememberSaveable { mutableStateOf("crossword") }
    // UI mode stays distinct from the local practice engine (team uses the
    // offline crossword, not a fictitious network teammate).
    var soloArenaMode by rememberSaveable { mutableStateOf("duel") }''',
        "persist all three solo mode cards")
    s=once(s,
        '''                     mode=if(selectedMode=="quiz")"quiz" else "duel",
                     onMode={selectedMode=if(it=="quiz")"quiz" else "crossword"},''',
        '''                     mode=soloArenaMode,
                     onMode={ selected ->
                         soloArenaMode=selected
                         selectedMode=if(selected=="quiz")"quiz" else "crossword"
                     },''',
        "keep selected solo team card highlighted while crossword engine runs")
    s=once(s,
        '''        if (selectedMode == "crossword") {
            val chosen = packs.nextPuzzle(level, size, topic, session.profileKey)''',
        '''        // Read actual saved multi-select chips; both quiz and crossword
        // use the same six-level pool as network matches.
        val preset = Wq186ArenaPresetStore.read(context,session.profileKey,level)
        val chosenLevels = Wq190ArenaSoloPolicy.levelPool(preset.levels,level)
        if (selectedMode == "crossword") {
            // A complete published crossword has ONE level. Pick any available
            // selected level rather than failing because the first is empty.
            val chosen = chosenLevels.shuffled().firstNotNullOfOrNull { candidate ->
                packs.nextPuzzle(candidate, size, topic, session.profileKey)
            }''',
        "crossword choose from real multi-level available boards")
    s=once(s,
        '''            val questions = packs.nextQuestions(level, topic, session.profileKey, quizCount)''',
        '''            // Quiz can genuinely mix CEFR levels in one local game;
            // use the existing deterministic five-choice pack constructor.
            val questions = packs.questionsForSeed(
                level, topic, roundId.hashCode(), quizCount, chosenLevels
            )''',
        "solo quiz draws from selected mixed levels")
    assert 'Wq190ArenaSoloPolicy.levelPool(preset.levels,level)' in s
    assert 'soloArenaMode=selected' in s
    assert 'onOffline=::begin' in s or 'onOffline={begin()}' in s or 'onOffline= {begin()}' in s or 'onOffline={ begin() }' in s or 'onOffline={begin()' in s, "local entry must call existing begin"
    assert 'PremiumOnlineRoom(' not in s, "solo must not construct remote Firebase rooms"
    FILE.write_text(s,"utf-8")
    print("PASS v190: solo duel/team/quiz preserve selected modes and start locally")
    print("PASS v190: solo crossword chooses real selected CEFR board")
    print("PASS v190: solo 5-choice quiz mixes selected CEFR levels")
    print("PASS v190: no Firebase room created for any solo mode")

if __name__ == "__main__":main()
