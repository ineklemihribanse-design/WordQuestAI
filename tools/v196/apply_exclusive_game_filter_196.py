#!/usr/bin/env python3
"""One New Game filter group: either topic OR word type, never intersect both.

Keep the existing card/button UI, CEFR/YDS multi-level selection, time modes,
70-word limit, countdown, and puzzle creation unchanged. Run after v195.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
HERE = Path(__file__).resolve().parent
FILE = SOURCE / "NewGameScreen.kt"


def one(s: str, old: str, new: str, name: str) -> str:
    count = s.count(old)
    assert count == 1, f"{name}: expected exactly one anchor, got {count}"
    return s.replace(old, new, 1)


def apply() -> None:
    original = FILE.read_text(encoding="utf-8")
    assert "MAX_NEW_GAME_WORDS = 70" in original, "keep 70-word board limit"
    assert "toggleLevel(value: String)" in original, "keep multi-level selection"
    assert 'session.newGameFiltered(levels, topics, kinds, count!!)' in original
    assert 'NewGameOptions(englishLevels, levels.toSet()' in original
    assert 'PuzzleClockMode.Countdown' in original

    s = one(
        original,
        '''    var kindsSelected by rememberSaveable { mutableStateOf(initialKind?.let(::listOf).orEmpty()) }
''',
        '''    var kindsSelected by rememberSaveable { mutableStateOf(initialKind?.let(::listOf).orEmpty()) }
    // This is a CHOICE of filter types, not an intersection of two filters.
    var filterMode by rememberSaveable {
        mutableStateOf(Wq196FilterPolicy.mode(initialTopic, initialKind))
    }
''',
        "save single filter mode"
    )
    s = one(
        s,
        '''    val topics = topicsSelected.filter { it in optionsTopic }.toSet()
    val kinds = kindsSelected.filter { it in optionsKind }.toSet()''',
        '''    // Restoration and deep-link safety: inactive filters are always empty
    // even when older saved state contained both kinds and topics.
    val topics = Wq196FilterPolicy.activeTopics(
        filterMode, topicsSelected.filter { it in optionsTopic }
    )
    val kinds = Wq196FilterPolicy.activeKinds(
        filterMode, kindsSelected.filter { it in optionsKind }
    )
    check(topics.isEmpty() || kinds.isEmpty())''',
        "enforce exclusive data-level selections"
    )
    a = s.index('        NewGameSection("Konu", "Birden fazla konu seç.')
    b = s.index('        NewGameSection("Oyun Biçimi",', a)
    previous = s[a:b]
    assert previous.count('NewGameSection("Konu"') == 1
    assert previous.count('NewGameSection("Kelime Türü"') == 1
    assert "NewGameOptions(optionsTopic, topics, !loading)" in previous
    assert "NewGameOptions(optionsKind, kinds, !loading)" in previous
    replacement = '''        NewGameSection("Kelime Havuzu", "Konu veya kelime türü: yalnızca birini seç.") {
            // Two large, existing rounded button cards; never two active groups.
            NewGameOptions(
                listOf(Wq196FilterPolicy.TOPIC, Wq196FilterPolicy.KIND),
                setOf(filterMode), !loading,
            ) { selected ->
                if (filterMode != selected) {
                    filterMode = selected
                    topicsSelected = emptyList()
                    kindsSelected = emptyList()
                    error = ""
                }
            }
            if (filterMode == Wq196FilterPolicy.TOPIC) {
                BodyText("Konular", Forest)
                if (optionsTopic.isEmpty()) BodyText("Konu seçenekleri yükleniyor…")
                else {
                    NewGameOptions(
                        listOf("Tüm konular"),
                        if (topics.isEmpty()) setOf("Tüm konular") else emptySet(),
                        !loading,
                    ) { topicsSelected = emptyList(); error = "" }
                    NewGameOptions(optionsTopic, topics, !loading) { name ->
                        topicsSelected =
                            if (name in topics) topics.filterNot { it == name }
                            else topics.toList() + name
                        kindsSelected = emptyList()
                        error = ""
                    }
                }
            } else {
                BodyText("Kelime Türleri", Forest)
                if (optionsKind.isEmpty()) {
                    BodyText("Bu seviyeler için bütün kelime türleri kullanılacak.")
                } else {
                    NewGameOptions(
                        listOf("Tüm türler"),
                        if (kinds.isEmpty()) setOf("Tüm türler") else emptySet(),
                        !loading,
                    ) { kindsSelected = emptyList(); error = "" }
                    NewGameOptions(optionsKind, kinds, !loading) { name ->
                        kindsSelected =
                            if (name in kinds) kinds.filterNot { it == name }
                            else kinds.toList() + name
                        topicsSelected = emptyList()
                        error = ""
                    }
                }
            }
        }

'''
    s = s[:a] + replacement + s[b:]
    assert s.replace(replacement, previous, 1).replace(
        '''    // Restoration and deep-link safety: inactive filters are always empty
    // even when older saved state contained both kinds and topics.
    val topics = Wq196FilterPolicy.activeTopics(
        filterMode, topicsSelected.filter { it in optionsTopic }
    )
    val kinds = Wq196FilterPolicy.activeKinds(
        filterMode, kindsSelected.filter { it in optionsKind }
    )
    check(topics.isEmpty() || kinds.isEmpty())''',
        '''    val topics = topicsSelected.filter { it in optionsTopic }.toSet()
    val kinds = kindsSelected.filter { it in optionsKind }.toSet()''', 1
    ).replace(
        '''    var kindsSelected by rememberSaveable { mutableStateOf(initialKind?.let(::listOf).orEmpty()) }
    // This is a CHOICE of filter types, not an intersection of two filters.
    var filterMode by rememberSaveable {
        mutableStateOf(Wq196FilterPolicy.mode(initialTopic, initialKind))
    }
''',
        '''    var kindsSelected by rememberSaveable { mutableStateOf(initialKind?.let(::listOf).orEmpty()) }
''', 1
    ) == original, "Unexpected change outside New Game filters"

    for protected in (
        'NewGameOptions(englishLevels, levels.toSet(), !loading, ::toggleLevel)',
        'NewGameOptions(ydsLevels.map { "YDS $it" }',
        'session.newGameFiltered(levels, topics, kinds, count!!)',
        'MAX_NEW_GAME_WORDS = 70',
        'val selectedClock = PuzzleClockMode.entries',
        'if (customCount) OutlinedTextField(',
        'Wq175NewGameHero(',
        'fun toggleLevel(value: String)',
    ):
        assert protected in s, protected

    assert s.count('NewGameSection("Kelime Havuzu"') == 1
    assert 'filterMode = selected' in s
    assert 'check(topics.isEmpty() || kinds.isEmpty())' in s
    assert s.count('NewGameOptions(optionsTopic, topics, !loading)') == 1
    assert s.count('NewGameOptions(optionsKind, kinds, !loading)') == 1

    for name in ("Wq196FilterPolicy.kt", "Wq196FilterPolicyTest.kt"):
        assert (HERE / name).is_file(), name
    TEST.mkdir(parents=True, exist_ok=True)
    FILE.write_text(s, encoding="utf-8")
    shutil.copyfile(HERE / "Wq196FilterPolicy.kt", SOURCE / "Wq196FilterPolicy.kt")
    shutil.copyfile(HERE / "Wq196FilterPolicyTest.kt", TEST / "Wq196FilterPolicyTest.kt")
    print("PASS v196: keep attractive existing rounded card buttons, not chips")
    print("PASS v196: either topic OR word kind with one selected panel")
    print("PASS v196: keep multi-selection within chosen group and all level/time/count settings")


if __name__ == "__main__":
    apply()
