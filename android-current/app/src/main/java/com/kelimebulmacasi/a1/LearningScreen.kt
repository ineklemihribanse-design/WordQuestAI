package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val NOTEBOOK_PAGE_SIZE = 24
private const val HISTORY_PAGE_SIZE = 12

@Composable
internal fun LegacyLearningScreen(session: AppSession, pronunciation: WordPronunciation, back: () -> Unit) {
    var notebook by rememberSaveable { mutableStateOf(false) }
    var detailKey by rememberSaveable(session.profileKey) { mutableStateOf<String?>(null) }
    val card = detailKey?.let(session::learningCard)
    if (card != null) {
        BackHandler { detailKey = null }
        LearningHistoryScreen(session, card, pronunciation) { detailKey = null }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }
    val dueCount = session.learningDueCount(now)
    ScreenPage("Kelime Hafızam", "", back) {
        if (session.accountName == null) LocalProfilePicker(session) else BodyText(session.learningProfileName)
        session.learningError?.let { BodyText(it, ErrorInk) }
        pronunciation.error?.let { BodyText(it, ErrorInk) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !notebook, onClick = { notebook = false },
                label = { Text("Tekrar ($dueCount)", fontFamily = Sans) })
            FilterChip(selected = notebook, onClick = { notebook = true },
                label = { Text("Kelime defteri", fontFamily = Sans) })
        }
        if (notebook) {
            key(session.profileKey) {
                Notebook(session) { detailKey = it }
            }
        } else {
            val current = session.dueLearningCards(now).firstOrNull()
            if (current == null) {
                LearningPanel {
                    Text("Tekrar bekleyen kelime yok.", color = Ink, fontFamily = Sans,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    BodyText("Bulmacadaki doğru çözümler, yanlış denemeler ve harf ipuçları kelime defterine eklenir.")
                }
            } else {
                RecallPanel(current, session.profileKey, dueCount, now, session::reviewLearning,
                    session.preferences, pronunciation)
            }
        }
    }
}

@Composable
internal fun RecallPanel(
    card: LearningCard, profile: String, dueCount: Int, now: Long, review: (LearningCard, ReviewRating) -> Unit,
    preferences: WordQuestPreferences, pronunciation: WordPronunciation,
) {
    var reveal by rememberSaveable(profile, card.key, card.dueAt) { mutableStateOf(false) }
    LearningPanel {
        BodyText("Hatırlamaya çalış · ${card.clue}")
        BodyText("${englishLetters(card.answer).length} harf · $dueCount kelime bekliyor")
        if (reveal) {
            Text(card.answer, fontFamily = Serif, fontSize = 29.sp, color = Forest)
            WordIpa(card.answer, preferences)
            if (card.context.isNotBlank()) BodyText(card.context)
            ListenToWord(card.answer, preferences, pronunciation)
            for (pair in ReviewRating.entries.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rating in pair) {
                        OutlinedButton(
                            onClick = { review(card, rating) }, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(rating.label, fontFamily = Sans, textAlign = TextAlign.Center)
                                Text(LearningSchedule.intervalLabel(card, rating, now), color = Muted, fontFamily = Sans)
                            }
                        }
                    }
                }
            }
        } else {
            PrimaryAction("Cevabı göster", { reveal = true })
        }
    }
}

@Composable
internal fun Notebook(session: AppSession, openHistory: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val cards = session.searchLearning(search, page * NOTEBOOK_PAGE_SIZE, NOTEBOOK_PAGE_SIZE + 1)
    OutlinedTextField(value = search, onValueChange = {
        search = it.take(80)
        page = 0
    }, label = { Text("Kelime veya Türkçe anlam ara", fontFamily = Sans) },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    if (cards.isEmpty()) {
        LearningPanel {
            BodyText(if (search.isBlank()) "Henüz öğrenme kaydı yok." else "Aramana uyan kelime yok.")
        }
    }
    for (card in cards.take(NOTEBOOK_PAGE_SIZE)) {
        key(card.key) {
            LearningPanel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(card.answer, fontFamily = Sans, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        WordIpa(card.answer, session.preferences)
                        BodyText(card.clue)
                        VocabularyWordTypes.label(card.answer)?.let { BodyText("Tür: " + it) }
                    }
                    TextButton(onClick = { session.pinLearning(card, !card.pinned) }) {
                        Text(if (card.pinned) "★ Kayıtlı" else "☆ Kaydet", color = Forest, fontFamily = Sans)
                    }
                }
                LearningTotals(card)
                TextButton(onClick = { openHistory(card.key) }) { Text("Geçmişi gör", fontFamily = Sans) }
            }
        }
    }
    LearningPages(page, cards.size > NOTEBOOK_PAGE_SIZE) { page = it }
}

@Composable
internal fun LearningHistoryScreen(session: AppSession, card: LearningCard, pronunciation: WordPronunciation, back: () -> Unit) {
    var page by rememberSaveable(session.profileKey, card.key) { mutableIntStateOf(0) }
    val events = session.learningHistory(card.key, page * HISTORY_PAGE_SIZE, HISTORY_PAGE_SIZE + 1)
    val dates = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag("tr-TR")) }
    ScreenPage(card.answer, card.clue, back) {
        LearningPanel {
            LearningTotals(card)
            if (card.context.isNotBlank()) BodyText(card.context)
            WordIpa(card.answer, session.preferences)
            ListenToWord(card.answer, session.preferences, pronunciation)
            pronunciation.error?.let { BodyText(it, ErrorInk) }
            if (card.dueAt > 0) BodyText("Tekrar zamanı · ${dates.format(Date(card.dueAt))}")
        }
        if (events.isEmpty()) BodyText("Bu kelime için geçmiş kaydı yok.")
        for (event in events.take(HISTORY_PAGE_SIZE)) {
            LearningPanel {
                val label = if (event.kind == LearningKind.SOLVED && event.assisted) "İpucuyla doğru çözüm" else event.kind.label
                Text(label, color = Ink, fontFamily = Sans, fontWeight = FontWeight.Bold)
                if (event.attempt.isNotBlank()) BodyText("Yazdığın cevap · ${event.attempt}")
                BodyText(dates.format(Date(event.at)))
            }
        }
        LearningPages(page, events.size > HISTORY_PAGE_SIZE) { page = it }
    }
}

@Composable
internal fun ListenToWord(word: String, preferences: WordQuestPreferences, pronunciation: WordPronunciation) {
    if (!preferences.pronunciation) return
    TextButton(onClick = { pronunciation.pronounce(listOf(word), preferences.voice, replay = true) }) {
        Text("Telaffuzu dinle", fontFamily = Sans)
    }
}

@Composable
internal fun LearningTotals(card: LearningCard) {
    BodyText("Doğru ${card.solved} · Yanlış ${card.wrong} · İpucu ${card.hints} · Tekrar ${card.remembered + card.forgotten}")
}

@Composable
private fun LearningPages(page: Int, more: Boolean, move: (Int) -> Unit) {
    if (page == 0 && !more) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = page > 0, onClick = { move(page - 1) }) {
            Text("Önceki", fontFamily = Sans)
        }
        OutlinedButton(enabled = more, onClick = { move(page + 1) }) {
            Text("Sonraki", fontFamily = Sans)
        }
    }
}

@Composable
internal fun LearningPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(White, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
