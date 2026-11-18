package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

/** No tracking service, server or account is needed. Profile keys never appear in filenames. */
internal class YdsProgressStore(context: Context, profile: String) {
    private val data = context.applicationContext.getSharedPreferences(
        "wq_yds_${stableHash(profile).take(24)}", Context.MODE_PRIVATE)

    fun attempts(question: YdsQuestion) = data.getInt("a.${question.id}", 0)
    fun correct(question: YdsQuestion) = data.getInt("c.${question.id}", 0)
    fun wrongQuestions() = YdsContent.questions.filter { attempts(it) > correct(it) }

    fun record(question: YdsQuestion, isCorrect: Boolean) {
        val count = attempts(question)
        data.edit().putInt("a.${question.id}", count + 1)
            .putInt("c.${question.id}", correct(question) + if (isCorrect) 1 else 0)
            .putInt("tests", data.getInt("tests", 0) + 1)
            .putString("lastDay", LocalDate.now().toString())
            .apply()
    }

    fun exportStats() = YdsContent.questions.mapNotNull { item ->
        val count = attempts(item)
        if (count == 0) null else item.id to (correct(item) to count)
    }.toMap()

    /** Safe monotone merge: importing the same backup twice never doubles counts. */
    fun importStats(stats: Map<String, Pair<Int, Int>>) {
        val allowed = YdsContent.questions.map { it.id }.toSet()
        val editor = data.edit()
        stats.forEach { (id, pair) ->
            if (id !in allowed) return@forEach
            val (right, total) = pair
            if (total !in 0..100000 || right !in 0..total) return@forEach
            val existingTotal = data.getInt("a.$id", 0)
            if (total > existingTotal) {
                editor.putInt("a.$id", total)
                editor.putInt("c.$id", right)
            }
        }
        editor.apply()
    }

    fun totalAttempts() = YdsContent.questions.sumOf(::attempts)
    fun totalCorrect() = YdsContent.questions.sumOf(::correct)
    fun percent(): Int = if (totalAttempts() == 0) 0 else (totalCorrect() * 100.0 / totalAttempts()).roundToInt()
    fun topicStats(topic: YdsTopic): Pair<Int, Int> {
        val questions = YdsContent.questions.filter { it.topic == topic }
        return questions.sumOf(::correct) to questions.sumOf(::attempts)
    }
    fun levelRecommendation(): YdsDifficulty {
        val attempts = totalAttempts()
        return when {
            attempts < 12 -> YdsDifficulty.Easy
            percent() < 55 -> YdsDifficulty.Easy
            percent() < 80 -> YdsDifficulty.Medium
            else -> YdsDifficulty.Hard
        }
    }
    fun weakestTopic(): YdsTopic? = YdsTopic.entries.mapNotNull { topic ->
        val (correct, attempts) = topicStats(topic)
        if (attempts < 1) null else topic to correct.toDouble() / attempts
    }.minByOrNull { it.second }?.first

    fun teacherPrompt(): String {
        val misses = wrongQuestions().take(6).joinToString("; ") { q ->
            "${q.topic.label}: ${q.question.take(105)} (doğru yanıt: ${q.options[q.answer]})"
        }
        return if (misses.isBlank())
            "YDS için Türkçe açıklamalı kişisel ders ve 5 özgün kontrol sorusu hazırla. " +
                "Zorluk seviyem: ${levelRecommendation().label}."
        else "YDS yanlışlarıma göre Türkçe açıklamalı bir ders ve 5 yeni özgün soru hazırla. " +
            "Zorluk seviyem: ${levelRecommendation().label}. Hatalarım: $misses"
    }
}

/** Deadline is wall-clock based so switching apps cannot pause a timed test. */
internal fun ydsRemainingSeconds(deadlineMillis: Long, nowMillis: Long): Int =
    ((deadlineMillis - nowMillis).coerceIn(0L, 15L * 60L * 1000L) / 1000L).toInt()

