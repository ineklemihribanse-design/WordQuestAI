#!/usr/bin/env python3
"""Apply Package 4 to an assembled 1.0.85 tree, never to production.

Fail-closed unified-diff application preserves all unrelated v1.0.85 changes.
Run only after the normal v185 assembly step. On ANY conflict no patch is applied.
"""
import difflib
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
P4 = ROOT / "tools/v186"
BASE = P4 / "base"
CHANGED = P4 / "staged"
MODIFIED = [
    "AppSession.kt", "CustomLayout.kt", "HomeScreen.kt",
    "NewGameScreen.kt", "PracticeHubScreen.kt", "PuzzleScreen.kt",
    "VocabularyBank.kt",
]
ADDED = [
    "PuzzleClockStore.kt", "Wq186GameCards.kt",
    "Wq186ParagraphRepository.kt", "Wq186ParagraphWorkspace.kt",
]

def main() -> None:
    if not (ROOT / "tools/v185/apply_profile_185.py").is_file():
        raise SystemExit("Missing v185 baseline; abort.")
    if not (SRC / "WordQuest175ParagraphDictation.kt").is_file():
        raise SystemExit("The full v175 paragraph classes were not assembled; abort.")
    if not (SRC / "Wq176DailyChallenge.kt").is_file():
        raise SystemExit("The daily YDS source is missing; abort.")
    # Three-way merge current 1.0.85 against the exact old source and new P4.
    # All merges are computed in memory before any application.
    import re

    def resolve_conflicts(name: str, text: str) -> str:
        # Select exact, audited combinations; never silently take an unknown side.
        pattern = re.compile(
            r"(?ms)^<<<<<<<[^\n]*\n(.*?)^\|\|\|\|\|\|\|[^\n]*\n"
            r"(.*?)^=======\n(.*?)^>>>>>>>[^\n]*\n"
        )
        choices = []
        def choose(match):
            ours, base, new = match.groups()
            # v175's premium home grid supersedes the old legacy menu.
            # Package 4 must preserve that approved grid rather than reintroduce it.
            if name == "HomeScreen.kt" and "Wq175PremiumHomeGrid(" in ours and 'PrimaryAction("Yeni Oyun", newGame)' in new:
                choices.append((name, "retain approved premium home navigation"))
                return ours
            # Keep the existing 1.0.85 deadline AND the optional new clock;
            # old saved timed games must keep their original time limit.
            if name == "PuzzleScreen.kt" and "if (shuffling || timedOut) return" in ours and "if (shuffling || outOfTime) return" in new:
                choices.append((name, "honor original timedOut and new outOfTime"))
                return ours.replace(
                    "if (shuffling || timedOut) return",
                    "if (shuffling || timedOut || outOfTime) return",
                )
            # Retain v175 progress/streak tracking, but display its large
            # original banner only when the compact daily-goal card is edited.
            if name == "PracticeHubScreen.kt" and "Wq175DailyPlanBanner(" in ours and 'Text("Günlük çalışma hedefi"' in new:
                start = "        Wq175DailyPlanBanner("
                end = "            onMinutes={value->store.minutes=value;refresh++}\n        )"
                if ours.count(start) != 1 or ours.count(end) != 1:
                    raise ValueError("Unexpected daily-plan banner layout; safe stop")
                choices.append((name, "retain premium daily banner inside compact editable card"))
                return ours.replace(
                    start,
                    "        Wq186CollapsibleDailyPlan(mins, doneMinutes, remaining, store.isGoalConfigured) {\n"
                    "            Wq175DailyPlanBanner(",
                    1,
                ).replace(
                    end,
                    "            onMinutes={value->store.minutes=value;refresh++}\n            )\n        }",
                    1,
                )
            if name == "HomeScreen.kt" and "session.profileKey" in ours and "clock = clock" in new:
                if "pronunciation, session.profileKey)" not in ours:
                    raise ValueError("HomeScreen signature unexpectedly changed")
                answer = ours.replace(
                    "pronunciation, session.profileKey) { progress ->",
                    "pronunciation, session.profileKey, clock = clock) { progress ->",
                )
                choices.append((name, "retain profileKey; add new countdown"))
                return answer
            if name == "PuzzleScreen.kt" and 'profileKey: String' in ours and 'clock: PuzzleClock?' in new:
                choices.append((name, "retain legacy timer profileKey; add independent clock"))
                return ours + new
            if name == "PracticeHubScreen.kt":
                if "DAILY_MARKER" in ours and "DAILY_GOAL_OPTIONS" in new:
                    choices.append((name, "retain safe legacy marker and add preset choices"))
                    return ours + "private val DAILY_GOAL_OPTIONS = listOf(15, 20, 25, 30, 40, 60)\n"
                if "get() = prefs.getInt(key" in ours:
                    choices.append((name, "retain existing 5..180 custom daily-goal support"))
                    return ours
                if 'val goal = prefs.getInt("plan.$date"' in ours:
                    choices.append((name, "retain existing imported daily-goal values"))
                    return ours
                if "day.goalMinutes !in 5..180" in ours:
                    choices.append((name, "retain existing daily-goal import validator"))
                    return ours
            if name == "HomeScreen.kt" and "Wq175PremiumHomeGrid" in ours:
                choices.append((name, "preserve approved v175 premium Home layout and mascot cards"))
                return ours
            if name == "PracticeHubScreen.kt" and "Wq175DailyPlanBanner" in ours:
                choices.append((name, "retain all v175/v176 daily widgets for targeted compact-banner insertion"))
                return ours
            if name == "PracticeHubScreen.kt":
                choices.append((name, "retain newer daily mode and progress logic"))
                return ours
            if name == "PuzzleScreen.kt" and "timedOut" in ours:
                choices.append((name, "retain legacy timer AND new countdown expiration"))
                return ours.replace("|| timedOut)", "|| timedOut || outOfTime)").replace(
                    "if (timedOut)", "if (timedOut || outOfTime)"
                )
            if name == "PuzzleScreen.kt":
                choices.append((name, "preserve v185 responsive board and input behavior"))
                return ours
            print("UNRESOLVED CONFLICT:", name, flush=True)
            print("ours: " + ours[:900], flush=True)
            print("theirs: " + new[:900], flush=True)
            raise ValueError(f"Manual decision required for {name}")

        resolved = pattern.sub(choose, text)
        if "<<<<<<<" in resolved or ">>>>>>>" in resolved:
            raise ValueError(f"Unresolved markers in {name}")
        print(f"Resolved {len(choices)} audited conflicts for {name}", flush=True)
        return resolved

    merged = {}
    conflicts = []
    for name in MODIFIED + ["Wq176DailyChallenge.kt"]:
        old = BASE / name
        revised = (P4 / "Wq176DailyChallenge_186.kt") if name.startswith("Wq176") else (CHANGED / name)
        current = SRC / name
        if not old.is_file() or not revised.is_file() or not current.is_file():
            raise SystemExit(f"Missing base/source/revised for {name}; abort.")
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            paths = [root / "ours.kt", root / "base.kt", root / "theirs.kt"]
            paths[0].write_bytes(current.read_bytes())
            paths[1].write_bytes(old.read_bytes())
            paths[2].write_bytes(revised.read_bytes())
            if name == "NewGameScreen.kt":
                # New settings form intentionally replaces the old single-choice form.
                # Preserve the approved v175 full-colour artwork.
                actual = current.read_text("utf-8")
                if "Wq175NewGameHero(level,session.profileKey,available)" not in actual:
                    raise SystemExit("SAFE STOP: approved New Game hero missing")
                revisedText = revised.read_text("utf-8")
                anchor = '    ScreenPage("Yeni Oyun", "Kelime havuzunu, oyun biçimini ve tahta boyutunu seç", back) {\n'
                if revisedText.count(anchor) != 1:
                    raise SystemExit("SAFE STOP: new card-form anchor missing")
                merged[name] = revisedText.replace(
                    anchor, anchor +
                    '        Wq175NewGameHero(levels.first(),session.profileKey,available)\n',
                )
                continue
            result = subprocess.run(
                ["git", "merge-file", "-p", "--diff3", *[str(x) for x in paths]],
                text=True, capture_output=True,
            )
            if result.returncode > 0:
                try:
                    merged[name] = resolve_conflicts(name, result.stdout)
                except ValueError as e:
                    conflicts.append(name)
                    print("SAFE STOP:", str(e), flush=True)
            elif result.returncode == 0:
                merged[name] = result.stdout
            else:
                conflicts.append(name)
                print("git merge-file fatal error:", result.stderr[:800], flush=True)
    if conflicts:
        raise SystemExit("SAFE STOP: manual merge required in " + ", ".join(conflicts))
    for name in ADDED:
        origin = (CHANGED / name) if (CHANGED / name).is_file() else (P4 / name)
        if not origin.is_file() or (SRC / name).exists():
            raise SystemExit(f"Missing new source or source already exists: {name}; abort.")
    for name, body in merged.items():
        (SRC / name).write_text(body, encoding='utf-8')
    for name in ADDED:
        origin = (CHANGED / name) if (CHANGED / name).is_file() else (P4 / name)
        shutil.copyfile(origin, SRC / name)
    # Replace only the single old daily-goal banner with the compact editor.
    # The custom paragraphs, YDS daily challenge, and five mini-games stay intact.
    hub = SRC / "PracticeHubScreen.kt"
    legacy = hub.read_text("utf-8")
    old_card = re.compile(
        r"(?ms)^        Wq175DailyPlanBanner\(\n.*?^        \)\n"
    )
    candidate = (CHANGED / "PracticeHubScreen.kt").read_text("utf-8")
    intro = '        androidx.compose.material3.ElevatedCard(Modifier.fillMaxWidth()) {'
    start = candidate.index(intro)
    end = candidate.index('        for (kind in PracticeKind.entries) {', start)
    new_card = candidate[start:end]
    if len(old_card.findall(legacy)) != 1:
        raise SystemExit("SAFE STOP: exactly one approved v175 DailyPlanBanner expected")
    legacy = old_card.sub(lambda _: new_card, legacy, count=1)
    hub.write_text(legacy, encoding="utf-8")
    # Add the new paragraph tool without losing existing dictation entries and callbacks.
    hub = SRC / "PracticeHubScreen.kt"
    text = hub.read_text("utf-8")
    if text.count("Wq175ParagraphPracticeEntry(session.profileKey)") != 2:
        raise SystemExit("SAFE STOP: paragraph hooks differ; manual review required.")
    text = text.replace("Wq175ParagraphPracticeEntry(session.profileKey)",
                        "Wq186ParagraphPracticeEntry(session.profileKey, ai)")
    old_header = "    session: AppSession,\n    pronunciation: WordPronunciation,"
    new_header = "    session: AppSession,\n    ai: AiSession,\n    pronunciation: WordPronunciation,"
    if text.count(old_header) != 1:
        raise SystemExit("SAFE STOP: PracticeHubScreen signature differs.")
    hub.write_text(text.replace(old_header, new_header, 1), "utf-8")
    home = SRC / "HomeScreen.kt"
    text = home.read_text("utf-8")
    old_call = "PracticeHubScreen(session, pronunciation,"
    if text.count(old_call) != 1:
        raise SystemExit("SAFE STOP: daily-plan navigation signature differs.")
    home.write_text(text.replace(old_call, "PracticeHubScreen(session, ai, pronunciation,", 1), "utf-8")

    # Existing paragraph dialog stays available in other routes; British is
    # the initial choice only and never overwrites an explicit selection.
    paragraph = SRC / "WordQuest175ParagraphDictation.kt"
    old_voice = 'var accent by rememberSaveable(profileKey) { mutableStateOf("US") }'
    new_voice = 'var accent by rememberSaveable(profileKey) { mutableStateOf("UK") }'
    if not paragraph.is_file():
        raise SystemExit("SAFE STOP: v175 paragraph voice component missing")
    paragraph_text = paragraph.read_text("utf-8")
    if old_voice in paragraph_text:
        if paragraph_text.count(old_voice) != 1:
            raise SystemExit("SAFE STOP: ambiguous paragraph voice default")
        paragraph.write_text(paragraph_text.replace(old_voice, new_voice, 1), "utf-8")
    elif new_voice not in paragraph_text:
        raise SystemExit("SAFE STOP: unknown paragraph voice default")
    assert new_voice in paragraph.read_text("utf-8")
    assert 'mutableStateOf("UK")' in (SRC / "Wq186ParagraphWorkspace.kt").read_text("utf-8")

    assert "MAX_BOARD_WORDS = 70" in (SRC / "CustomLayout.kt").read_text("utf-8")
    assert "newGameFiltered(" in (SRC / "AppSession.kt").read_text("utf-8")
    assert "Wq186ParagraphPracticeEntry(session.profileKey, ai)" in hub.read_text("utf-8")
    assert "Wq186ParagraphRepository.kt" in [p.name for p in SRC.iterdir()]
    print("PASS: staged Package 4 source merged only where v185 anchors match.")
    print("No signing, APK publishing, Firebase or account-data migration.")

if __name__ == "__main__":
    main()
