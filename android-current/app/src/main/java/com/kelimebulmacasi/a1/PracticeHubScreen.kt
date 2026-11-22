package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

/** Daily progress belongs to the selected player. A different profile cannot see another's sessions. */
internal data class DailyDay(val goalMinutes: Int, val activeSeconds: Long, val completed: Set<String>)
private val DAILY_MARKER = Regex("[A-Z_]{1,30}:[a-f0-9]{12}")
private val DAILY_GOAL_OPTIONS = listOf(15, 20, 25, 30, 40, 60)

internal class DailyPlanStore(context: Context, profileKey: String) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "wq_daily_${stableHash(profileKey).take(24)}", Context.MODE_PRIVATE)
    private val today = LocalDate.now().toString()
    private val key = "plan.$today"
    private val completedKey = "done.$today"
    private val secondsKey = "active.$today"

    var minutes: Int
        get() = prefs.getInt(key, 15).takeIf { it in DAILY_GOAL_OPTIONS } ?: 15
        set(value) { if (value in DAILY_GOAL_OPTIONS) prefs.edit().putInt(key, value).apply() }

    val isGoalConfigured: Boolean get() = prefs.contains(key)
    val seconds: Long get() = prefs.getLong(secondsKey, 0).coerceAtLeast(0)
    fun addTime(seconds: Long = 1) {
        prefs.edit().putLong(secondsKey, (this.seconds + seconds).coerceAtMost(24 * 3600)).apply()
    }

    fun completed() = prefs.getStringSet(completedKey, emptySet()).orEmpty().toSet()

    /** Only the selected profile is exported; backup preserves up to a year of daily plans. */
    fun exportDays(): Map<String, DailyDay> = prefs.all.keys
        .filter { it.startsWith("plan.") || it.startsWith("done.") || it.startsWith("active.") }
        .map { it.substringAfter('.') }
        .distinct().sortedDescending().take(366)
        .mapNotNull { date ->
            if (runCatching { LocalDate.parse(date) }.isFailure) return@mapNotNull null
            val goal = prefs.getInt("plan.$date", 15).takeIf { it in DAILY_GOAL_OPTIONS } ?: 15
            val active = prefs.getLong("active.$date", 0).coerceIn(0L, 86_400L)
            val done = prefs.getStringSet("done.$date", emptySet()).orEmpty()
                .filter { DAILY_MARKER.matches(it) }.take(250).toSet()
            date to DailyDay(goal, active, done)
        }.toMap()

    /** Repeated imports are idempotent. Never overwrite a higher local total. */
    fun importDays(days: Map<String, DailyDay>) {
        val editor = prefs.edit()
        for ((date, day) in days.entries.take(366)) {
            if (runCatching { LocalDate.parse(date) }.isFailure) continue
            if (day.goalMinutes !in DAILY_GOAL_OPTIONS || day.activeSeconds !in 0L..86_400L ||
                day.completed.size > 250 || day.completed.any { !DAILY_MARKER.matches(it) }) continue
            editor.putInt("plan.$date", day.goalMinutes)
            editor.putLong("active.$date", maxOf(day.activeSeconds, prefs.getLong("active.$date", 0L)))
            editor.putStringSet("done.$date", prefs.getStringSet("done.$date", emptySet()).orEmpty() + day.completed)
        }
        editor.apply()
    }
    fun mark(kind: PracticeKind, id: String): Boolean {
        val current = completed()
        val key = "${kind.name}:${stableHash(id).take(12)}"
        if (key in current) return false
        prefs.edit().putStringSet(completedKey, current + key).apply()
        return true
    }
}