@Composable
internal fun YdsCenterScreen(session: AppSession, back: () -> Unit, teacher: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = session.profileKey
    val store = remember(context, profile) { YdsProgressStore(context, profile) }
    var topicName by rememberSaveable(profile) { mutableStateOf("") }
    var levelName by rememberSaveable(profile) { mutableStateOf("") }
    var activeIds by rememberSaveable(profile) { mutableStateOf(emptyList<String>()) }
    var activePosition by rememberSaveable(profile) { mutableIntStateOf(0) }
    var answerChoice by rememberSaveable(profile) { mutableIntStateOf(-1) }
    var exam by rememberSaveable(profile) { mutableStateOf(false) }
    var remaining by rememberSaveable(profile) { mutableIntStateOf(0) }
    var deadlineMillis by rememberSaveable(profile) { mutableLongStateOf(0L) }
    var finished by rememberSaveable(profile) { mutableStateOf(false) }
    var score by rememberSaveable(profile) { mutableIntStateOf(0) }
    var answeredCount by rememberSaveable(profile) { mutableIntStateOf(0) }
    var history by rememberSaveable(profile) { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(exam, finished, activeIds, deadlineMillis) {
        while (exam && !finished && activeIds.isNotEmpty()) {
            remaining = ydsRemainingSeconds(deadlineMillis, System.currentTimeMillis())
            if (remaining <= 0) {
                finished = true
                break
            }
            delay(250L)
        }
    }
    fun start(modeExam: Boolean, wrongOnly: Boolean = false) {
        val topic = YdsTopic.entries.firstOrNull { it.name == topicName }
        val level = YdsDifficulty.entries.firstOrNull { it.name == levelName }
        val pool = if (wrongOnly) store.wrongQuestions() else YdsContent.pool(topic, level)
        if (pool.isEmpty()) return
        val seed = System.nanoTime().toInt()
        val selection = pool.shuffled(kotlin.random.Random(seed))
        activeIds = selection.take(if (modeExam) 15 else 12).map { it.id }
        activePosition = 0
        answerChoice = -1
        finished = false
        exam = modeExam
        score = 0
        answeredCount = 0
        remaining = if (modeExam) 15 * 60 else 0
        deadlineMillis = if (modeExam) System.currentTimeMillis() + 15L * 60L * 1000L else 0L
        history = false
    }

    val current = if (finished) null else activeIds.getOrNull(activePosition)
        ?.let { id -> YdsContent.questions.firstOrNull { it.id == id } }

    ScreenPage("YDS ve İleri İngilizce", "Özgün sorular · çevrimdışı çalışma · kişisel istatistikler", back) {
        BodyText("${session.learningProfileName} · ${store.totalAttempts()} soru · %${store.percent()} başarı")
        if (current != null) {
            val choices = remember(current.id) { YdsContent.choices(current) }
            if (exam) BodyText("Süre: %02d:%02d".format(remaining / 60, remaining % 60), Forest)
            BodyText("${activePosition + 1}/${activeIds.size} · ${current.topic.label} · ${current.difficulty.label}")
            Text(current.question, color = Ink, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            choices.forEachIndexed { index, (originalIndex, text) ->
                OutlinedButton(
                    onClick = {
                        if (answerChoice == -1 && (!exam || System.currentTimeMillis() < deadlineMillis)) {
                            answerChoice = originalIndex
                            val correct = originalIndex == current.answer
                            if (correct) score++
                            answeredCount++
                            store.record(current, correct)
                            session.recordYdsAttempt(current, correct, text)
                            refresh++
                        }
                    },
                    enabled = answerChoice == -1,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("${index + 1}. $text", fontFamily = Sans) }
            }
            if (answerChoice != -1) {
                BodyText(if (answerChoice == current.answer) "✓ Doğru cevap" else "Doğrusu: ${current.options[current.answer]}",
                    if (answerChoice == current.answer) Forest else ErrorInk)
                BodyText(current.explanation)
                PrimaryAction(if (activePosition == activeIds.lastIndex) "Sonucu gör" else "Sonraki soru", onClick = {
                    if (activePosition == activeIds.lastIndex) finished = true
                    else activePosition++
                    answerChoice = -1
                })
            }
            TextButton(onClick = { finished = true }) { Text("Çalışmayı bitir", fontFamily = Sans) }
        } else if (finished && activeIds.isNotEmpty()) {
            Text("Çalışma özeti", color = Forest, fontFamily = Serif)
            BodyText("Bu çalışmada $score/$answeredCount doğru yanıt. " +
                "Konu istatistiklerine işlendi.")
            PrimaryAction("Yeni çalışma", { activeIds = emptyList(); finished = false })
            OutlinedButton(onClick = { teacher(store.teacherPrompt()) }, Modifier.fillMaxWidth()) {
                Text("Yanlışlarıma göre AI öğretmeninden ders iste")
            }
        } else {
            val recommended = refresh.let { store.levelRecommendation() }
            BodyText("Önerilen seviye: ${recommended.label}. Soruların tamamı çevrimdışı çalışır.")
            Text("Zorluk", color = Ink, fontFamily = Serif)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("" to "Tümü") + YdsDifficulty.entries.map { it.name to it.label }).forEach { (value, label) ->
                    FilterChip(selected = levelName == value, onClick = { levelName = value }, label = { Text(label) })
                }
            }
            Text("Konu / soru türü", color = Ink, fontFamily = Serif)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("" to "Tümü") + YdsTopic.entries.map { it.name to it.label }).forEach { (value, label) ->
                    FilterChip(selected = topicName == value, onClick = { topicName = value }, label = { Text(label) })
                }
            }
            BodyText("Seçili havuz: ${YdsContent.pool(YdsTopic.entries.firstOrNull { it.name == topicName }, YdsDifficulty.entries.firstOrNull { it.name == levelName }).size} soru")
            PrimaryAction("Çevrimdışı soru çalış", { start(false) })
            PrimaryAction("15 dakikalık süreli mini deneme", { start(true) })
            if (store.wrongQuestions().isNotEmpty()) {
                OutlinedButton(onClick = { start(false, true) }, Modifier.fillMaxWidth()) {
                    Text("Yanlışlarımdan tekrar çalış (${store.wrongQuestions().size})")
                }
            }
            OutlinedButton(onClick = { history = !history }, Modifier.fillMaxWidth()) {
                Text(if (history) "İstatistikleri kapat" else "Konu bazlı sınav istatistiklerini göster")
            }
            OutlinedButton(onClick = { teacher(store.teacherPrompt()) }, Modifier.fillMaxWidth()) {
                Text("Kişisel AI öğretmeninden YDS dersi iste")
            }
            if (history) {
                Text("Konu performansım", color = Ink, fontFamily = Serif)
                for (topic in YdsTopic.entries) {
                    val (right, total) = store.topicStats(topic)
                    val ratio = if (total == 0) 0f else right.toFloat() / total
                    BodyText("${topic.label}: $right/$total doğru")
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
                }
                BodyText("Veriler yalnızca ${session.learningProfileName} profilinde saklanır. Yanlış YDS kelimeleri akıllı tekrar defterine de eklenir.")
            }
        }
    }
}
