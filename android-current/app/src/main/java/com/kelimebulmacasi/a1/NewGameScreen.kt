package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Never force a 70-word selection onto several screens: one complete crossword.
private const val MAX_NEW_GAME_WORDS = 70
private val gameSizes = listOf(10, 20, 30, 50, 70)
private val timeSizes = listOf(15, 20, 25, 30, 40)

@Composable
internal fun NewGameScreen(
    session: AppSession, initialLevel: String, initialTopic: String?,
    initialKind: String?, back: () -> Unit, open: (Game) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var bank by remember(session) { mutableStateOf<VocabularyBank?>(null) }

    // Blank topic / kind selections mean ALL; other selections are multi-select.
    val initial = initialLevel.takeIf { it in englishLevels || it in ydsLevels } ?: "A1"
    var levels by rememberSaveable { mutableStateOf(listOf(initial)) }
    var topicsSelected by rememberSaveable { mutableStateOf(initialTopic?.let(::listOf).orEmpty()) }
    var kindsSelected by rememberSaveable { mutableStateOf(initialKind?.let(::listOf).orEmpty()) }
    var clockMode by rememberSaveable { mutableStateOf(PuzzleClockMode.Classic.name) }
    var clockPreset by rememberSaveable { mutableIntStateOf(20) }
    var customTime by rememberSaveable { mutableStateOf(false) }
    var customMinutes by rememberSaveable { mutableStateOf("") }
    var countPreset by rememberSaveable { mutableIntStateOf(20) }
    var customCount by rememberSaveable { mutableStateOf(false) }
    var countText by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(session) {
        try {
            bank = session.vocabulary()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Kelime havuzu yüklenemedi."
        }
    }

    val yds = levels.firstOrNull() in ydsLevels
    val type = if (yds) GameMode.Yds else GameMode.Topics
    val optionsTopic = remember(bank, levels) {
        levels.flatMap { bank?.categories(it, type).orEmpty() }.distinct().sorted()
    }
    val optionsKind = remember(bank, levels) {
        levels.flatMap { bank?.categories(it, GameMode.Kinds).orEmpty() }.distinct().sorted()
    }
    val topics = topicsSelected.filter { it in optionsTopic }.toSet()
    val kinds = kindsSelected.filter { it in optionsKind }.toSet()
    val available = remember(bank, levels, topics, kinds) {
        val lib = bank ?: return@remember 0
        val ts: List<String?> = if (topics.isEmpty()) listOf(null) else topics.toList()
        val ks: List<String?> = if (kinds.isEmpty()) listOf(null) else kinds.toList()
        levels.flatMap { lv ->
            ts.flatMap { t -> ks.flatMap { k -> lib.pool(lv, t, k) } }
        }.distinctBy(Entry::letters).size
    }
    val maxAllowed = minOf(available, MAX_NEW_GAME_WORDS)
    val count = if (customCount) countText.toIntOrNull() else countPreset
    val minutes = if (customTime) customMinutes.toIntOrNull() else clockPreset
    val selectedClock = PuzzleClockMode.entries.firstOrNull { it.name == clockMode }
        ?: PuzzleClockMode.Classic

    fun toggleLevel(value: String) {
        val group = if (value in ydsLevels) ydsLevels else englishLevels
        val same = levels.filter { it in group }
        levels = when {
            value !in same -> (same + value).distinct()
            same.size > 1 -> same - value
            else -> same
        }
        topicsSelected = emptyList()
        kindsSelected = emptyList()
        error = ""
    }

    ScreenPage("Yeni Oyun", "Kelime havuzunu, oyun biçimini ve tahta boyutunu seç", back) {
        NewGameSection("Seviye", "Birden fazla CEFR veya YDS seviyesi seçebilirsin") {
            BodyText("CEFR seviyeleri", Forest)
            NewGameOptions(englishLevels, levels.toSet(), !loading, ::toggleLevel)
            BodyText("YDS seviyeleri", Forest)
            NewGameOptions(ydsLevels.map { "YDS $it" },
                levels.map { "YDS $it" }.toSet(), !loading) { label ->
                toggleLevel(label.removePrefix("YDS "))
            }
        }

        NewGameSection("Konu", "Birden fazla konu seç. Seçim yoksa tüm konular kullanılır.") {
            if (optionsTopic.isEmpty()) BodyText("Konu seçenekleri yükleniyor…")
            else {
                NewGameOptions(listOf("Tüm konular"), if (topics.isEmpty()) setOf("Tüm konular") else emptySet(), !loading) {
                    topicsSelected = emptyList(); error = ""
                }
                NewGameOptions(optionsTopic, topics, !loading) { name ->
                    topicsSelected = if (name in topics) topics.filterNot { it == name } else topics.toList() + name
                    error = ""
                }
            }
        }

        NewGameSection("Kelime Türü", "İsim, fiil, sıfat ve diğer türleri birlikte seç") {
            if (optionsKind.isEmpty()) BodyText("Bu seviyeler için bütün kelime türleri kullanılacak.")
            else {
                NewGameOptions(listOf("Tüm türler"), if (kinds.isEmpty()) setOf("Tüm türler") else emptySet(), !loading) {
                    kindsSelected = emptyList(); error = ""
                }
                NewGameOptions(optionsKind, kinds, !loading) { name ->
                    kindsSelected = if (name in kinds) kinds.filterNot { it == name } else kinds.toList() + name
                    error = ""
                }
            }
        }

        NewGameSection("Oyun Biçimi", "Klasik, süreli veya kronometre ile çöz") {
            val modes = listOf("Klasik", "Süreli", "Sayaçlı")
            val names = listOf(PuzzleClockMode.Classic.name, PuzzleClockMode.Countdown.name, PuzzleClockMode.Stopwatch.name)
            NewGameOptions(modes, setOf(modes[names.indexOf(clockMode).coerceAtLeast(0)]), !loading) { label ->
                clockMode = names[modes.indexOf(label)]
                error = ""
            }
            if (selectedClock == PuzzleClockMode.Countdown) {
                BodyText("Süre seç (dakika)", Forest)
                NewGameOptions(timeSizes.map { "$it dk" } + "Özel",
                    if (customTime) setOf("Özel") else setOf("$clockPreset dk"), !loading) { label ->
                    customTime = label == "Özel"
                    if (!customTime) clockPreset = label.removeSuffix(" dk").toInt()
                }
                if (customTime) OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter(Char::isDigit).take(3) },
                    label = { Text("Özel süre (1–180 dakika)", fontFamily = Sans) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = minutes != null && minutes !in 1..180,
                )
            }
            if (selectedClock == PuzzleClockMode.Stopwatch)
                BodyText("Süre yukarı doğru sayılır; zaman sınırı yoktur.")
        }

        NewGameSection("Kelime Sayısı", "70 kelimeye kadar tek, büyük bir bulmaca tahtası") {
            NewGameOptions(gameSizes.map(Int::toString) + "Özel",
                if (customCount) setOf("Özel") else setOf(countPreset.toString()), !loading) { label ->
                customCount = label == "Özel"
                if (!customCount) countPreset = label.toInt()
                error = ""
            }
            if (customCount) OutlinedTextField(
                countText, { countText = it.filter(Char::isDigit).take(2); error = "" },
                modifier = Modifier.fillMaxWidth(), label = { Text("Özel kelime sayısı (8–70)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = count != null && count !in MIN_NEW_GAME_WORDS..maxAllowed,
            )
            when {
                bank == null && error.isBlank() -> BodyText("Kelime kütüphanesi yükleniyor…")
                available < MIN_NEW_GAME_WORDS -> BodyText("Seçilen filtrelerde en az 8 farklı kelime bulunmalı.", ErrorInk)
                else -> BodyText("$available farklı kelime · seçilebilir aralık 8–$maxAllowed.", Forest)
            }
        }

        if (error.isNotBlank()) BodyText(error, ErrorInk)
        val valid = !loading && bank != null && count != null &&
            count in MIN_NEW_GAME_WORDS..maxAllowed &&
            (selectedClock != PuzzleClockMode.Countdown || minutes != null && minutes in 1..180)
        PrimaryAction(if (loading) "Bulmaca hazırlanıyor…" else "Bulmacayı başlat", {
            if (!valid) return@PrimaryAction
            focus.clearFocus()
            loading = true
            error = ""
            scope.launch {
                try {
                    val game = session.newGameFiltered(levels, topics, kinds, count!!)
                    if (game == null) {
                        error = "Bu kelimeler tek tahtaya sığmadı. Konuları genişlet veya farklı sayı seç."
                    } else {
                        // Clock starts exactly when a new game has been successfully saved.
                        PuzzleClockStore(context, session.profileKey).save(
                            game.id, selectedClock, minutes?.takeIf { it in 1..180 } ?: 20,
                        )
                        open(game)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "Bulmaca oluşturulamadı. Farklı filtrelerle tekrar dene."
                } finally {
                    loading = false
                }
            }
        }, enabled = valid)
    }
}

@Composable
private fun NewGameSection(
    title: String, subtitle: String,
    body: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontFamily = Serif, color = Ink, fontWeight = FontWeight.Bold)
            BodyText(subtitle)
            body()
        }
    }
}

@Composable
private fun NewGameOptions(
    options: List<String>, selected: Set<String>, enabled: Boolean,
    choose: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        for (group in options.chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (label in group) {
                    val active = label in selected
                    OutlinedButton(
                        onClick = { choose(label) }, enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (active) LightLime else White,
                            contentColor = Ink,
                        ),
                    ) {
                        Text(
                            (if (active) "✓ " else "") + label,
                            fontFamily = Sans, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (group.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
