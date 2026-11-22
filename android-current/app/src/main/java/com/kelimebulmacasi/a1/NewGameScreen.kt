package com.kelimebulmacasi.a1

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 20 words per board; 21–70 words are generated as linked 8–20 word boards.
private const val SAFE_SESSION_MAX_WORDS = 70
private val gameSizes = listOf(10, 20, 30, 50, 70)

@Composable
internal fun NewGameScreen(
    session: AppSession, initialLevel: String, initialTopic: String?, initialKind: String?, back: () -> Unit, open: (Game) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var bank by remember(session) { mutableStateOf<VocabularyBank?>(null) }
    var level by rememberSaveable { mutableStateOf(initialLevel) }
    var topic by rememberSaveable { mutableStateOf(initialTopic) }
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var preset by rememberSaveable { mutableIntStateOf(20) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var customValue by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val count = if (custom) customValue.toIntOrNull() else preset
    val yds = level in ydsLevels
    val topics = bank?.categories(level, if (yds) GameMode.Yds else GameMode.Topics).orEmpty()
    val kinds = bank?.categories(level, GameMode.Kinds).orEmpty()
    val available = remember(bank, level, topic, kind) { bank?.pool(level, topic, kind)?.size ?: 0 }
    val permitted = MIN_NEW_GAME_WORDS..minOf(available, SAFE_SESSION_MAX_WORDS)

    LaunchedEffect(session) {
        try { bank = session.vocabulary() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Kelime havuzu açılamadı." }
    }

    fun selectLevel(value: String) {
        level = value
        topic = null
        kind = null
        error = ""
    }

    ScreenPage("Yeni Oyun", "Kelime havuzunu ve tahta boyutunu seç", back) {
        BodyText("Seviye")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (value in englishLevels) FilterChip(
                selected = level == value, onClick = { selectLevel(value) }, enabled = !loading,
                label = { Text(value, fontFamily = Sans) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (value in ydsLevels) FilterChip(
                selected = level == value, onClick = { selectLevel(value) }, enabled = !loading,
                label = { Text("YDS $value", fontFamily = Sans) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
            )
        }

        GameFilter("Konu", topic, topics, bank != null && !loading) { topic = it; error = "" }
        if (kinds.isNotEmpty()) GameFilter("Kelime türü", kind, kinds, !loading) { kind = it; error = "" }

        BodyText("Kelime sayısı")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (value in gameSizes) FilterChip(
                selected = !custom && preset == value, onClick = { preset = value; custom = false; error = "" },
                enabled = !loading && value in permitted, label = { Text(value.toString(), fontFamily = Sans) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
            )
            FilterChip(selected = custom, onClick = { custom = !custom; error = "" }, enabled = !loading && !permitted.isEmpty(),
                label = { Text("Özel", fontFamily = Sans) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime))
        }
        if (custom) OutlinedTextField(
            customValue, { customValue = it.filter { c -> c in '0'..'9' }.take(3); error = "" },
            modifier = Modifier.fillMaxWidth(), label = { Text("Özel kelime sayısı") }, singleLine = true,
            enabled = !loading, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = count != null && count !in permitted,
        )
        when {
            bank == null && error.isEmpty() -> BodyText("Kelime havuzu yükleniyor…")
            bank != null && permitted.isEmpty() -> BodyText("Bu filtrede $available kelime var. Tahta için en az $MIN_NEW_GAME_WORDS kelime gerekir.", ErrorInk)
            bank != null -> BodyText("$available farklı kelime · ${permitted.first}–${permitted.last} kelime. 20 üzeri seçimler, 20 kelimeyi geçmeyen bölümlere ayrılır.", Forest)
        }
        if (error.isNotBlank()) BodyText(error, ErrorInk)
        PrimaryAction(if (loading) "Bulmacalar hazırlanıyor…" else "Bulmacayı başlat", {
            val size = count ?: return@PrimaryAction
            if (loading) return@PrimaryAction
            focus.clearFocus()
            loading = true
            error = ""
            scope.launch {
                try {
                    val game = if (size <= 20) session.newGame(level, topic, kind, size)
                        else session.newGameSeries(level, topic, kind, size)
                    if (game != null) open(game)
                    else error = "Bu seçimle geçerli bir tahta kurulamadı. Başka bir sayı seç veya filtreyi genişlet."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "Bulmaca oluşturulamadı. Tekrar deneyebilirsin."
                } finally { loading = false }
            }
        }, enabled = !loading && count != null && count in permitted)
    }
}

@Composable
private fun GameFilter(label: String, value: String?, options: List<String>, enabled: Boolean, select: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = enabled && options.isNotEmpty()) {
            Text("$label · ${value ?: "Tümü"}", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            Text("▾", fontFamily = Sans)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Tümü", fontFamily = Sans) }, onClick = { select(null); expanded = false })
            for (option in options) DropdownMenuItem(text = { Text(option, fontFamily = Sans) }, onClick = { select(option); expanded = false })
        }
    }
}