@Composable
internal fun PracticeHubScreen(
    session: AppSession,
    pronunciation: WordPronunciation,
    back: () -> Unit,
    assistant: () -> Unit,
    openGame: (Game) -> Unit,
) {
    val context = LocalContext.current
    val profileKey = session.profileKey
    val store = remember(profileKey) { DailyPlanStore(context, profileKey) }
    var goalEditorOpen by rememberSaveable(profileKey) { mutableStateOf(!store.isGoalConfigured) }
    var goalDraft by rememberSaveable(profileKey) { mutableIntStateOf(store.minutes) }
    var selected by rememberSaveable(profileKey) { mutableStateOf<PracticeKind?>(null) }
    var questionIndex by rememberSaveable(profileKey) { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var preparing by remember { mutableStateOf(false) }
    var boardError by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Time is accumulated only while this daily learning screen is visible.
    LaunchedEffect(profileKey) {
        while (true) {
            delay(1000)
            store.addTime()
            refresh++
        }
    }
    val mins = store.minutes
    val done = refresh.let { store.completed().size }
    val due = session.learningDueCount()
    val doneMinutes = (store.seconds / 60).toInt()
    val remaining = (mins * 60 - store.seconds).coerceAtLeast(0L)

    if (selected != null) {
        val kind = selected!!
        PracticeGameScreen(session, kind, questionIndex, pronunciation, store,
            back = { selected = null }, next = { questionIndex++ ; refresh++ },
            onComplete = { refresh++ })
        return
    }

    ScreenPage("Günlük Planım & Yeni Oyunlar", session.learningProfileName, back) {
        if (session.accountName == null) LocalProfilePicker(session)
        val fraction = (store.seconds.toFloat() / (mins * 60f)).coerceIn(0f, 1f)
        androidx.compose.material3.ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Günlük çalışma hedefi", fontFamily = Serif, color = Ink, fontWeight = FontWeight.Bold)
                        Text("$mins dk hedef · $doneMinutes dk tamamlandı", fontFamily = Sans, color = Ink)
                        if (remaining == 0L) {
                            Text("Hedef tamamlandı!", fontFamily = Sans, fontWeight = FontWeight.SemiBold, color = Forest)
                        } else {
                            Text("Kalan ${remaining / 60} dk ${remaining % 60} sn", fontFamily = Sans, color = Forest)
                        }
                    }
                    if (remaining == 0L) {
                        Text("✓", color = Forest, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!goalEditorOpen) {
                        androidx.compose.material3.IconButton(
                            onClick = { goalDraft = store.minutes; goalEditorOpen = true },
                        ) {
                            Text("✎", color = Forest, fontSize = 23.sp)
                        }
                    }
                }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = Forest,
                )
                if (goalEditorOpen) {
                    BodyText("Günlük hedef süreni seç")
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (option in DAILY_GOAL_OPTIONS) FilterChip(
                            selected = goalDraft == option,
                            onClick = { goalDraft = option },
                            label = { Text("$option dk", fontFamily = Sans) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            store.minutes = goalDraft
                            goalEditorOpen = false
                            refresh++
                        }) { Text("Ayarları kaydet", fontFamily = Sans) }
                        if (store.isGoalConfigured) {
                            TextButton(onClick = { goalDraft = store.minutes; goalEditorOpen = false }) {
                                Text("Vazgeç", fontFamily = Sans)
                            }
                        }
                    }
                    BodyText("Çalışma süresi yalnızca bu sayfa açıkken sayılır.")
                } else {
                    BodyText("Bugün $done etkinlik tamamlandı · $due kelime tekrar bekliyor", Forest)
                }
            }
        }
        for (kind in PracticeKind.entries) {
            MenuItem(kind.title, kind.detail, "✦") { selected = kind; questionIndex = 0 }
        }
        MenuItem("Yanlışlarımdan bulmaca", "Yanlış yaptığın kelimelerden gerçek bir bulmaca oluştur", "↻") {
            if (preparing) return@MenuItem
            preparing = true
            boardError = ""
            val owner = session.profileKey
            val cards = session.searchLearning("", 0, 1500)
                .filter { it.wrong > 0 || it.forgotten > 0 }
                .filter { isEnglishAnswer(it.answer) }
                .distinctBy { englishLetters(it.answer) }
                .sortedByDescending { it.wrong * 4 + it.forgotten * 3 + it.hints }
            scope.launch {
                try {
                    if (cards.size < 6) {
                        boardError = "Henüz yeterli yanlış kelimen yok. En az 6 farklı yanlış kelime biriktiğinde bulmaca oluşturabilirsin."
                        return@launch
                    }
                    val board = withContext(Dispatchers.Default) {
                        val random = kotlin.random.Random((profileKey + LocalDate.now().toString()).hashCode())
                        var result: Puzzle? = null
                        for (attempt in 0 until 12) {
                            val choices = cards.take(36).shuffled(random).take(minOf(9, cards.size))
                            if (choices.size < 6) break
                            val entries = choices.map { Entry(it.answer, it.clue, it.context) }
                            result = customPuzzle(entries)
                            if (result != null) break
                        }
                        result
                    }
                    if (owner != session.profileKey) return@launch
                    if (board == null) {
                        boardError = "Bu kelimelerle kesişen bir tahta kurulamadı. Biraz daha kelime biriktirip tekrar dene."
                        return@launch
                    }
                    val title = "Yanlışlarımdan · ${LocalDate.now()}"
                    val game = Game(
                        "weak_${UUID.randomUUID()}", GameMode.Custom, "Kişisel", "Tekrar",
                        title, board,
                    )
                    if (session.saveCustom(listOf(game), null)) openGame(game)
                    else boardError = "Bulmaca kaydedilemedi. Depolama alanını kontrol et."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    boardError = "Bulmaca hazırlanırken hata oluştu. Yeniden deneyebilirsin."
                } finally { preparing = false }
            }
        }
        if (preparing) BodyText("Kişisel bulmaca hazırlanıyor…")
        if (boardError.isNotBlank()) BodyText(boardError, ErrorInk)
        MenuItem("Kişisel AI öğretmenim", "Kendi yanlışlarından Türkçe anlatımlı ders ve hata düzeltme", "✦", assistant)
        BodyText("İki ayrı Android cihazla çevrimiçi yarışma, ana ekrandaki Arena'da bulunur.")
    }
}

