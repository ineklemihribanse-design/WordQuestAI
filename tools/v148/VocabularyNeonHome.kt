package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

internal val MemoryNavy = Color(0xFF173445)
internal val MemoryBlue = Color(0xFF31576A)
internal val MemoryCyan = Color(0xFF83E4D9)
internal val MemoryLilac = Color(0xFFA88DEB)
internal val MemoryGold = Color(0xFFFFDDA5)
internal val MemoryWhite = Color(0xFFF8F8FF)
internal val MemoryMuted = Color(0xFFD0E3E9)

private data class StudyTile(val title: String, val subtitle: String, val symbol: String, val mode: String, val accent: Color)
private val studyTiles = listOf(
    StudyTile("Kartlarla Çalış", "Çevir, öğren, hatırla", "▤", "card", MemoryCyan),
    StudyTile("Yazarak Öğren", "Yaz ve kontrol et", "✎", "write", MemoryLilac),
    StudyTile("Tür Eşleştir", "İsim, fiil, sıfat…", "◇", "type", Color(0xFF54E1C7)),
    StudyTile("Karma Antrenman", "Farklı soru türleri", "◎", "mixed", MemoryGold),
)

/** The selection belongs to the logged-in or local profile, not the AI teacher. */
internal object ProfileRobotSelection {
    private const val STORE = "wordquest_profile_robots"
    fun read(context: Context, profile: String): Int =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getInt("robot_$profile", 0).coerceIn(0, 3)
    fun write(context: Context, profile: String, selected: Int) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putInt("robot_$profile", selected.coerceIn(0, 3)).apply()
    }
    val names = listOf("Nova", "Orbit", "Mira", "Atlas")
    val accents = listOf(MemoryCyan, MemoryLilac, MemoryGold, Color(0xFF56D6A2))
}

@Composable
internal fun ProfileRobotPortrait(robot: Int, modifier: Modifier = Modifier) {
    val accent = ProfileRobotSelection.accents[robot.coerceIn(0, 3)]
    val rig = rememberInfiniteTransition(label = "robot-rig")
    val phase by rig.animateFloat(0f, 6.283185f,
        infiniteRepeatable(tween(5100, easing = LinearEasing)), label = "breathing")
    val blink by rig.animateFloat(0f, 1f,
        infiniteRepeatable(keyframes {
            durationMillis = 4900
            0f at 0
            0f at 4190
            1f at 4290
            0f at 4410
            0f at 4900
        }), label = "blink")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val breath = sin(phase)
        val sway = sin(phase * .7f)
        val headCenter = Offset(w * .50f, h * .40f)
        val shell = Brush.linearGradient(listOf(
            Color(0xFFFFFFFF), Color(0xFFE2EFF7), Color(0xFF8DA9BF),
            Color(0xFF4C6988)), start = Offset(w * .25f, 0f),
            end = Offset(w * .82f, h * .82f))
        val limb = Brush.linearGradient(listOf(
            Color(0xFFE9F7FF), Color(0xFF7896AF), Color(0xFF3E5875)))
        // Shadow anchors the character to its environment.
        drawOval(Color(0xFF00111D).copy(alpha = .22f),
            Offset(w * .24f, h * .94f), Size(w * .52f, h * .055f))
        drawCircle(accent.copy(alpha = .13f), w * .43f,
            Offset(w * .5f, h * .44f))
        // Neck and articulated shoulders sit behind the head.
        drawRoundRect(Color(0xFF59758D), Offset(w * .43f, h * .65f),
            Size(w * .14f, h * .15f), CornerRadius(w * .06f))
        rotate(-sway * 7f, Offset(w * .23f, h * .67f)) {
            drawCircle(Color(0xFF58738E), w * .087f,
                Offset(w * .23f, h * .67f))
            drawRoundRect(limb, Offset(w * .14f, h * .65f),
                Size(w * .16f, h * .23f), CornerRadius(w * .08f))
            drawCircle(Color(0xFFE8F6FF), w * .065f,
                Offset(w * .22f, h * .86f))
        }
        rotate(sway * 7f, Offset(w * .77f, h * .67f)) {
            drawCircle(Color(0xFF58738E), w * .087f,
                Offset(w * .77f, h * .67f))
            drawRoundRect(limb, Offset(w * .70f, h * .65f),
                Size(w * .16f, h * .23f), CornerRadius(w * .08f))
            drawCircle(Color(0xFFE8F6FF), w * .065f,
                Offset(w * .78f, h * .86f))
        }
        // Curved chest with a rim, reflection and glowing heart module.
        drawRoundRect(Brush.verticalGradient(listOf(
            Color(0xFFB9D4E5), Color(0xFF526D8B), Color(0xFF213B57))),
            Offset(w * .27f, h * (.70f + breath * .006f)),
            Size(w * .46f, h * .24f), CornerRadius(w * .15f))
        drawRoundRect(accent.copy(alpha = .45f),
            Offset(w * .39f, h * .78f), Size(w * .22f, h * .045f),
            CornerRadius(w * .025f))
        drawCircle(accent.copy(alpha = .22f), w * .07f,
            Offset(w * .5f, h * .84f))
        // Entire face is one parent transform: no floating eyes.
        rotate(sway * 2.4f, headCenter) {
            drawRoundRect(shell, Offset(w * .16f, h * .13f),
                Size(w * .68f, h * .60f), CornerRadius(w * .25f))
            drawRoundRect(Color(0xFF405F79), Offset(w * .17f, h * .27f),
                Size(w * .66f, h * .36f), CornerRadius(w * .17f))
            drawRoundRect(Brush.verticalGradient(listOf(
                Color(0xFF07192C), Color(0xFF173B57), Color(0xFF0A1B30))),
                Offset(w * .22f, h * .30f), Size(w * .56f, h * .30f),
                CornerRadius(w * .14f))
            drawRoundRect(Color.White.copy(alpha = .15f),
                Offset(w * .27f, h * .315f), Size(w * .42f, h * .028f),
                CornerRadius(w * .014f))
            // Independent eye sockets, blink shutters and gaze motion.
            val gaze = sway * w * .016f
            for (eye in listOf(.38f, .62f)) {
                drawCircle(accent.copy(alpha = .22f), w * .085f,
                    Offset(w * eye + gaze, h * .435f))
                drawOval(accent, Offset(w * (eye - .047f) + gaze,
                    h * (.385f + blink * .045f)),
                    Size(w * .094f, h * .10f * (1f - blink).coerceAtLeast(.06f)))
                drawCircle(Color.White.copy(alpha = .9f), w * .013f,
                    Offset(w * (eye - .018f) + gaze, h * .408f))
            }
            drawArc(accent, 18f, 144f, false,
                Offset(w * .435f, h * .505f),
                Size(w * .13f, h * .052f),
                style = Stroke(w * .016f, cap = StrokeCap.Round))
            drawOval(Color.White.copy(alpha = .22f),
                Offset(w * .24f, h * .18f), Size(w * .23f, h * .055f))
        }
    }
}

