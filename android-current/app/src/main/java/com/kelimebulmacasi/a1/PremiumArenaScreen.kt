package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val ArenaNight = Color(0xFF112720)
private val ArenaPanel = Color(0xFF1C3A30)
private val ArenaPanelLight = Color(0xFF2A4B3E)
private val ArenaGold = Color(0xFFE7C582)
private val ArenaCream = Color(0xFFF4ECD9)
private val ArenaTextSoft = Color(0xFFC5D8CD)
private val ArenaSuccess = Color(0xFF91DDB3)

private enum class PremiumStage { Lobby, Crossword, FiveChoices, Result }

/** The new Arena is the only public multiplayer entrance; offline is one optional tab. */
@Composable
internal fun PremiumArenaScreen(session: AppSession, back: () -> Unit, account: () -> Unit,
                                openLearning: () -> Unit, openTeacher: (String) -> Unit) {
    var offline by rememberSaveable { mutableStateOf(false) }
    if (offline) PremiumArenaOfflineScreen(session, { offline = false }, { offline = false },
        openLearning, openTeacher)
    else PremiumOnlineScreen(session, back, account, { offline = true }, openLearning, openTeacher)
}

@Composable
private fun PremiumArenaOfflineScreen(session: AppSession, back: () -> Unit, online: () -> Unit,
                                      openLearning: () -> Unit, openTeacher: (String) -> Unit) {
    val context = LocalContext.current
    val packs = remember(context) { PremiumArenaContent(context.applicationContext) }
    val sounds = remember(context) { PremiumFeedbackSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    val offlineMistakes = remember { mutableStateMapOf<Int, Int>() }
    val offlineUnknown = remember { mutableStateListOf<Int>() }
    val quizAnswers = remember { mutableStateMapOf<Int, Int>() }
    var roundId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var stage by remember { mutableStateOf(PremiumStage.Lobby) }
    var level by rememberSaveable { mutableStateOf("A1") }
    var topic by rememberSaveable { mutableStateOf(PremiumArenaRules.allTopics) }
    var size by rememberSaveable { mutableIntStateOf(30) }
    var timeSeconds by rememberSaveable { mutableIntStateOf(30) }
    var selectedMode by rememberSaveable { mutableStateOf("crossword") }
    var feedback by remember { mutableStateOf("") }
    var board by remember { mutableStateOf<PremiumArenaBoard?>(null) }
    var currentWord by remember { mutableIntStateOf(0) }
    val solved = remember { mutableStateListOf<Int>() }
    var wordAnswer by remember { mutableStateOf("") }
    var hintLetter by remember { mutableStateOf("") }
    var turns by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(timeSeconds) }
    var quiz by remember { mutableStateOf<List<PremiumQuestion>>(emptyList()) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var quizChoice by remember { mutableIntStateOf(-1) }
    var quizCorrect by remember { mutableIntStateOf(0) }
    var quizAnswered by remember { mutableIntStateOf(0) }

    fun nextUnsolved(after: Int, words: Int): Int {
        if (words == 0) return 0
        for (delta in 1..words) {
            val index = (after + delta) % words
            if (index !in solved) return index
        }
        return after
    }
    fun resetBoard() {
        solved.clear()
        currentWord = 0
        turns = 0
        score = 0
        feedback = ""
        wordAnswer = ""
        hintLetter = ""
        remaining = timeSeconds
    }
    fun begin() {
        roundId = java.util.UUID.randomUUID().toString()
        offlineMistakes.clear()
        offlineUnknown.clear()
        quizAnswers.clear()
        feedback = ""
        if (selectedMode == "crossword") {
            val chosen = packs.nextPuzzle(level, size, topic, session.profileKey)
            if (chosen == null) {
                feedback = "Bu ayarlarda yeterli kesişimli bulmaca bulunamadı. Başka konu veya kelime sayısı seç."
                return
            }
            board = chosen
            resetBoard()
            stage = PremiumStage.Crossword
        } else {
            val questions = packs.nextQuestions(level, topic, session.profileKey)
            if (questions.size < 5) {
                feedback = "Seçilen konu için yeterli farklı soru yok. Tüm konuları deneyebilirsin."
                return
            }
            quiz = questions
            quizIndex = 0
            quizChoice = -1
            quizCorrect = 0
            quizAnswered = 0
            score = 0
            stage = PremiumStage.FiveChoices
            remaining = timeSeconds
        }
    }
    fun returnToLobby() { stage = PremiumStage.Lobby; feedback = "" }
    BackHandler { if (stage == PremiumStage.Lobby) back() else returnToLobby() }

    LaunchedEffect(stage, board?.id, currentWord, quizIndex, timeSeconds) {
        remaining = timeSeconds
        if (timeSeconds == 0 || stage !in setOf(PremiumStage.Crossword, PremiumStage.FiveChoices)) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
        if (stage == PremiumStage.Crossword && board != null && solved.size < board!!.puzzle.words.size) {
            feedback = "Süre doldu; diğer kelimeye geçildi."
            wordAnswer = ""
            hintLetter = ""
            currentWord = nextUnsolved(currentWord, board!!.puzzle.words.size)
        } else if (stage == PremiumStage.FiveChoices && quizChoice == -1 && quiz.isNotEmpty()) {
            quizChoice = -2 // time out: unanswered, still allow reviewing the correct choice
            quizAnswers[quizIndex] = -1
            quizAnswered++
            feedback = "Süre doldu. Doğru yanıtı inceleyebilirsin."
        }
    }

    Column(
        Modifier.fillMaxSize().background(ArenaNight).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (stage == PremiumStage.Lobby) back() else returnToLobby() }) {
                Text("‹ Geri", color = ArenaGold, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text("WORDQUEST  /  ARENA", color = ArenaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        when (stage) {
            PremiumStage.Lobby -> {
                ArenaBanner("İNGİLİZCE ARENASI", "Her maç yeni bir keşif", "Kelime bulmacaları ve 5 şıklı bilgi alıştırmaları")
                ArenaPanelBlock {
                    Text("OYUN TÜRÜ", color = ArenaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        ArenaSelectCard("Bulmaca", "Kesişen kelimeler", selectedMode == "crossword", Modifier.weight(1f)) { selectedMode = "crossword" }
                        ArenaSelectCard("5 Şıklı", "Kelime bilgisi", selectedMode == "quiz", Modifier.weight(1f)) { selectedMode = "quiz" }
                    }
                }
                ArenaPanelBlock {
                    Text("İNGİLİZCE SEVİYESİ", color = ArenaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    PremiumArenaRules.allowedLevels.chunked(4).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            group.forEach { value ->
                                ArenaChoice(value, value == level, Modifier.weight(1f)) {
                                    level = value; topic = PremiumArenaRules.allTopics
                                    size = packs.availableSizes(value).firstOrNull() ?: 20
                                }
                            }
                            repeat(4 - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text("A1–C2: seviyeni seç. Hazır bulmaca boyutları seviyeye göre değişir.", color = ArenaTextSoft, fontSize = 12.sp)
                }
                ArenaPanelBlock {
                    Text("KONU / PAKET", color = ArenaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val topics = packs.topics(level, selectedMode == "crossword")
                    topics.chunked(2).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            line.forEach { value ->
                                ArenaChoice(value, topic == value, Modifier.weight(1f)) {
                                    topic = value; size = packs.availableSizes(level, value).firstOrNull() ?: 20
                                }
                            }
                            if (line.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (selectedMode == "crossword") ArenaPanelBlock {
                    Text("BULMACA BÜYÜKLÜĞÜ", color = ArenaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        packs.availableSizes(level, topic).forEach { value ->
                            ArenaChoice("$value", size == value, Modifier.weight(1f)) { size = value }
                        }
                    }
                }
                ArenaPanelBlock {
                    Text("KELİME / SORU SÜRESİ", color = ArenaGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        PremiumArenaRules.allowedTimes.forEach { value ->
                            ArenaChoice(if (value == 0) "Süresiz" else "${value} sn", timeSeconds == value, Modifier.weight(1f)) {
                                timeSeconds = value
                            }
                        }
                    }
                }
                ArenaGoldButton("OYUNA BAŞLA", ::begin)
                if (feedback.isNotEmpty()) Text(feedback, color = Color(0xFFFFC1A8))
                ArenaPanelBlock {
                    Text("İÇERİK KÜTÜPHANESİ", color = ArenaGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${packs.wordCount} A1–C2 kelime kaydı · ${packs.boardCount} kesişimli bulmaca düzeni", color = ArenaCream)
                    Text("Yakın zamanda görülen kelimeler ve bulmacalar bu cihazdaki hesap için hatırlanır; yeni oyunlar önce görülmemiş içeriklerden seçilir.", color = ArenaTextSoft, fontSize = 12.sp)
                }
                ArenaPanelBlock {
                    Text("ARKADAŞLARLA ÇEVRİM İÇİ ARENA", color = ArenaGold, fontWeight = FontWeight.Bold)
                    Text("Düello, ortak bulmaca ve 5 şıklı yarışma artık Arena içinde çevrimiçi oynanır.", color = ArenaTextSoft, fontSize = 12.sp)
                    ArenaOutlineButton("Çevrimiçi Arenaya dön", online)
                }
            }
            PremiumStage.Crossword -> {
                val currentBoard = board
                if (currentBoard != null) {
                    val puzzle = currentBoard.puzzle
                    val word = puzzle.words[currentWord.coerceIn(puzzle.words.indices)]
                    ArenaBanner("${currentBoard.level} · ${currentBoard.category}", "Kelime Bulmacası", "${puzzle.words.size} kelime · ${if (timeSeconds == 0) "Süresiz" else "$timeSeconds saniye"}")
                    ArenaScoreStrip(solved.size, puzzle.words.size, score, remaining.takeIf { timeSeconds > 0 })
                    ArenaPuzzleGrid(puzzle, solved.toSet(), currentWord) { index ->
                        if (index !in solved) {
                            currentWord = index; wordAnswer = ""; hintLetter = ""; feedback = ""
                        }
                    }
                    ArenaPanelBlock {
                        Text("${word.number} ${if (word.direction == Direction.Across) "YATAY" else "DİKEY"} · ${word.entry.lengthLabel} harf", color = ArenaGold, fontWeight = FontWeight.Bold)
                        Text(word.entry.clue, color = ArenaCream, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        if (hintLetter.isNotEmpty()) Text("İpucu: $hintLetter", color = ArenaSuccess)
                        OutlinedTextField(
                            value = wordAnswer,
                            onValueChange = { wordAnswer = it.take(word.entry.answer.length + 4) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("İngilizce cevabını yaz") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ArenaCream, unfocusedTextColor = ArenaCream,
                                focusedLabelColor = ArenaGold, unfocusedLabelColor = ArenaTextSoft,
                                focusedBorderColor = ArenaGold, unfocusedBorderColor = ArenaTextSoft,
                            ),
                        )
                        ArenaGoldButton("CEVABI KONTROL ET", {
                            turns++
                            if (englishLetters(wordAnswer) == word.entry.letters) {
                                if (currentWord !in solved) {
                                    solved.add(currentWord)
                                    score += if (word.entry.letters.length >= 9) 15 else 10
                                }
                                feedback = "Doğru! ${word.entry.answer} — ${word.entry.clue}"
                                sounds.correct(session.preferences)
                                if (solved.size == puzzle.words.size) stage = PremiumStage.Result
                                else currentWord = nextUnsolved(currentWord, puzzle.words.size)
                            } else {
                                score = (score - 2).coerceAtLeast(0)
                                offlineMistakes[currentWord] = (offlineMistakes[currentWord] ?: 0) + 1
                                sounds.wrong(session.preferences)
                                feedback = "Henüz doğru değil. Farklı bir kelime deneyebilirsin."
                            }
                            wordAnswer = ""; hintLetter = ""
                        }, enabled = wordAnswer.isNotBlank())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ArenaOutlineButton("Bilmiyorum", {
                                if (currentWord !in offlineUnknown) offlineUnknown.add(currentWord)
                                currentWord = nextUnsolved(currentWord, puzzle.words.size)
                                wordAnswer = ""; hintLetter = ""; feedback = ""
                            }, Modifier.weight(1f))
                            ArenaOutlineButton("Harf ipucu (−2)", {
                                val known = word.cells.mapIndexedNotNull { i, cell ->
                                    if (puzzle.words.indices.any { it in solved && cell in puzzle.words[it].cells }) i else null
                                }.toSet()
                                val index = word.entry.letters.indices.firstOrNull { it !in known } ?: 0
                                hintLetter = "${index + 1}. harf ${word.entry.letters[index]}"
                                score = (score - 2).coerceAtLeast(0)
                            }, Modifier.weight(1f))
                        }
                        if (feedback.isNotEmpty()) Text(feedback, color = ArenaSuccess, fontSize = 12.sp)
                    }
                    ArenaOutlineButton("OYUNU BİTİR · SONUÇLAR", { stage = PremiumStage.Result })
                    ArenaOutlineButton("Yeni rastgele bulmaca", ::begin)
                }
            }
            PremiumStage.FiveChoices -> {
                if (quiz.isNotEmpty()) {
                    val current = quiz[quizIndex.coerceIn(quiz.indices)]
                    ArenaBanner("${level} · ${topic}", "5 Şıklı Kelime Düellosu", "12 soruluk alıştırma · A / B / C / D / E")
                    ArenaScoreStrip(quizAnswered, quiz.size, score, remaining.takeIf { timeSeconds > 0 })
                    ArenaPanelBlock {
                        Text("SORU ${quizIndex + 1} / ${quiz.size}", color = ArenaGold, fontWeight = FontWeight.Bold)
                        Text(current.prompt, color = ArenaCream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        current.options.forEachIndexed { index, option ->
                            val resolved = quizChoice != -1
                            val correct = index == current.correct
                            val container = when {
                                resolved && correct -> Color(0xFF3C7055)
                                resolved && quizChoice == index -> Color(0xFF80423D)
                                else -> ArenaPanelLight
                            }
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(container)
                                    .border(1.dp, if (resolved && correct) ArenaSuccess else Color(0xFF496C5A), RoundedCornerShape(12.dp))
                                    .clickable(enabled = !resolved) {
                                        quizChoice = index
                                        quizAnswers[quizIndex] = index
                                        quizAnswered++
                                        if (correct) {
                                            quizCorrect++; score += 10
                                            sounds.correct(session.preferences)
                                        } else {
                                            score = (score - 2).coerceAtLeast(0)
                                            sounds.wrong(session.preferences)
                                        }
                                    }.padding(14.dp),
                            ) {
                                Text("${"ABCDE"[index]}.  $option", color = ArenaCream, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (quizChoice != -1) {
                            Text("Doğru cevap: ${current.answer}", color = ArenaSuccess)
                            if (feedback.isNotEmpty()) Text(feedback, color = ArenaTextSoft, fontSize = 12.sp)
                            ArenaGoldButton(if (quizIndex == quiz.lastIndex) "SONUÇLARI GÖR" else "SONRAKİ SORU", {
                                if (quizIndex == quiz.lastIndex) stage = PremiumStage.Result
                                else { quizIndex++; quizChoice = -1; feedback = "" }
                            })
                        }
                    }
                }
            }
            PremiumStage.Result -> {
                val review = if (selectedMode == "crossword") {
                    board?.puzzle?.words?.mapIndexed { index, word ->
                        PremiumReviewWord(index, word.entry, offlineMistakes[index] ?: 0,
                            index in offlineUnknown)
                    }.orEmpty()
                } else quiz.mapIndexedNotNull { index, question ->
                    val answer = quizAnswers[index] ?: return@mapIndexedNotNull null
                    PremiumReviewWord(index, Entry(question.answer, question.prompt.substringBefore(" — "),
                        "Arena bilgi yarışması"),
                        if (answer >= 0 && answer != question.correct) 1 else 0, answer < 0)
                }
                val summary = PremiumMatchSummary(
                    correct = if (selectedMode == "crossword") solved.size else quizCorrect,
                    wrong = review.sumOf { it.wrongAttempts },
                    unknown = review.count { it.unknown },
                    review = review.filter { it.needsReview },
                    all = review,
                )
                var memorySaved by remember(roundId) { mutableStateOf(false) }
                LaunchedEffect(roundId, session.profileKey) {
                    session.recordPremiumMatchLearning("premium-solo-$roundId", summary,
                        if (selectedMode == "crossword") "Tek Kişilik Bulmaca" else "Beş Şıklı Yarışma")
                    memorySaved = session.learningError == null
                }
                ArenaBanner("OYUN TAMAMLANDI", "Maç özetin hazır!", "Yanlış ve bilinmeyen kelimeler tekrar listene eklenecek.")
                ArenaPanelBlock {
                    Text("TOPLAM PUAN", color = ArenaGold, fontWeight = FontWeight.Bold)
                    Text("$score", color = ArenaCream, fontSize = 50.sp, fontWeight = FontWeight.Black)
                    Text("${summary.correct} doğru · ${summary.wrong} yanlış · ${summary.unknown} bilmiyorum",
                        color = Color.White, fontSize = 16.sp)
                    Text(if (memorySaved) "Kelime Hafızası'na kaydedildi."
                         else session.learningError ?: "Kelime hafızasına kaydediliyor…", color = ArenaTextSoft)
                }
                ArenaPanelBlock {
                    Text("TEKRAR EDİLECEK KELİMELER", color = ArenaGold, fontWeight = FontWeight.Bold)
                    if (summary.review.isEmpty()) Text("Bu oyunda yanlış veya bilinmeyen kelime yok.", color = ArenaSuccess)
                    summary.review.take(25).forEach { record ->
                        Text("${record.entry.answer} — ${record.entry.clue}", color = ArenaCream,
                            fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(
                            if (record.wrongAttempts > 0) "${record.wrongAttempts} yanlış" else null,
                            if (record.unknown) "Bilmiyorum" else null).joinToString(" · "),
                            color = ArenaGold, fontSize = 12.sp)
                    }
                }
                ArenaGoldButton("KELİME HAFIZAMA GİT", {
                    session.recordPremiumMatchLearning("premium-solo-$roundId", summary,
                        if (selectedMode == "crossword") "Tek Kişilik Bulmaca" else "Beş Şıklı Yarışma")
                    session.openLearningFolder("premium-solo-$roundId")
                    openLearning()
                })
                ArenaOutlineButton("AI ÖĞRETMENİ İLE ÇALIŞ", {
                    session.recordPremiumMatchLearning("premium-solo-$roundId", summary,
                        if (selectedMode == "crossword") "Tek Kişilik Bulmaca" else "Beş Şıklı Yarışma")
                    openTeacher(PremiumMatchReview.prompt(summary))
                })
                ArenaOutlineButton("YENİ RASTGELE OYUN", ::begin)
                ArenaOutlineButton("Arena menüsüne dön", ::returnToLobby)
            }
        }
    }
}

@Composable
internal fun ArenaBanner(tag: String, heading: String, detail: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ArenaPanelLight)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(tag.uppercase(), color = ArenaGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(heading, color = ArenaCream, fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 31.sp)
        Text(detail, color = ArenaTextSoft, fontSize = 13.sp)
    }
}

@Composable
internal fun ArenaPanelBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(ArenaPanel)
            .border(1.dp, Color(0xFF385A4A), RoundedCornerShape(19.dp))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content,
    )
}

@Composable
internal fun ArenaSelectCard(title: String, description: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) ArenaPanelLight else ArenaNight)
            .border(if (selected) 2.dp else 1.dp, if (selected) ArenaGold else Color(0xFF436052), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = if (selected) ArenaGold else ArenaCream, fontSize = 16.sp)
        Text(description, color = ArenaTextSoft, fontSize = 11.sp)
    }
}

@Composable
internal fun ArenaChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(11.dp)).background(if (selected) ArenaGold else ArenaPanelLight)
            .border(1.dp, if (selected) ArenaGold else Color(0xFF51725E), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) ArenaNight else ArenaCream, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ArenaGoldButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ArenaGold, contentColor = ArenaNight,
            disabledContainerColor = Color(0xFF5E6855)),
    ) { Text(label, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 5.dp)) }
}

