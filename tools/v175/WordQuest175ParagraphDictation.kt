package com.kelimebulmacasi.a1

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.roundToInt

/** Paragraph practice is additive. The old word/sentence dictation and US/UK voice paths remain untouched. */
internal data class Wq175Paragraph(val level: String, val english: String, val note: String)

internal object Wq175ParagraphLibrary {
    val items = listOf(
        Wq175Paragraph("A1", "Every morning I eat breakfast with my family. We drink tea and talk about our plans. Then I walk to school.",
            "Günlük rutin ve geniş zaman."),
        Wq175Paragraph("A1", "My friend Anna has a small garden. She grows flowers and waters them every day. I help her on Sundays.",
            "Sahiplik, günlük alışkanlıklar ve zaman ifadeleri."),
        Wq175Paragraph("A2", "Last weekend we visited a museum near the river. The guide told us interesting stories about the city. After the tour, we had lunch at a small cafe.",
            "Geçmiş zaman ve olay sırası."),
        Wq175Paragraph("A2", "I am learning to cook because I want to prepare healthier meals. At first, I made many mistakes. Now I follow simple recipes and enjoy trying new vegetables.",
            "Neden bildiren bağlaçlar ve gelişim."),
        Wq175Paragraph("B1", "Many people use bicycles to travel around their cities. Cycling can save money and reduce traffic, but riders need safe roads. Some towns are building separate bicycle lanes to make daily journeys easier.",
            "Avantajlar, karşıtlık ve amaç."),
        Wq175Paragraph("B1", "When our community centre opened a reading club, only a few neighbours joined. Over time, more people began attending the weekly meetings. Members now exchange books and recommend stories to one another.",
            "Zaman bağlaçları ve alışkanlıklar."),
        Wq175Paragraph("B2", "Although online courses offer flexibility, they also require strong self-discipline. Students who plan regular study sessions are more likely to complete their assignments. Clear feedback from teachers can help learners stay motivated when lessons become difficult.",
            "Karşıtlık, ilgi cümleciği ve olasılık."),
        Wq175Paragraph("B2", "The local council introduced a programme to reduce food waste. Restaurants began donating unused meals, while volunteers delivered them to community kitchens. The programme has expanded since its launch, although organisers still face transport challenges.",
            "Eş zamanlı eylemler ve present perfect."),
        Wq175Paragraph("C1", "Scientific findings often attract public attention before researchers have fully assessed their limitations. A single study may reveal an interesting association without establishing a causal relationship. Responsible reporting therefore distinguishes preliminary evidence from conclusions supported by repeated investigation.",
            "Akademik söylem ve neden-sonuç ayrımı."),
        Wq175Paragraph("C1", "As cities become more densely populated, urban planners must balance housing needs with access to green spaces. Measures that appear efficient in the short term can have unintended consequences for public health. Careful consultation with residents may expose problems that technical forecasts overlook.",
            "Karmaşık cümle yapıları ve çıkarım."),
        Wq175Paragraph("C2", "The apparent precision of a numerical forecast should not be mistaken for certainty. Every model rests on assumptions, some of which may remain invisible to readers unfamiliar with its methodology. Acknowledging these constraints is not a rejection of evidence but a prerequisite for interpreting it responsibly.",
            "İleri akademik üslup ve sınırlandırma."),
        Wq175Paragraph("C2", "Institutions that evaluate policies solely through immediate outcomes may fail to recognise their cumulative effects. An intervention that initially appears unsuccessful can produce substantial benefits once complementary systems are established. Conversely, early gains may prove temporary when the underlying incentives remain unchanged.",
            "Gelişmiş karşıtlık, koşul ve nedensellik.")
    )
}

/** Ordered word-error rate; punctuation and case are ignored, but missing/extra words still count. */
internal data class Wq175ParagraphGrade(
    val accuracy: Int,
    val missing: List<String>,
    val extra: List<String>,
    val different: List<String>
)

internal fun wq175ParagraphWords(input: String): List<String> =
    Regex("[\\p{L}\\p{N}']+").findAll(input.lowercase(Locale.ROOT))
        .map { it.value }.toList()