@Composable
internal fun VocabularyNeonHome(
    session: AppSession, pronunciation: WordPronunciation, back: () -> Unit
) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompact = screenWidthDp < 600
    val ai: AiSession = viewModel()
    LaunchedEffect(session.profileKey) {
        if (ai.boundProfile != session.profileKey) ai.bindProfile(context, session.profileKey)
    }
    var page by rememberSaveable(session.profileKey) { mutableStateOf("home") }
    var collection by rememberSaveable(session.profileKey) { mutableStateOf("all") }
    var activeFolder by rememberSaveable(session.profileKey) { mutableStateOf<String?>(null) }
    var cardDetail by rememberSaveable(session.profileKey) { mutableStateOf<String?>(null) }
    var robot by remember(session.profileKey) {
        mutableIntStateOf(ProfileRobotSelection.read(context, session.profileKey))
    }
    var robotPicker by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf<VocabularyStudyConfig?>(null) }
    val folders = session.learningFolders()
    val total = session.learningCardCount()
    val due = session.learningDueCount(System.currentTimeMillis())
    val allCards = session.searchLearning("", 0, 5000)
    val learned = allCards.count { it.remembered > 0 && it.forgotten == 0 }
    val fresh = allCards.count { it.remembered == 0 && it.forgotten == 0 }
    val struggling = allCards.count { it.wrong > 0 || it.forgotten > 0 }
    val favorites = allCards.count { it.pinned }
    val progress = if (total == 0) 0f else learned.toFloat() / total.toFloat()

    LaunchedEffect(session.requestedLearningFolder, session.profileKey) {
        session.requestedLearningFolder?.let {
            activeFolder = it
            page = "history"
            session.clearLearningFolderRequest()
        }
    }
    val selectedHistory = cardDetail?.let(session::learningCard)
    if (selectedHistory != null) {
        LearningHistoryScreen(session, selectedHistory, pronunciation) { cardDetail = null }
        return
    }
    val active = config
    if (active != null) {
        NeonVocabularyPractice(session, pronunciation, active, ai) { config = null }
        return
    }
    fun start(mode: String, filter: String = "", folder: String = "all") {
        if (vocabularyStudyCards(session, folder, filter).isNotEmpty()) {
            config = VocabularyStudyConfig(folder, filter, mode, "tr-en", 20,
                System.currentTimeMillis().toInt())
        }
    }
    if (robotPicker) {
        AlertDialog(onDismissRequest = { robotPicker = false },
            title = { Text("Profil robotun") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Yalnızca aktif profilin için kaydedilir.")
                    ProfileRobotSelection.names.forEachIndexed { index, name ->
                        OutlinedButton(onClick = {
                            robot = index
                            ProfileRobotSelection.write(context, session.profileKey, index)
                            robotPicker = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text((if (robot == index) "✓  " else "") + name)
                        }
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { robotPicker = false }) { Text("Kapat") }
            })
    }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF224E59), Color(0xFF102936), Color(0xFF173445)))
    ), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = if (isCompact) 16.dp else 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (page == "home") back()
                else { page = "home"; activeFolder = null }
            }) { Text("‹  Geri", color = MemoryWhite) }
            Spacer(Modifier.weight(1f))
            Text(session.learningProfileName, color = MemoryMuted, fontSize = 12.sp)
        }
        when (page) {
            "home" -> {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = .075f))
                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(width = if (isCompact) 92.dp else 112.dp,
                        height = if (isCompact) 112.dp else 132.dp)
                        .clickable { robotPicker = true }) {
                        ProfileRobotPortrait(robot, Modifier.fillMaxSize())
                        Text("Robotunu değiştir", modifier = Modifier.align(Alignment.BottomCenter),
                            fontSize = 9.sp, color = MemoryMuted)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Kelime Hafızam", fontSize = if (isCompact) 23.sp else 29.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Black, color = MemoryGold)
                        Text("Kelimelerini öğren, pekiştir ve kalıcı hâle getir.",
                            fontSize = 12.sp, color = MemoryMuted, lineHeight = 17.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MemoryStat("▤", "$total", "Kelimem", MemoryCyan, Modifier.weight(1f))
                    MemoryStat("↻", "$due", "Tekrar", MemoryLilac, Modifier.weight(1f))
                    MemoryStat("✓", "$learned", "Çalışılmış", MemoryGold, Modifier.weight(1f))
                }
                NeonPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Bugünkü Tekrarın", color = MemoryWhite,
                                fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("$due kelime seni bekliyor.", color = MemoryMuted, fontSize = 12.sp)
                        }
                        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { progress },
                                modifier = Modifier.fillMaxSize(), color = MemoryCyan,
                                trackColor = Color.White.copy(alpha = .16f), strokeWidth = 6.dp)
                            Text("${(progress * 100).toInt()}%", color = MemoryWhite,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = { start("card", "DUE") }, enabled = due > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryGold,
                            contentColor = MemoryNavy), modifier = Modifier.fillMaxWidth()) {
                        Text(if (due > 0) "Tekrara Başla" else "Bugünlük tekrar tamam",
                            fontWeight = FontWeight.Bold)
                    }
                }
                Text("Çalışma Modları", color = MemoryWhite, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                for (pair in studyTiles.chunked(2)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { tile ->
                            MemoryModeTile(tile, Modifier.weight(1f), total > 0) {
                                start(tile.mode)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Akıllı Koleksiyonlar", color = MemoryWhite,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { collection = "all"; page = "collection" }) {
                        Text("Tümünü Gör ›", color = MemoryLilac, fontSize = 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("Yeni", fresh, "new"),
                        Triple("Öğreniyorum", allCards.count {
                            it.remembered > 0 && it.forgotten > 0 }, "learning"),
                        Triple("Zorlandıklarım", struggling, "hard"),
                        Triple("Çalışılmış", learned, "learned"),
                        Triple("Favorilerim", favorites, "favorite")
                    ).forEach { (label, amount, code) ->
                        Column(Modifier.width(98.dp).clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = .085f))
                            .border(1.dp, MemoryCyan.copy(alpha = .24f), RoundedCornerShape(15.dp))
                            .clickable { collection = code; page = "collection" }.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(when (code) {
                                "new" -> "◈"; "learning" -> "▤"; "hard" -> "⚡"
                                "learned" -> "✓"; else -> "☆"
                            }, fontSize = 23.sp, color = MemoryCyan)
                            Text(label, fontSize = 10.sp, color = MemoryWhite,
                                maxLines = 1, textAlign = TextAlign.Center)
                            Text("$amount", fontSize = 21.sp, color = MemoryWhite,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                NeonPanel(click = { page = "history"; activeFolder = null }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▣", fontSize = 26.sp, color = MemoryGold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Geçmiş Oyunlarım", color = MemoryWhite,
                                fontWeight = FontWeight.Bold)
                            Text("${folders.size} arşiv · Oyun kayıtların korunur",
                                fontSize = 11.sp, color = MemoryMuted)
                        }
                        Text("›", fontSize = 24.sp, color = MemoryMuted)
                    }
                }
                session.learningError?.let {
                    Text(it, color = MemoryGold, fontSize = 12.sp)
                }
            }
            "collection" -> {
                Text("Kelime Koleksiyonum", color = MemoryWhite,
                    fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "Tümü", "new" to "Yeni",
                        "learning" to "Öğreniyorum", "hard" to "Zorlandıklarım",
                        "learned" to "Çalışılmış", "favorite" to "Favorilerim"
                    ).forEach { (id, label) ->
                        FilterChip(selected = collection == id,
                            onClick = { collection = id }, label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MemoryBlue, labelColor = MemoryWhite,
                                selectedContainerColor = MemoryLilac,
                                selectedLabelColor = MemoryNavy))
                    }
                }
                val cards = allCards.filter {
                    when (collection) {
                        "new" -> it.remembered == 0 && it.forgotten == 0
                        "learning" -> it.remembered > 0 && it.forgotten > 0
                        "hard" -> it.wrong > 0 || it.forgotten > 0
                        "learned" -> it.remembered > 0 && it.forgotten == 0
                        "favorite" -> it.pinned
                        else -> true
                    }
                }
                Text("${cards.size} kelime", color = MemoryMuted)
                val activeSmartFilter = when (collection) {
                    "new" -> "SMART_NEW"
                    "learning" -> "SMART_LEARNING"
                    "hard" -> "SMART_HARD"
                    "learned" -> "SMART_LEARNED"
                    "favorite" -> "SMART_FAVORITE"
                    else -> ""
                }
                if (cards.isNotEmpty()) {
                    Button(onClick = { start("card", activeSmartFilter) },
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryCyan,
                            contentColor = MemoryNavy), modifier = Modifier.fillMaxWidth()) {
                        Text("Bu Kelimelerle Çalış")
                    }
                }
                cards.take(150).forEach { card ->
                    NeonPanel(click = { cardDetail = card.key }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(card.answer, color = MemoryWhite,
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(card.clue, color = MemoryMuted, fontSize = 12.sp)
                            }
                            Text("›", color = MemoryCyan, fontSize = 21.sp)
                        }
                    }
                }
            }
            "history" -> {
                Text("Geçmiş Oyunlarım", color = MemoryWhite, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold)
                if (activeFolder != null) {
                    val folder = folders.firstOrNull { it.id == activeFolder }
                    if (folder == null) {
                        Text("Bu oyun arşivi bulunamadı.", color = MemoryMuted)
                    } else {
                        Text(folder.title, color = MemoryGold)
                        Button(onClick = { start("card", folder = folder.id) }) {
                            Text("Bu kelimelerle çalış")
                        }
                        session.learningFolderCards(folder.id).take(160).forEach { card ->
                            NeonPanel(click = { cardDetail = card.key }) {
                                Text(card.answer, color = MemoryWhite, fontWeight = FontWeight.Bold)
                                Text(card.clue, color = MemoryMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    folders.forEach { folder ->
                        NeonPanel(click = { activeFolder = folder.id }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(folder.title, color = MemoryWhite,
                                        fontWeight = FontWeight.Bold)
                                    Text("${folder.total} kelime · ${folder.wrong} yanlış",
                                        color = MemoryMuted, fontSize = 12.sp)
                                }
                                Text("›", color = MemoryGold, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    }
}

@Composable
private fun MemoryStat(symbol: String, amount: String, title: String,
    accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp))
        .background(Color(0xFF0B2B51))
        .border(1.dp, accent.copy(alpha = .55f), RoundedCornerShape(15.dp))
        .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(symbol, color = accent, fontSize = 22.sp)
        Text(amount, color = MemoryWhite, fontSize = 23.sp,
            fontWeight = FontWeight.Bold)
        Text(title, color = MemoryMuted, fontSize = 10.sp)
    }
}

@Composable
private fun MemoryModeTile(tile: StudyTile, modifier: Modifier,
    enabled: Boolean, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(15.dp))
        .background(Brush.linearGradient(
            listOf(tile.accent.copy(alpha = .16f), Color(0xFF24465A))))
        .border(1.dp, tile.accent.copy(alpha = .28f), RoundedCornerShape(15.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tile.symbol, color = tile.accent, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Text("›", fontSize = 22.sp, color = MemoryWhite)
        }
        Text(tile.title, color = MemoryWhite, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, maxLines = 1)
        Text(tile.subtitle, color = MemoryMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
internal fun NeonPanel(click: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
        .background(Brush.linearGradient(listOf(
            Color(0xFF315C6C), Color(0xFF244252))))
        .border(1.dp, Color.White.copy(alpha = .13f), RoundedCornerShape(19.dp))
    Column((if (click != null) base.clickable(onClick = click) else base)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content)
}
