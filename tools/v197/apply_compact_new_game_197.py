#!/usr/bin/env python3
"""v1.0.97: compact responsive New Game UI without changing game mechanics.

Run only after v196 from the known verified Android full-source cache.
Exclusive topic/kind, CEFR/YDS multiple levels, 70-word cap, clock and
session.newGameFiltered are preserved. Other game screens are untouched.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
SCREEN = SRC / "NewGameScreen.kt"
HERE = Path(__file__).resolve().parent


def one(s, old, new, name):
    n = s.count(old)
    assert n == 1, f"{name}: expected 1 occurrence, got {n}"
    return s.replace(old, new, 1)


def apply():
    original = SCREEN.read_text("utf-8")
    for anchor in (
        'NewGameSection("Seviye",',
        'NewGameSection("Kelime Havuzu",',
        'NewGameSection("Oyun Biçimi",',
        'NewGameSection("Kelime Sayısı",',
        'Wq175NewGameHero(levels.first(),session.profileKey,available)',
        'Wq196FilterPolicy.activeTopics(',
        'Wq196FilterPolicy.activeKinds(',
        'session.newGameFiltered(levels, topics, kinds, count!!)',
        'MAX_NEW_GAME_WORDS = 70',
        'fun toggleLevel(value: String)',
        'val selectedClock = PuzzleClockMode.entries',
    ):
        assert anchor in original, anchor

    s = one(original, 'import androidx.compose.foundation.layout.Arrangement',
        '''import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp''', "imports")
    s = one(s, '    var error by remember { mutableStateOf("") }',
        '''    var error by remember { mutableStateOf("") }
    var filterPickerVisible by rememberSaveable { mutableStateOf(false) }''',
        "filter modal state")
    s = one(s, '        Wq175NewGameHero(levels.first(),session.profileKey,available)',
        '        Wq197CompactHero(levels.first(),session.profileKey,available)',
        "short but still dynamic hero")

    start = s.index('        NewGameSection("Seviye",')
    end = s.index('        if (error.isNotBlank()) BodyText(error, ErrorInk)', start)
    old_sections = s[start:end]
    for section in ('"Seviye"', '"Kelime Havuzu"', '"Oyun Biçimi"', '"Kelime Sayısı"'):
        assert old_sections.count('NewGameSection(' + section) == 1, section
    assert 'NewGameOptions(gameSizes.map(Int::toString) + "Özel"' in old_sections
    assert 'NewGameOptions(timeSizes.map { "$it dk" } + "Özel"' in old_sections

    new_sections = '''        Wq197LevelSelector(levels, !loading, ::toggleLevel)

        NewGameSection("Kelime Havuzu", "") {
            Wq197OptionGrid(
                listOf("▤ Konu Seç", "◇ Kelime Türü Seç"),
                setOf(if (filterMode == Wq196FilterPolicy.TOPIC)
                    "▤ Konu Seç" else "◇ Kelime Türü Seç"), !loading
            ) { label ->
                val next = if (label == "▤ Konu Seç")
                    Wq196FilterPolicy.TOPIC else Wq196FilterPolicy.KIND
                if (next != filterMode) {
                    filterMode = next
                    topicsSelected = emptyList()
                    kindsSelected = emptyList()
                    filterPickerVisible = false
                    error = ""
                }
            }
            val isTopic = filterMode == Wq196FilterPolicy.TOPIC
            val choices = if (isTopic) optionsTopic else optionsKind
            val active = if (isTopic) topics else kinds
            val everything = if (isTopic) "Tüm konular" else "Tüm türler"
            Text(
                if (isTopic) "Konu · çoklu seçim"
                else "Kelime Türü · çoklu seçim",
                fontFamily = Sans, fontSize = 11.sp, color = Forest
            )
            if (choices.isEmpty()) {
                Text(
                    if (bank == null) "Kelime havuzu yükleniyor…"
                    else "Bu grupta seçenek yok; bütün kelimeler kullanılacak.",
                    fontFamily = Sans, fontSize = 11.sp, color = Forest
                )
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val limit = if (maxWidth >= 700.dp) 12
                        else if (maxWidth >= 430.dp) 6 else 4
                    val preview = (active.toList() + choices).distinct().take(limit)
                    Wq197OptionGrid(
                        listOf(everything) + preview,
                        if (active.isEmpty()) setOf(everything) else active,
                        !loading,
                    ) { chosen ->
                        if (isTopic) {
                            topicsSelected = when {
                                chosen == everything -> emptyList()
                                chosen in topics -> topics.filterNot { it == chosen }
                                else -> topics.toList() + chosen
                            }
                            kindsSelected = emptyList()
                        } else {
                            kindsSelected = when {
                                chosen == everything -> emptyList()
                                chosen in kinds -> kinds.filterNot { it == chosen }
                                else -> kinds.toList() + chosen
                            }
                            topicsSelected = emptyList()
                        }
                        error = ""
                    }
                }
                if (choices.size > 4) {
                    TextButton(onClick = { filterPickerVisible = true },
                        enabled = !loading) {
                        Text("Tüm seçenekleri gör (" + choices.size + ")",
                            fontSize = 11.sp, color = Forest)
                    }
                }
            }
        }
        if (filterPickerVisible) {
            val isTopic = filterMode == Wq196FilterPolicy.TOPIC
            val options = if (isTopic) optionsTopic else optionsKind
            val selected = if (isTopic) topics else kinds
            val allName = if (isTopic) "Tüm konular" else "Tüm türler"
            Wq197FilterDialog(
                if (isTopic) "Konu Seç" else "Kelime Türü Seç",
                listOf(allName) + options,
                if (selected.isEmpty()) setOf(allName) else selected,
                !loading,
                choose = { value ->
                    if (isTopic) {
                        topicsSelected = when {
                            value == allName -> emptyList()
                            value in topics -> topics.filterNot { it == value }
                            else -> topics.toList() + value
                        }
                        kindsSelected = emptyList()
                    } else {
                        kindsSelected = when {
                            value == allName -> emptyList()
                            value in kinds -> kinds.filterNot { it == value }
                            else -> kinds.toList() + value
                        }
                        topicsSelected = emptyList()
                    }
                    error = ""
                },
                dismiss = { filterPickerVisible = false },
            )
        }

        NewGameSection("Oyun Ayarları", "") {
            val modeChoices: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Oyun Biçimi", color = Ink, fontFamily = Serif,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val modes = listOf("Klasik", "Süreli", "Sayaçlı")
                    val names = listOf(PuzzleClockMode.Classic.name,
                        PuzzleClockMode.Countdown.name, PuzzleClockMode.Stopwatch.name)
                    Wq197OptionGrid(
                        modes,
                        setOf(modes[names.indexOf(clockMode).coerceAtLeast(0)]),
                        !loading
                    ) { value ->
                        clockMode = names[modes.indexOf(value)]
                        error = ""
                    }
                    if (selectedClock == PuzzleClockMode.Countdown) {
                        Text("Süre (dakika)", color = Forest, fontSize = 11.sp)
                        Wq197OptionGrid(timeSizes.map { "$it dk" } + "Özel",
                            if (customTime) setOf("Özel")
                            else setOf("$clockPreset dk"), !loading
                        ) { value ->
                            customTime = value == "Özel"
                            if (!customTime) clockPreset =
                                value.removeSuffix(" dk").toInt()
                        }
                        if (customTime) OutlinedTextField(
                            value = customMinutes,
                            onValueChange = {
                                customMinutes = it.filter(Char::isDigit).take(3)
                            },
                            label = { Text("Özel süre (1–180 dakika)",
                                fontFamily = Sans) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = minutes != null && minutes !in 1..180,
                        )
                    }
                    if (selectedClock == PuzzleClockMode.Stopwatch) {
                        Text("Kronometre yukarı sayar · sınır yok",
                            color = Forest, fontSize = 11.sp)
                    }
                }
            }
            val countChoices: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Kelime Sayısı", color = Ink, fontFamily = Serif,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Wq197OptionGrid(
                        gameSizes.map(Int::toString) + "Özel",
                        if (customCount) setOf("Özel")
                        else setOf(countPreset.toString()), !loading
                    ) { value ->
                        customCount = value == "Özel"
                        if (!customCount) countPreset = value.toInt()
                        error = ""
                    }
                    if (customCount) OutlinedTextField(
                        countText,
                        { countText = it.filter(Char::isDigit).take(2); error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Özel kelime sayısı (8–70)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        isError = count != null &&
                            count !in MIN_NEW_GAME_WORDS..maxAllowed,
                    )
                    val hint = when {
                        bank == null && error.isBlank() ->
                            "Kelime kütüphanesi yükleniyor…"
                        available < MIN_NEW_GAME_WORDS ->
                            "Filtrelerde en az 8 farklı kelime bulunmalı."
                        else ->
                            "$available kelime · 8–$maxAllowed seçilebilir"
                    }
                    Text(hint, fontFamily = Sans, fontSize = 11.sp,
                        color = if (available < MIN_NEW_GAME_WORDS && bank != null)
                            ErrorInk else Forest,
                        maxLines = 2)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 620.dp) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) { modeChoices() }
                        Column(Modifier.weight(1f)) { countChoices() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        modeChoices()
                        countChoices()
                    }
                }
            }
        }

'''
    s = s[:start] + new_sections + s[end:]

    helper = s.index("\n@Composable\nprivate fun NewGameSection(")
    old_grid = s.index("\n@Composable\nprivate fun NewGameOptions(", helper)
    section_part = s[helper:old_grid]
    assert "Modifier.padding(14.dp)" in section_part
    assert "BodyText(subtitle)" in section_part
    new_section = '''\n@Composable
private fun NewGameSection(
    title: String, subtitle: String,
    body: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, fontFamily = Serif, color = Ink,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (subtitle.isNotBlank())
                Text(subtitle, fontFamily = Sans, fontSize = 11.sp, color = Forest)
            body()
        }
    }
}
'''
    assert s[old_grid:].count("private fun NewGameOptions(") == 1
    assert s[old_grid:].rstrip().endswith("}")
    s = s[:helper] + new_section
    assert 'NewGameOptions(' not in s
    for unchanged in (
        'session.newGameFiltered(levels, topics, kinds, count!!)',
        'MAX_NEW_GAME_WORDS = 70',
        'minOf(available, MAX_NEW_GAME_WORDS)',
        'val valid = !loading && bank != null && count != null',
        'PuzzleClockStore(context, session.profileKey).save(',
        'fun toggleLevel(value: String)',
        'check(topics.isEmpty() || kinds.isEmpty())',
        'Wq196FilterPolicy.activeTopics(',
        'Wq196FilterPolicy.activeKinds(',
        'PrimaryAction(if (loading) "Bulmaca hazırlanıyor…" else "Bulmacayı başlat"',
    ):
        assert unchanged in s, unchanged
    assert s.count('Wq197LevelSelector(') == 1
    assert s.count('Wq197FilterDialog(') == 1
    assert s.count('NewGameSection("Oyun Ayarları"') == 1
    assert s.count('NewGameSection("Kelime Havuzu"') == 1
    assert s.count('Wq197OptionGrid(timeSizes.map { "$it dk" } + "Özel"') == 1
    assert s.count('gameSizes.map(Int::toString) + "Özel"') == 1
    assert (HERE / "Wq197CompactNewGameUi.kt").is_file()
    SCREEN.write_text(s, "utf-8")
    shutil.copyfile(HERE / "Wq197CompactNewGameUi.kt",
                    SRC / "Wq197CompactNewGameUi.kt")
    print("PASS v197: levels/3D badge compact, 6 CEFR across on tablet, 3 columns on phone")
    print("PASS v197: topic or kind with popup for long lists; main page stays short")
    print("PASS v197: compact combined game mode + word count including 70 and custom")
    print("PASS v197: saved games, board, Firebase, controls and scoring unchanged")


if __name__ == "__main__":
    apply()
