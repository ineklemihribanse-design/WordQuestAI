package com.kelimebulmacasi.a1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val OnlineNight = Color(0xFF112720)
private val OnlinePanel = Color(0xFF1C3A30)
private val OnlineCream = Color(0xFFF4ECD9)
private val OnlineGold = Color(0xFFE7C582)
private val OnlineSoft = Color(0xFFC5D8CD)
private val OnlineGood = Color(0xFF98D8A4)

private fun gameTitleForFolder(mode: String): String = when (mode) {
    "team" -> "Ortak Bulmaca"; "quiz" -> "Beş Şıklı Düello"; else -> "Bulmaca Düellosu"
}


@Composable
internal fun PremiumOnlineScreen(session: AppSession, back: () -> Unit,
                                 account: () -> Unit, offline: () -> Unit,
                                 openLearning: () -> Unit, openTeacher: (String) -> Unit) {
    val context = LocalContext.current
    val client = remember(session) { PremiumOnlineRoom(context.applicationContext, session) }
    val packs = remember(context) { PremiumArenaContent(context.applicationContext) }
    var selectedMode by rememberSaveable { mutableStateOf("duel") }
    var level by rememberSaveable { mutableStateOf("A1") }
    var topic by rememberSaveable { mutableStateOf(PremiumArenaRules.allTopics) }
    var size by rememberSaveable { mutableIntStateOf(30) }
    var seconds by rememberSaveable { mutableIntStateOf(30) }
    var codeInput by rememberSaveable { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var wordIndex by remember { mutableIntStateOf(0) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var wallTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val match = client.match
    val isHost = client.isHost
    val myUid = session.accounts.uid.orEmpty()

    DisposableEffect(client) { onDispose { client.close() } }
    LaunchedEffect(session.social.currentUid) { client.listenQuota(); client.listenServerTime() }
    LaunchedEffect(Unit) {
        while (true) { wallTime = System.currentTimeMillis(); client.refreshQuotaForToday(); delay(1000) }
    }
    val invite = session.social.joinTarget
    LaunchedEffect(invite?.roomCode, invite?.arenaType, client.ready) {
        if (invite != null && invite.arenaType == "premium" && client.ready && !client.busy) {
            client.join(invite.roomCode) {
                session.social.dismissInvite(invite.senderUid)
                session.social.clearJoinTarget()
            }
            // Clear after first attempt, even if access fails; a retry remains possible by code.
            session.social.clearJoinTarget()
        }
    }
    // Active players do not need to press a button when the clock expires.
    LaunchedEffect(match?.code, match?.phase, match?.turnUid, match?.turnUntil) {
        val active = match
        if (active != null && active.phase == "playing" && active.mode == "duel" &&
            active.guestUid.isNotBlank() && active.turnUntil > 0L) {
            while (true) {
                val latest = client.match ?: break
                if (latest.phase != "playing" || latest.turnUid != active.turnUid ||
                    latest.turnUntil != active.turnUntil) break
                val remaining = latest.turnUntil - client.serverNow()
                if (remaining <= 0L) { client.passIfExpired(); break }
                delay(remaining.coerceIn(150L, 1000L))
            }
        }
    }
    LaunchedEffect(match?.code, match?.unknownBy, match?.mistakesBy, match?.solvedBy) {
        client.syncFailedAttemptReveals()
    }
    LaunchedEffect(match?.code, match?.solvedBy?.size, match?.phase) {
        if (match != null && match.isCrossword && match.solvedBy.containsKey(wordIndex)) {
            wordIndex = (0 until match.size).firstOrNull { it !in match.solvedBy } ?: wordIndex
            answerText = ""
        }
        if (match != null && match.complete && match.phase == "playing" && isHost) client.finish()
    }
    BackHandler(match != null) { client.leave() }

    val keyboardIsOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboardWord = remember(match?.boardId, wordIndex) {
        if (match?.isCrossword == true) packs.puzzleById(match.boardId)?.puzzle?.words?.getOrNull(wordIndex)
        else null
    }
    Box(Modifier.fillMaxSize().background(OnlineNight)) {
    Column(Modifier.widthIn(max = 960.dp).fillMaxWidth().align(Alignment.TopCenter)
        .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (match == null) 12.dp else 9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (match != null) client.leave() else back() }) {
                Text("‹ Geri", color = OnlineGold, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text(if (match == null) "WORDQUEST · ARENA" else "ARENA · CANLI",
                color = OnlineGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        if (match == null) {
            ArenaBanner("CANLI YARIŞMALAR", "Arkadaşınla aynı arenada", "Bulmaca düellosu, ortak çözüm ve 5 şıklı bilgi yarışması")
            ArenaPanelBlock {
                Text("OYUN MODU", color = OnlineGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                listOf(Triple("duel", "Bulmaca Düellosu", "Doğru yapan devam eder"),
                    Triple("team", "Ortak Bulmaca", "İki oyuncu, tek hedef"),
                    Triple("quiz", "5 Şıklı Düello", "Aynı 12 soru, beş seçenek"))
                    .forEach { (value, label, description) ->
                        ArenaSelectCard(label, description, selectedMode == value,
                            Modifier.fillMaxWidth()) { selectedMode = value }
                    }
            }
            ArenaPanelBlock {
                Text("İNGİLİZCE SEVİYESİ", color = OnlineGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                Text("Her maç farklı içerik; her iki cihazda aynı bulmaca veya soru seti.",
                    color = OnlineSoft, fontSize = 11.sp)
            }
            ArenaPanelBlock {
                Text("KONU / İÇERİK PAKETİ", color = OnlineGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                packs.topics(level, selectedMode != "quiz").chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { value ->
                            ArenaChoice(value, value == topic, Modifier.weight(1f)) {
                                topic = value; size = packs.availableSizes(level, value).firstOrNull() ?: 20
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (selectedMode != "quiz") ArenaPanelBlock {
                Text("BULMACA BÜYÜKLÜĞÜ", color = OnlineGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    packs.availableSizes(level, topic).forEach { n ->
                        ArenaChoice("$n", size == n, Modifier.weight(1f)) { size = n }
                    }
                }
            }
            if (selectedMode == "team") ArenaPanelBlock {
                Text("ORTAK BULMACA SÜRESİ", color = OnlineGold,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Ortak çözüm süresizdir. İki arkadaş birlikte ve acele etmeden çözebilir.", color = OnlineSoft)
            } else ArenaPanelBlock {
                Text(if (selectedMode == "quiz") "SORU SÜRESİ" else "KELİME SÜRESİ", color = OnlineGold,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PremiumArenaRules.allowedTimes.forEach { n ->
                        ArenaChoice(if (n == 0) "Süresiz" else "${n} sn", seconds == n,
                            Modifier.weight(1f)) { seconds = n }
                    }
                }
            }
            ArenaPanelBlock {
                Text("ÇEVRİM İÇİ ODA", color = OnlineGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (!client.ready) {
                    Text("Oda açmak veya katılmak için Hesap bölümünde oturum açıp kullanıcı adını kaydet.",
                        color = OnlineSoft)
                    ArenaGoldButton("HESABA GİT", account)
                } else {
                    Text("Oyuncu: ${session.profileName}", color = OnlineCream)
                    Text(if (client.quotaLoaded) "Bugün kalan oda hakkın: ${10 - client.usedToday} / 10 · Türkiye saatiyle 00.00'da yenilenir"
                        else "Günlük oda kotası Firebase'den okunuyor…", color = OnlineSoft, fontSize = 12.sp)
                    ArenaGoldButton("ÇEVRİM İÇİ ODA OLUŞTUR", {
                        val boardId = if (selectedMode == "quiz") "" else
                            packs.nextPuzzle(level, size, topic, session.profileKey)?.id.orEmpty()
                        if (selectedMode == "quiz" || boardId.isNotBlank()) {
                            client.create(selectedMode, level, topic,
                                if (selectedMode == "quiz") 12 else size,
                                if (selectedMode == "team") 0 else seconds, boardId)
                        } else message = "Bu konu ve seviyede yeterli bulmaca yok. Tüm konuları seç."
                    }, enabled = client.canCreate)
                }
                Text("ARKADAŞININ ODASINA KATIL", color = OnlineGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                OutlinedTextField(codeInput, { codeInput = it.take(9).uppercase() },
                    Modifier.fillMaxWidth(), label = { Text("9 karakterli oda kodu") }, singleLine = true,
                    colors = premiumTextFieldColors())
                ArenaOutlineButton("ODA KODUYLA KATIL", {
                    client.join(codeInput)
                }, Modifier.fillMaxWidth())
                if (client.lastRoom.isNotBlank()) ArenaOutlineButton("SON ODAMI AÇ: ${client.lastRoom}", {
                    client.join(client.lastRoom)
                })
            }
            if (session.social.enabled) ArenaPanelBlock {
                Text("ARKADAŞLAR VE DAVETLER", color = OnlineGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                MaterialTheme(colorScheme = darkColorScheme(primary = OnlineGold, onSurface = OnlineCream,
                    onSurfaceVariant = OnlineSoft, surface = OnlinePanel)) {
                    FriendsPanel(session.social, onJoin = { sender, code ->
                        client.join(code) { session.social.dismissInvite(sender) }
                    }, premium = true)
                }
            }
            ArenaPanelBlock {
                Text("ÇEVRİMDIŞI OYUN", color = OnlineGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Aynı Arena tasarımında tek başına oynayabilirsin.", color = OnlineSoft)
                ArenaOutlineButton("ÇEVRİMDIŞI OYNA", offline, Modifier.fillMaxWidth())
            }
            if (message.isNotBlank()) Text(message, color = OnlineGold)
        } else {
            PremiumOnlineMatchScreen(client, packs, session, match, myUid, isHost,
                wordIndex, { wordIndex = it; answerText = "" }, answerText, { answerText = it },
                quizIndex, { quizIndex = it }, wallTime, openLearning, openTeacher)
        }
        if (client.status.isNotBlank()) Text(client.status, color = OnlineSoft, fontSize = 12.sp)
    }
    // Sticky clue stays above the Android keyboard even when the board is scrolled.
    if (keyboardIsOpen && match?.isCrossword == true && match.phase == "playing" && keyboardWord != null) {
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 960.dp).fillMaxWidth()
            .imePadding().padding(horizontal = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(OnlinePanel).border(1.dp, OnlineGold, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("İPUCU · ${keyboardWord.number} ${if (keyboardWord.direction == Direction.Across) "YATAY" else "DİKEY"}",
                color = OnlineGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(keyboardWord.entry.clue, color = Color.White, maxLines = 3,
                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            // A miniature live strip makes every typed letter and its position visible
            // even on phones where the full crossword is covered by the soft keyboard.
            val draft = englishLetters(answerText)
            val fullBoard = remember(match.boardId) { packs.puzzleById(match.boardId)?.puzzle }
            val crossed = remember(match.solvedBy, match.boardId) {
                buildMap<Cell, Char> {
                    fullBoard?.words?.forEachIndexed { index, entry ->
                        if (index in match.solvedBy) entry.cells.forEachIndexed { i, cell ->
                            put(cell, entry.entry.letters[i])
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                keyboardWord.cells.forEachIndexed { index, cell ->
                    val fixed = crossed[cell]
                    val typed = draft.getOrNull(index)
                    Box(Modifier.weight(1f).height(31.dp)
                        .background(if (fixed != null) Color(0xFF3E7555) else OnlineGold,
                            RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text((fixed ?: typed)?.toString() ?: "·",
                            color = if (fixed != null) Color.White else OnlineNight,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Text("${keyboardWord.number} numaralı ${if (keyboardWord.direction == Direction.Across) "yatay" else "dikey"} kelimeye yazıyorsun · ${draft.length}/${keyboardWord.cells.size}",
                color = OnlineGold, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }
    }
    }
}

@Composable
private fun PremiumOnlineMatchScreen(client: PremiumOnlineRoom, packs: PremiumArenaContent,
    session: AppSession, match: PremiumOnlineMatch, myUid: String, isHost: Boolean,
    wordIndex: Int, setWordIndex: (Int) -> Unit,
    answerText: String, setAnswerText: (String) -> Unit,
    quizIndex: Int, setQuizIndex: (Int) -> Unit, wallTime: Long,
    openLearning: () -> Unit, openTeacher: (String) -> Unit,
) {
    val context = LocalContext.current
    val board = remember(match.boardId) {
        if (match.isCrossword) packs.puzzleById(match.boardId) else null
    }
    val questions = remember(match.code, match.quizSeed, match.level, match.topic) {
        if (match.mode == "quiz") packs.questionsForSeed(match.level, match.topic, match.quizSeed) else emptyList()
    }
    val hostName = ArenaNickname.safeDisplay(match.hostName, "Kurucu")
    val guestName = ArenaNickname.safeDisplay(match.guestName, "Arkadaş bekleniyor")
    val myName = if (isHost) hostName else guestName
    val rivalName = if (isHost) guestName else hostName
    val myScore = match.score(myUid, questions)
    val otherScore = match.score(PremiumOnlineLimits.otherPlayer(match, myUid), questions)
    val solved = match.solvedBy.keys
    val sounds = remember(context) { PremiumFeedbackSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    val summary = remember(match, questions, board, myUid) {
        if (match.mode == "quiz") PremiumMatchReview.quiz(match, questions, myUid)
        else PremiumMatchReview.crossword(match, board?.puzzle?.words?.map { it.entry }.orEmpty(), myUid)
    }
    // Stable game ID + deterministic learning event IDs make this safe to replay on both
    // devices and on Activity recreation. Each client persists only its OWN answers.
    var savedToNotebook by remember(match.code, myUid) { mutableStateOf(false) }
    LaunchedEffect(match.phase, match.code, myUid, session.profileKey) {
        if (match.phase == "finished" && myUid.isNotBlank()) {
            session.recordPremiumMatchLearning("premium-${match.code}-$myUid", summary, gameTitleForFolder(match.mode))
            savedToNotebook = session.learningError == null
        }
    }
    LaunchedEffect(match.phase, match.code, myUid) {
        if (match.phase == "finished" && !match.cooperative && myScore > otherScore) {
            sounds.victory(session.preferences)
        }
    }
    var optionsOpen by remember(match.code) { mutableStateOf(false) }
    val gameTitle = when (match.mode) {
        "team" -> "Ortak Bulmaca"; "quiz" -> "5 Şıklı Düello"; else -> "Bulmaca Düellosu"
    }
    // Gameplay stays compact. The room code, sharing and full settings are under ⋮.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(gameTitle, color = OnlineCream, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text("${match.level} · ${if (match.phase == "lobby") "Lobi" else if (match.phase == "finished") "Sonuçlar" else "Canlı oyun"}",
                color = OnlineGold, fontSize = 12.sp)
        }
        Box {
            TextButton(onClick = { optionsOpen = true }) {
                Text("⋮", color = OnlineCream, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false },
                modifier = Modifier.background(OnlinePanel)) {
                DropdownMenuItem(text = { Text("Oda: ${match.code}", color = Color.White) },
                    onClick = { optionsOpen = false })
                DropdownMenuItem(text = { Text("${match.topic} · ${match.size} · ${if (match.seconds == 0) "Süresiz" else "${match.seconds} sn"}",
                    color = OnlineSoft) }, onClick = { optionsOpen = false })
                DropdownMenuItem(text = { Text("Oda kodunu kopyala", color = Color.White) }, onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                        ?.setPrimaryClip(ClipData.newPlainText("WordQuest Arena", match.code))
                    optionsOpen = false
                })
                DropdownMenuItem(text = { Text("Daveti paylaş", color = Color.White) }, onClick = {
                    optionsOpen = false
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "WordQuest AI Arena daveti! Oda kodu: ${match.code}")
                    }, "Arkadaşını davet et"))
                })
                DropdownMenuItem(text = { Text("Odadan ayrıl", color = OnlineGold) }, onClick = {
                    optionsOpen = false; client.leave()
                })
            }
        }
    }
    // Two scores remain visible while the large settings card is hidden.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$hostName  ★ ${match.score(match.hostUid, questions)}", color = Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("$guestName  ★ ${match.score(match.guestUid, questions)}", color = OnlineGold,
            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
    if (match.cooperative) Text("Takım: ${solved.size * 10} puan", color = OnlineGood, fontSize = 12.sp)
    if (match.phase == "lobby") ArenaPanelBlock {
        Text("ODA KODU: ${match.code}", color = OnlineGold, fontWeight = FontWeight.Bold)
        ArenaOutlineButton("KODU KOPYALA", {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                ?.setPrimaryClip(ClipData.newPlainText("WordQuest Arena", match.code))
        })
    }
    when (match.phase) {
        "lobby" -> {
            ArenaPanelBlock {
                Text(if (match.guestUid.isBlank()) "Arkadaşını bekliyoruz…" else "$guestName katıldı. Hazır!",
                    color = OnlineCream, fontSize = 17.sp)
                Text("İki oyuncuya aynı kare düzeni ve aynı kelimeler gösterilecek.", color = OnlineSoft)
                if (isHost) ArenaGoldButton("YARIŞMAYI BAŞLAT", client::start,
                    enabled = match.guestUid.isNotBlank())
                else Text("Odayı kuran oyuncu yarışmayı başlatacak.", color = OnlineSoft)
            }
            if (isHost && session.social.enabled) ArenaPanelBlock {
                Text("ARKADAŞLARINI DAVET ET", color = OnlineGold, fontWeight = FontWeight.Bold)
                MaterialTheme(colorScheme = darkColorScheme(primary = OnlineGold, onSurface = OnlineCream,
                    onSurfaceVariant = OnlineSoft, surface = OnlinePanel)) {
                    FriendsPanel(session.social, roomCode = match.code,
                        onJoin = { sender, code -> client.join(code) { session.social.dismissInvite(sender) } },
                        premium = true)
                }
            }
        }
        "playing" -> {
            if (match.mode == "quiz") {
                if (questions.size != 12) {
                    Text("Bu soru paketi cihazında eksik. Aynı uygulama sürümünü kullan.", color = OnlineCream)
                } else {
                    val own = match.answers[myUid] ?: emptyMap()
                    val index = if (quizIndex !in questions.indices) 0 else quizIndex
                    val question = questions[index]
                    // The per-question deadline survives Back / Next navigation; moving between
                    // questions no longer resets the timer. Both devices use server time offset.
                    val quizDeadlines = remember(match.code, myUid) { mutableStateMapOf<Int, Long>() }
                    val quizDeadline = if (match.seconds > 0 && !own.containsKey(index))
                        quizDeadlines.getOrPut(index) { client.serverNow() + match.seconds * 1000L }
                    else 0L
                    LaunchedEffect(match.code, index, quizDeadline, own.containsKey(index), match.phase) {
                        if (quizDeadline > 0 && !own.containsKey(index)) {
                            delay((quizDeadline - client.serverNow()).coerceAtLeast(0L))
                            if (client.match?.answers?.get(myUid)?.containsKey(index) != true)
                                client.answer(index, -1)
                        }
                    }
                    ArenaScoreStrip(own.size, questions.size, myScore, null)
                    ArenaPanelBlock {
                        Text("SORU ${index + 1} / 12" + (if (quizDeadline > 0)
                            " · ${PremiumOnlineLimits.remainingSeconds(quizDeadline, client.serverNow(wallTime))} sn" else ""),
                            color = OnlineGold, fontWeight = FontWeight.Bold)
                        Text(question.prompt, color = OnlineCream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        question.options.forEachIndexed { n, option ->
                            val answered = own[index]
                            val bg = when {
                                answered != null && n == question.correct -> Color(0xFF355E48)
                                answered == n -> Color(0xFF743B35)
                                else -> Color(0xFF294C3E)
                            }
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(bg).clickable(enabled = answered == null) {
                                        client.answer(index, n)
                                        if (n == question.correct) sounds.correct(session.preferences)
                                        else sounds.wrong(session.preferences)
                                    }
                                .padding(14.dp)) {
                                Text("${"ABCDE"[n]}. $option", color = OnlineCream,
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (own.containsKey(index)) Text(
                            if (own[index] == -1) "Süre doldu. Doğru cevap: ${question.answer}" else "Doğru cevap: ${question.answer}",
                            color = OnlineGood)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ArenaOutlineButton("Önceki", { setQuizIndex((index - 1).coerceAtLeast(0)) }, Modifier.weight(1f))
                            ArenaOutlineButton("Sonraki", { setQuizIndex((index + 1).coerceAtMost(11)) }, Modifier.weight(1f))
                        }
                    }
                }
            } else if (board == null || board.puzzle.words.size != match.size) {
                Text("Bulmaca paketi uyuşmuyor. Her iki cihazda aynı sürümü kullan.", color = OnlineCream)
            } else {
                val puzzle = board.puzzle
                val selected = wordIndex.coerceIn(0, puzzle.words.lastIndex)
                val word = puzzle.words[selected]
                val myTurn = match.cooperative || match.turnUid == myUid
                val countdown = if (match.seconds == 0) null else
                    PremiumOnlineLimits.remainingSeconds(match.turnUntil, client.serverNow(wallTime))
                ArenaScoreStrip(solved.size, match.size, myScore, countdown)
                if (!match.cooperative) Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (myTurn) "● SIRA SENDE" else "● $rivalName OYNUYOR",
                        color = if (myTurn) OnlineGood else OnlineGold,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (match.seconds > 0 && countdown == 0)
                        Text("Sıra otomatik değişiyor…", color = OnlineSoft, fontSize = 11.sp)
                }
                ArenaPuzzleGrid(puzzle, solved, selected, answerText) { setWordIndex(it) }
                ArenaPanelBlock {
                    Text("${word.number} ${if (word.direction == Direction.Across) "YATAY" else "DİKEY"} · ${word.entry.lengthLabel} HARF",
                        color = OnlineGold, fontWeight = FontWeight.Bold)
                    Text(word.entry.clue, color = OnlineCream, fontWeight = FontWeight.Medium)
                    if (selected in solved) {
                        if (match.solvedBy[selected] == "reveal")
                            Text("İki oyuncu kelimeyi çözemedi. Cevap: ${word.entry.answer}. Yanlış cevap cezası geçerli.",
                                color = OnlineGold, fontWeight = FontWeight.SemiBold)
                        else Text("Çözülen kelime: ${word.entry.answer}", color = OnlineGood)
                    } else {
                        val alreadyUnknown = myUid in (match.unknownBy[selected] ?: emptySet())
                        val rivalFailed = PremiumOnlineLimits.gaveUpOrFailed(match, selected,
                            PremiumOnlineLimits.otherPlayer(match, myUid))
                        if (alreadyUnknown) Text("Bilmiyorum kaydedildi. Rakibin yanlış yaparsa veya Bilmiyorum derse cevap açılır.",
                            color = OnlineGold, fontSize = 12.sp)
                        else if (rivalFailed) Text("Rakip bu kelimeyi çözemedi. Yanlış cevap verirsen -3 puan; Bilmiyorum dersen cezasız cevap açılır.",
                            color = OnlineGold, fontSize = 12.sp)
                        val canAnswer = myTurn && (countdown == null || countdown > 0) && !alreadyUnknown
                        fun submitAnswer() {
                            if (!canAnswer || englishLetters(answerText).isEmpty()) return
                            if (englishLetters(answerText) == word.entry.letters) {
                                client.markSolved(selected)
                                sounds.correct(session.preferences)
                            } else {
                                client.wrong(selected)
                                sounds.wrong(session.preferences)
                            }
                            setAnswerText("")
                        }
                        OutlinedTextField(answerText, { setAnswerText(it.take(word.entry.answer.length + 3)) },
                            Modifier.fillMaxWidth(), label = { Text("İngilizce cevabını yaz") }, singleLine = true,
                            enabled = canAnswer,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
                            colors = premiumTextFieldColors())
                        Text("Yazdığın harfler seçili altın karelerde de görünecek.",
                            color = OnlineSoft, fontSize = 11.sp)
                        ArenaGoldButton("CEVABI KONTROL ET", { submitAnswer() },
                            enabled = answerText.isNotBlank() && canAnswer)
                        if (match.cooperative || !alreadyUnknown) {
                            ArenaOutlineButton("BİLMİYORUM · CEZASIZ", {
                                client.dontKnow(selected)
                                setAnswerText("")
                            }, Modifier.fillMaxWidth())
                        }
                        if (match.cooperative) ArenaOutlineButton("BAŞKA KELİME SEÇ", {
                            setWordIndex((0 until match.size).firstOrNull { it !in solved && it != selected } ?: selected)
                        }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        "finished" -> {
            // Both players see the winner animation before their own match summary.
            val winnerName = when {
                match.cooperative -> "İKİ OYUNCU BİRLİKTE KAZANDI"
                myScore == otherScore -> "BERABERE"
                myScore > otherScore -> myName
                else -> rivalName
            }
            var resultReady by remember(match.code) { mutableStateOf(false) }
            LaunchedEffect(match.code) {
                delay(2300)
                resultReady = true
            }
            if (!resultReady) {
                ArenaBanner("MAÇ TAMAMLANDI", "Kazanan: $winnerName",
                    "${hostName}: ${match.score(match.hostUid, questions)} · ${guestName}: ${match.score(match.guestUid, questions)}")
                PremiumWinnerCelebration(match.code, winnerName)
                Text("Maç özeti ve kelime klasörün hazırlanıyor…", color = OnlineSoft)
            } else {
                ArenaBanner("YARIŞMA TAMAMLANDI", when {
                    match.cooperative -> "Birlikte başardınız!"
                    myScore > otherScore -> "Kazandın!"
                    myScore < otherScore -> "Güzel mücadele!"
                    else -> "Berabere!"
                }, "Kazanan: $winnerName · ${hostName}: ${match.score(match.hostUid, questions)} · ${guestName}: ${match.score(match.guestUid, questions)}")
                ArenaPanelBlock {
                    Text("SANA ÖZEL MAÇ ÖZETİ", color = OnlineGold, fontWeight = FontWeight.Bold)
                    Text("${summary.correct} doğru · ${summary.wrong} yanlış · ${summary.unknown} bilmiyorum",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("${summary.all.size} kelime oyun klasöründe · ${summary.reviewedWords} kelime tekrar edilecek",
                        color = OnlineSoft)
                    Text(if (savedToNotebook) "Kelimelerin tarihli oyun klasörüne kaydedildi."
                        else session.learningError ?: "Öğrenme kayıtları hazırlanıyor…",
                        color = if (savedToNotebook) OnlineGood else OnlineGold, fontSize = 12.sp)
                }
                ArenaPanelBlock {
                    Text("YANLIŞ VE BİLİNMEYEN KELİMELER", color = OnlineGold, fontWeight = FontWeight.Bold)
                    if (summary.review.isEmpty()) Text("Bu maçta zorlandığın kelime yok!", color = OnlineGood)
                    else summary.review.take(25).forEach { record ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${record.entry.answer} — ${record.entry.clue}", color = Color.White,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(
                                if (record.wrongAttempts > 0) "${record.wrongAttempts} yanlış deneme" else null,
                                if (record.unknown) "Bilmiyorum" else null).joinToString(" · "),
                                color = OnlineGold, fontSize = 11.sp)
                        }
                    }
                    if (summary.review.size > 25) Text("Diğer kelimeler tarihli klasöründe bulunuyor.", color = OnlineSoft)
                }
                ArenaGoldButton("OYUN KLASÖRÜNÜ AÇ", {
                    session.recordPremiumMatchLearning("premium-${match.code}-$myUid", summary,
                        gameTitleForFolder(match.mode))
                    session.openLearningFolder("premium-${match.code}-$myUid")
                    openLearning()
                })
                ArenaOutlineButton("AI ÖĞRETMENİ İLE ÇALIŞ", {
                    session.recordPremiumMatchLearning("premium-${match.code}-$myUid", summary,
                        gameTitleForFolder(match.mode))
                    openTeacher(PremiumMatchReview.prompt(summary))
                })
                var askNewGame by remember(match.code) { mutableStateOf(true) }
                if (askNewGame) ArenaPanelBlock {
                    Text("Yeni oyuna başlamak ister misin?", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Önce sonuçlarını inceleyebilir veya yeni bir oda oluşturabilirsin.",
                        color = OnlineSoft)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArenaOutlineButton("ÖZETTE KAL", { askNewGame = false }, Modifier.weight(1f))
                        Box(Modifier.weight(1f)) { ArenaGoldButton("YENİ OYUN", { client.leave() }) }
                    }
                }
            }

        }
    }
    // Exit is available in the ⋮ menu, not as a giant button under every word.
}

@Composable
private fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OnlineCream, unfocusedTextColor = OnlineCream,
    focusedLabelColor = OnlineGold, unfocusedLabelColor = OnlineSoft,
    focusedBorderColor = OnlineGold, unfocusedBorderColor = OnlineSoft,
    cursorColor = OnlineGold, focusedPlaceholderColor = Color.White,
    unfocusedPlaceholderColor = OnlineSoft,
)

/** Short, one-time gold and mint confetti. No repeated animation or audio loop. */
@Composable
private fun PremiumWinnerCelebration(roomCode: String, winnerName: String) {
    val progress = remember(roomCode, winnerName) { Animatable(0f) }
    LaunchedEffect(roomCode, winnerName) { progress.animateTo(1f, tween(durationMillis = 2100)) }
    Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp))
        .background(OnlinePanel), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val colors = listOf(OnlineGold, OnlineGood, Color.White, Color(0xFFFFD978))
            repeat(28) { index ->
                val x = size.width * (((index * 37 + 13) % 100) / 100f)
                val y = size.height * (((index * 23 + 7) % 95) / 100f + progress.value * 1.08f - 0.04f)
                if (y in 0f..size.height) drawCircle(colors[index % colors.size],
                    radius = if (index % 3 == 0) 5.dp.toPx() else 3.dp.toPx(),
                    center = Offset(x, y))
            }
        }
        Text(if (winnerName == "BERABERE") "BERABERE!" else "🏆 $winnerName",
            color = OnlineGold, fontSize = 20.sp,
            fontWeight = FontWeight.Black)
    }
}