@Composable
private fun PracticeGameScreen(
    session: AppSession, kind: PracticeKind, number: Int,
    pronunciation: WordPronunciation, store: DailyPlanStore,
    back: () -> Unit, next: () -> Unit, onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = session.preferences
    val cards = remember(session.profileKey, number) {
        session.searchLearning("", 0, 400).filter { isEnglishAnswer(it.answer) }
            .sortedByDescending { it.wrong * 3 + it.forgotten * 2 + it.hints }
    }
    val dictationCard = cards.getOrNull(number % cards.size.coerceAtLeast(1))
    val word = dictationCard?.answer ?: listOf(
        "water", "tomato", "beautiful", "mountain", "journey", "careful", "library", "together", "important", "tomorrow",
        "knowledge", "language", "weather", "teacher", "friendship", "improve", "remember", "practice", "umbrella", "schedule",
    )[number % 20]
    val item = when (kind) {
        PracticeKind.Sentence -> PracticeContent.sentences[number % PracticeContent.sentences.size]
        PracticeKind.Grammar -> PracticeContent.grammar[number % PracticeContent.grammar.size]
        PracticeKind.Story -> PracticeContent.story[number % PracticeContent.story.size]
        else -> PracticeQuestion("Telaffuzu dinle ve yaz", word)
    }
    var input by rememberSaveable(session.profileKey, kind.name, number) { mutableStateOf("") }
    var checked by rememberSaveable(session.profileKey, kind.name, number) { mutableStateOf(false) }
    var success by rememberSaveable(session.profileKey, kind.name, number) { mutableStateOf(false) }
    var recognized by rememberSaveable(session.profileKey, kind.name, number) { mutableStateOf("") }
    var problem by rememberSaveable(session.profileKey, kind.name, number) { mutableStateOf("") }
    var chosen by remember(session.profileKey, kind, number) { mutableStateOf(emptyList<Int>()) }
    val shuffled = remember(number, kind) { PracticeContent.mixedTokens(item, number * 43 + 19) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { outcome ->
        if (outcome.resultCode == Activity.RESULT_OK) {
            val text = outcome.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            recognized = text
            checked = text.isNotBlank()
            success = PracticeContent.correct(text, word)
            if (success) {
                store.mark(kind, "${number}:$word")
                onComplete()
            }
        } else problem = "Ses tanıma iptal edildi veya sonuç alınamadı."
    }

    fun evaluate(answer: String) {
        if (checked) return
        checked = true
        success = PracticeContent.correct(answer, item.answer)
        if (success) {
            store.mark(kind, "${number}:${item.answer}")
            onComplete()
            if (kind == PracticeKind.Dictation && dictationCard != null &&
                dictationCard.dueAt > 0L && dictationCard.dueAt <= System.currentTimeMillis()) {
                session.reviewLearning(dictationCard, ReviewRating.Good)
            }
        }
    }

    ScreenPage(kind.title, "${session.learningProfileName} · Alıştırma ${number + 1}", back) {
        if (kind in listOf(PracticeKind.Dictation, PracticeKind.Speak)) {
            BodyText("Önce sesini dinle. Başlat düğmesini kullanmadıkça mikrofon açılmaz.")
            OutlinedButton(onClick = { pronunciation.pronounce(listOf(word), preferences.voice, replay = true) },
                enabled = preferences.pronunciation) { Text("${preferences.voice.label} aksanında dinle") }
            if (kind == PracticeKind.Speak) {
                PrimaryAction("Konuşmayı dene", {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferences.voice.locale.toLanguageTag())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Duyduğun İngilizce ifadeyi söyle")
                        }
                        speechLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        problem = "Bu cihazda ses tanıma uygulaması yok."
                    } catch (_: Exception) {
                        problem = "Cihazın ses tanıma uygulaması başlatılamadı."
                    }
                })
                if (recognized.isNotBlank()) BodyText("Algılanan: $recognized")
                BodyText("Ses tanıma cihazın sağlayıcısına bağlı olabilir; bu bir fonetik aksan puanı değildir.")
            } else {
                OutlinedTextField(input, { input = it.take(60) }, modifier = Modifier.fillMaxWidth(),
                    enabled = !checked, singleLine = true, label = { Text("Duyduğun kelimeyi yaz") })
                PrimaryAction("Kontrol et", { evaluate(input) }, !checked && input.isNotBlank())
            }
        } else if (kind == PracticeKind.Sentence) {
            BodyText(item.instruction)
            BodyText("Sıraladığın kelimeler: " + chosen.joinToString(" ") { index -> shuffled[index].second })
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for ((index, pair) in shuffled.withIndex()) {
                    FilterChip(selected = index in chosen, enabled = !checked && index !in chosen,
                        onClick = { chosen = chosen + index }, label = { Text(pair.second) })
                }
            }
            OutlinedButton(onClick = { if (chosen.isNotEmpty()) chosen = chosen.dropLast(1) },
                enabled = !checked && chosen.isNotEmpty()) { Text("Son kelimeyi geri al") }
            PrimaryAction("Kontrol et", {
                evaluate(chosen.joinToString(" ") { shuffled[it].second })
            }, !checked && chosen.size == shuffled.size)
        } else {
            BodyText(if (kind == PracticeKind.Grammar) "Hatalı cümle: ${item.instruction}" else item.instruction)
            BodyText(if (kind == PracticeKind.Grammar) "Doğru cümleyi seç" else "Hikâyeyi en uygun sözcükle tamamla")
            for (option in item.options) {
                OutlinedButton(onClick = { evaluate(option) }, modifier = Modifier.fillMaxWidth(), enabled = !checked) {
                    Text(option, fontFamily = Sans)
                }
            }
        }
        if (problem.isNotBlank()) BodyText(problem, ErrorInk)
        if (checked) {
            BodyText(if (success) "✓ Doğru!" else "Tekrar çalışılacak · Doğru cevap: ${item.answer}",
                if (success) Forest else ErrorInk)
            if (item.explanation.isNotBlank()) BodyText(item.explanation)
            if (kind in listOf(PracticeKind.Dictation, PracticeKind.Speak)) WordIpa(word, preferences)
            PrimaryAction("Sonraki alıştırma", next)
        }
        TextButton(onClick = back) { Text("Günlük plana dön", fontFamily = Sans) }
    }
}