@Composable
internal fun ArenaOutlineButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArenaGold),
    ) { Text(label, fontSize = 12.sp, maxLines = 2, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
}

@Composable
internal fun ArenaScoreStrip(done: Int, total: Int, score: Int, remainingSeconds: Int?) {
    ArenaPanelBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("İlerleme $done/$total", color = ArenaCream, fontWeight = FontWeight.Bold)
            Text("★ $score puan" + (remainingSeconds?.let { " · ${it} sn" } ?: ""), color = ArenaGold, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { if (total <= 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp)),
            color = ArenaGold, trackColor = ArenaPanelLight,
        )
    }
}

@Composable
internal fun ArenaPuzzleGrid(puzzle: Puzzle, solved: Set<Int>, selected: Int,
    typedWord: String = "", onSelect: (Int) -> Unit) {
    val chosen = puzzle.words.getOrNull(selected)
    val visibleLetters = remember(puzzle, solved) {
        buildMap<Cell, Char> {
            puzzle.words.forEachIndexed { i, word -> if (i in solved) {
                word.cells.forEachIndexed { position, cell -> put(cell, word.entry.letters[position]) }
            } }
        }
    }
    val draftLetters = remember(typedWord) { englishLetters(typedWord) }
    val selectedDraft = remember(puzzle, selected, solved, draftLetters) {
        buildMap<Cell, Char> {
            if (selected !in solved) chosen?.cells?.forEachIndexed { i, cell ->
                if (i < draftLetters.length && cell !in visibleLetters) put(cell, draftLetters[i])
            }
        }
    }
    val boardScroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Scale the grid to the device: smaller cells on phones, larger cells on tablets.
        // Oversized grids can still scroll horizontally without cutting off entries.
        val fitted = ((maxWidth.value - 28f) / puzzle.columns - 1f).dp
        val cellSize = fitted.coerceIn(19.dp, 34.dp)
        val fontSize = if (cellSize < 25.dp) 11.sp else 14.sp
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(ArenaPanel).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Kareye dokun · kelime seç · gerektiğinde yatay kaydır",
                color = ArenaTextSoft, fontSize = 11.sp)
            Column(Modifier.fillMaxWidth().horizontalScroll(boardScroll),
                verticalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(puzzle.rows) { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        repeat(puzzle.columns) { c ->
                            val cell = Cell(r, c)
                            val indexes = puzzle.wordsAt[cell]
                            val active = indexes != null
                            val selectedCell = active && chosen?.cells?.contains(cell) == true
                            val solvedCell = cell in visibleLetters
                            val background = when {
                                !active -> ArenaNight
                                solvedCell -> Color(0xFF437858)
                                selectedCell -> ArenaGold
                                else -> ArenaCream
                            }
                            Box(
                                Modifier.size(cellSize).background(background, RoundedCornerShape(2.dp))
                                    .clickable(enabled = active) {
                                        val next = indexes?.firstOrNull { it !in solved } ?: return@clickable
                                        onSelect(next)
                                    }, contentAlignment = Alignment.Center,
                            ) {
                                if (active) {
                                    val lettering = if (solvedCell) Color.White else ArenaNight
                                    puzzle.numbers[cell]?.let { number ->
                                        Text("$number", modifier = Modifier.align(Alignment.TopStart)
                                            .padding(start = 2.dp), color = lettering,
                                            fontSize = 7.sp, lineHeight = 8.sp)
                                    }
                                    Text((visibleLetters[cell] ?: selectedDraft[cell])?.toString() ?: "", color = lettering,
                                        fontWeight = FontWeight.Bold, fontSize = fontSize)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