internal fun wq175GradeParagraph(expected: String, actual: String): Wq175ParagraphGrade {
    val a = wq175ParagraphWords(expected)
    val b = wq175ParagraphWords(actual)
    if (a.isEmpty()) return Wq175ParagraphGrade(100, emptyList(), b, emptyList())
    val n = a.size
    val m = b.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 0..n) dp[i][0] = i
    for (j in 0..m) dp[0][j] = j
    for (i in 1..n) for (j in 1..m) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[i][j] = minOf(
            dp[i - 1][j] + 1,
            dp[i][j - 1] + 1,
            dp[i - 1][j - 1] + cost
        )
    }
    val missing = mutableListOf<String>()
    val extra = mutableListOf<String>()
    val different = mutableListOf<String>()
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && a[i - 1] == b[j - 1] &&
            dp[i][j] == dp[i - 1][j - 1]) {
            i--; j--
        } else if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1) {
            different.add(a[i - 1] + " → " + b[j - 1])
            i--; j--
        } else if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
            missing.add(a[i - 1])
            i--
        } else {
            extra.add(b[j - 1])
            j--
        }
    }
    val score = ((1.0 - dp[n][m].toDouble() / n.toDouble()) * 100.0)
        .coerceIn(0.0, 100.0).roundToInt()
    return Wq175ParagraphGrade(score, missing.reversed(), extra.reversed(), different.reversed())
}

@Composable
internal fun Wq175ParagraphPracticeEntry(profileKey: String) {
    var opened by rememberSaveable(profileKey) { mutableStateOf(false) }
    val border = RoundedCornerShape(20.dp)
    Surface(
        onClick = { opened = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp),
        shape = border,
        color = Color(0xFFFFF1E7),
        border = BorderStroke(1.dp, Color(0xFFFFC3AA))
    ) {
        Row(Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Wq175PracticeIcon(PracticeKind.Dictation, Modifier.size(42.dp))
            Column(Modifier.weight(1f)) {
                Text("Paragraf Dinle ve Yaz", fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold, color = Color(0xFF813E40))
                Text("A1–C2 · Paragrafı dinle, yaz ve kelime kelime kontrol et",
                    fontSize = 12.sp, color = Color(0xFF8D635D))
            }
            Text("›", fontSize = 26.sp, color = Color(0xFFB75E51))
        }
    }
    if (opened) Wq175ParagraphPracticeDialog(profileKey) { opened = false }
}

