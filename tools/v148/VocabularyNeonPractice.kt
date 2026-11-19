package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

private val NeonCardPaper = Color(0xFFFFFAF7)
private val DarkText = Color(0xFF102447)

/** A separate, immersive study page. Translation remains hidden on the front. */
@Composable
internal fun NeonVocabularyPractice(
    session: AppSession,
    pronunciation: WordPronunciation,
    config: VocabularyStudyConfig,
    ai: AiSession,
    back: () -> Unit
) {
    var direction by rememberSaveable(config.seed) { mutableStateOf(config.direction) }
    var limit by rememberSaveable(config.seed) { mutableIntStateOf(config.count) }
    var filter by rememberSaveable(config.seed) { mutableStateOf(config.filter) }
    var seed by rememberSaveable(config.seed) { mutableIntStateOf(config.seed) }
    var settings by remember { mutableStateOf(false) }
    val source = vocabularyStudyCards(session, config.folderId, filter)
    val filtered = if (config.mode == "type") source.filter {
        VocabularyWordTypes.get(it.answer) != null
    } else source
    val deck = remember(session.profileKey, config.folderId, config.mode, seed,
        limit, filter, filtered.map { it.key }) {
        filtered.shuffled(Random(seed)).take(limit)
    }
    var position by rememberSaveable(config.seed, seed, filter, limit) { mutableIntStateOf(0) }
    var completed by rememberSaveable(config.seed, seed, filter, limit) { mutableStateOf(false) }
    val reviews = remember(config.seed, seed, filter, limit) { mutableStateMapOf<Int, Boolean>() }
    val correctCount = reviews.values.count { it }
    val focus = LocalFocusManager.current

    if (settings) {
        AlertDialog(onDismissRequest = { settings = false },
            title = { Text("Çalışma ayarları") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Çeviri yönü")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("tr-en" to "TR → EN", "en-tr" to "EN → TR",
                            "mixed" to "Karışık").forEach { (value, title) ->
                            FilterChip(selected = direction == value,
                                onClick = { direction = value },
                                label = { Text(title, fontSize = 11.sp) })
                        }
                    }
                    Text("Kelime sayısı")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 20, 30, 50).forEach { count ->
                            FilterChip(selected = limit == count, onClick = { limit = count },
                                label = { Text("$count") })
                        }
                    }
                    if (config.folderId == "all") {
                        Text("Kelime havuzu")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("" to "Hepsi", "DUE" to "Tekrar",
                                "YANLIS" to "Yanlış").forEach { (value, title) ->
                                FilterChip(selected = filter == value,
                                    onClick = { filter = value },
                                    label = { Text(title, fontSize = 11.sp) })
                            }
                        }
                    }
                    OutlinedButton(onClick = {
                        seed += 1
                        settings = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Kartları yeniden karıştır")
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { settings = false }) { Text("Tamam") }
            })
    }
    val card = deck.getOrNull(position)
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(MemoryNavy, Color(0xFF08274D), Color(0xFF111B3F))))
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 17.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back) { Text("‹", fontSize = 26.sp, color = MemoryWhite) }
            Spacer(Modifier.weight(1f))
            Text("Kartlarla Çalış", color = MemoryWhite, fontSize = 19.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { settings = true }) {
                Text("⚙", fontSize = 25.sp, color = MemoryLilac)
            }
        }
        if (completed) {
            NeonPanel {
                Text("Çalışma tamamlandı", color = MemoryGold,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${deck.size} kelime · $correctCount başarılı · ${reviews.size - correctCount} tekrar gerekli",
                    color = MemoryWhite)
                Button(onClick = back, modifier = Modifier.fillMaxWidth()) {
                    Text("Kelime Hafızam'a Dön")
                }
            }
            return@Column
        }
        if (card == null) {
            NeonPanel {
                Text("Bu seçimde uygun kelime yok.", color = MemoryWhite)
                OutlinedButton(onClick = { settings = true }) {
                    Text("Seçimleri Değiştir")
                }
            }
            return@Column
        }
        val mode = if (config.mode == "mixed") when (position % 3) {
            0 -> "card"; 1 -> "write"; else -> "type"
        } else config.mode
        val studyMode = if (mode == "type" && VocabularyWordTypes.get(card.answer) == null)
            "write" else mode
        val studyDirection = if (direction == "mixed") {
            if (position % 2 == 0) "tr-en" else "en-tr"
        } else direction
        val englishAnswer = studyDirection == "tr-en"
        val prompt = if (englishAnswer) card.clue else card.answer
        val answer = if (englishAnswer) card.answer else card.clue
        var flipped by rememberSaveable(config.seed, seed, filter, position) {
            mutableStateOf(false)
        }
        var response by rememberSaveable(config.seed, seed, filter, position) {
            mutableStateOf("")
        }
        var feedback by remember(config.seed, seed, filter, position) {
            mutableStateOf<VocabularyGrade?>(null)
        }
        var wrongBefore by remember(config.seed, seed, filter, position) {
            mutableStateOf(false)
        }
        var typeFeedback by remember(config.seed, seed, filter, position) {
            mutableStateOf("")
        }
        val reviewed = reviews.containsKey(position)
        fun record(success: Boolean, difficult: Boolean = false) {
            if (reviewed) return
            val correct = success && !wrongBefore
            reviews[position] = correct
            if (studyMode != "type") {
                session.reviewAnyLearning(card,
                    if (correct && !difficult) ReviewRating.Good else ReviewRating.Again)
            }
        }
        fun next() {
            if (!reviews.containsKey(position)) record(false)
            focus.clearFocus()
            if (position == deck.lastIndex) completed = true else position++
        }
        fun check() {
            if (reviewed || response.isBlank()) return
            val grade = VocabularyAnswerChecker.grade(response, answer, englishAnswer)
            feedback = grade
            if (grade == VocabularyGrade.CORRECT) {
                record(true)
                focus.clearFocus()
            } else wrongBefore = true
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${position + 1} / ${deck.size}", color = MemoryMuted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(when (studyMode) {
                "write" -> "Yazarak Öğren"
                "type" -> "Tür Eşleştir"
                else -> "Kartlarla Çalış"
            }, color = MemoryMuted, fontSize = 13.sp)
        }
        LinearProgressIndicator(progress = { (position + 1f) / deck.size },
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(5.dp)), color = MemoryCyan,
            trackColor = Color(0xFF24456A))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = {
                direction = if (direction == "tr-en") "en-tr" else "tr-en"
                flipped = false
            }) { Text(if (englishAnswer) "🇹🇷 TR  →  EN 🇬🇧" else "🇬🇧 EN  →  TR 🇹🇷",
                color = MemoryWhite, fontSize = 12.sp) }
            Text(if (config.folderId == "all") "Tüm Kelimeler" else "Seçili Koleksiyon",
                color = MemoryMuted, fontSize = 12.sp)
        }
        Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 355.dp),
            shape = RoundedCornerShape(26.dp), color = NeonCardPaper,
            shadowElevation = 14.dp) {
            Column(Modifier.fillMaxWidth().padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(VocabularyWordTypes.label(card.answer)?.let { "$it · Word Type" }
                        ?: "Kelime Çalışması",
                        modifier = Modifier.background(
                            Color(0xFFE6DEF9), RoundedCornerShape(30.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFF4D3677), fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { session.pinLearning(card, !card.pinned) }) {
                        Text(if (card.pinned) "★" else "☆",
                            color = Color(0xFF4A6496), fontSize = 26.sp)
                    }
                }
                Text(if (studyMode == "type") card.answer
                    else if (flipped || reviewed) answer else prompt,
                    color = DarkText, fontSize = 27.sp, lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                if (studyMode == "card") {
                    VocabularyConceptVisual(card, session.profileKey, ai)
                    Text(if (flipped) "Kart çevrildi · Cevabı kontrol et"
                        else "Cevabı görmek için kartı çevir",
                        fontSize = 12.sp, color = Color(0xFF6E7391))
                    if (flipped) {
                        WordIpa(card.answer, session.preferences)
                        ListenToWord(card.answer, session.preferences, pronunciation)
                    }
                } else if (studyMode == "write") {
                    Text(if (englishAnswer) "İngilizcesini yaz" else "Türkçesini yaz",
                        color = Color(0xFF61708C), fontSize = 13.sp)
                    OutlinedTextField(value = response,
                        onValueChange = { response = it.take(120); feedback = null },
                        enabled = !reviewed, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { check() }),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cevabın") })
                    when (feedback) {
                        VocabularyGrade.CORRECT -> Text("Doğru cevap!", color = Color(0xFF137657))
                        VocabularyGrade.CLOSE -> Text("Çok yakın. Yazımını kontrol et.",
                            color = Color(0xFFB87425))
                        VocabularyGrade.WRONG -> Text("Tekrar dene veya cevabı göster.",
                            color = Color(0xFFB93E50))
                        null -> Unit
                    }
                    if (reviewed) {
                        Text("Doğru karşılık: $answer", color = DarkText)
                        ListenToWord(card.answer, session.preferences, pronunciation)
                    } else {
                        Button(onClick = { check() }, enabled = response.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Cevabı Kontrol Et")
                        }
                        TextButton(onClick = { record(false) }) {
                            Text("Cevabı Göster")
                        }
                    }
                } else {
                    Text(card.clue, color = Color(0xFF61708C))
                    for (types in VocabularyType.entries.chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (type in types) {
                                OutlinedButton(onClick = {
                                    if (type == VocabularyWordTypes.get(card.answer)) {
                                        record(true)
                                        typeFeedback = "Doğru!"
                                    } else {
                                        wrongBefore = true
                                        typeFeedback = "Bir daha dene."
                                    }
                                }, enabled = !reviewed, modifier = Modifier.weight(1f)) {
                                    Text(type.label)
                                }
                            }
                        }
                    }
                    if (typeFeedback.isNotBlank()) Text(typeFeedback, color = DarkText)
                    if (reviewed) Text("Doğru tür: " +
                        VocabularyWordTypes.label(card.answer).orEmpty(), color = DarkText)
                    else TextButton(onClick = { record(false) }) {
                        Text("Doğru Türü Göster")
                    }
                }
            }
        }
        if (studyMode == "card") {
            if (!flipped && !reviewed) {
                Button(onClick = { flipped = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MemoryLilac,
                        contentColor = MemoryNavy)) {
                    Text("↻  Kartı Çevir", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            } else if (!reviewed) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("✕\nBilmiyorum", false, true),
                        Triple("◔\nZor", false, true),
                        Triple("◉\nOrta", true, true),
                        Triple("✓\nBiliyorum", true, false)
                    ).forEach { (text, success, difficult) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFF173A61))
                            .border(1.dp, MemoryLilac.copy(alpha = .4f),
                                RoundedCornerShape(15.dp))
                            .clickable { record(success, difficult); next() }
                            .padding(vertical = 14.dp, horizontal = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text, color = MemoryWhite, fontSize = 12.sp,
                                textAlign = TextAlign.Center, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { focus.clearFocus(); position-- },
                enabled = position > 0, modifier = Modifier.weight(1f)) {
                Text("‹ Önceki", color = MemoryWhite)
            }
            Button(onClick = { next() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MemoryCyan,
                    contentColor = MemoryNavy)) {
                Text(if (position == deck.lastIndex) "Bitir" else
                    if (reviewed) "Sonraki ›" else "Atla ›")
            }
        }
        if (reviewed) Text("Tamamlanan: ${reviews.size} · Başarılı: $correctCount",
            color = MemoryMuted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun VocabularyConceptVisual(card: LearningCard, profile: String, ai: AiSession) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bytes by remember(profile, card.key) { mutableStateOf<ByteArray?>(null) }
    var busy by remember(profile, card.key) { mutableStateOf(false) }
    var error by remember(profile, card.key) { mutableStateOf<String?>(null) }
    LaunchedEffect(profile, card.key) {
        bytes = VocabularyVisualCache.load(context, profile, card.answer, card.clue)
    }
    val bitmap = remember(bytes) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    Box(Modifier.fillMaxWidth().height(190.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(Brush.verticalGradient(listOf(
            Color(0xFFE9E8F5), Color(0xFFEAF3F4)))),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "AI ile hazırlanmış kelime görseli",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (busy) CircularProgressIndicator(color = MemoryBlue)
                else {
                    Text("✧", color = Color(0xFF607A9A), fontSize = 34.sp)
                    Text("Kelimeye özel görsel", color = Color(0xFF4C6282),
                        fontSize = 12.sp)
                    Button(onClick = {
                        if (ai.provider != AiProvider.Gemini || !ai.hasKey) {
                            error = "AI görsel için Ayarlar'da Gemini anahtarını etkinleştir."
                        } else {
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val generated = ai.generateVocabularyVisual(card.answer, card.clue)
                                    VocabularyVisualCache.save(context, profile,
                                        card.answer, card.clue, generated)
                                    bytes = generated
                                } catch (problem: Exception) {
                                    error = problem.message?.take(160) ?: "Görsel oluşturulamadı."
                                } finally { busy = false }
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = MemoryBlue)) {
                        Text("AI Görsel Oluştur", color = Color.White, fontSize = 12.sp)
                    }
                    Text("Gemini görselleri ücretli olabilir; yalnızca dokununca oluşturulur.",
                        color = Color(0xFF4C6282), fontSize = 10.sp,
                        textAlign = TextAlign.Center)
                }
                if (error != null) Text(error.orEmpty(), color = Color(0xFF9B3440),
                    fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
