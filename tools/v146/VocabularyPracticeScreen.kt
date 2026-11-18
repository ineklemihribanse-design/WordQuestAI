package com.kelimebulmacasi.a1

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private val StudyDark = Color(0xFF143A30)
private val StudyGold = Color(0xFFDFBB72)
private val StudyPaper = Color(0xFFF9F7EF)
private val StudyPale = Color(0xFFDBE8DD)

@Composable
internal fun VocabularyPracticeScreen(
    session: AppSession,
    pronunciation: WordPronunciation,
    config: VocabularyStudyConfig,
    back: () -> Unit
) {
    val deck = remember(session.profileKey, config) {
        val source = vocabularyStudyCards(session, config.folderId, config.filter)
        val eligible = if (config.mode == "type") source.filter {
            VocabularyWordTypes.get(it.answer) != null
        } else source
        eligible.shuffled(Random(config.seed)).take(config.count)
    }
    var position by rememberSaveable(config.seed) { mutableIntStateOf(0) }
    var finished by rememberSaveable(config.seed) { mutableStateOf(false) }
    // Each question is recorded at most once, including on navigation back to earlier cards.
    val reviews = remember(config.seed) { mutableStateMapOf<Int, Boolean>() }
    val correct = reviews.values.count { it }
    if (finished) {
        ScreenPage("Çalışma tamamlandı", "Kelime Hafızam", back) {
            LearningPanel {
                Text("Çalışma bitti!", fontFamily = Sans, fontSize = 23.sp,
                    fontWeight = FontWeight.Bold, color = Forest)
                BodyText(deck.size.toString() + " kelime · " + correct + " başarılı · " +
                    (reviews.size - correct) + " tekrar gerekli")
                BodyText("Yazarak verilen yanıtlar ve çevrilen kartlar mevcut tekrar sistemine işlendi. " +
                    "Kelime türü soruları yalnızca çalışma sonucuna dahil edilir.")
                Button(onClick = back, modifier = Modifier.fillMaxWidth()) {
                    Text("Kelime Hafızam'a dön", fontFamily = Sans)
                }
            }
        }
        return
    }
    val card = deck.getOrNull(position)
    if (card == null) {
        ScreenPage("Çalışma", "Kelime Hafızam", back) {
            LearningPanel { BodyText("Bu çalışma için uygun kelime bulunamadı.") }
        }
        return
    }

    val choiceMode = when (config.mode) {
        "mixed" -> when (position % 3) { 0 -> "card"; 1 -> "write"; else -> "type" }
        else -> config.mode
    }
    val currentMode = if (choiceMode == "type" && VocabularyWordTypes.get(card.answer) == null)
        "write" else choiceMode
    val direction = if (config.direction == "mixed") {
        if (position % 2 == 0) "tr-en" else "en-tr"
    } else config.direction
    val englishAnswer = direction == "tr-en"
    val prompt = if (englishAnswer) card.clue else card.answer
    val target = if (englishAnswer) card.answer else card.clue
    val focusManager = LocalFocusManager.current
    var flipped by rememberSaveable(config.seed, position) { mutableStateOf(false) }
    var response by rememberSaveable(config.seed, position) { mutableStateOf("") }
    var answerGrade by remember(config.seed, position) { mutableStateOf<VocabularyGrade?>(null) }
    var typeFeedback by remember(config.seed, position) { mutableStateOf<String?>(null) }
    var hadMistake by remember(config.seed, position) { mutableStateOf(false) }
    val reviewed = reviews.containsKey(position)

    fun record(success: Boolean) {
        if (reviews.containsKey(position)) return
        val retained = success && !hadMistake
        reviews[position] = success
        // Recognizing a part of speech does NOT imply knowing a translation.
        if (currentMode != "type") {
            session.reviewAnyLearning(card, if (retained) ReviewRating.Good else ReviewRating.Again)
        }
    }
    fun advance() {
        if (!reviews.containsKey(position)) record(false)
        focusManager.clearFocus()
        if (position < deck.lastIndex) position += 1 else finished = true
    }
    fun checkWritten() {
        if (reviewed || response.isBlank()) return
        val grade = VocabularyAnswerChecker.grade(response, target, englishAnswer)
        answerGrade = grade
        if (grade == VocabularyGrade.CORRECT) {
            record(true)
            focusManager.clearFocus()
        } else hadMistake = true
    }
    ScreenPage("Kelime Çalışması", "Kelimeler ayrı bir çalışma sayfasında", back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BodyText((position + 1).toString() + " / " + deck.size)
            BodyText(when (currentMode) {
                "card" -> "Kartlarla öğren"
                "type" -> "Kelime türü"
                else -> "Yazarak cevapla"
            })
        }
        LinearProgressIndicator(progress = { (position + 1f) / deck.size },
            modifier = Modifier.fillMaxWidth(), color = StudyDark)

        when (currentMode) {
            "card" -> {
                Crossfade(targetState = flipped, label = "Kart çevir") { reveal ->
                    Surface(
                        color = if (reveal) StudyGold else StudyDark,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 265.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().clickable { flipped = !flipped }.padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (reveal) { if (englishAnswer) "İNGİLİZCE" else "TÜRKÇE" }
                                 else { if (englishAnswer) "TÜRKÇE" else "İNGİLİZCE" },
                                color = if (reveal) StudyDark else StudyGold,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(20.dp))
                            Text(if (reveal) target else prompt,
                                color = if (reveal) StudyDark else StudyPaper,
                                fontSize = 27.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(18.dp))
                            Text(if (reveal) "Kartı tekrar çevirmek için dokun" else
                                "Cevabı görmek için dokun",
                                color = if (reveal) StudyDark else StudyPale,
                                fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (flipped || reviewed) {
                    VocabularyDetails(card, session, pronunciation)
                    if (!reviewed) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { record(false); advance() },
                            modifier = Modifier.weight(1f)) { Text("Tekrar çalış", fontFamily = Sans) }
                        Button(onClick = { record(true); advance() },
                            modifier = Modifier.weight(1f)) { Text("Hatırladım", fontFamily = Sans) }
                    }
                }
            }
            "write" -> {
                LearningPanel {
                    BodyText(if (englishAnswer) "TÜRKÇE → İNGİLİZCE" else "İNGİLİZCE → TÜRKÇE")
                    Text(prompt, fontFamily = Sans, fontWeight = FontWeight.Bold,
                        color = Forest, fontSize = 24.sp)
                    VocabularyWordTypes.label(card.answer)?.let { BodyText("Kelime türü: " + it) }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = response,
                        onValueChange = { response = it.take(120); answerGrade = null },
                        enabled = !reviewed,
                        label = { Text(if (englishAnswer) "İngilizcesini yaz" else "Türkçesini yaz") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { checkWritten() }),
                        modifier = Modifier.fillMaxWidth().imePadding()
                    )
                    if (answerGrade != null || reviewed) {
                        when (answerGrade) {
                            VocabularyGrade.CORRECT -> BodyText("Doğru! Cevabın kaydedildi.")
                            VocabularyGrade.CLOSE -> BodyText("Çok yakın. Yazımını kontrol edip tekrar dene.")
                            VocabularyGrade.WRONG -> BodyText("Henüz doğru değil. Yeniden deneyebilir veya cevabı görebilirsin.")
                            else -> if (reviewed) BodyText("Bu soru değerlendirildi.")
                        }
                    }
                    if (reviewed) {
                        BodyText("Kabul edilen cevap: " + target)
                        if (englishAnswer) ListenToWord(card.answer, session.preferences, pronunciation)
                    } else {
                        Button(onClick = { checkWritten() }, enabled = response.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Cevabımı kontrol et", fontFamily = Sans)
                        }
                        OutlinedButton(onClick = { record(false); focusManager.clearFocus() },
                            modifier = Modifier.fillMaxWidth()) { Text("Cevabı göster", fontFamily = Sans) }
                    }
                }
            }
            else -> {
                val expectedType = VocabularyWordTypes.get(card.answer)
                LearningPanel {
                    BodyText("BU KELİMENİN TÜRÜ NEDİR?")
                    Text(card.answer, fontFamily = Sans, color = Forest,
                        fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    BodyText(card.clue)
                    for (pair in VocabularyType.entries.toList().chunked(2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (type in pair) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    enabled = !reviewed,
                                    onClick = {
                                        if (type == expectedType) {
                                            record(true)
                                            typeFeedback = "Doğru! " + type.label
                                        } else {
                                            hadMistake = true
                                            typeFeedback = "Bu tür değil. Yeniden dene."
                                        }
                                    }
                                ) { Text(type.label, fontFamily = Sans) }
                            }
                        }
                    }
                    typeFeedback?.let { BodyText(it) }
                    if (reviewed) BodyText("Doğru tür: " + expectedType?.label)
                    else OutlinedButton(onClick = { record(false); typeFeedback = "Doğru tür: " + expectedType?.label },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Türü göster", fontFamily = Sans)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = position > 0,
                modifier = Modifier.weight(1f),
                onClick = { focusManager.clearFocus(); position -= 1 }
            ) { Text("‹ Önceki", fontFamily = Sans) }
            Button(modifier = Modifier.weight(1f), onClick = { advance() }) {
                Text(if (position == deck.lastIndex) "Bitir" else
                    if (reviewed) "Sonraki ›" else "Atla ›", fontFamily = Sans)
            }
        }
        if (reviewed) BodyText("Tamamlanan sorular: " + reviews.size + " · Başarılı: " + correct)
    }
}

@Composable
private fun VocabularyDetails(card: LearningCard, session: AppSession, pronunciation: WordPronunciation) {
    VocabularyWordTypes.label(card.answer)?.let { BodyText("Kelime türü: " + it) }
    WordIpa(card.answer, session.preferences)
    ListenToWord(card.answer, session.preferences, pronunciation)
    if (card.context.isNotBlank()) BodyText(card.context)
}