@Composable
private fun Wq175ParagraphPracticeDialog(profileKey: String, close: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var level by rememberSaveable(profileKey) { mutableStateOf("A1") }
    var index by rememberSaveable(profileKey) { mutableIntStateOf(0) }
    var answer by rememberSaveable(profileKey) { mutableStateOf("") }
    var accent by rememberSaveable(profileKey) { mutableStateOf("US") }
    var checked by rememberSaveable(profileKey) { mutableStateOf(false) }
    var voiceReady by remember { mutableStateOf(false) }
    var voiceMessage by remember { mutableStateOf("Ses motoru hazırlanıyor…") }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    val entries = remember(level) { Wq175ParagraphLibrary.items.filter { it.level == level } }
    val item = entries[index.coerceIn(0, entries.lastIndex)]
    val grade = remember(item.english, answer, checked) {
        if (checked) wq175GradeParagraph(item.english, answer) else null
    }
    val prefs = remember(context) {
        context.getSharedPreferences("wq175_paragraph_learning", Context.MODE_PRIVATE)
    }
    val prefix = remember(profileKey) { "p" + profileKey.hashCode().toString() + "_" }
    var totalAttempts by remember(profileKey) {
        mutableIntStateOf(prefs.getInt(prefix + "attempts", 0))
    }

    DisposableEffect(context) {
        val player = TextToSpeech(context.applicationContext) { code ->
            voiceReady = code == TextToSpeech.SUCCESS
            voiceMessage = if (voiceReady) "Dinlemeye hazır" else
                "Cihazın metin okuma motoru açılamadı. Ses ayarlarını kontrol et."
        }
        speaker = player
        onDispose { player.stop(); player.shutdown(); speaker = null }
    }

    fun reset(next: Int) {
        speaker?.stop()
        index = next
        answer = ""
        checked = false
        voiceMessage = if (voiceReady) "Dinlemeye hazır" else "Ses motoru hazırlanıyor…"
    }
    fun play() {
        val player = speaker ?: return
        if (!voiceReady) return
        val locale = if (accent == "UK") Locale.UK else Locale.US
        if (player.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
            voiceMessage = "Bu aksan için cihazda İngilizce ses paketi bulunamadı."
            return
        }
        player.language = locale
        player.setSpeechRate(0.90f)
        val result = player.speak(item.english, TextToSpeech.QUEUE_FLUSH, null,
            "wq175_para_" + level + index.toString())
        voiceMessage = if (result == TextToSpeech.SUCCESS) "Paragraf oynatılıyor" else
            "Ses başlatılamadı; cihazın konuşma motorunu kontrol et."
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFFBF7)
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Paragraf Dinle ve Yaz",
                        fontWeight = FontWeight.Black, fontSize = 21.sp,
                        color = Color(0xFF803B42))
                    TextButton(onClick = close) { Text("Kapat") }
                }
                Text("Dinlediğin İngilizce paragrafı yaz. Noktalama ve büyük harf puanı etkilemez.",
                    fontSize = 13.sp, color = Color(0xFF7A6664))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("A1","A2","B1","B2","C1","C2").forEach { option ->
                        FilterChip(
                            selected = level == option,
                            onClick = { level = option; reset(0) },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }
                Text("Paragraf " + (index + 1) + " / " + entries.size,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF835B57))
                LinearProgressIndicator(
                    progress = { (index + 1).toFloat() / entries.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE57B69), trackColor = Color(0xFFFFE4D5)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("US","UK").forEach { option ->
                        FilterChip(selected = accent == option,
                            onClick = { accent = option },
                            label = { Text(if (option == "US") "Amerikan" else "Britanya") })
                    }
                }
                Button(onClick = { play() }, enabled = voiceReady,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB95551))) {
                    Text("▶  Paragrafı dinle / tekrar dinle")
                }
                Text(voiceMessage, fontSize = 12.sp, color = Color(0xFF77595A))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { if (!checked) answer = it.take(1800) },
                    readOnly = checked,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5, maxLines = 9,
                    label = { Text("Duyduklarını buraya yaz") },
                    placeholder = { Text("İngilizce paragraf...") }
                )
                if (!checked) {
                    Button(onClick = {
                        val report = wq175GradeParagraph(item.english, answer)
                        totalAttempts++
                        prefs.edit()
                            .putInt(prefix + "attempts", totalAttempts)
                            .putInt(prefix + "last_score", report.accuracy)
                            .apply()
                        checked = true
                        speaker?.stop()
                    }, enabled = answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF376D59))) {
                        Text("Yazdığımı Kontrol Et")
                    }
                }
                if (grade != null) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(
                                Color(0xFFE9F8EE), Color(0xFFFFF5E9))),
                                RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kelime doğruluğu: %" + grade.accuracy,
                            fontWeight = FontWeight.Black, fontSize = 19.sp,
                            color = Color(0xFF245B49))
                        if (grade.missing.isNotEmpty()) Text(
                            "Eksik kelimeler: " + grade.missing.joinToString(", "))
                        if (grade.different.isNotEmpty()) Text(
                            "Farklı yazılanlar (doğrusu → yazdığın): " +
                                grade.different.joinToString(", "))
                        if (grade.extra.isNotEmpty()) Text(
                            "Fazladan yazılanlar: " + grade.extra.joinToString(", "))
                        if (grade.accuracy == 100) Text("Harika! Paragrafı eksiksiz yazdın.")
                        Text("Doğru paragraf:", fontWeight = FontWeight.Bold)
                        Text(item.english, fontSize = 14.sp, lineHeight = 21.sp)
                        Text(item.note, fontSize = 12.sp,
                            color = Color(0xFF66706A))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { reset(index) },
                                modifier = Modifier.weight(1f)) { Text("Tekrar dene") }
                            Button(onClick = { reset((index + 1) % entries.size) },
                                modifier = Modifier.weight(1f)) { Text("Sonraki") }
                        }
                    }
                }
                Text("Bu profilde tamamlanan paragraf denemesi: " + totalAttempts,
                    fontSize = 12.sp, color = Color(0xFF7D716B))
            }
        }
    }
}
